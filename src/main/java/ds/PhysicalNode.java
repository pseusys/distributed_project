package ds;

import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.nio.NioParams;

import java.io.*;
import java.util.concurrent.TimeoutException;

public class PhysicalNode extends Node {
    int nodesNumber;    //should be 5

    int[][] routingTable;

    private final Channel channel; //in

    private final Connection connection; //to open and close separately

    private int round_counter = 1;
    private int neighbor_counter = 0;   //for each round
    private int real_neighbors = 0;

    public PhysicalNode(int id, int nodesNumber, int[] neighbors) {
        this.id = id;
        this.nodesNumber = nodesNumber;
        this.neighbors = neighbors;
        this.routingTable = new int[nodesNumber][3];

        for (int i = 0; i < nodesNumber; i++) {
            if (i == id) routingTable[i] = new int[] {id, id, 0};
            else routingTable[i] = new int[] {-1, i, 3000};
        }

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

    public int[][] getRoutingTable() {
        return routingTable;
    }

    public void start() {
        //send a msg to the neighbors
        for (int i = 0; i < neighbors.length; i++)
            if (neighbors[i] != -1)
                sendMsg(new MessageObj(this.routingTable, this.id), i);
    }

    public void sendMsg(MessageObj msg, int neighbor) {

        byte[] byteMsg = getByteArray(msg);

        try {
            channel.basicPublish("", id + "-" + neighbor,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    byteMsg);
            //System.out.println(this.id + " sent " + msg.getMsg());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getByteArray(MessageObj msg) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    public void processMsg() {

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            byte[] byteMsg = delivery.getBody();
            MessageObj message;
            try {
                message = (MessageObj) parseMsg(byteMsg);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            //receiveMsg
            //update routing table
            neighbor_counter++;
            updateRoutingTable(message);
            if (neighbor_counter == real_neighbors) {
                System.out.println("\t" + id + " : my round_counter " + round_counter);
                neighbor_counter = 0;
                round_counter++;
                if (round_counter <= nodesNumber) {
                    for (int i = 0; i < neighbors.length; i++) {
                        if (neighbors[i] != -1)
                            sendMsg(new MessageObj(this.routingTable, this.id), i);
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

    private Object parseMsg(byte[] byteMsg) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(byteMsg);
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(bais);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ois.readObject();
    }

    private void updateRoutingTable(MessageObj message) {
        for (int i = 0; i < message.routingTable.length; i++) {
            if (message.routingTable[i][2] < this.routingTable[message.routingTable[i][1]][2]) {
                //update the shortest path
                this.routingTable[message.routingTable[i][1]][2] = message.routingTable[i][2] + 1;
                //update the parent node
                this.routingTable[message.routingTable[i][1]][0] = message.nodeId;
            }
        }
    }


}
