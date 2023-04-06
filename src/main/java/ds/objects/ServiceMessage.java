package ds.objects;

import ds.base.BaseMessage;


public class ServiceMessage extends BaseMessage {
    public static final byte code = 0x00;

    public static enum MessageType {
        INITIALIZED, CASCADE_DEATH;
    }

    public final int sender;

    public ServiceMessage(int sender) {
        this.sender = sender;
    }

    @Override
    public byte getMessageTypeCode() {
        return code;
    }
    
}
