package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;

public class PreVoteResp extends Msg
{
    public static final int TYPE = 0x15;

    private long term;
    private long index;
    private boolean granted;

    public PreVoteResp(long term, long index, boolean granted)
    {
        this.term    = term;
        this.index   = index;
        this.granted = granted;
    }

    public PreVoteResp(Buffer buf, int len)
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
            length = Encoder.byteLen(PreVoteResp.TYPE) +
                     Encoder.longLen(term) +
                     Encoder.longLen(index) +
                     Encoder.booleanLen(granted);

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(PreVoteResp.TYPE);
            rawMsg.putLong(term);
            rawMsg.putLong(index);
            rawMsg.putBoolean(granted);
            rawMsg.flip();

            rawReady = true;

            assert (rawMsg.remaining() >= Msg.MIN_MSG_SIZE);
        }
    }

    @Override
    public void decode()
    {
        term    = rawMsg.getLong();
        index   = rawMsg.getLong();
        granted = rawMsg.getBoolean();

        rawMsg.rewind();
        rawReady = true;
    }

    @Override
    public void handle(MsgHandler handler)
    {
        handler.handlePreVoteResp(this);
    }

    public long getTerm()
    {
        return term;
    }

    public void setTerm(long term)
    {
        this.term = term;
    }

    public long getIndex()
    {
        return index;
    }

    public boolean isGranted()
    {
        return granted;
    }

    @Override
    public String toString()
    {
        final String nl = Util.newLine();
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[PreVoteResp][")
               .append("Term : "   ).append(term)   .append(", ")
               .append("Index : "  ).append(index)  .append(", ")
               .append("Granted : ").append(granted).append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
