package ds;

import java.util.ArrayList;
import java.util.List;

public class Launcher {
    private static int[][] matrix = {
            {-1, -1, 1, -1, -1},
            {-1, -1, 1, 1, 1},
            {1, 1, -1, -1, -1},
            {-1, 1, -1, -1, -1},
            {-1, 1, -1, -1, -1}
    };

    public static void main(String[] args) {
        List<PhysicalNode> physical = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            physical.add(new PhysicalNode(i, 5, matrix[i]));
        }
    }
}
