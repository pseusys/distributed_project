package ds.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class RoutingTable implements Serializable {
    public class RoutingTableEntry implements Serializable {
        int gate, destination, distance;

        RoutingTableEntry(int id, boolean isCurrent) {
            if (isCurrent) {
                gate = destination = id;
                distance = 0;
            } else {
                gate = -1;
                destination = id;
                distance = 3000; // TODO: change to "undefined" value.
            }
        }

        @Override
        public String toString() {
            return "(" + gate + ", " + destination + ", " + distance + ")";
        }
    }

    private List<RoutingTableEntry> table = new ArrayList<RoutingTableEntry>();

    public RoutingTable(int current, int neighbors) {
        for (int i = 0; i < neighbors; i++) table.add(new RoutingTableEntry(i, i == current));
    }

    public RoutingTableEntry path(int node) {
        for (RoutingTableEntry entry: table) if (entry.destination == node) return entry;
        throw new RuntimeException("Route to node " + node + " is not defined!");
    }

    public void update(RoutingTable another, int sender) {
        for (RoutingTableEntry entry: another.table) {
            RoutingTableEntry current = path(entry.destination);
            if (entry.distance < current.distance) {
                //update the shortest path
                current.distance = entry.distance + 1;
                //update the parent node
                current.gate = sender;
            }
        }
    }

    @Override
    public String toString() {
        return table.toString();
    }
}
