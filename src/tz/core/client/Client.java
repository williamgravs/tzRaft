package tz.core.client;


import tz.base.common.Buffer;
import tz.base.log.Log;
import tz.base.poll.Event;
import tz.base.poll.TimerEvent;
import tz.base.record.ClusterRecord;
import tz.base.record.NodeRecord;
import tz.base.record.TransportRecord;
import tz.base.transport.sock.Sock;
import tz.base.transport.sock.SockOwner;
import tz.base.transport.sock.TcpSock;
import tz.core.cluster.command.NoOPCommand;
import tz.core.cluster.state.State;
import tz.core.msg.*;
import tz.core.worker.Worker;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client
 *
 * Client is used for remote cluster connections
 *
 */
public class Client extends Worker implements SockOwner, MsgHandler
{
    enum ConnectionState
    {
        WILL_CONNECT,
        CONNECT_IN_PROGRESS,
        CONNECTED
    }

    private final String name;
    private final String group;
    private int clientId;

    private AtomicLong sequence;
    private long acknowledge;
    private long outAcknowledge;
    private long outTimestamp;
    private ClusterRecord clusterRecord;
    private NodeRecord local;
    private NodeRecord remote;
    private final TimerEvent tryConnect;
    private final TimerEvent pingTimer;
    private TimerEvent connectTimeout;
    private AtomicBoolean initialized;
    private boolean connected;

    private Sock sock;
    private int leaderIndex;
    private int transportIndex;
    private ConnectionState connectionState;
    private final ArrayDeque<Msg> incomings;
    private final ArrayDeque<Msg> outgoings;
    private final Buffer header;
    private Buffer raw;
    private final ClientListener listener;

    private Map<Long, FutureRequest> requests;
    private CompletableFuture<Boolean> tillConnect;
    private CompletableFuture<Boolean> tillDisconnect;
    private int onFlightCount;
    private final AtomicReference<CountDownLatch> available;

    private long receivedMsgCount;
    private long sentMsgCount;

    public Client(String clusterName, String name, String group,
                  ClientListener listener, String logLevel)
    {
        super(new Log(listener, logLevel), "Client-" + name, true);

        this.name          = name;
        this.group         = group;
        this.clusterRecord = new ClusterRecord(clusterName);
        this.local         = new NodeRecord(name, group);
        this.remote        = new NodeRecord(name, group);
        this.listener      = listener;

        header         = new Buffer(Msg.MIN_MSG_SIZE);
        incomings      = new ArrayDeque<>();
        outgoings      = new ArrayDeque<>();
        requests       = new TreeMap<>();
        tryConnect     = new TryConnect(this, true, 2000, timestamp());
        pingTimer      = new Ping(this, true, 2000, timestamp() + 2000);
        outTimestamp   = timestamp();
        sequence       = new AtomicLong(0);
        initialized    = new AtomicBoolean(false);
        tillConnect    = new CompletableFuture<>();
        tillDisconnect = new CompletableFuture<>();
        available      = new AtomicReference<>(new CountDownLatch(0));


        local.setClient();

        addTimer(pingTimer);
    }

    public void waitTillAvailable() throws InterruptedException
    {
        if (available.get().getCount() != 0) {
            available.get().await();
        }
    }

    @Override
    public void handleEvents(Deque<Event> events)
    {
        try {
            Event event;
            while ((event = events.poll()) != null) {
                event.onEvent();
            }
        }
        catch (Exception e) {
            logError(e);
        }

        flush();
    }

    @Override
    public void run()
    {
        if (sock != null) {
            disconnect(true);
        }

        sock = new TcpSock(this, null);
        sock.setOwner(this);
        connectionState = ConnectionState.WILL_CONNECT;
        startConnectTimer();

        addTimer(connectTimeout);

        super.run();
    }

    /**
     * Start client
     */
    public void connect(long millis) throws InterruptedException, ExecutionException
    {
        if (tillConnect.isDone()) {
            throw new IllegalStateException("Client is already connected");
        }

        connectTimeout = new ConnectTimeout(this, false, millis,
                                            timestamp() + millis);
        start();

        boolean status = tillConnect.get();
        if (!status) {
            throw new IllegalStateException("Cannot connect to cluster");
        }
    }

    public void disconnect() throws ExecutionException, InterruptedException
    {
        addEvent(new Stop(this));
        boolean status = tillDisconnect.get();
        if (!status) {
            throw new IllegalStateException("Disconnect failed");
        }
    }


    public void handleConnectTimeout()
    {
        handleStop();
    }

    public void handleStop()
    {
        disconnect(true);
        for (FutureRequest request : requests.values()) {
            request.finishExceptionally(new IllegalStateException("Cannot connect to cluster"));
        }
        removeTimer(connectTimeout);

        requests.clear();
        tillConnect.complete(false);
        tillConnect = new CompletableFuture<>();
        tillDisconnect.complete(true);
        tillDisconnect = new CompletableFuture<>();
        stop();
    }

    /**
     * Start connect timer
     */
    private void startConnectTimer()
    {
        addTimer(tryConnect);
    }

    /**
     * Stop connect timer
     */
    private void stopConnectTimer()
    {
        removeTimer(tryConnect);
    }

    /**
     * Add transport clusterRecord
     * @param record new transport clusterRecord
     */
    public void addTransport(TransportRecord record)
    {
        remote.addTransport(record);
    }

    /**
     * Get next transport clusterRecord
     *
     * @return next clusterRecord to try to connect based on group id's
     */
    private TransportRecord getNextTransport()
    {
        if (group.equals(remote.group)) {
            return remote.transports.get(transportIndex++ %
                                         remote.transports.size());
        }
        else {
            return remote.secureTransports.get(transportIndex++ %
                                               remote.secureTransports.size());
        }
    }



    /**
     * Handle try connect callback
     */
    public void handleTryConnect()
    {
        if (connectionState == ConnectionState.CONNECT_IN_PROGRESS) {
            sock.close();
            this.sock = new TcpSock(this, null);
            connectionState = ConnectionState.WILL_CONNECT;
        }

        if (connectionState == ConnectionState.WILL_CONNECT) {
            connectionState = ConnectionState.CONNECT_IN_PROGRESS;

            TransportRecord record = getNextTransport();
            logInfo("trying to connect to", record.hostName, ":", record.port);

            if (sock.connect(record.hostName, record.port)) {
                handleConnectEvent(sock);
            }
            else {
                register(sock, SelectionKey.OP_CONNECT);
            }
        }
    }

    /**
     * Flush outgoing messages
     */
    private void flush()
    {
        while (!outgoings.isEmpty()) {
            Msg msg = outgoings.element();
            msg.encode();
            msg.writeTo(sock);

            if (msg.written()) {
                logInfo("Message is sent to ", this, " ", msg);
                outTimestamp = timestamp();
                outgoings.pop();
                sentMsgCount++;
            }

            if (!sock.hasRemaining() || outgoings.isEmpty()) {
                if (!sock.sendAll()) {
                    return;
                }
            }
        }
    }


    public void sendPing()
    {
        if (!requests.isEmpty()) {
            return;
        }

        final long timeDiff = timestamp() - outTimestamp;
        final long ackDiff  = acknowledge - outAcknowledge;
        if (connected && timeDiff > 2000 && ackDiff > 1) {
            outgoings.add(new ClientReq(State.INTERNAL_ID,
                                        sequence.getAndIncrement(),
                                        acknowledge,
                                        new NoOPCommand().getRaw()));
            outAcknowledge = acknowledge;
            flush();
        }
    }

    /**
     * Send a request
     * @param data     raw application command
     * @return         ClientReq clusterRecord holding the request
     */
    public FutureRequest sendRequest(ByteBuffer data) throws InterruptedException
    {
        if (!initialized.get()) {
            throw new IllegalStateException("Client is not connected yet");
        }

        waitTillAvailable();

        long currentSequence = sequence.getAndIncrement();
        ClientReq reqMsg = new ClientReq(State.USER_ID, currentSequence,
                                         acknowledge, data);
        reqMsg.encode();

        FutureRequest req = new FutureRequest(reqMsg, currentSequence);
        addEvent(new SendRequest(this, req));

        return req;
    }

    /**
     * Send request callback
     * @param req client request
     */
    public void handleSendRequestEvent(FutureRequest req)
    {
        onFlightCount++;
        if (onFlightCount > 20000) {
            if (available.get().getCount() == 0) {
                available.set(new CountDownLatch(1));
            }
        }

        requests.put(req.getSequence(), req);
        if (connected) {
            outgoings.add(req.getRequest());
            outAcknowledge = req.getAcknowledge();
        }
    }

    /**
     * Connection established, send connect request to leader
     */
    private void sendConnectReq()
    {
        outgoings.add(new ConnectReq(clusterRecord.getName(), name, true));
        flush();
    }


    /**
     * Handle shutdown
     *
     * @param e    Exception that caused this call
     * @param sock Sock obj about to shutdown
     */
    @Override
    public void handleShutdown(Exception e, Sock sock)
    {
        disconnect(true);
        this.sock = new TcpSock(this, null);
        connectionState = ConnectionState.WILL_CONNECT;

        startConnectTimer();
    }

    /**
     * Connection established callback
     *
     * @param sock Connected socket
     */
    @Override
    public void handleConnectEvent(Sock sock)
    {
        try {
            if (sock.finishConnect()) {
                connectionState = ConnectionState.CONNECTED;
                sock.setOp(SelectionKey.OP_READ);
                stopConnectTimer();
                sendConnectReq();
            }
        }
        catch (Exception e) {
            logWarn("Cannot connect to ", this);
        }
    }

    /**
     * Disconnect
     * @param error true if an error cause the disconnection
     */
    private void disconnect(boolean error)
    {
        if (sock != null) {
            if (error) {
                logInfo("Disconnected : ", sock);
            }

            stopConnectTimer();
            connectionState = ConnectionState.WILL_CONNECT;
            sock.close();
            sock = null;
            incomings.clear();
            outgoings.clear();
            connected = false;
            connectTimeout = new ConnectTimeout(this, false, connectTimeout.interval,
                                                timestamp() + connectTimeout.interval);
            addTimer(connectTimeout);
        }
    }

    /**
     * Decode message from raw buffer
     * @param buf buffer holding raw message
     * @return    decoded message as Msg object
     */
    public Msg decode(ByteBuffer buf)
    {
        if (raw == null) {
            header.put(buf);
            if (header.remaining() != 0) {
                return null;
            }

            header.flip();
            raw = new Buffer(header.getVarInt() + header.position());
            header.rewind();
            raw.put(header);
            header.clear();
        }

        raw.put(buf);

        if (raw.remaining() == 0) {
            raw.flip();
            Buffer msg = raw;
            receivedMsgCount++;
            raw = null;
            return Msg.create(msg);
        }

        return null;
    }

    /**
     * Handle decoded messages
     */
    private void handleMsgs()
    {
        for (Msg msg : incomings) {
            logInfo("Received msg for : ", this, " ", msg);
            msg.handle(this);
        }

        incomings.clear();
    }

    /**
     * Sock is ready to be read
     * @param sock Sock has unread data
     */
    @Override
    public void handleReadEvent(Sock sock)
    {
        boolean willDisconnect = false;

            int read = 1;
            while (read > 0) {
                read = sock.recv();
                if (read == -1) {
                    willDisconnect = true;
                }

                ByteBuffer buf = sock.getRecvBuf();
                buf.flip();

                while (buf.hasRemaining()) {
                    Msg msg = decode(buf);
                    if (msg == null) {
                        break;
                    }

                    incomings.add(msg);
                }

                handleMsgs();

                if (willDisconnect) {
                    disconnect(true);
                }
            }
    }

    /**
     * Sock is ready to be written more data
     * @param sock Sock has space in its write buffer
     */
    @Override
    public void handleWriteEvent(Sock sock)
    {
        sock.setOp(SelectionKey.OP_READ);
        if (!sock.sendAll()) {
            return;
        }
        flush();
    }

    /**
     * Handle connect resp callback
     *
     * @param msg ConnectResp message
     */
    @Override
    public void handleConnectResp(ConnectResp msg)
    {
        clusterRecord = msg.getClusterRecord();

        remote.clearTransports();
        for (NodeRecord record : clusterRecord.peers) {
            if (record.isLeader()) {
                remote.inheritTransports(true, record);
            }
            else if (record.isPeer()) {
                remote.inheritTransports(false, record);
            }
        }

        if (!msg.isSuccessful()) {
            disconnect(true);
            this.sock = new TcpSock(this, null);
            connectionState = ConnectionState.WILL_CONNECT;
            startConnectTimer();
            return;
        }

        removeTimer(connectTimeout);
        sequence.set(msg.getSequence() + 1);
        acknowledge = msg.getAcknowledge();
        connected = true;
        initialized.set(true);
        listener.connectionState(true);

        flush();
        tillConnect.complete(true);

        for (FutureRequest request : requests.values()) {
            ClientReq req = request.getRequest();
            req.rewind();
            outgoings.add(request.getRequest());
            outAcknowledge = req.getAcknowledge();
        }
    }

    /**
     * Handle client resp callback
     * @param msg ClientResp message
     */
    @Override
    public void handleClientResp(ClientResp msg)
    {
        acknowledge = msg.getSequence();
        FutureRequest req = requests.remove(msg.getSequence());
        if (req != null) {
            req.finish(msg.getData());
            onFlightCount--;
            if (onFlightCount < 20000) {
                available.get().countDown();
            }
        }
    }

    @Override
    public void handlePublishReq(PublishReq msg)
    {
        clusterRecord = msg.getRecord();
        listener.configChange(clusterRecord);
    }
}
