package ds;

import java.io.IOException;

import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import ds.base.BaseMessage;
import ds.objects.DataMessage;
import ds.objects.RoutingMessage;
import ds.objects.ServiceMessage;


public class NodeMessageCallback implements DeliverCallback {
    private Node node;
    // private int round_counter = 0;
    // private int neighbor_counter = 0; //for each round

    public NodeMessageCallback(Node node) {
        this.node = node;
    }

    @Override
    public void handle(String consumerTag, Delivery delivery) throws IOException {
        BaseMessage message;
        try {
            message = BaseMessage.parse(delivery.getBody());
        } catch (ClassNotFoundException e) {
            // TODO: change
            throw new RuntimeException(e);
        }

        switch (message.getMessageTypeCode()) {
            case ServiceMessage.code:
                
                break;
        
            case DataMessage.code:
                DataMessage dm = (DataMessage) message;
                if (dm.receiver == node.physID) {
                    if (node.virtualCallback != null) node.virtualCallback.apply(dm.message, dm.sender, node);
                    else System.out.println("Physical node " + node.physID + " received a message, but it doesn't have a callback for it!");
                } else {
                    System.out.println("Physical node " + node.physID + " forwards a message (from " + dm.sender + ", to: " + dm.receiver + ")!");
                    node.forwardText(dm.sender, dm.receiver, dm.message);
                }
                break;

            case RoutingMessage.code:
                RoutingMessage rm = (RoutingMessage) message;
                node.neighbor_counter++;
                node.routingTable.update(rm.table, rm.sender);
                if (node.neighbor_counter == node.real_neighbors) {
                    node.neighbor_counter = 0;
                    node.round_counter++;
                    if (node.round_counter < node.neighbors.length)
                        for (int i = 0; i < node.neighbors.length; i++)
                            if (node.neighbors[i] != -1) node.sendMsg(new RoutingMessage(node.routingTable, node.physID), i);
                }
                break;

            default:
                System.out.println("Undefined message received (" + message.getMessageTypeCode() + ")!");
                break;
        }
    }
}
