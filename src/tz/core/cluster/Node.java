package tz.core.cluster;

import tz.base.record.ClusterRecord;
import tz.base.record.NodeRecord;
import tz.base.record.TransportRecord;
import tz.core.Connection;
import tz.core.msg.*;
import tz.core.worker.IOWorker.IOWorker;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class Node implements MsgHandler
{
    enum Role
    {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }

    public enum Type
    {
        PEER,
        CLIENT
    }

    private Cluster cluster;
    private IOWorker worker;
    private Connection conn;

    private long nextIndex;
    private long matchIndex;
    private long sequence;
    private long acknowledge;

    private NodeRecord local;
    private NodeRecord remote;
    private int id;

    private Role role;
    private Type type;

    private Deque<Msg> incomings;
    private Deque<Msg> outgoings;

    private long inTimestamp;
    private long outTimestamp;
    private boolean connected;

    private int transport;

    public Node(Cluster cluster, Connection conn,
                NodeRecord local, NodeRecord remote, Type type)
    {
        this.cluster = cluster;
        this.worker  = cluster.getIoWorker();
        this.conn    = conn;
        this.local   = local;
        this.remote  = remote;
        this.type    = type;

        nextIndex    = 0;
        matchIndex   = 0;
        incomings    = new ArrayDeque<>();
        outgoings    = new ArrayDeque<>();
        role         = Role.FOLLOWER;
        transport    = 0;
        connected    = false;
        inTimestamp  = cluster.timestamp();
        outTimestamp = cluster.timestamp();

        if (conn != null) {
            connected = true;
            conn.setAttachment(this);
        }
    }

    public boolean isClient()
    {
        return type == Type.CLIENT;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public long getInTimestamp()
    {
        return inTimestamp;
    }

    public long getOutTimestamp()
    {
        return outTimestamp;
    }

    public NodeRecord getRemote()
    {
        return remote;
    }

    public long nextSequence()
    {
        return sequence++;
    }

    public long getSequence()
    {
        return sequence;
    }

    public void setSequence(long sequence)
    {
        this.sequence = sequence;
    }

    public long getAcknowledge()
    {
        return acknowledge;
    }

    public void setAcknowledge(long acknowledge)
    {
        this.acknowledge = acknowledge;
    }

    public void setMatchIndex(long matchIndex)
    {
        this.matchIndex = matchIndex;
    }

    public long getMatchIndex()
    {
        return matchIndex;
    }

    public long getNextIndex()
    {
        return nextIndex;
    }

    public void setNextIndex(long nextIndex)
    {
        this.nextIndex = nextIndex;
    }


    public String getName()
    {
        return remote.name;
    }

    public int getId()
    {
        return id;
    }

    public void addIncomingMsgs(Deque<Msg> msgs)
    {
        incomings.addAll(msgs);
    }

    public void handleMsgs()
    {
        inTimestamp = cluster.timestamp();
        for (Msg msg : incomings) {
            if (!connected) {
                break;
            }
            msg.handle(this);
        }

        incomings.clear();
    }

    public boolean isConnectionValid(Connection conn)
    {
        return this.conn == conn;
    }

    public void markConnected()
    {
        connected = true;
    }

    public void setConnection(Connection other)
    {
        if (this.conn != null) {
            worker.cancelConnection(conn);
        }

        conn = other;
        conn.setAttachment(this);
        connected = true;
    }

    public void connect()
    {
        List<TransportRecord> transports;

        transports = local.isSameGroup(remote) ? local.transports :
                                                 local.secureTransports;

        TransportRecord record = transports.get(transport++ % transports.size());

        conn = new Connection(worker, null, record);
        conn.setAttachment(this);
        worker.addConnection(conn);
    }

    public void disconnect()
    {
        cluster.getIoWorker().cancelConnection(conn);
        conn = null;
        connected = false;
    }

    public void sendConnectReq(String clusterName, String nodeName, boolean client)
    {
        worker.addOutgoingMsg(conn, new ConnectReq(clusterName, nodeName, client));
    }

    public void sendConnectResp(boolean success, ClusterRecord clusterRecord,
                                long sequence, long acknowledge)
    {
        worker.addOutgoingMsg(conn, new ConnectResp(success, clusterRecord,
                                                    sequence, acknowledge));
    }

    public void sendPreVoteReq(long term, long lastLogIndex, long lastLogTerm)
    {
        worker.addOutgoingMsg(conn, new PreVoteReq(term, lastLogIndex, lastLogTerm));
    }

    public void sendPreVoteResp(long term, long index, boolean granted)
    {
        worker.addOutgoingMsg(conn, new PreVoteResp(term, index, granted));
    }

    public void sendRequestVote(long term, long lastLogIndex,
                                long lastLogTerm, boolean leaderTransfer)
    {
        worker.addOutgoingMsg(conn, new ReqVoteReq(term, lastLogIndex,
                                                   lastLogTerm, leaderTransfer));
    }

    public void sendRequestVoteResp(long term, long index, boolean granted)
    {
        worker.addOutgoingMsg(conn, new ReqVoteResp(term, index, granted));
    }

    public void sendAppendReq(AppendReq appendEntries)
    {
        worker.addOutgoingMsg(conn, appendEntries);
    }

    public void sendAppendResp(long index, long term, boolean result)
    {
        worker.addOutgoingMsg(conn, new AppendResp(index, term, result));
    }

    public void sendClientResp(long sequence, boolean result, ByteBuffer data)
    {
        worker.addOutgoingMsg(conn, new ClientResp(sequence, result, data));
    }

    public void sendPublishReq(ClusterRecord record)
    {
        worker.addOutgoingMsg(conn, new PublishReq(record));
    }

    @Override
    public void handleConnectResp(ConnectResp msg)
    {
       cluster.handleConnectRespMsg(this, msg);
    }

    @Override
    public void handleJoinReq(JoinReq msg)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleJoinResp(JoinResp msg)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleAppendReq(AppendReq msg)
    {
        cluster.handleAppendReq(this, msg);
    }

    @Override
    public void handleAppendResp(AppendResp msg)
    {
        cluster.handleAppendResp(this, msg);
    }

    @Override
    public void handlePreVoteReq(PreVoteReq msg)
    {
        cluster.handlePreVoteReq(this, msg);
    }

    @Override
    public void handlePreVoteResp(PreVoteResp msg)
    {
        cluster.handlePreVoteResp(this, msg);
    }

    @Override
    public void handleReqVoteReq(ReqVoteReq msg)
    {
        cluster.handleReqVoteReq(this, msg);
    }

    @Override
    public void handleReqVoteResp(ReqVoteResp msg)
    {
        cluster.handleReqVoteResp(this, msg);
    }

    @Override
    public void handleClientReq(ClientReq msg)
    {
        cluster.handleClientRequest(this, msg);
    }

    @Override
    public void handleClientResp(ClientResp msg)
    {
        throw new UnsupportedOperationException();
    }
}
