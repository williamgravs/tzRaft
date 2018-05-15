package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;
import tz.base.transport.sock.Sock;

import java.nio.ByteBuffer;

/**
 * ClientReq message
 *
 * Clients to leader
 */
public class ClientReq extends Msg
{
    public static final int TYPE = 0x10;

    private int stateId;
    private long sequence;
    private long acknowledge;
    private ByteBuffer data;

    /**
     * Create new ClientReq message
     *
     * @param stateId  state to apply command
     * @param sequence sequence of the client
     * @param data     raw command
     */
    public ClientReq(int stateId, long sequence, long acknowledge, ByteBuffer data)
    {
        this.stateId     = stateId;
        this.sequence    = sequence;
        this.acknowledge = acknowledge;
        this.data        = data;
    }

    /**
     * Create new Client Req message
     *
     * @param buf raw encoded message
     * @param len raw encoded length
     */
    public ClientReq(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    public void rewind()
    {
        rawMsg.rewind();
        data.rewind();
    }


    /**
     * Get state id
     * @return state id of the command
     */
    public int getStateId()
    {
        return stateId;
    }

    /**
     * Get client sequence
     * @return client sequence
     */
    public long getSequence()
    {
        return sequence;
    }

    public long getAcknowledge()
    {
        return acknowledge;
    }

    /**
     * Get raw command or query
     * @return raw command or query
     */
    public ByteBuffer getData()
    {
        return data;
    }

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

    @Override
    public boolean written()
    {
        return (!rawMsg.hasRemaining() && !data.hasRemaining());
    }

    /**
     * Encode message
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            int headerLen = Encoder.byteLen(TYPE) + Encoder.varIntLen(stateId)
                                                  + Encoder.varLongLen(sequence)
                                                  + Encoder.varLongLen(acknowledge);

            length = headerLen + data.remaining();

            headerLen += Encoder.varIntLen(length);
            if (rawMsg == null) {
                rawMsg = new Buffer(headerLen);
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(ClientReq.TYPE);
            rawMsg.putVarLong(stateId);
            rawMsg.putVarLong(sequence);
            rawMsg.putVarLong(acknowledge);
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
        stateId     = rawMsg.getVarInt();
        sequence    = rawMsg.getVarLong();
        acknowledge = rawMsg.getVarLong();
        data        = rawMsg.getByteBuffer(rawMsg.remaining());

        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Handle message callback
     * @param handler message handler
     */
    @Override
    public void handle(MsgHandler handler)
    {
        handler.handleClientReq(this);
    }

    /**
     * toString
     * @return string representation of the message
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[ClientReq][")
               .append("Total Size : " ).append(length)     .append(", ")
               .append("State ID : "   ).append(stateId)    .append(", ")
               .append("Sequence : "   ).append(sequence)   .append(", ")
               .append("Acknowledge : ").append(acknowledge).append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
