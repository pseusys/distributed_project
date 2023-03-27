package main.java.ds;

import java.util.List;

public class VirtualNode extends Node {
    List<VirtualNode> neighbors;

    public VirtualNode(int id) {
        this.id = id;
    }
}
