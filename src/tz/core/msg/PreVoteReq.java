package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;

public class PreVoteReq extends Msg
{
    public static final int TYPE = 0x14;

    private long term;
    private long lastLogIndex;
    private long lastLogTerm;

    public PreVoteReq(long term, long lastLogIndex, long lastLogTerm)
    {
        this.term         = term;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm  = lastLogTerm;
    }

    public PreVoteReq(Buffer buf, int len)
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
            length = Encoder.byteLen(PreVoteReq.TYPE) +
                     Encoder.longLen(term) +
                     Encoder.longLen(lastLogIndex) +
                     Encoder.longLen(lastLogTerm);

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(PreVoteReq.TYPE);
            rawMsg.putLong(term);
            rawMsg.putLong(lastLogIndex);
            rawMsg.putLong(lastLogTerm);
            rawMsg.flip();

            rawReady = true;

            assert (rawMsg.remaining() >= Msg.MIN_MSG_SIZE);
        }
    }

    @Override
    public void decode()
    {
        term         = rawMsg.getVarLong();
        lastLogIndex = rawMsg.getVarLong();
        lastLogTerm  = rawMsg.getVarLong();

        rawMsg.rewind();
        rawReady = true;
    }

    @Override
    public void handle(MsgHandler handler)
    {
        handler.handlePreVoteReq(this);
    }

    public long getTerm()
    {
        return term;
    }

    public void setTerm(long term)
    {
        this.term = term;
    }

    public long getLastLogIndex()
    {
        return lastLogIndex;
    }


    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[PreVoteReq][")
               .append("Term: "           ).append(term)        .append(", ")
               .append("Last log index : ").append(lastLogIndex).append(", ")
               .append("Last log term : " ).append(lastLogTerm) .append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
