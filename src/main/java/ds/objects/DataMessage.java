package ds.objects;

import ds.base.BaseMessage;


public class DataMessage extends BaseMessage {
    public static final byte code = 0x02;

    public final int sender, receiver;
    public final String message;

    public DataMessage(String message, int sender, int receiver) {
        this.message = message;
        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public byte getMessageTypeCode() {
        return code;
    }
}
