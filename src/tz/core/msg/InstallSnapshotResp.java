package tz.core.msg;

import tz.base.common.Buffer;

public class InstallSnapshotResp extends Msg
{
    public static final int TYPE = 0x01;

    private long term;
    private boolean success;
    private boolean done;



    public InstallSnapshotResp(long term, boolean success, boolean done)
    {
        this.term    = term;
        this.success = success;
        this.done    = done;
    }

    public InstallSnapshotResp(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    public long getTerm()
    {
        return term;
    }

    public boolean isDone()
    {
        return done;
    }

    public boolean isSuccess()
    {
        return success;
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            length = Encoder.byteLen(InstallSnapshotResp.TYPE) +
                     Encoder.longLen(term) +
                     Encoder.booleanLen(done) +
                     Encoder.booleanLen(success);

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(InstallSnapshotResp.TYPE);
            rawMsg.putLong(term);
            rawMsg.putBoolean(done);
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
        done    = rawMsg.getBoolean();
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
               .append("Success : ").append(success).append(", ")
               .append("Done : "   ).append(done)   .append("]]");


        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
