package ds.objects;

import ds.base.BaseMessage;


public class ConnectionMessage extends BaseMessage {
    public static final byte code = 0x03;

    public final int sender;
    public final boolean computed;
    public final int[] permutation;

    public ConnectionMessage(int[] permutation, int sender, boolean computed) {
        this.permutation = permutation;
        this.sender = sender;
        this.computed = computed;
    }

    @Override
    public byte getMessageTypeCode() {
        return code;
    }
}
