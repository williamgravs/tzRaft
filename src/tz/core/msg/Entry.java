package tz.core.msg;

import tz.base.common.Buffer;

import java.nio.ByteBuffer;

/**
 * Entry class
 */
public class Entry
{
    public static final int MAX_ENTRY_HEADER;

    static {
        int len = Encoder.intLen(0) + Encoder.intLen(0) +
                                      Encoder.longLen(0) +
                                      Encoder.longLen(0) +
                                      Encoder.longLen(0);
        MAX_ENTRY_HEADER = len + Encoder.varIntLen(len);
    }

    public static final int MIN_ENTRY_SIZE = Encoder.MAX_VAR_INT_LEN;
    private static final Buffer EMPTY_BUF = new Buffer(0);

    private int stateId;
    private int clientId;
    private long sequence;
    private long acknowledge;
    private long term;
    private Buffer header;
    private Buffer data;

    private transient long index;



    /**
     * Create new Entry
     *
     * @param stateId   state id of the entry
     * @param clientId  client id of the entry originator
     * @param sequence  client sequence
     * @param term      entry term
     * @param data      raw command
     */
    public Entry(int stateId, int clientId, long sequence,
                 long acknowledge, long term, ByteBuffer data)
    {
        this.stateId     = stateId;
        this.clientId    = clientId;
        this.sequence    = sequence;
        this.acknowledge = acknowledge;
        this.term        = term;
        this.index       = 0;

        int headerLen = Encoder.varIntLen(stateId)  +
                        Encoder.varIntLen(clientId) +
                        Encoder.varLongLen(sequence) +
                        Encoder.varLongLen(acknowledge) +
                        Encoder.varLongLen(term);

        int total = headerLen + Encoder.varIntLen(headerLen + data.remaining());

        header = new Buffer(total);
        header.putVarInt(headerLen + data.remaining());
        header.putVarInt(stateId);
        header.putVarInt(clientId);
        header.putVarLong(sequence);
        header.putVarLong(acknowledge);
        header.putVarLong(term);
        header.flip();

        this.data = new Buffer(data);
    }

    /**
     * Create new Entry
     * @param buffer raw encoded entry
     */
    public Entry(Buffer buffer)
    {
        this.index = 0;

        decode(buffer);
    }

    /**
     * Get encoded length of the entry
     * @return encoded length of the entry
     */
    public int encodedLen()
    {
        return header.remaining() + data.remaining();
    }

    /**
     * Encode entry to destination Buffer
     * @param buffer destination Buffer
     */
    public void encode(Buffer buffer)
    {
        buffer.put(header);
        buffer.put(data);
    }

    public int headerLen()
    {
        return header.remaining();
    }

    public int dataLen()
    {
        return data.remaining();
    }

    public void setHeader(Buffer header)
    {
        this.header = header;
    }

    public void setData(Buffer data)
    {
        this.data = data;
    }

    public void setIndex(long index)
    {
        this.index = index;
    }

    /**
     * Is entry written to destination
     * @return true if all raw bytes are written
     */
    public boolean isWritten()
    {
        return !(header.hasRemaining() || data.hasRemaining());
    }

    /**
     * Decode entry
     * @param buffer raw encoded entry
     */
    public void decode(Buffer buffer)
    {
        final int head = buffer.position();
        final int len  = buffer.getVarInt();
        final int pos  = buffer.position();


        stateId     = buffer.getVarInt();
        clientId    = buffer.getVarInt();
        sequence    = buffer.getVarLong();
        acknowledge = buffer.getVarLong();
        term        = buffer.getVarLong();

        header = buffer.slice(head, buffer.position() - head);

        final int count = len - header.remaining() + (pos - head);
        data   = buffer.slice(buffer.position(), count);
        buffer.advance(count);
    }

    public int getOffset()
    {
        return header.getOffset();
    }
    /**
     * Get state id
     * @return state id
     */
    public int getStateId()
    {
        return stateId;
    }

    /**
     * Get client id
     * @return client id
     */
    public int getClientId()
    {
        return clientId;
    }

    /**
     * Get sequence
     * @return sequence
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
     * Get term
     * @return term
     */
    public long getTerm()
    {
        return term;
    }

    public long getIndex()
    {
        return index;
    }

    public Buffer getData()
    {
        return data;
    }

    public ByteBuffer getCommand()
    {
        return data.backend().asReadOnlyBuffer();
    }
}
