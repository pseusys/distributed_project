package main.java.ds;

public class PhysicalNode extends Node {
    int nodesNumber;

    int[] neighbors;

    int[][] routingTable;

    public PhysicalNode(int id, int nodesNumber, int[] neighbors) {
        this.id = id;
        this.nodesNumber = nodesNumber;
        this.neighbors = neighbors;
        this.routingTable = new int[nodesNumber][3];
        for (int i = 0; i < nodesNumber; i++) {
            if (i == id) routingTable[i] = new int[] {id, id, 0};
            else routingTable[i] = new int[] {-1, i, Integer.MAX_VALUE};
        }
    }


}
