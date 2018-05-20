package tz.core.worker.IOWorker;

import tz.base.log.Log;
import tz.base.poll.Event;
import tz.base.record.TransportRecord;
import tz.base.transport.TlsConfig;
import tz.base.transport.listener.Listener;
import tz.base.transport.listener.ListenerOwner;
import tz.base.transport.sock.Sock;
import tz.core.Connection;
import tz.core.cluster.Node;
import tz.core.msg.ClientReq;
import tz.core.msg.ClientResp;
import tz.core.msg.ConnectReq;
import tz.core.msg.Msg;
import tz.core.worker.Worker;

import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class IOWorker extends Worker implements ListenerOwner
{
    private IOOwner owner;
    private final List<Listener> listeners;
    private final List<Connection> connections;
    private final Set<Connection> readyConnections;
    private int count;
    private int outCount;

    public IOWorker(IOOwner owner, Log log, String name)
    {
        super(log, name, true);

        this.owner       = owner;
        listeners        = new ArrayList<>();
        connections      = new ArrayList<>();
        readyConnections = new HashSet<>();
    }

    public void addConnection(Connection conn)
    {
        addEvent(new AddConnection(this, conn));
    }

    public void handleAddConnection(Connection conn)
    {
        connections.add(conn);
        conn.start(this);
    }

    public void cancelConnection(Connection conn)
    {
        addEvent(new CancelConnection(this, conn));
    }

    public void handleCancelConnection(Connection conn)
    {
        conn.disconnect(false);
        connections.remove(conn);
        readyConnections.remove(conn);
    }

    public void addEndpoint(TransportRecord transportRecord)
    {
        addEvent(new AddEndpoint(this, transportRecord));
    }

    public void handleAddEndpoint(TransportRecord transportRecord)
    {
        for (Listener listener : listeners) {
            if (listener.getRecord().equals(transportRecord)) {
                return;
            }
        }

        listeners.add(Listener.create(this, poll.getSelector(), transportRecord));
        logInfo("Listening on : ", transportRecord);
    }

    public void cancelEndpoint(TransportRecord transportRecord)
    {
        poll.addEvent(new CancelEndpoint(this, transportRecord));
    }

    public void handleCancelEndpoint(TransportRecord transportRecord)
    {
        for (Listener listener : listeners) {
            if (listener.getRecord().equals(transportRecord)) {
                listeners.remove(listener);
                listener.close();
                break;
            }
        }
    }

    public void addOutgoingMsg(Connection conn, Msg msg)
    {
        Deque<Msg> msgs = new ArrayDeque<>();
        msgs.add(msg);
        count++;

        poll.addEvent(new OutgoingMsg(this, conn, msgs));
    }

    public void handleOutgoingMsg(Connection conn, Deque<Msg> msgs)
    {
        outCount++;
        //System.out.println("Out count : " + outCount);
        if (connections.contains(conn)) {
            conn.addMsgs(msgs);
            readyConnections.add(conn);
        }
        else {
            System.out.println("Weri2d");
        }
    }

    @Override
    public void handleShutdown(Exception e, Listener listener)
    {
        logError(e, "Listener failed : ", listener);
        listeners.remove(listener);
        owner.sendListenerUpdate(listener, false);
    }

    @Override
    public void handleAcceptEvent(Listener listener)
    {
        Sock sock;

        try {
            sock = listener.accept();
        }
        catch (Exception e) {
            logError(e);
            return;
        }

        try {
            logInfo(listener.toString(), " accepted : ", sock);
            sock.register(poll.getSelector(), SelectionKey.OP_READ);
            sock.handshake();
        }
        catch (Exception e) {
            sock.close();
            logInfo("Disconnected : ", sock.toString());

            return;
        }

        connections.add(new Connection(this, sock, null));
    }

    @Override
    public TlsConfig getTlsConfig()
    {
        return null;
    }

    public void handleConnectionUpdate(Connection conn, Connection.Status status)
    {
        if (status == Connection.Status.DISCONNECTED ||
            status == Connection.Status.OUTGOING_FAILED) {
            connections.remove(conn);
            readyConnections.remove(conn);
        }

        logInfo("Connection ", conn, " status ", status);
        owner.sendConnectionUpdate(conn, status);
    }


    public void handleIncomingMsg(Connection conn, Msg msg)
    {
        owner.sendIncomingMsg(conn, msg);
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

    private void flush()
    {
        for (Connection conn : readyConnections) {
            conn.flush();
        }

        readyConnections.clear();
    }
}
