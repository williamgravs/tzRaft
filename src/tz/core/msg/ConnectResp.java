package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;
import tz.base.record.ClusterRecord;
import tz.base.record.NodeRecord;


/**
 * ConnectResp message
 * Peer to peer, leader to client
 */
public class ConnectResp extends Msg
{
    public static final int TYPE = 0x03;

    private boolean successful;
    private ClusterRecord clusterRecord;
    private long sequence;
    private long acknowledge;


    public ConnectResp(boolean successful, ClusterRecord clusterRecord,
                       long sequence, long acknowledge)
    {
        this.successful    = successful;
        this.clusterRecord = clusterRecord;
        this.sequence      = sequence;
        this.acknowledge   = acknowledge;

    }

    /**
     * Create new ConnectResp message
     *
     * @param buf raw encoded message
     * @param len raw encoded length
     */
    public ConnectResp(Buffer buf, int len)
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

    public long getAcknowledge()
    {
        return acknowledge;
    }

    public ClusterRecord getClusterRecord()
    {
        return clusterRecord;
    }


    /**
     * Get successful
     * @return true if successful
     */
    public boolean isSuccessful()
    {
        return successful;
    }

    /**
     * Encode message
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            length = Encoder.byteLen(ConnectResp.TYPE) + Encoder.booleanLen(successful)
                                                       + clusterRecord.rawLen()
                                                       + Encoder.varLongLen(sequence)
                                                       + Encoder.varLongLen(acknowledge);
            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();

            rawMsg.putVarInt(length);
            rawMsg.put(ConnectResp.TYPE);
            rawMsg.putBoolean(successful);
            clusterRecord.encode(rawMsg);
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
        successful    = rawMsg.getBoolean();
        clusterRecord = new ClusterRecord(rawMsg);
        sequence      = rawMsg.getVarLong();
        acknowledge   = rawMsg.getVarLong();

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
        handler.handleConnectResp(this);
    }

    /**
     * toString
     * @return string representation of the message
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[ConnectResp][")
               .append("Success: "     ).append(successful)   .append(", ")
               .append("Cluster : "    ).append(clusterRecord).append(", ")
               .append("Sequence : "   ).append(sequence)     .append(", ")
               .append("Acknowledge : ").append(acknowledge)  .append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
