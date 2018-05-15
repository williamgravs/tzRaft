package tz.core.worker.IOWorker;

import tz.base.transport.listener.Listener;
import tz.core.Connection;
import tz.core.msg.ConnectReq;
import tz.core.msg.Msg;

import java.util.Deque;

public interface IOOwner
{
    void sendListenerUpdate(Listener listener, boolean active);
    void handleListenerUpdate(Listener listener, boolean active);

    void sendConnectionUpdate(Connection conn, boolean active);
    void handleConnectionUpdate(Connection conn, boolean active);

    void sendIncomingConn(Connection conn, ConnectReq req);
    void handleIncomingConn(Connection conn, ConnectReq req);

    void sendIncomingMsg(Connection conn, Deque<Msg> msgs);
    void handleIncomingMsg(Connection conn, Deque<Msg> msgs);
}
