package ds.objects;

import ds.base.BaseMessage;


public class ServiceMessage extends BaseMessage {
    public static final byte code = 0x00;

    public static enum MessageType {
        INITIALIZED, CASCADE_DEATH;
    }

    public final int sender;
    public final MessageType type;

    public ServiceMessage(int sender, MessageType type) {
        this.sender = sender;
        this.type = type;
    }

    @Override
    public byte getMessageTypeCode() {
        return code;
    }
    
}
