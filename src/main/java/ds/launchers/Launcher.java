package ds.launchers;

import java.util.ArrayList;
import java.util.List;

import ds.Node;
import ds.misc.TripleConsumer;


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
    public static void main(String[] args) throws InterruptedException {
        List<Node> nodes = new ArrayList<>();
        
        // If process 0, print message, otherwise forward to the next in the ring.
        TripleConsumer<String, Integer, Node> callback = (message, sender, self) -> {
            if (self.getVirtualID() == 0) {
                System.out.println("Virtual node 0 passed the message '" + message + " -> 0' round the ring!");
                self.die(null);
            } else {
                String newMessage = message + " -> " + self.getVirtualID();
                int newRecipient = self.getVirtualID() == num - 1 ? 0 : self.getVirtualID() + 1;
                System.out.println("Virtual node " + self.getVirtualID() + " passes message '" + newMessage + "' to node " + newRecipient + "!");
                self.sendTextVirtual(newMessage, newRecipient);
            }
        };
        
        for (int i = 0; i < num; i++) nodes.add(new Node(i, num, matrix[i], mapping[i], callback, (node) -> {
            System.out.println(node);
            if (node.getPhysicalID() == 0) node.sendTextVirtual("Forwarding 0", 1);
        }));
        for (int i = 0; i < num; i++) nodes.get(i).start();
    }
}
