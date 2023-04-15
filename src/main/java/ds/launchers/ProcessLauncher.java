package ds.launchers;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

import ds.misc.TripleConsumer;
import ds.misc.Utils;
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


    private static int num = -1; 
    private static int[][] matrix;
    private static int[][] mapping;

    private static Consumer<Node> initializationCallback = (node) -> {
        System.out.println(node);
        if (node.getVirtualID() == 0) {
            int receiver = node.nodesCount() - 1;
            String message = "Forwarding 0";
            System.out.println(node.virtualRepresentation() + " sends message '" + message + "' to node " + receiver + "!");
            node.sendTextVirtual(message, receiver);
        }
    };

    // If process 0, print message, otherwise forward to the previous in the ring.
    private static TripleConsumer<String, Integer, Node> messageCallback = (message, sender, self) -> {
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


    public static void main(String[] args) throws InterruptedException {
        String physicalMappingFile, virtualMappingFile;

        if (args.length == 0) {
            physicalMappingFile = "default_physical";
            virtualMappingFile = "default_virtual";
        } else if (args.length == 2) {
            physicalMappingFile = args[0];
            virtualMappingFile = args[1];
        } else throw new RuntimeException("Unexpected arguments number: " + args.length + "!");

        try {
            matrix = Utils.readMatrixFromResource(physicalMappingFile + ".csv");
            mapping = Utils.readMatrixFromResource(virtualMappingFile + ".csv");
            num = matrix.length;
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Couldn't read physical and virtual mappings (" + physicalMappingFile + ", " + virtualMappingFile + ")!");
        }

        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        for (int i = 0; i < num; i++)
            commonPool.invoke(new NodeTask(i, initializationCallback, messageCallback));
    }
}
