package ds.launchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ds.Node;


public class Launcher {
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
        List<Node> nodes = new ArrayList<>();

        for (int i = 0; i < num; i++) nodes.add(new Node(i, num, matrix[i]));
        for (int i = 0; i < num; i++) nodes.get(i).start();

        barrier(nodes);

        int[][] distances = new int[num][num];
        for (int i = 0; i < num; i++) distances[i] = nodes.get(i).distances();
        for (int i = 0; i < num; i++) System.out.println(nodes.get(i));
    }

    static private void barrier(List<Node> nodes) {
        boolean proceed = true;
        while (proceed == false) for (Node node: nodes) proceed &= node.initialized();
    }
}
