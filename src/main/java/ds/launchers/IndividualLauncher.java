package ds.launchers;

import java.util.function.Consumer;

import ds.misc.TripleConsumer;
import ds.node.Node;
import ds.node.NodeBuilder;


public class IndividualLauncher {
    private static int[][] matrix = {
        {-1, -1, 1, -1, -1},
        {-1, -1, 1, 1, 1},
        {1, 1, -1, -1, -1},
        {-1, 1, -1, -1, -1},
        {-1, 1, -1, -1, -1}
    };

    private static int[][] mapping = {
        {-1, 1, -1, -1, 1},
        {1, -1, 1, -1, -1},
        {-1, 1, -1, 1, -1},
        {-1, -1, 1, -1, 1},
        {1, -1, -1, 1, -1}
    };

    public static void main(String[] args) throws InterruptedException {
        Consumer<Node> initializationCallback = (node) -> {
            System.out.println(node);
            if (node.getVirtualID() == 0) {
                int receiver = 1;
                String message = "Forwarding 0";
                System.out.println(node.virtualRepresentation() + " sends message '" + message + "' to node " + receiver + "!");
                node.sendTextVirtual(message, receiver);
            }
        };

        // If process 0, print message, otherwise forward to the next in the ring.
        TripleConsumer<String, Integer, Node> messageCallback = (message, sender, self) -> {
            if (self.getVirtualID() == 0) {
                System.out.println(self.virtualRepresentation() + " passed the message '" + message + " -> 0' round the ring!");
                self.die(null);
            } else {
                String newMessage = message + " -> " + self.getVirtualID();
                int newRecipient = self.getVirtualID() == self.nodesCount() - 1 ? 0 : self.getVirtualID() + 1;
                System.out.println(self.virtualRepresentation() + " passes message '" + newMessage + "' to node " + newRecipient + "!");
                self.sendTextVirtual(newMessage, newRecipient);
            }
        };

        int id = Integer.parseInt(System.getenv().getOrDefault("PHYSICAL_ID", "-1"));
        if (id == -1) throw new RuntimeException("Node id not defined ('PHYSICAL_ID' environmental variable)!");

        NodeBuilder.create(id, matrix[id])
            .computeVirtual(mapping)
            .afterInitialization(initializationCallback)
            .onVirtualMessage(messageCallback)
            .build();
    }
}
