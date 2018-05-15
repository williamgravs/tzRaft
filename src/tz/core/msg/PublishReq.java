package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.record.ClusterRecord;

public class PublishReq extends Msg
{
    public static final int TYPE = 0x12;
    private ClusterRecord record;

    public PublishReq(ClusterRecord record)
    {
        this.record = record;
    }

    public ClusterRecord getRecord()
    {
        return record;
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            int headerLen = Encoder.byteLen(PublishReq.TYPE) + record.rawLen();
            length = headerLen;
            headerLen += Encoder.varIntLen(length);
            if (rawMsg == null) {
                rawMsg = new Buffer(headerLen);
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(PublishReq.TYPE);
            record.encode(rawMsg);
            rawMsg.flip();
            rawReady = true;

            assert (rawMsg.remaining() >= Msg.MIN_MSG_SIZE);
        }
    }

    @Override
    public void decode()
    {
        record = new ClusterRecord(rawMsg);
        rawMsg.rewind();
        rawReady = true;
    }

    @Override
    public void handle(MsgHandler handler)
    {
        handler.handlePublishReq(this);
    }

    @Override
    public int getType()
    {
        return TYPE;
    }
}
