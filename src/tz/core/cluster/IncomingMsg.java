package tz.core.cluster;

import tz.base.poll.Event;
import tz.core.Connection;
import tz.core.msg.Msg;

public class IncomingMsg implements Event
{
    private Cluster cluster;
    private Connection conn;
    private Msg msg;

    public IncomingMsg(Cluster cluster, Connection conn, Msg msg)
    {
        this.cluster = cluster;
        this.conn    = conn;
        this.msg     = msg;
    }

    @Override
    public void onEvent()
    {
        cluster.handleIncomingMsg(conn, msg);
    }
}
