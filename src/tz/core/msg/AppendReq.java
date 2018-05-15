package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;
import tz.base.transport.sock.Sock;

import java.util.ArrayList;
import java.util.List;

/**
 * AppendReq message
 * Leader to followers, contains zero or more entries
 */
public class AppendReq extends Msg
{
    public static final int TYPE = 0x06;


    private long term;
    private long prevLogIndex;
    private long prevLogTerm;
    private long leaderCommit;

    //We use entries list when message is decoded (incoming)
    private transient List<Entry> entries;

    //Raw buffer of the outgoing entries
    private Buffer entryBufs;


    /**
     * Create new AppendReq message
     *
     * @param term         Leader term
     * @param prevLogIndex Previous log index
     * @param prevLogTerm  Previous log term
     * @param leaderCommit Leader commit
     */
    public AppendReq(long term, long prevLogIndex,
                     long prevLogTerm, long leaderCommit)
    {
        this.term         = term;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm  = prevLogTerm;
        this.leaderCommit = leaderCommit;
        this.entries      = new ArrayList<>();
    }

    /**
     * Create new AppendReq message
     *
     * @param buf raw encoded AppendReq message
     * @param len raw encoded length
     */
    public AppendReq(Buffer buf, int len)
    {
        super(buf, len);
        entries = new ArrayList<>();

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Set entry/entries data
     *
     * @param entryBufs     buffer list holding entry data
     */
    public void setEntriesBuffer(Buffer entryBufs)
    {
        this.entryBufs = entryBufs;
    }

    /**
     * Get entries
     * @return entries of the message
     */
    public List<Entry> getEntries()
    {
        return entries;
    }

    /**
     * Get previous log index
     * @return previous log index
     */
    public long getPrevLogIndex()
    {
        return prevLogIndex;
    }

    /**
     * Get previous log term
     * @return previous log term
     */
    public long getPrevLogTerm()
    {
        return prevLogTerm;
    }

    /**
     * Get leader commit
     * @return leader commit
     */
    public long getLeaderCommit()
    {
        return leaderCommit;
    }

    /**
     * Get leader term
     * @return leader term
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
                                           + Encoder.varLongLen(prevLogIndex)
                                           + Encoder.varLongLen(prevLogTerm)
                                           + Encoder.varLongLen(leaderCommit);


            Buffer buf = entryBufs;
            while (buf != null){
                length += buf.remaining();
                buf = buf.next;
            }

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(AppendReq.TYPE);
            rawMsg.putVarLong(term);
            rawMsg.putVarLong(prevLogIndex);
            rawMsg.putVarLong(prevLogTerm);
            rawMsg.putVarLong(leaderCommit);

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
        prevLogIndex = rawMsg.getVarLong();
        prevLogTerm  = rawMsg.getVarLong();
        leaderCommit = rawMsg.getVarLong();

        long index = prevLogIndex + 1;
        while (rawMsg.remaining() > 0) {
            Entry entry = new Entry(rawMsg);
            entry.setIndex(index++);
            entries.add(entry);
        }
        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Copy raw encoded message to sock's outgoing buffer
     * @param sock outgoing sock
     */
    @Override
    public void writeTo(Sock sock)
    {
        if (rawMsg.hasRemaining()) {
            sock.copy(rawMsg.backend());
        }

        Buffer buf = entryBufs;
        while (buf != null) {
            if (sock.append(buf.backend())) {
                entryBufs = buf.next;
                buf = buf.next;
            }
            else {
                return;
            }
        }
    }

    /**
     * If raw message is written to destination(socket buffers mostly)
     * @return true if all bytes written
     */
    @Override
    public boolean written()
    {
        return (!rawMsg.hasRemaining() && entryBufs == null);
    }

    /**
     * Handle callback
     * @param handler message handler
     */
    @Override
    public void handle(MsgHandler handler)
    {
        handler.handleAppendReq(this);
    }

    @Override
    public int getType()
    {
        return TYPE;
    }

    /**
     * toString
     * @return string representation of the message
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[AppendReq][")
               .append("Total Size : "  ).append(length)        .append(", ")
               .append("Term : "        ).append(term)          .append(", ")
               .append("PrevLogIndex : ").append(prevLogIndex)  .append(", ")
               .append("PrevLogTerm : " ).append(prevLogTerm)   .append(", ")
               .append("LeaderCommit : ").append(leaderCommit)  .append(", ")
               .append("Entry count : " ).append(entries.size()).append("]]");

        return builder.toString();
    }
}
