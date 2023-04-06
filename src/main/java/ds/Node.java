package ds;

import ds.misc.TripleConsumer;
import ds.objects.DataMessage;
import ds.objects.RoutingMessage;
import ds.objects.RoutingTable;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.Connection;


public class Node {
    public int id;
    private int[] connections;
    private int[] neighbors;

    private RoutingTable routingTable;

    private final Channel channel; //in

    private int round_counter = 0;
    private int neighbor_counter = 0; //for each round
    private int real_neighbors = 0;

    private String[] callbackID;

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

            callbackID = new String[nodesNumber];
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

    public void map(int[] connections, TripleConsumer<String, Integer, Node> callback) {
        if (!initialized()) throw new RuntimeException("Map should be invoked only after node initialization!");
        this.connections = connections;

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            DataMessage message;
            try {
                message = DataMessage.parse(delivery.getBody());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
    
            if (message.receiver == id) {
                if (callback != null) callback.apply(message.message, message.sender, this);
                else System.out.println("Physical node " + id + " received a message, but it doesn't have a callback for it!");
            } else {
                System.out.println("Physical node " + id + " forwards a message (from " + message.sender + ", to: " + message.receiver + ")!");
                forwardText(message.sender, message.receiver, message.message);
            }
        };
        setupCallbacks(deliverCallback);
    }

    public void sendText(int recipient, String message) {
        if (!initialized()) throw new RuntimeException("Send text should be invoked only after node initialization!");
        if (connections[recipient] != -1) forwardText(id, recipient, message);
        else System.out.println("Node " + id + " can't pass a message to node " + recipient + ": they aren't connected!");
    }

    private void forwardText(int sender, int recipient, String message) {
        try {
            int gate = routingTable.path(recipient).gate;
            byte[] byteMsg = (new DataMessage(message, sender, recipient)).dump();
            channel.basicPublish("", id + "-" + gate, MessageProperties.PERSISTENT_TEXT_PLAIN, byteMsg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        setupCallbacks(deliverCallback);
    }

    private void setupCallbacks(DeliverCallback callback) {
        try {
            for (int i = 0; i < neighbors.length; i++) {
                if (callbackID[i] != null) channel.basicCancel(callbackID[i]);
                if (neighbors[i] != -1) callbackID[i] = channel.basicConsume(i + "-" + id, true, callback, consumerTag -> {});
                else callbackID[i] = null;
            }
        } catch (IOException e) {
            //TODO: change?
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Node " + id + " connected: " + routingTable;
    }
}
