package tz.base.record;

import tz.base.common.Buffer;
import tz.base.common.Util;
import tz.core.msg.Encoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Cluster record
 */
public class ClusterRecord
{
    public String name;
    public List<NodeRecord> peers;

    /**
     * Create new ClusterRecord
     *
     * @param name cluster name
     */
    public ClusterRecord(String name)
    {
        this.name    = name;
        this.peers   = new ArrayList<>();
    }

    /**
     * Create new ClusterRecord
     *
     * @param buf Raw encoded clusterRecord
     */
    public ClusterRecord(Buffer buf)
    {
        peers   = new ArrayList<>();

        decode(buf);
    }

    public String getName()
    {
        return name;
    }

    /**
     * Get record by name
     *
     * @param name Record name
     * @return     Record of the node
     */
    public NodeRecord getRecord(String name)
    {
        for (NodeRecord record : peers) {
            if (record.getName().equals(name)) {
                return record;
            }
        }

        return null;
    }

    /**
     * Remove record
     *
     * @param name Record name
     *
     * @return     Removed record
     */
    public NodeRecord removeRecord(String name)
    {
        for (int i = 0; i < peers.size(); i++) {
            NodeRecord record = peers.get(i);
            if (record.getName().equals(name)) {
                return peers.remove(i);
            }
        }

        return null;
    }

    /**
     * Get encoded len
     *
     * @return Encoded length of the record
     */
    public int rawLen()
    {
        int len = Encoder.stringLen(name);

        len += Encoder.varIntLen(peers.size());
        for (NodeRecord node : peers) {
            len += node.rawLen();
        }

        return len;
    }

    /**
     * Encode the record
     *
     * @param buf Destination buffer for encoded record
     */
    public void encode(Buffer buf)
    {
        buf.putString(name);

        buf.putVarInt(peers.size());
        for (NodeRecord node : peers) {
            node.encode(buf);
        }
    }

    /**
     * Decode the record
     *
     * @param buf Raw encoded record holder buffer
     */
    public void decode(Buffer buf)
    {
        name = buf.getString();

        int len = buf.getVarInt();
        for (int i = 0; i < len; i++) {
            NodeRecord node = new NodeRecord(buf);
            peers.add(node);
        }
    }

    /**
     * Add node to cluster peers
     *
     * @param node New node
     */
    public void addNode(NodeRecord node)
    {
        if (!peers.contains(node)) {
            peers.add(node);
        }
    }

    /**
     * Equals
     *
     * @param obj Compare obj
     * @return    True if this object equals to 'obj'
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }

        if (!ClusterRecord.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final ClusterRecord other = (ClusterRecord) obj;

        return name.equals(other.name);
    }

    /**
     * toString
     *
     * @return String representation of the cluster record
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(1024);

        builder.append("[Cluster : ").append(name).append("] ");
        builder.append("[Peers : ");
        for (NodeRecord record : peers) {
            builder.append("(").append(record).append(")");
        }
        builder.append("] ");

        return builder.toString();
    }
}
