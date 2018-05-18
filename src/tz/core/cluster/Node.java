package tz.core.cluster;

import tz.base.poll.TimerEvent;
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
import java.util.Random;

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

    enum State
    {
        CONNECTED,
        CONNECTION_IN_PROGRESS,
        DISCONNECTED,
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
    private State connectionState;

    private int transport;
    private final TimerEvent reconnectTimer;

    public Node(Cluster cluster, Connection conn,
                NodeRecord local, NodeRecord remote, Type type)
    {
        this.cluster    = cluster;
        this.worker     = cluster.getIoWorker();
        this.conn       = conn;
        this.local      = local;
        this.remote     = remote;
        this.type       = type;

        nextIndex       = 0;
        matchIndex      = 0;
        incomings       = new ArrayDeque<>();
        outgoings       = new ArrayDeque<>();
        role            = Role.FOLLOWER;
        transport       = 0;
        connectionState = State.DISCONNECTED;
        inTimestamp     = cluster.timestamp();
        outTimestamp    = cluster.timestamp();

        reconnectTimer  = new ReconnectTimer(cluster, this, false,
                                             new Random().nextInt(2000), 0);

        if (conn != null) {
            connectionState = State.CONNECTED;
            conn.setAttachment(this);
        }
    }

    public void reconnect()
    {
        conn = null;
        reconnectTimer.timeout = cluster.timestamp() + reconnectTimer.interval;
        cluster.addTimer(reconnectTimer);
    }

    public void stopReconnectTimer()
    {
        cluster.removeTimer(reconnectTimer);
    }

    public boolean isClient()
    {
        return type == Type.CLIENT;
    }

    public boolean isPeer()
    {
        return type == Type.PEER;
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

    public void addIncomingMsg(Msg msg)
    {
        incomings.add(msg);
    }

    public void handleMsgs()
    {
        inTimestamp = cluster.timestamp();
        for (Msg msg : incomings) {
            if (connectionState != State.CONNECTED) {
                break;
            }
            msg.handle(this);
        }

        incomings.clear();
    }

    public boolean isDisconnected()
    {
        return connectionState == State.DISCONNECTED;
    }

    public boolean isConnected()
    {
        return connectionState == State.CONNECTED;
    }

    public boolean isConnectionValid(Connection conn)
    {
        return this.conn == conn;
    }

    public boolean isConnectionInProgress()
    {
        return connectionState == State.CONNECTION_IN_PROGRESS;
    }

    public boolean isValid(Connection conn)
    {
        return this.conn == conn;
    }

    public void setConnected()
    {
        connectionState = State.CONNECTED;
        stopReconnectTimer();
    }

    public void setConnection(Connection other)
    {
        if (conn != null && conn != other) {
            worker.cancelConnection(conn);
        }

        conn = other;
        conn.setAttachment(this);
        setConnected();
    }

    public void connect()
    {
        List<TransportRecord> transports = remote.transports;
        TransportRecord record = transports.get(transport++ % transports.size());

        conn = new Connection(worker, null, record);
        conn.setAttachment(this);
        worker.addConnection(conn);
        connectionState = State.CONNECTION_IN_PROGRESS;
    }

    public void disconnect()
    {
        cluster.getIoWorker().cancelConnection(conn);
        conn = null;
        connectionState = State.DISCONNECTED;
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

    public void sendReqVoteReq(long term, long lastLogIndex,
                               long lastLogTerm, boolean leaderTransfer)
    {
        worker.addOutgoingMsg(conn, new ReqVoteReq(term, lastLogIndex,
                                                   lastLogTerm, leaderTransfer));
    }

    public void sendReqVoteResp(long term, long index, boolean granted)
    {
        worker.addOutgoingMsg(conn, new ReqVoteResp(term, index, granted));
    }

    public void sendAppendReq(AppendReq appendReq)
    {
        worker.addOutgoingMsg(conn, appendReq);
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
        cluster.handleClientReq(this, msg);
    }

    @Override
    public void handleClientResp(ClientResp msg)
    {
        throw new UnsupportedOperationException();
    }
}
