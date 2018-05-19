package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.transport.sock.Sock;

public class InstallSnapshotReq extends Msg
{
    public static final int TYPE = 0x00;

    private long term;
    private long lastIndex;
    private long lastTerm;
    private Buffer data;
    private boolean done;

    private transient long dataLength;

    public InstallSnapshotReq(long term, long lastIndex,
                              long lastTerm, Buffer data, boolean done)
    {
        this.term      = term;
        this.lastIndex = lastIndex;
        this.lastTerm  = lastTerm;
        this.data      = data;
        this.done      = done;
        dataLength     = data.remaining();
    }

    public InstallSnapshotReq(Buffer buf, int len)
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
            length = Encoder.byteLen(InstallSnapshotReq.TYPE) +
                     Encoder.longLen(term) +
                     Encoder.longLen(lastIndex) +
                     Encoder.longLen(lastTerm) +
                     Encoder.booleanLen(done);

            length += data.remaining();

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(InstallSnapshotReq.TYPE);
            rawMsg.putLong(term);
            rawMsg.putLong(lastIndex);
            rawMsg.putLong(lastTerm);
            rawMsg.putBoolean(done);
            rawMsg.flip();
            rawReady = true;

            assert (rawMsg.remaining() >= Msg.MIN_MSG_SIZE);
        }
    }

    @Override
    public void decode()
    {
        term       = rawMsg.getLong();
        lastIndex  = rawMsg.getLong();
        lastTerm   = rawMsg.getLong();
        done       = rawMsg.getBoolean();
        data       = rawMsg.getBuffer(rawMsg.remaining());
        dataLength = data.remaining();

        rawMsg.rewind();
        rawReady = true;
    }

    @Override
    public void writeTo(Sock sock)
    {
        if (rawMsg.hasRemaining()) {
            sock.copy(rawMsg.backend());
        }

        if (data != null) {
            sock.append(data.backend());
            data = null;
        }
    }

    /**
     * If raw message is written to destination(socket buffers mostly)
     * @return true if all bytes written
     */
    @Override
    public boolean written()
    {
        return (!rawMsg.hasRemaining() && data == null);
    }

    @Override
    public void handle(MsgHandler handler)
    {
        handler.handleInstallSnapshotReq(this);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[InstallSnapshotReq][")
               .append("Term: "       ).append(term)            .append(", ")
               .append("Last index : ").append(lastIndex)       .append(", ")
               .append("Last term : " ).append(lastTerm)        .append(", ")
               .append("Done : "      ).append(lastTerm)        .append(", ")
               .append("Data len  : " ).append(dataLength).append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
