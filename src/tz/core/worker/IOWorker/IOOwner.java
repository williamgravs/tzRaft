package tz.core.worker.IOWorker;

import tz.base.transport.listener.Listener;
import tz.core.Connection;
import tz.core.msg.Msg;

public interface IOOwner
{
    void sendListenerUpdate(Listener listener, boolean active);
    void handleListenerUpdate(Listener listener, boolean active);

    void sendConnectionUpdate(Connection conn, Connection.Status status);
    void handleConnectionUpdate(Connection conn, Connection.Status status);

    void sendIncomingMsg(Connection conn, Msg msg);
    void handleIncomingMsg(Connection conn, Msg msg);
}
