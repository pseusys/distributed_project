package ds.objects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


// TODO: extend a message interface
public class RoutingMessage implements Serializable {
    public int current;
    public RoutingTable table;

    public static RoutingMessage parse(byte[] bytes) throws IOException, ClassNotFoundException {
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
            ObjectInputStream objectStream = new ObjectInputStream(byteStream);
            return (RoutingMessage) objectStream.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RoutingMessage(RoutingTable table, int current) {
        this.current = current;
        this.table = table;
    }

    public byte[] dump() {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(this);
            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
