package ds.launchers;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

import ds.misc.TripleConsumer;
import ds.node.Node;
import ds.node.NodeBuilder;


public class ProcessLauncher {
    private static class NodeTask extends ForkJoinTask<Void> {
        private final int id;
        private final Consumer<Node> initializationCallback;
        private final TripleConsumer<String, Integer, Node> messageCallback;

        NodeTask(int physID, Consumer<Node> initializationCallback, TripleConsumer<String, Integer, Node> messageCallback) {
            this.id = physID;
            this.initializationCallback = initializationCallback;
            this.messageCallback = messageCallback;
        }

        @Override
        protected boolean exec() {
            NodeBuilder.create(id, matrix[id])
                .computeVirtual(mapping)
                .afterInitialization(initializationCallback)
                .onVirtualMessage(messageCallback)
                .build();
            return true;
        }

        @Override
        public Void getRawResult() {
            return null;
        }

        @Override
        protected void setRawResult(Void arg) {}
    }



    private static int num = 5; 

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
                int receiver = node.nodesCount() - 1;
                String message = "Forwarding 0";
                System.out.println(node.virtualRepresentation() + " sends message '" + message + "' to node " + receiver + "!");
                node.sendTextVirtual(message, receiver);
            }
        };

        // If process 0, print message, otherwise forward to the previous in the ring.
        TripleConsumer<String, Integer, Node> messageCallback = (message, sender, self) -> {
            if (self.getVirtualID() == 0) {
                System.out.println(self.virtualRepresentation() + " passed the message '" + message + " -> 0' round the ring!");
                self.die(null);
            } else {
                String newMessage = message + " -> " + self.getVirtualID();
                int newRecipient = self.getVirtualID() - 1;
                System.out.println(self.virtualRepresentation() + " passes message '" + newMessage + "' to node " + newRecipient + "!");
                self.sendTextVirtual(newMessage, newRecipient);
            }
        };

        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        for (int i = 0; i < num; i++)
            commonPool.invoke(new NodeTask(i, initializationCallback, messageCallback));
    }
}
