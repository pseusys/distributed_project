package ds.node;

import ds.base.BaseMessage;
import ds.base.PhysicalNode;
import ds.base.VirtualNode;
import ds.misc.TripleConsumer;
import ds.objects.DataMessage;
import ds.objects.RoutingTable;
import ds.objects.ServiceMessage;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.Connection;


public class Node implements PhysicalNode, VirtualNode {
    private Integer virtID;
    private final Integer physID;
    private final RoutingTable routingTable;

    private int[] connections;
    private int[] mapping;
    private final int[] neighbors;
    private final String[] callbackIDs;

    private final Connection connection;
    private final Channel channel;

    protected final TripleConsumer<String, Integer, Node> virtualCallback;
    protected final Consumer<Node> initializationCallback;

    protected Node(int physID, int nodesNumber, int[] neighbors, int[] connections, int[][] connectivity, int[] mapping, TripleConsumer<String, Integer, Node> virtualCallback, Consumer<Node> initializationCallback) {
        this.physID = physID;
        this.neighbors = neighbors;
        this.connections = connections;
        this.mapping = mapping;
        this.routingTable = new RoutingTable(physID, nodesCount());
        this.initializationCallback = initializationCallback;
        this.virtualCallback = virtualCallback;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            createQueues();
        } catch (IOException | TimeoutException e) {
            // TODO: do something else?
            throw new RuntimeException(e);
        }

        callbackIDs = new String[nodesNumber];
        for (int i = 0; i < nodesCount(); i++) callbackIDs[i] = null;
        if (mapping != null) setVirtualMappings(mapping, null);
        NodeMessageCallback messageCallback = new NodeMessageCallback(this, neighbors, channel, connectivity);

        try {
            for (int i = 0; i < nodesCount(); i++) {
                if (callbackIDs[i] != null) channel.basicCancel(callbackIDs[i]);
                if (neighbors[i] != -1) callbackIDs[i] = channel.basicConsume(i + "-" + physID, false, messageCallback, consumerTag -> {});
                else callbackIDs[i] = null;
            }
        } catch (IOException e) {
            //TODO: do something else?
            throw new RuntimeException(e);
        }

        messageCallback.initialize();
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public int nodesCount() {
        return neighbors.length;
    }

    @Override
    public String toString() {
        return "Node (physical: " + physID + ", virtual: " + virtID + ") with routing: " + routingTable;
    }

    public void setVirtualMappings(int[] mapping, int[][] connectivity) {
        for (int i = 0; i < mapping.length; i++) if (mapping[i] == physID) this.virtID = i;
        if (connectivity != null) this.connections = connectivity[virtID];
        this.mapping = mapping;
    }


    // PHYSICAL UTILITIES:

    private void createQueues() {
        try {
            for (int i = 0; i < nodesCount(); i++) if (neighbors[i] != -1) {
                channel.queueDeclare(physID + "-" + i, false, false, false, null);
                channel.queueDeclare(i + "-" + physID, false, false, false, null);
            }
        } catch (IOException e) {
            // TODO: do something else? Maybe tighten the clause?
            throw new RuntimeException(e);
        }
    }

    private void deleteQueues() {
        try {
            for (int i = 0; i < nodesCount(); i++) if (neighbors[i] != -1) {
                try {
                    channel.queueDelete(physID + "-" + i);
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

    @Override
    public int[] physicalDistances() {
        int[] dists = new int[nodesCount()];
        for (int i = 0; i < nodesCount(); i++) dists[i] = routingTable.path(i).distance;
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
        for (int i = 0; i < nodesCount(); i++) if (neighbors[i] != -1) sendMessagePhysical(message, i);
    }

    @Override
    public void die(Integer cause) {
        if (!connection.isOpen()) return;
        if (cause != null) System.out.println(physicalRepresentation() + " died because of node " + cause + "!");
        else System.out.println(physicalRepresentation() + " said 'uhhh' and just died!");
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
        else System.out.println(virtualRepresentation() + " can't pass a message to node " + recipient + ": they aren't connected!");
    }

    @Override
    public void forwardMessageVirtual(BaseMessage message, int recipient) {
        int gate = routingTable.path(mapping[recipient]).gate;
        sendMessagePhysical(message, gate);
    }

    @Override
    public void broadcastMessageVirtual(BaseMessage message) {
        for (int i = 0; i < nodesCount(); i++) if (connections[i] != -1) sendMessageVirtual(message, i);
    }
}
