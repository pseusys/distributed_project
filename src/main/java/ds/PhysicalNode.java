package ds;

import com.rabbitmq.client.*;

import java.io.*;
import java.util.concurrent.TimeoutException;

public class PhysicalNode extends Node {
    int nodesNumber;    //should be 5

    int[] neighbors;

    int[][] routingTable;

    private boolean started = false;

    private final Channel channel; //in

    private final Connection connection; //to open and close separately

    public PhysicalNode(int id, int nodesNumber, int[] neighbors) {
        this.id = id;
        this.nodesNumber = nodesNumber;
        this.neighbors = neighbors;
        this.routingTable = new int[nodesNumber][3];

        for (int i = 0; i < nodesNumber; i++) {
            if (i == id) routingTable[i] = new int[] {id, id, 0};
            else routingTable[i] = new int[] {-1, i, Integer.MAX_VALUE};
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            this.channel.queueDeclare(String.valueOf(id), false, false, false, null);

            processMsg();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        //send a msg to the neighbors
        for (int i = 0; i < neighbors.length; i++)
            if (neighbors[i] != -1)
                sendMsg(new MessageObj(this.routingTable, this.id), this.neighbors[i]);
    }

    public void sendMsg(MessageObj msg, int neighbor) {

        byte[] byteMsg = getByteArray(msg);

        try {
            channel.basicPublish("", String.valueOf(neighbor),
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
            updateRoutingTable(message);

        };
        boolean autoAck = true; // acknowledgment is covered below
        try {
            channel.basicConsume(String.valueOf(id), autoAck, deliverCallback, consumerTag -> {
            });
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
            if (message.routingTable[i][2] <= this.routingTable[message.routingTable[i][1]][2]) {
                //update the shortest path
                this.routingTable[message.routingTable[i][1]][2] = message.routingTable[i][2];
                //update the parent node
                this.routingTable[message.routingTable[i][1]][0] = message.nodeId;
            }
        }
    }


}
