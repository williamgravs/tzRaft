package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.transport.sock.Sock;

import java.nio.ByteBuffer;

/**
 * Abstract message class
 *
 * Messages are used between leader - follower or leader - clients
 */
public abstract class Msg
{
    public static final int MSG_TYPE_SIZE = 1;
    public static final int MIN_MSG_SIZE = Encoder.MAX_VAR_INT_LEN;

    protected int length;
    protected Buffer rawMsg;
    protected boolean rawReady;

    /**
     * Create new Msg
     */
    protected Msg()
    {

    }

    /**
     * Create new Msg
     *
     * @param buf raw encoded msg
     * @param len raw encoded len
     */
    protected Msg(Buffer buf, int len)
    {
        this.rawMsg = buf;
        this.length = len;
    }

    public int getLength()
    {
        return length;
    }

    /**
     * Decode and create message from a buffer
     *
     * @param buf buffer holding raw encoded message
     * @return    decoded message object
     *
     * @throws UnsupportedOperationException if message type is unknown
     */
    public static Msg create(Buffer buf)
    {
        int len  = buf.getVarInt();
        int type = buf.get();

        switch (type) {
            case ConnectReq.TYPE:
                return new ConnectReq(buf, len);
            case ConnectResp.TYPE:
                return new ConnectResp(buf, len);
            case ReqVoteReq.TYPE:
                return new ReqVoteReq(buf, len);
            case ReqVoteResp.TYPE:
                return new ReqVoteResp(buf, len);
            case AppendReq.TYPE:
                return new AppendReq(buf, len);
            case AppendResp.TYPE:
                return new AppendResp(buf, len);
            case JoinReq.TYPE:
                return new JoinReq(buf, len);
            case JoinResp.TYPE:
                return new JoinResp(buf, len);
            case ClientReq.TYPE:
                return new ClientReq(buf, len);
            case ClientResp.TYPE:
                return new ClientResp(buf, len);
            case PreVoteReq.TYPE:
                return new PreVoteReq(buf, len);
            case PreVoteResp.TYPE:
                return new PreVoteResp(buf, len);
            case InstallSnapshotReq.TYPE:
                return new InstallSnapshotReq(buf, len);
            case InstallSnapshotResp.TYPE:
                return new InstallSnapshotResp(buf, len);
            default:
                throw new UnsupportedOperationException("Unknown msg type : " + type);
        }
    }

    /**
     * If raw message is written to destination(socket buffers mostly)
     * @return true if all bytes written
     */
    public boolean written()
    {
        return rawMsg.remaining() == 0;
    }

    /**
     * Copy raw encoded message to destination buffer
     * @param buf destination buffer
     */
    public void writeTo(ByteBuffer buf)
    {
        rawMsg.get(buf);
    }

    /**
     * Write raw encoded message to socket buffer
     * @param sock outgoing sock
     */
    public void writeTo(Sock sock)
    {
        sock.copy(rawMsg.backend());
    }

    /**
     * Encode message
     */
    public abstract void encode();

    /**
     * Decode message
     */
    public abstract void decode();

    /**
     * Handle callback
     * @param handler message handler
     */
    public abstract void handle(MsgHandler handler);

    public abstract int getType();
}
