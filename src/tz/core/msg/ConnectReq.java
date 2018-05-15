package tz.core.msg;

import tz.base.common.Buffer;
import tz.base.common.Util;


/**
 * ConnectReq message
 * Peer to peer, client to leader
 */
public class ConnectReq extends Msg
{
    public static final int TYPE = 0x02;

    private String clusterName;
    private String name;
    private boolean client;

    /**
     * Create new ConnectReq message
     */
    public ConnectReq(String clusterName, String name, boolean client)
    {
        this.clusterName = clusterName;
        this.name        = name;
        this.client      = client;
    }

    /**
     * Create new ConnectReq message
     *
     * @param buf raw encoded message
     * @param len raw encoded length
     */
    public ConnectReq(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    public boolean isClient()
    {
        return client;
    }

    public String getClusterName()
    {
        return clusterName;
    }

    public String getName()
    {
        return name;
    }

    /**
     * Encode message
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            length = Encoder.byteLen(ConnectReq.TYPE) +
                     Encoder.stringLen(clusterName) +
                     Encoder.stringLen(name) +
                     Encoder.booleanLen(client);


            if (rawMsg == null) {
                rawMsg = new Buffer(length + Encoder.varIntLen(length));
            }

            rawMsg.clear();
            rawMsg.putVarInt(length);
            rawMsg.put(ConnectReq.TYPE);
            rawMsg.putString(clusterName);
            rawMsg.putString(name);
            rawMsg.putBoolean(client);

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
        clusterName = rawMsg.getString();
        name        = rawMsg.getString();
        client = rawMsg.getBoolean();

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
        handler.handleConnectReq(this);
    }

    /**
     * toString
     * @return string representation of the message
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(" [[ConnectReq][")
               .append("Cluster Name : ").append(clusterName).append(", ")
               .append("Name: "         ).append(name)       .append(", ")
               .append("Is client : "   ).append(client)     .append("]]");

        return builder.toString();
    }

    @Override
    public int getType()
    {
        return TYPE;
    }


}
