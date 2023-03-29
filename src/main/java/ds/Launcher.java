package ds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Thread.sleep;

public class Launcher {
    private static int[][] matrix = {
            {-1, -1, 1, -1, -1},
            {-1, -1, 1, 1, 1},
            {1, 1, -1, -1, -1},
            {-1, 1, -1, -1, -1},
            {-1, 1, -1, -1, -1}
    };

    public static void main(String[] args) throws InterruptedException {
        List<PhysicalNode> physical = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            physical.add(new PhysicalNode(i, 5, matrix[i]));
        }

        for (int i = 0; i < 5; i++) {
            physical.get(i).start();
        }

        sleep(5000);

        for (int i = 0; i < 5; i++) {
            System.out.println("i'm node " + physical.get(i).id);
            System.out.println(Arrays.deepToString(physical.get(i).getRoutingTable()));
        }
    }
}
