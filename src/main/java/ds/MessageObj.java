package ds;

import java.io.Serializable;

public class MessageObj implements Serializable {

    int[][] routingTable;

    int nodeId;

    public MessageObj(int[][] routingTable, int nodeId) {
        this.routingTable = routingTable;
        this.nodeId = nodeId;
    }
}

