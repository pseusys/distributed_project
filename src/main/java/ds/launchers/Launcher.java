package ds.launchers;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ds.misc.TripleConsumer;
import ds.node.Node;
import ds.node.NodeBuilder;
import ds.objects.RoutingMessage;
import ds.objects.RoutingTable;


public class Launcher {
    private static int num = 5; 

    private static int[][] matrix = {
        {-1, -1, 1, -1, -1},
        {-1, -1, 1, 1, 1},
        {1, 1, -1, -1, -1},
        {-1, 1, -1, -1, -1},
        {-1, 1, -1, -1, -1}
    };

    // TODO: auto mapping?
    private static int[][] mapping = {
        {-1, 1, -1, -1, 1},
        {1, -1, 1, -1, -1},
        {-1, 1, -1, 1, -1},
        {-1, -1, 1, -1, 1},
        {1, -1, -1, 1, -1}
    };

    // TODO: same superclass for all testing cases?
    // TODO: add DEATH on exceptions.
    public static void main(String[] args) throws InterruptedException {
        // TODO: accept routing table as well
        BiConsumer<Node, RoutingTable> creationCallback = (node, table) -> {
            node.broadcastMessagePhysical(new RoutingMessage(table, node.getPhysicalID()));
        };

        Consumer<Node> initializationCallback = (node) -> {
            System.out.println(node);
            if (node.getPhysicalID() == 0) node.sendTextVirtual("Forwarding 0", 1);
        };

        // If process 0, print message, otherwise forward to the next in the ring.
        TripleConsumer<String, Integer, Node> messageCallback = (message, sender, self) -> {
            if (self.getVirtualID() == 0) {
                System.out.println(self.virtualRepresentation() + " passed the message '" + message + " -> 0' round the ring!");
                self.die(null);
            } else {
                String newMessage = message + " -> " + self.getVirtualID();
                int newRecipient = self.getVirtualID() == num - 1 ? 0 : self.getVirtualID() + 1;
                System.out.println(self.virtualRepresentation() + " passes message '" + newMessage + "' to node " + newRecipient + "!");
                self.sendTextVirtual(newMessage, newRecipient);
            }
        };

        for (int i = 0; i < num; i++)
            NodeBuilder.create(i, matrix[i])
                .defineVirtual(i, mapping[i])
                .afterInitialization(initializationCallback)
                .onVirtualMessage(messageCallback)
                .build(creationCallback);
    }
}
