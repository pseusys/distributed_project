package ds.launchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ds.Node;

import static java.lang.Thread.sleep;

public class Launcher {
    private static int[][] matrix = {
            {-1, -1, 1, -1, -1},
            {-1, -1, 1, 1, 1},
            {1, 1, -1, -1, -1},
            {-1, 1, -1, -1, -1},
            {-1, 1, -1, -1, -1}
    };

    private static Map<String, List<String>> mapping = Map.of(
        "NODE_1", List.of("NODE_5", "NODE_2"),
        "NODE_2", List.of("NODE_1", "NODE_3"),
        "NODE_3", List.of("NODE_2", "NODE_4"),
        "NODE_4", List.of("NODE_3", "NODE_5"),
        "NODE_5", List.of("NODE_4", "NODE_1")
    );

    public static void main(String[] args) throws InterruptedException {
        List<Node> physical = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            physical.add(new Node(String.valueOf(i), 5, matrix[i]));
        }

        for (int i = 0; i < 5; i++) {
            physical.get(i).start();
        }

        sleep(5000);

        for (int i = 0; i < 5; i++) {
            System.out.println("i'm node " + physical.get(i).id);
            System.out.println(physical.get(i).getRoutingTable());
        }
    }
}
