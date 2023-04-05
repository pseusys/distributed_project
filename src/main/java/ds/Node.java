package ds;

import ds.objects.RoutingMessage;
import ds.objects.RoutingTable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.Connection;


public class Node {
    private int id;
    private int[] neighbors;

    private RoutingTable routingTable;

    private final Channel channel; //in

    private int round_counter = 0;
    private int neighbor_counter = 0; //for each round
    private int real_neighbors = 0;

    public Node(int id, int nodesNumber, int[] neighbors) {
        this.id = id;
        this.neighbors = neighbors;
        this.routingTable = new RoutingTable(id, neighbors.length);

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            this.channel = connection.createChannel();

            // we create a queue for every neighbor with name this.id + neighbor.id
            for (int i = 0; i < neighbors.length; i++) if (neighbors[i] != -1) {
                this.channel.queueDeclare(id + "-" + i, false, false, false, null);
                this.channel.queueDeclare(i + "-" + id, false, false, false, null);
                real_neighbors++;
            }

            processMsg();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        //send a msg to the neighbors
        for (int i = 0; i < neighbors.length; i++)
            if (neighbors[i] != -1) sendMsg(new RoutingMessage(routingTable, id), i);
    }

    public boolean initialized() {
        return round_counter == neighbors.length;
    }

    public int[] distances() {
        int[] dists = new int[neighbors.length];
        for (int i = 0; i < neighbors.length; i++) dists[i] = routingTable.path(i).distance;
        return dists;
    }

    public void map(Map<String, List<String>> map) {

    }

    private void sendMsg(RoutingMessage msg, int neighbor) {
        try {
            byte[] byteMsg = msg.dump();
            channel.basicPublish("", id + "-" + neighbor, MessageProperties.PERSISTENT_TEXT_PLAIN, byteMsg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processMsg() {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            RoutingMessage message;
            try {
                message = RoutingMessage.parse(delivery.getBody());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            //receiveMsg
            //update routing table
            neighbor_counter++;
            routingTable.update(message.table, message.current);

            if (neighbor_counter == real_neighbors) {
                neighbor_counter = 0;
                round_counter++;
                if (round_counter < neighbors.length)
                    for (int i = 0; i < neighbors.length; i++)
                        if (neighbors[i] != -1) sendMsg(new RoutingMessage(routingTable, id), i);
            }
        };
        boolean autoAck = true; // acknowledgment is covered below
        try {
            for (int i = 0; i < neighbors.length; i++)
                if (neighbors[i] != -1) channel.basicConsume(i + "-" + id, autoAck, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            //todo: change?
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Node " + id + " connected: " + routingTable;
    }
}
