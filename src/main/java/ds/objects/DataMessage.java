package ds.objects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


// TODO: extend a message interface
public class DataMessage implements Serializable {
    public int sender, receiver;
    public String message;

    public static DataMessage parse(byte[] bytes) throws IOException, ClassNotFoundException {
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
            ObjectInputStream objectStream = new ObjectInputStream(byteStream);
            return (DataMessage) objectStream.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DataMessage(String message, int sender, int receiver) {
        this.message = message;
        this.sender = sender;
        this.receiver = receiver;
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
