package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;


/**
 * ReqVoteResp
 *
 * Sent by peers to candidates
 */
public class ReqVoteResp extends Msg
{
    public static final int TYPE = 0x05;

    private long term;
    private long index;
    private boolean voteGranted;


    /**
     * Create new ReqVoteResp
     * @param term        local node term
     * @param voteGranted true if vote is granted
     */
    public ReqVoteResp(long term, long index, boolean voteGranted)
    {
        this.term        = term;
        this.index       = index;
        this.voteGranted = voteGranted;
    }

    /**
     * Create new ReqVoteResp
     * @param buf raw encoded ReqVoteResp
     * @param len raw encoded ReqVoteResp length
     */
    public ReqVoteResp(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
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
     * Is vote granted
     * @return true if vote is granted
     */
    public boolean isVoteGranted()
    {
        return voteGranted;
    }

    public long getIndex()
    {
        return index;
    }

    /**
     * Encode message
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            length = Encoder.byteLen(ReqVoteResp.TYPE) +
                     Encoder.varLongLen(term) +
                     Encoder.varLongLen(index) +
                     Encoder.booleanLen(voteGranted);

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(ReqVoteResp.TYPE);
            rawMsg.putVarLong(term);
            rawMsg.putVarLong(index);
            rawMsg.putBoolean(voteGranted);

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
        term         = rawMsg.getVarLong();
        index        = rawMsg.getVarLong();
        voteGranted  = rawMsg.getBoolean();

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
        handler.handleReqVoteResp(this);
    }

    /**
     * toString
     * @return string representation of the message
     */
    @Override
    public String toString()
    {
        final String nl = Util.newLine();
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[ReqVoteResp][")
               .append("Term : "        ).append(term)       .append(", ")
               .append("Index : "       ).append(index)      .append(", ")
               .append("Vote Granted : ").append(voteGranted).append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
