package tz.core.cluster;

import tz.base.poll.Event;
import tz.core.Connection;
import tz.core.msg.Msg;

import java.util.Deque;

public class IncomingMsg implements Event
{
    private static int count;

    private Cluster cluster;
    private Connection conn;
    private Deque<Msg> msgs;

    public IncomingMsg(Cluster cluster, Connection conn, Deque<Msg> msgs)
    {
        this.cluster = cluster;
        this.conn    = conn;
        this.msgs    = msgs;
    }

    @Override
    public void onEvent()
    {
        cluster.handleIncomingMsg(conn, msgs);
    }
}
