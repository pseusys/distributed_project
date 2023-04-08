package ds.objects;

import ds.base.BaseMessage;


public class RoutingMessage extends BaseMessage {
    public static final byte code = 0x01;

    public final int sender;
    public final RoutingTable table;

    public RoutingMessage(RoutingTable table, int sender) {
        this.sender = sender;
        this.table = table;
    }

    @Override
    public byte getMessageTypeCode() {
        return code;
    }
}
