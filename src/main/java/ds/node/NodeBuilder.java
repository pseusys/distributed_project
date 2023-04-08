package ds.node;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ds.misc.TripleConsumer;
import ds.objects.RoutingTable;


public class NodeBuilder {
    private int physID, virtID;
    private int nodesNumber;
    private Boolean computeVirtual = null;

    private int[] connections;
    private int[] neighbors;

    TripleConsumer<String, Integer, Node> virtualCallback = (message, sender, self) -> {};
    Consumer<Node> initializationCallback = (node) -> {};

    public static NodeBuilder create(int physicalID, int[] neighbors) {
        return new NodeBuilder(physicalID, neighbors);
    }

    private NodeBuilder(int physicalID, int[] neighbors) {
        this.physID = physicalID;
        this.neighbors = neighbors;
        this.nodesNumber = neighbors.length;
    }

    public NodeBuilder defineVirtual(int virtualID, int[] connections) {
        this.virtID = virtualID;
        this.connections = connections;
        this.computeVirtual = false;
        if (connections.length != nodesNumber) throw new RuntimeException("Connection length (" + connections.length + ") doesn't match nodes number (" + nodesNumber + ")!");
        return this;
    }

    // TODO: implement
    public NodeBuilder computeVirtual(int[][] mapping) {
        this.computeVirtual = true;
        throw new UnsupportedOperationException("Unimplemented method 'computeVirtual'");
    }

    public NodeBuilder afterInitialization(Consumer<Node> initializationCallback) {
        this.initializationCallback = initializationCallback;
        return this;
    }

    public NodeBuilder onVirtualMessage(TripleConsumer<String, Integer, Node> virtualCallback) {
        this.virtualCallback = virtualCallback;
        return this;
    }

    public Node build(BiConsumer<Node, RoutingTable> creationCallback) {
        if (computeVirtual == null) throw new RuntimeException("Virtual topology must be defined or computed!");
        Node node = new Node(physID, nodesNumber, neighbors, connections, virtualCallback, initializationCallback);
        creationCallback.accept(node, node.routingTable);
        return node;
    }
}
