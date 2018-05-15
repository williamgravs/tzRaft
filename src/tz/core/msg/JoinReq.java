package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.record.ClusterRecord;

/**
 * JoinReq
 *
 * New peers send this message to leader to join the cluster
 */
public class JoinReq extends Msg
{
    public static final int TYPE = 0x08;

    public ClusterRecord record;

    /**
     * Create new JoinReq
     * @param record last known clusterRecord
     */
    public JoinReq(ClusterRecord record)
    {
        this.record = record;
    }

    /**
     * Create new JoinReq
     * @param buf raw encoded JoinReq
     * @param len raw encoded JoinReq length
     */
    public JoinReq(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Encode JoinReq
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            length  = 1 + record.rawLen();

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(JoinReq.TYPE);
            record.encode(rawMsg);

            rawMsg.flip();
            rawReady = true;

            assert (rawMsg.remaining() >= Msg.MIN_MSG_SIZE);
        }
    }

    /**
     * Decode JoinReq
     */
    @Override
    public void decode()
    {
        record = new ClusterRecord(rawMsg);

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
        handler.handleJoinReq(this);
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
