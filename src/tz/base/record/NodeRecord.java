package tz.base.record;

import tz.base.common.Buffer;
import tz.core.msg.Encoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Node record, can be peer or clients
 */
public class NodeRecord
{
    public enum Role
    {
        LEADER,
        PEER,
        PROMOTABLE,
        BACKUP,
        CLIENT;

        static final Role[] values = Role.values();
    }

    //Unique name among peers and clients
    public String name;

    /**
     * We use group id's to decide if we use TCP or TLS, same group id peers
     * are mostly in the same data center
     */
    public String group;

    //Whether node is connected to cluster
    public boolean connected;

    //Role of the node
    public Role role;

    //Tcp & Tls transports of the node
    public List<TransportRecord> transports;


    /**
     * Create new NodeRecord
     *
     * @param name  node name
     * @param group group id
     */
    public NodeRecord(String name, String group)
    {
        this.name              = name;
        this.group             = group;
        this.transports        = new ArrayList<>();
        this.role              = Role.PEER;
    }

    /**
     * Create new NodeRecord
     *
     * @param buf raw encoded node record
     */
    public NodeRecord(Buffer buf)
    {
        transports = new ArrayList<>();

        decode(buf);
    }

    /**
     * Clear records
     */
    public void clearTransports()
    {
        transports.clear();
    }


    /**
     * Get other record's transports into this
     *
     * @param head  Should we place transports at the head of the list
     * @param other NodeRecord to collect its transports
     */
    public void inheritTransports(boolean head, NodeRecord other)
    {
        transports.addAll(head ? 0 : transports.size(), other.transports);
    }

    /**
     * Set connected
     * @param connected True if connected
     */
    public void setConnected(boolean connected)
    {
        this.connected = connected;
    }

    /**
     * Set role to PEER
     */
    public void setPeer()
    {
        this.role = Role.PEER;
    }

    /**
     * Is leader
     * @return True if this node is PEER
     */
    public boolean isPeer()
    {
        return role == Role.PEER;
    }

    /**
     * Set role to LEADER
     */
    public void setLeader()
    {
        this.role = Role.LEADER;
    }

    /**
     * Is leader
     * @return True if this node is LEADER
     */
    public boolean isLeader()
    {
        return role == Role.LEADER;
    }

    /**
     * Set role to CLIENT
     */
    public void setClient()
    {
        this.role = Role.CLIENT;
    }

    /**
     * Is leader
     * @return True if this node is CLIENT
     */
    public boolean isClient()
    {
        return this.role == Role.CLIENT;
    }

    /**
     * Get name of the record
     * @return Name of the record
     */
    public String getName()
    {
        return name;
    }

    /**
     * Clear transports
     */
    public void clear()
    {
        transports.clear();
    }

    /**
     * Get encoded len
     * @return encoded length of the record
     */
    public int rawLen()
    {
        int len = Encoder.stringLen(name) +
                  Encoder.stringLen(group) +
                  Encoder.booleanLen(connected) +
                  Encoder.varIntLen(role.ordinal());


        len += Encoder.varIntLen(transports.size());
        for (TransportRecord transport : transports) {
            len += transport.encodedLen();
        }

        return len;
    }

    /**
     * Encode record to destination buffer
     * @param buf destination buffer
     */
    public void encode(Buffer buf)
    {
        buf.putString(name);
        buf.putString(group);
        buf.putBoolean(connected);
        buf.putVarInt(role.ordinal());


        buf.putVarInt(transports.size());
        for (TransportRecord transport : transports) {
            transport.encode(buf);
        }
    }

    /**
     * Decode node record
     * @param buf buffer holding raw node record
     */
    public void decode(Buffer buf)
    {
        name      = buf.getString();
        group     = buf.getString();
        connected = buf.getBoolean();
        role      = Role.values[buf.getVarInt()];


        int len = buf.getVarInt();
        for (int i = 0; i < len; i++) {
            transports.add(new TransportRecord(buf));
        }
    }

    /**
     * Add transport to node record
     * @param record new transport record
     * @throws UnsupportedOperationException on an unsupported protocol
     */
    public void addTransport(TransportRecord record)
    {
        if (!transports.contains(record)) {
            transports.add(record);
        }
    }

    /**
     * Equals
     * @param obj obj to compare with 'this'
     * @return    true if obj equals to 'this'
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }

        if (!NodeRecord.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final NodeRecord other = (NodeRecord) obj;

        return name.equals(other.name);
    }

    /**
     * Get string representation
     * @return string representation of the record
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(1024);

        builder.append(" Name : ").append(name).append(",");
        builder.append(" Group :").append(group).append(",");
        builder.append(" Connected : ").append(connected).append(",");
        builder.append(" Role : ").append(role);

        return builder.toString();
    }
}
