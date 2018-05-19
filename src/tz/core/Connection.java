package tz.core;

import tz.base.common.Buffer;
import tz.base.common.Util;
import tz.base.record.TransportRecord;
import tz.base.transport.sock.Sock;
import tz.base.transport.sock.SockOwner;
import tz.base.transport.sock.TcpSock;
import tz.core.msg.*;
import tz.core.worker.IOWorker.IOWorker;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.PrimitiveIterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Connection
 *
 * Handles tcp and tls connections
 */
public class Connection implements SockOwner, MsgHandler
{
    public enum Status
    {
        INCOMING,
        OUTGOING_SUCCEED,
        OUTGOING_FAILED,
        DISCONNECTED
    }

    private IOWorker worker;
    private Sock sock;
    private TransportRecord record;

    private Deque<Msg> incomings;
    private Deque<Msg> outgoings;

    private final Buffer header;
    private Buffer raw;

    private Object attachment;

    private long receivedMsgCount;
    private long sentMsgCount;

    /**
     * Create a new Connection
     * @param worker IO worker
     * @param sock   Sock
     */
    public Connection(IOWorker worker, Sock sock, TransportRecord record)
    {
        this.worker  = worker;
        this.sock    = sock;
        this.record  = record;

        header       = new Buffer(Msg.MIN_MSG_SIZE);
        incomings    = new ArrayDeque<>();
        outgoings    = new ArrayDeque<>();

        if (this.sock != null) {
            this.sock.setOwner(this);
        }
    }

    public Object getAttachment()
    {
        return attachment;
    }

    public void setAttachment(Object attachment)
    {
        this.attachment = attachment;
    }

    public boolean hasAttachment()
    {
        return attachment != null;
    }

    /**
     * Get string representation
     * @return string representation
     */
    @Override
    public String toString()
    {
        if (sock.getRemoteAddress() != null) {
            return sock.getRemoteAddress();
        }
        else {
            return record.toString();
        }
    }

    /**
     * Get IO worker
     * @return IO worker
     */
    public IOWorker getWorker()
    {
        return worker;
    }

    /**
     * Remove this connection from worker's loop(selector)
     */
    public void unregister()
    {
        sock.cancel();
    }

    /**
     * Set worker
     * @param worker new worker
     */
    public void setWorker(IOWorker worker)
    {
        this.worker = worker;
    }

    /**
     * Start this connection
     *
     * @param worker new worker
     */
    public void start(IOWorker worker)
    {
        setWorker(worker);

        if (sock == null) {
            sock = new TcpSock(this, null);
            tryConnect();
        }
        else {
            worker.register(sock, SelectionKey.OP_READ);
        }

        sock.setOwner(this);

    }

    /**
     * Disconnect
     *
     * @param error true if an error occured
     */
    public void disconnect(boolean error)
    {
        if (error) {
            worker.handleConnectionUpdate(this, Status.DISCONNECTED);
        }

        if (sock != null) {
            sock.close();
            sock = null;
        }
    }

    /**
     * Decode received messages
     * @param buf received data from socket
     * @return    decoded message
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
     * Flush outgoing messages
     * This call provides efficient batching mechanism
     */
    public void flush()
    {
        Msg msg;
        while ((msg = outgoings.peek()) != null) {
            msg.encode();
            msg.writeTo(sock);
            if (msg.written()) {
                worker.logInfo("Msg sent : ", msg, " to ", this);
                sentMsgCount++;
                outgoings.pop();
            }

            if (!sock.hasRemaining() || outgoings.isEmpty()) {
                if (!sock.sendAll()) {
                    return;
                }
            }
        }
    }


    public void addMsgs(Deque<Msg> msgs)
    {
        outgoings.addAll(msgs);
    }

    /**
     * TryConnect timeout occured
     */
    public void tryConnect()
    {
        worker.logInfo("Trying to connect to ", record.hostName, ":", record.port);

        try {
            if (sock.connect(record.hostName, record.port)) {
                handleConnectEvent(sock);
            }
            else {
                worker.register(sock, SelectionKey.OP_CONNECT);
            }
        }
        catch (UncheckedIOException e) {
            worker.handleConnectionUpdate(this, Status.OUTGOING_FAILED);
        }
    }


    /**
     * Shutdown required callback
     *
     * @param e    Exception that caused this call
     * @param sock Sock obj about to shutdown
     */
    @Override
    public void handleShutdown(Exception e, Sock sock)
    {
        worker.logError(e, sock);
        disconnect(true);
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
                sock.setOp(SelectionKey.OP_READ);
                worker.handleConnectionUpdate(this, Status.OUTGOING_SUCCEED);
            }
        }
        catch (Exception e) {
            worker.handleConnectionUpdate(this, Status.OUTGOING_FAILED);
        }
    }

    /**
     * Socket is ready to be read callback
     *
     * @param sock Sock has unread data
     */
    @Override
    public void handleReadEvent(Sock sock)
    {
        try {
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

                    worker.logInfo("Msg recv : ", msg, " from ", this);
                    worker.handleIncomingMsg(this, msg);
                }

                if (willDisconnect) {
                    disconnect(true);
                }
            }
        }
        catch (Exception e) {
            worker.logError(e);
            disconnect(true);
        }
    }

    /**
     * handleWriteEvent
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

    @Override
    public void handleConnectReq(ConnectReq msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handleConnectResp(ConnectResp msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handleJoinReq(JoinReq msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handleJoinResp(JoinResp msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handleAppendReq(AppendReq msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handleAppendResp(AppendResp msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handlePreVoteReq(PreVoteReq msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handlePreVoteResp(PreVoteResp msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handleReqVoteReq(ReqVoteReq msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handleReqVoteResp(ReqVoteResp msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handleClientReq(ClientReq msg)
    {
        incomings.add(msg);
    }

    @Override
    public void handleClientResp(ClientResp msg)
    {
        incomings.add(msg);
    }
}
