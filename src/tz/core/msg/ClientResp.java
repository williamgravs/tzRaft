package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;
import tz.base.transport.sock.Sock;

import java.nio.ByteBuffer;

/**
 * ClientReq message
 *
 * Leader to clients
 */
public class ClientResp extends Msg
{
    public static final int TYPE         = 0x11;

    private boolean result;
    private long sequence;
    private ByteBuffer data;
    public long ioTs;
    public long clusterTs;

    /**
     * Create new ClientResp message
     *
     * @param result   is successful
     * @param sequence client sequence
     * @param data     response
     */
    public ClientResp(long sequence, boolean result, ByteBuffer data)
    {
        this.sequence = sequence;
        this.result   = result;
        this.data     = data;
    }

    public ByteBuffer getData()
    {
        return data;
    }

    /**
     * Create new ClientResp message
     *
     * @param buf raw encoded message
     * @param len raw encoded length
     */
    public ClientResp(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    public long getSequence()
    {
        return sequence;
    }

    /**
     * Encode message
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            int headerLen = Encoder.byteLen(TYPE) + Encoder.longLen(sequence)
                                                  + Encoder.booleanLen(result);

            length = headerLen + data.remaining();

            headerLen += Encoder.varIntLen(length);
            if (rawMsg == null) {
                rawMsg = new Buffer(headerLen);
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(ClientResp.TYPE);
            rawMsg.putLong(sequence);
            rawMsg.putBoolean(result);
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
        sequence = rawMsg.getLong();
        result   = rawMsg.getBoolean();
        data     = rawMsg.getByteBuffer(rawMsg.remaining());

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

        if (data.hasRemaining()) {
            sock.copy(data);
        }
    }

    /**
     * If raw message is written to destination(socket buffers mostly)
     * @return true if all bytes written
     */
    @Override
    public boolean written()
    {
        return (!rawMsg.hasRemaining() && !data.hasRemaining());
    }

    /**
     * Handle callback
     * @param handler message handler
     */
    @Override
    public void handle(MsgHandler handler)
    {
        handler.handleClientResp(this);
    }

    /**
     * toString
     * @return string representation of the message
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[ClientResp][" )
               .append("Total Size : " ).append(length)          .append(", ")
               .append("Sequence : "   ).append(sequence)        .append(", ")
               .append("Result : "     ).append(result)          .append(", ")
               .append("Data size : "  ).append(data.remaining()).append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
