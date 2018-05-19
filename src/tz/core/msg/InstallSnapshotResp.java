package tz.core.msg;

import tz.base.common.Buffer;

public class InstallSnapshotResp extends Msg
{
    public static final int TYPE = 0x01;

    private long term;
    private long offset;
    private boolean success;



    public InstallSnapshotResp(long term, long offset, boolean success)
    {
        this.term    = term;
        this.offset  = offset;
        this.success = success;
    }

    public InstallSnapshotResp(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            length = Encoder.byteLen(InstallSnapshotResp.TYPE) +
                     Encoder.longLen(term) +
                     Encoder.longLen(offset) +
                     Encoder.booleanLen(success);

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(InstallSnapshotResp.TYPE);
            rawMsg.putLong(term);
            rawMsg.putLong(offset);
            rawMsg.putBoolean(success);
            rawMsg.flip();

            rawReady = true;

            assert (rawMsg.remaining() >= Msg.MIN_MSG_SIZE);
        }
    }

    @Override
    public void decode()
    {
        term    = rawMsg.getLong();
        offset  = rawMsg.getLong();
        success = rawMsg.getBoolean();

        rawMsg.rewind();
        rawReady = true;
    }

    @Override
    public void handle(MsgHandler handler)
    {
        handler.handleInstallSnapshotResp(this);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[InstallSnapshotResp][")
               .append("Term : "   ).append(term)   .append(", ")
               .append("Offset : " ).append(offset) .append(", ")
               .append("Success : ").append(success).append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
