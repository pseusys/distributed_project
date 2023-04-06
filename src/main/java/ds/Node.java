package ds;

import ds.base.BaseMessage;
import ds.base.PhysicalNode;
import ds.base.VirtualNode;
import ds.misc.TripleConsumer;
import ds.objects.DataMessage;
import ds.objects.RoutingMessage;
import ds.objects.RoutingTable;
import ds.objects.ServiceMessage;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.Connection;


public class Node implements PhysicalNode, VirtualNode {
    protected int physID, virtID;
    protected RoutingTable routingTable;

    protected int[] connections;
    protected int[] neighbors;
    protected String[] callbackIDs;

    protected final Connection connection;
    protected final Channel channel;

    protected int round_counter = 0;
    protected int neighbor_counter = 0;
    protected int real_neighbors = 0;

    DeliverCallback messageCallback;
    TripleConsumer<String, Integer, Node> virtualCallback;
    Consumer<Node> initializationCallback;

    public Node(int id, int nodesNumber, int[] neighbors, int[] connections, TripleConsumer<String, Integer, Node> virtualCallback, Consumer<Node> initializationCallback) {
        this.physID = this.virtID = id;
        this.neighbors = neighbors;
        this.connections = connections;
        this.routingTable = new RoutingTable(id, neighbors.length);
        this.initializationCallback = initializationCallback;
        this.virtualCallback = virtualCallback;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            real_neighbors = createQueues();
        } catch (IOException | TimeoutException e) {
            // TODO: do something else?
            throw new RuntimeException(e);
        }

        callbackIDs = new String[nodesNumber];
        setupCallbacks(new NodeMessageCallback(this));
    }

    @Override
    public String toString() {
        return "Node " + physID + " connected: " + routingTable;
    }


    // PHYSICAL UTILITIES:

    private void setupCallbacks(DeliverCallback callback) {
        try {
            for (int i = 0; i < neighbors.length; i++) {
                if (callbackIDs[i] != null) channel.basicCancel(callbackIDs[i]);
                if (neighbors[i] != -1 && callback != null) callbackIDs[i] = channel.basicConsume(i + "-" + physID, true, callback, consumerTag -> {});
                else callbackIDs[i] = null;
            }
        } catch (IOException e) {
            //TODO: do something else?
            throw new RuntimeException(e);
        }
    }

    public int createQueues() {
        try {
            int neighbors_count = 0;
            for (int i = 0; i < neighbors.length; i++) if (neighbors[i] != -1) {
                this.channel.queueDeclare(physID + "-" + i, false, false, false, null);
                this.channel.queueDeclare(i + "-" + physID, false, false, false, null);
                neighbors_count++;
            }
            return neighbors_count;
        } catch (IOException e) {
            // TODO: do something else? Maybe tighten the clause?
            throw new RuntimeException(e);
        }
    }

    public void deleteQueues() {
        try {
            for (int i = 0; i < neighbors.length; i++) if (neighbors[i] != -1) {
                try {
                    this.channel.queueDelete(physID + "-" + i);
                } catch (AlreadyClosedException e) {
                    // Do nothing, queue already deleted.
                }
            }
        } catch (IOException e) {
            // TODO: do something else? Maybe tighten the clause?
            throw new RuntimeException(e);
        }
    }


    // PHYSICAL NODE:

    @Override
    public int getPhysicalID() {
        return physID;
    }

    @Override
    public String physicalRepresentation() {
        return "Physical node " + physID;
    }

    // TODO: create auto-mapping option
    @Override
    public int[] physicalDistances() {
        int[] dists = new int[neighbors.length];
        for (int i = 0; i < neighbors.length; i++) dists[i] = routingTable.path(i).distance;
        return dists;
    }

    @Override
    public void sendMessagePhysical(BaseMessage message, int neighbor) {
        try {
            byte[] byteMsg = message.dump();
            channel.basicPublish("", physID + "-" + neighbor, MessageProperties.PERSISTENT_TEXT_PLAIN, byteMsg);
        } catch (IOException e) {
            // TODO: do something else?
            throw new RuntimeException(e);
        }
    }

    @Override
    public void broadcastMessagePhysical(BaseMessage message) {
        for (int i = 0; i < neighbors.length; i++) if (neighbors[i] != -1) sendMessagePhysical(message, i);
    }

    @Override
    public void die(Integer cause) {
        if (!connection.isOpen()) return;
        if (cause != null) System.out.println("Physical node " + physID + " died because of node " + cause + "!");
        else System.out.println("Physical node " + physID + " said 'uhhh' and just died!");
        broadcastMessagePhysical(new ServiceMessage(physID, ServiceMessage.MessageType.CASCADE_DEATH));
        deleteQueues();
        try {
            connection.abort();
        } catch (AlreadyClosedException e) {
            // Do nothing, connection already closed.
        }
    }


    // VIRTUAL NODE:

    @Override
    public int getVirtualID() {
        return virtID;
    }

    @Override
    public String virtualRepresentation() {
        return "Virtual node " + virtID;
    }

    @Override
    public void sendTextVirtual(String message, int recipient) {
        sendMessageVirtual(new DataMessage(message, virtID, recipient), recipient);
    }

    @Override
    public void sendMessageVirtual(BaseMessage message, int recipient) {
        if (connections[recipient] != -1) forwardMessageVirtual(message, recipient);
        else System.out.println("Virtual node " + virtID + " can't pass a message to node " + recipient + ": they aren't connected!");
    }

    @Override
    public void forwardMessageVirtual(BaseMessage message, int recipient) {
        int gate = routingTable.path(recipient).gate;
        sendMessagePhysical(message, gate);
    }

    @Override
    public void broadcastMessageVirtual(BaseMessage message) {
        for (int i = 0; i < connections.length; i++) if (connections[i] != -1) sendMessageVirtual(message, i);
    }


    // TODO: remove

    public void start() {
        //send a msg to the neighbors
        for (int i = 0; i < neighbors.length; i++)
            if (neighbors[i] != -1) sendMessagePhysical(new RoutingMessage(routingTable, physID), i);
    }
}
