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

        for (int i = 0; i < num; i++) nodes.add(new Node(i, num, matrix[i]));
        for (int i = 0; i < num; i++) nodes.get(i).start();

        // TODO: centralized solution, doesn't fit for a distributed system.
        // TODO: exception if virtual node methods are called on node before algorithm finishes.
        barrier(nodes);

        int[][] distances = new int[num][num];
        for (int i = 0; i < num; i++) {
            Node node = nodes.get(i);
            distances[i] = node.distances();
            System.out.println(node);
        }
        
        // If process 0, print message, otherwise forward to the next in the ring.
        TripleConsumer<String, Integer, Node> callback = (message, sender, self) -> {
            if (self.getVirtualID() == 0) {
                System.out.println("Virtual node 0 passed the message '" + message + " -> 0' round the ring!");
            } else {
                String newMessage = message + " -> " + self.getVirtualID();
                int newRecipient = self.getVirtualID() == num - 1 ? 0 : self.getVirtualID() + 1;
                System.out.println("Virtual node " + self.getVirtualID() + " passes message '" + newMessage + "' to node " + newRecipient + "!");
                self.sendText(newRecipient, newMessage);
            }
        };
        for (int i = 0; i < num; i++) nodes.get(i).map(i, mapping[i], callback);
        nodes.get(0).sendText(1, "Forwarding 0");
    }

    static private void barrier(List<Node> nodes) {
        boolean proceed;
        do {
            proceed = true;
            for (Node node: nodes) proceed &= node.initialized();
        } while (proceed != true);
    }
}
