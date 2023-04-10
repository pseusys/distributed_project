package ds.node;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import ds.base.BaseMessage;
import ds.misc.PermutationCalculator;
import ds.objects.ConnectionMessage;
import ds.objects.DataMessage;
import ds.objects.RoutingMessage;
import ds.objects.ServiceMessage;
import ds.objects.ServiceMessage.MessageType;


public class NodeMessageCallback implements DeliverCallback {
    private final Node node;
    private int round_counter = 0;
    private boolean compute;

    private final int[] neighbors;
    private final boolean[] round_received;
    private final boolean[] initialized;

    private PermutationCalculator calculator;
    private final Channel channel;
    private final int[][] connectivity;
    private final int[][] distances;
    private final int[][] mappings;

    protected NodeMessageCallback(Node node, int[] neighbors, Channel channel, int[][] connectivity) {
        this.node = node;
        this.neighbors = neighbors;
        this.channel = channel;
        this.compute = connectivity != null;
        this.connectivity = connectivity;
        this.round_received = new boolean[node.nodesCount()];
        init(round_received, (idx) -> neighbors[idx] == -1);
        this.initialized = new boolean[node.nodesCount()];
        init(initialized, (idx) -> false);
        this.distances = new int[node.nodesCount()][];
        this.mappings = new int[node.nodesCount()][];
    }

    // TODO: reduce number of rounds? Check from what nodes info already received?
    @Override
    public void handle(String consumerTag, Delivery delivery) throws IOException {
        BaseMessage message;
        try {
            message = BaseMessage.parse(delivery.getBody());
        } catch (ClassNotFoundException e) {
            // TODO: do something else?
            throw new RuntimeException(e);
        }
        boolean ack = true;

        switch (message.getMessageTypeCode()) {
            case ServiceMessage.code:
                ServiceMessage sm = (ServiceMessage) message;
                switch (sm.type) {
                    case INITIALIZED:
                        if (initialized[sm.sender]) break;
                        node.broadcastMessagePhysical(sm);
                        initialized[sm.sender] = true;
                        if (check(initialized)) node.initializationCallback.accept(node);
                        break;
                    
                    case CASCADE_DEATH:
                        node.die(sm.sender);
                        break;

                    default:
                        System.out.println("Unexpected service message received (" + sm.type.name() + ")!");
                        break;
                }
                break;
        
            case DataMessage.code:
                DataMessage dm = (DataMessage) message;
                if (dm.receiver == node.getVirtualID()) {
                    if (node.virtualCallback != null) node.virtualCallback.apply(dm.message, dm.sender, node);
                    else System.out.println(node.virtualRepresentation() + " received a message, but it doesn't have a callback for it!");
                } else {
                    System.out.println(node.virtualRepresentation() + " forwards a message (from " + dm.sender + ", to: " + dm.receiver + ")!");
                    node.forwardMessageVirtual(message, dm.receiver);
                }
                break;

            case ConnectionMessage.code:
                ConnectionMessage cm = (ConnectionMessage) message;
                if (cm.computed) {
                    if (mappings[cm.sender] != null) break;
                    node.broadcastMessagePhysical(cm);
                    mappings[cm.sender] = cm.permutation;
                    if (checkm(mappings, (idx, val) -> val != null)) chooseMapping();
                } else {
                    if (distances[cm.sender] != null) break;
                    node.broadcastMessagePhysical(cm);
                    distances[cm.sender] = cm.permutation;
                    if (checkm(distances, (idx, val) -> val != null)) computeMapping();
                }
                break;

            case RoutingMessage.code:
                RoutingMessage rm = (RoutingMessage) message;
                ack = !round_received[rm.sender];
                node.getRoutingTable().update(rm.table, rm.sender);
                round_received[rm.sender] = true;
                if (check(round_received)) nextRound();
                break;

            default:
                System.out.println("Unexpected message received (" + message.getMessageTypeCode() + ")!");
                break;
        }

        if (channel.isOpen()) {
            if (ack) channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            else channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
        }
    }

    protected void initialize() {
        node.broadcastMessagePhysical(new RoutingMessage(node.getRoutingTable(), node.getPhysicalID()));
    }

    // TODO: revise + rename;
    private boolean check(boolean[] arr) {
        boolean init = true;
        for (int i = 0; i < arr.length; i++) init &= arr[i];
        return init;
    }

    private void init(boolean[] arr, Function<Integer, Boolean> initializer) {
        for (int i = 0; i < node.nodesCount(); i++) arr[i] = initializer.apply(i);
    }

    private boolean checkm(int[][] arr, BiFunction<Integer, int[], Boolean> comparator) {
        boolean init = true;
        for (int i = 0; i < arr.length; i++) init &= comparator.apply(i, arr[i]);
        return init;
    }

    private void nextRound() {
        init(round_received, (idx) -> neighbors[idx] == -1);
        if (round_counter == node.nodesCount()) {
            System.out.println(node.physicalRepresentation() + " has routes: " + Arrays.toString(node.physicalDistances()));
            initialized[node.getPhysicalID()] = true;
            if (compute) {
                int[] dists = node.physicalDistances();
                distances[node.getPhysicalID()] = dists;
                node.broadcastMessagePhysical(new ConnectionMessage(dists, node.getPhysicalID(), false));
                if (checkm(distances, (idx, val) -> val != null)) computeMapping();
            } else {
                node.broadcastMessagePhysical(new ServiceMessage(node.getPhysicalID(), MessageType.INITIALIZED));
                if (check(initialized)) node.initializationCallback.accept(node);
            }
        } else node.broadcastMessagePhysical(new RoutingMessage(node.getRoutingTable(), node.getPhysicalID()));
        round_counter++;
    }

    private void computeMapping() {
        calculator = new PermutationCalculator(distances, connectivity);
        int[] perm = calculator.calculateBestPermutationForShare(node.getPhysicalID());
        mappings[node.getPhysicalID()] = perm;
        node.broadcastMessagePhysical(new ConnectionMessage(perm, node.getPhysicalID(), true));
        if (checkm(mappings, (idx, val) -> val != null)) chooseMapping();
    }

    private void chooseMapping() {
        int[] minPerm = mappings[node.getPhysicalID()];
        int minScore = calculator.calculatePermutationScore(minPerm);
        for (int i = 0; i < mappings.length; i++) {
            int curScore = calculator.calculatePermutationScore(mappings[i]);
            if (curScore <= minScore) {
                minScore = curScore;
                minPerm = mappings[i];
            }
        }
        System.out.println(node.physicalRepresentation() + " has chosen mapping: " + Arrays.toString(minPerm));
        node.setVirtualMappings(minPerm, connectivity);
        node.initializationCallback.accept(node);
    }
}
