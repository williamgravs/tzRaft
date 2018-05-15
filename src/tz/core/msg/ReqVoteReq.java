package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;


/**
 * ReqVote
 *
 * Candidates send to peers to get their votes to become leader.
 */
public class ReqVoteReq extends Msg
{
    public static final int TYPE = 0x04;

    private long term;
    private long lastLogIndex;
    private long lastLogTerm;
    private boolean leaderTransfer;


    public ReqVoteReq(long term, long lastLogIndex,
                      long lastLogTerm, boolean leaderTransfer)
    {
        this.term           = term;
        this.lastLogIndex   = lastLogIndex;
        this.lastLogTerm    = lastLogTerm;
        this.leaderTransfer = leaderTransfer;
    }

    /**
     * Create new ReqVoteReq
     * @param buf raw encoded ReqVoteReq
     * @param len raw encoded ReqVoteReq length
     */
    public ReqVoteReq(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Decode message
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            length = Encoder.byteLen(ReqVoteReq.TYPE) +
                     Encoder.varLongLen(term) +
                     Encoder.varLongLen(lastLogIndex) +
                     Encoder.varLongLen(lastLogTerm) +
                     Encoder.booleanLen(leaderTransfer);

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(ReqVoteReq.TYPE);
            rawMsg.putVarLong(term);
            rawMsg.putVarLong(lastLogIndex);
            rawMsg.putVarLong(lastLogTerm);
            rawMsg.putBoolean(leaderTransfer);

            rawMsg.flip();
            rawReady = true;

            assert (rawMsg.remaining() >= Msg.MIN_MSG_SIZE);
        }
    }

    /**
     * Encode message
     */
    @Override
    public void decode()
    {
        term           = rawMsg.getVarLong();
        lastLogIndex   = rawMsg.getVarLong();
        lastLogTerm    = rawMsg.getVarLong();
        leaderTransfer = rawMsg.getBoolean();

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
        handler.handleReqVoteReq(this);
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
     * Set term
     * @param term term
     */
    public void setTerm(long term)
    {
        this.term = term;
    }

    /**
     * Get last log index
     * @return last log index
     */
    public long getLastLogIndex()
    {
        return lastLogIndex;
    }

    public boolean isLeaderTransfer()
    {
        return leaderTransfer;
    }

    /**
     * toString
     * @return string representation of the message
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[ReqVoteReq][")
               .append("Term : "           ).append(term)          .append(", ")
               .append("Last log index : " ).append(lastLogIndex)  .append(", ")
               .append("Last log term : "  ).append(lastLogTerm)   .append(", ")
               .append("Leader Transfer : ").append(leaderTransfer).append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
