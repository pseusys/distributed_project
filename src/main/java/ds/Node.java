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


// TODO: extend a virtual node interface and physical node interface
public class Node {
    protected int physID, virtID;
    protected int[] connections;
    protected int[] neighbors;

    protected RoutingTable routingTable;

    protected final Channel channel; //in

    protected int round_counter = 0;
    protected int neighbor_counter = 0; //for each round
    protected int real_neighbors = 0;

    TripleConsumer<String, Integer, Node> virtualCallback;
    protected String[] callbackIDs;

    DeliverCallback messageCallback;

    public Node(int id, int nodesNumber, int[] neighbors) {
        this.physID = id;
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

            callbackIDs = new String[nodesNumber];
            setupCallbacks(new NodeMessageCallback(this));
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPhysicalID() {
        return physID;
    }

    public int getVirtualID() {
        if (!initialized()) throw new RuntimeException("Virtual ID should be invoked only after node initialization!");
        return virtID;
    }

    public void start() {
        //send a msg to the neighbors
        for (int i = 0; i < neighbors.length; i++)
            if (neighbors[i] != -1) sendMsg(new RoutingMessage(routingTable, physID), i);
    }

    public boolean initialized() {
        return round_counter == neighbors.length;
    }

    public int[] distances() {
        int[] dists = new int[neighbors.length];
        for (int i = 0; i < neighbors.length; i++) dists[i] = routingTable.path(i).distance;
        return dists;
    }

    public void map(int id, int[] connections, TripleConsumer<String, Integer, Node> callback) {
        if (!initialized()) throw new RuntimeException("Map should be invoked only after node initialization!");
        this.virtID = id;
        this.connections = connections;
        this.virtualCallback = callback;
    }

    public void sendText(int recipient, String message) {
        if (!initialized()) throw new RuntimeException("Send text should be invoked only after node initialization!");
        if (connections[recipient] != -1) forwardText(physID, recipient, message);
        else System.out.println("Virtual node " + virtID + " can't pass a message to node " + recipient + ": they aren't connected!");
    }

    protected void forwardText(int sender, int recipient, String message) {
        try {
            int gate = routingTable.path(recipient).gate;
            byte[] byteMsg = (new DataMessage(message, sender, recipient)).dump();
            channel.basicPublish("", physID + "-" + gate, MessageProperties.PERSISTENT_TEXT_PLAIN, byteMsg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void sendMsg(RoutingMessage msg, int neighbor) {
        try {
            byte[] byteMsg = msg.dump();
            channel.basicPublish("", physID + "-" + neighbor, MessageProperties.PERSISTENT_TEXT_PLAIN, byteMsg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void setupCallbacks(DeliverCallback callback) {
        try {
            for (int i = 0; i < neighbors.length; i++) {
                if (callbackIDs[i] != null) channel.basicCancel(callbackIDs[i]);
                if (neighbors[i] != -1) callbackIDs[i] = channel.basicConsume(i + "-" + physID, true, callback, consumerTag -> {});
                else callbackIDs[i] = null;
            }
        } catch (IOException e) {
            //TODO: change?
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Node " + physID + " connected: " + routingTable;
    }
}
