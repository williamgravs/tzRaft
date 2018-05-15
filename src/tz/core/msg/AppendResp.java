package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;

/**
 * AppendResp message
 * Follower to Leaders
 */
public class AppendResp extends Msg
{
    public static final int TYPE = 0x07;

    private long index;
    private long term;
    private boolean success;


    /**
     * Create new AppendResp message
     *
     * @param index   last index (error or successful)
     * @param term    current term
     * @param success is successful
     */
    public AppendResp(long index, long term, boolean success)
    {
        this.index   = index;
        this.term    = term;
        this.success = success;
    }

    /**
     * Create new AppendResp message
     *
     * @param buf raw encoded AppendResp message
     * @param len raw encoded length
     */
    public AppendResp(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Get index
     * @return index
     */
    public long getIndex()
    {
        return index;
    }

    /**
     * Is successful
     * @return is successful
     */
    public boolean isSuccess()
    {
        return success;
    }

    /**
     * Get term
     * @return term
     */
    public long getTerm()
    {
        return term;
    }

    /**
     * Encode message
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            length = Encoder.byteLen(TYPE) + Encoder.varLongLen(term)
                                             + Encoder.varLongLen(index)
                                             + Encoder.booleanLen(success);

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(AppendResp.TYPE);
            rawMsg.putVarLong(index);
            rawMsg.putVarLong(term);
            rawMsg.putBoolean(success);

            rawMsg.flip();
            rawReady = true;

            assert (rawMsg.remaining() >= Msg.MIN_MSG_SIZE);
        }
    }

    /**
     * Decode message
     */
    @Override
    public void decode()
    {
        index   = rawMsg.getVarLong();
        term    = rawMsg.getVarLong();
        success = rawMsg.getBoolean();

        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Handle callback
     * @param handler message handler
     */
    @Override
    public void handle(MsgHandler handler)
    {
        handler.handleAppendResp(this);
    }

    /**
     * toString
     * @return string representation of the message
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[AppendResp][")
               .append("Index : "  ).append(index)  .append(", ")
               .append("Term : "   ).append(term)   .append(", ")
               .append("Success : ").append(success).append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
