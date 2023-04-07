package ds.node;

import java.io.IOException;

import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import ds.base.BaseMessage;
import ds.objects.DataMessage;
import ds.objects.RoutingMessage;
import ds.objects.ServiceMessage;
import ds.objects.ServiceMessage.MessageType;


public class NodeMessageCallback implements DeliverCallback {
    private Node node;
    private int round_counter = 0;
    private int neighbor_counter = 0;
    private int real_neighbors = 0;

    private int nodeNumber;
    private boolean[] initialized;

    protected NodeMessageCallback(Node node, int[] neighbors, int real_neighbors) {
        this.node = node;
        this.real_neighbors = real_neighbors;
        this.nodeNumber = node.nodesCount();
        this.initialized = new boolean[nodeNumber];
        for (int i = 0; i < nodeNumber; i++) initialized[i] = false;
    }

    @Override
    public void handle(String consumerTag, Delivery delivery) throws IOException {
        BaseMessage message;
        try {
            message = BaseMessage.parse(delivery.getBody());
        } catch (ClassNotFoundException e) {
            // TODO: do something else?
            throw new RuntimeException(e);
        }

        switch (message.getMessageTypeCode()) {
            case ServiceMessage.code:
                ServiceMessage sm = (ServiceMessage) message;
                switch (sm.type) {
                    case INITIALIZED:
                        if (initialized[sm.sender]) break;
                        node.broadcastMessagePhysical(sm);
                        initialized[sm.sender] = true;
                        if (checkInitialized()) node.initializationCallback.accept(node);
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
                if (dm.receiver == node.getPhysicalID()) {
                    if (node.virtualCallback != null) node.virtualCallback.apply(dm.message, dm.sender, node);
                    else System.out.println(node.physicalRepresentation() + " received a message, but it doesn't have a callback for it!");
                } else {
                    System.out.println(node.physicalRepresentation() + " forwards a message (from " + dm.sender + ", to: " + dm.receiver + ")!");
                    node.forwardMessageVirtual(message, dm.receiver);
                }
                break;

            case RoutingMessage.code:
                RoutingMessage rm = (RoutingMessage) message;
                neighbor_counter++;
                node.routingTable.update(rm.table, rm.sender);
                if (neighbor_counter == real_neighbors) {
                    neighbor_counter = 0;
                    round_counter++;
                    if (round_counter == nodeNumber + 1) { // TODO: Pia, check this please, why it's nodeNumber + 1? Is it right? Will always work?
                        initialized[node.getPhysicalID()] = true;
                        node.broadcastMessagePhysical(new ServiceMessage(node.getPhysicalID(), MessageType.INITIALIZED));
                    } else if (round_counter <= nodeNumber + 1) node.broadcastMessagePhysical(new RoutingMessage(node.routingTable, node.getPhysicalID()));
                }
                break;

            default:
                System.out.println("Unexpected message received (" + message.getMessageTypeCode() + ")!");
                break;
        }
    }

    private boolean checkInitialized() {
        boolean init = true;
        for (int i = 0; i < nodeNumber; i++) init &= initialized[i];
        return init;
    }
}
