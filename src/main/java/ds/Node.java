package ds;

import com.rabbitmq.client.*;

import ds.objects.RoutingMessage;
import ds.objects.RoutingTable;

import java.io.*;
import java.util.concurrent.TimeoutException;

public class Node {
    public int id;
    int[] neighbors;
    int nodesNumber;    //should be 5

    RoutingTable routingTable;

    private final Channel channel; //in

    private final Connection connection; //to open and close separately

    private int round_counter = 1;
    private int neighbor_counter = 0;   //for each round
    private int real_neighbors = 0;

    public Node(int id, int nodesNumber, int[] neighbors) {
        this.id = id;
        this.nodesNumber = nodesNumber;
        this.neighbors = neighbors;
        this.routingTable = new RoutingTable(id, neighbors.length);
        System.out.println("Node '" + id + "' table: " + routingTable);

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();

            // we create a queue for every neighbor with name this.id + neighbor.id
            for (int i = 0; i < neighbors.length; i++) {
                int neighbor = neighbors[i];
                if (neighbor != -1) {
                    this.channel.queueDeclare(id + "-" + i, false, false, false, null);
                    this.channel.queueDeclare(i + "-" + id, false, false, false, null);
                    real_neighbors++;
                }
            }

            processMsg();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }

    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public void start() {
        //send a msg to the neighbors
        for (int i = 0; i < neighbors.length; i++)
            if (neighbors[i] != -1)
                sendMsg(new RoutingMessage(routingTable, id), i);
    }

    public void sendMsg(RoutingMessage msg, int neighbor) {

        byte[] byteMsg = msg.dump();

        try {
            channel.basicPublish("", id + "-" + neighbor,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    byteMsg);
            //System.out.println(this.id + " sent " + msg.getMsg());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void processMsg() {

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            byte[] byteMsg = delivery.getBody();
            RoutingMessage message;
            try {
                message = RoutingMessage.parse(byteMsg);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            //receiveMsg
            //update routing table
            neighbor_counter++;
            routingTable.update(message.table, message.current);
            if (neighbor_counter == real_neighbors) {
                System.out.println("\t" + id + " : my round_counter " + round_counter);
                neighbor_counter = 0;
                round_counter++;
                if (round_counter <= nodesNumber) {
                    for (int i = 0; i < neighbors.length; i++) {
                        if (neighbors[i] != -1)
                            sendMsg(new RoutingMessage(routingTable, id), i);
                    }
                }
            }
        };
        boolean autoAck = true; // acknowledgment is covered below
        try {
            for (int i = 0; i < neighbors.length; i++) {
                int neighbor = neighbors[i];
                if (neighbor != -1) {
                    channel.basicConsume(i + "-" + id, autoAck, deliverCallback, consumerTag -> {
                    });
                }
            }
        } catch (IOException e) {
            //todo: change?
            throw new RuntimeException(e);
        }
    }
}
