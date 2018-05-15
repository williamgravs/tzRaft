package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.record.ClusterRecord;

/**
 * JoinResp
 *
 * JoinResp is sent by leader to peers in response to JoinReq, indicates if
 * joining cluster is successful
 */
public class JoinResp extends Msg
{
    public static final int TYPE = 0x09;
    public ClusterRecord record;
    public boolean result;

    /**
     * Create new JoinResp
     * @param record current cluster record
     * @param result is successful
     */
    public JoinResp(ClusterRecord record, boolean result)
    {
        this.record = record;
        this.result = result;
    }

    /**
     * Create new JoinResp
     * @param buf raw encoded JoinResp
     * @param len raw encoded JoinResp length
     */
    public JoinResp(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Encode JoinResp
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            length  = 1 + + Encoder.booleanLen(result) + record.rawLen();

            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(JoinReq.TYPE);
            record.encode(rawMsg);
            rawMsg.putBoolean(result);

            rawMsg.flip();
            rawReady = true;

            assert (rawMsg.remaining() >= Msg.MIN_MSG_SIZE);
        }
    }

    /**
     * Decode JoinResp
     */
    @Override
    public void decode()
    {
        record = new ClusterRecord(rawMsg);
        result = rawMsg.getBoolean();

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
        handler.handleJoinResp(this);
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
