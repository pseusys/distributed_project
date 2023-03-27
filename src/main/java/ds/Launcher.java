package main.java.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Launcher {
    private int[][] matrix = {
            {-1, -1, 1, -1, -1},
            {-1, -1, 1, 1, 1},
            {1, 1, -1, -1, -1},
            {-1, 1, -1, -1, -1},
            {-1, 1, -1, -1, -1}
    };

    private Map<PhysicalNode, VirtualNode> map = Map.of(
            new PhysicalNode(0), new VirtualNode(0),
            new PhysicalNode(1), new VirtualNode(1),
            new PhysicalNode(2), new VirtualNode(2),
            new PhysicalNode(3), new VirtualNode(3),
            new PhysicalNode(4), new VirtualNode(4)
    );

    public static void main(String[] args) {
        List<PhysicalNode> physical = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            physical.add(new PhysicalNode(i));
        }


    }
}
