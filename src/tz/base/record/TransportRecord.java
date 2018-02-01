package tz.base.record;

import tz.base.common.Buffer;
import tz.core.msg.Encoder;

/**
 * Transport record, used for peers or clients
 */
public final class TransportRecord
{
    public String protocol;
    public String hostName;
    public int port;


    /**
     * Create new TransportRecord
     *
     * @param protocol Protocol string, tcp or tls
     * @param hostName Hostname
     * @param port     Port
     */
    public TransportRecord(String protocol, String hostName, int port)
    {
        this.protocol  = protocol;
        this.hostName  = hostName;
        this.port      = port;
    }


    /**
     * Create new TransportRecord
     * @param buf Buffer holding raw transport record
     */
    public TransportRecord(Buffer buf)
    {
        decode(buf);
    }

    /**
     * Get encoded len
     * @return Encoded length of the record
     */
    public int encodedLen()
    {
        int len = Encoder.stringLen(protocol) +
                  Encoder.stringLen(hostName) +
                  Encoder.varIntLen(port);

        return len;
    }

    /**
     * Encode the record
     * @param buf Destination buffer
     */
    public void encode(Buffer buf)
    {
        buf.putString(protocol);
        buf.putString(hostName);
        buf.putVarInt(port);
    }

    /**
     * Decode record
     * @param buf Source raw encoded record holder
     */
    public void decode(Buffer buf)
    {
        protocol = buf.getString();
        hostName = buf.getString();
        port     = buf.getVarInt();
    }

    /**
     * to string
     * @return String representation of the record
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append(protocol)
               .append("://")
               .append(hostName)
               .append(":")
               .append(port);

        return builder.toString();
    }

    /**
     * Compare two records
     * @param obj Another record
     * @return    True if both equals
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }

        if (!TransportRecord.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final TransportRecord other = (TransportRecord) obj;

        return (protocol.equals(other.protocol) &&
                hostName.equals(other.hostName) &&
                port == other.port);
    }
}
