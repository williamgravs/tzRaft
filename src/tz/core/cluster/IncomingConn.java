package tz.core.cluster;

import tz.base.poll.Event;
import tz.core.Connection;
import tz.core.msg.ConnectReq;

public class IncomingConn implements Event
{
    private Cluster cluster;
    private Connection conn;
    private ConnectReq req;

    public IncomingConn(Cluster cluster, Connection conn, ConnectReq req)
    {
        this.cluster = cluster;
        this.conn = conn;
        this.req = req;
    }

    @Override
    public void onEvent()
    {
        cluster.handleIncomingConn(conn, req);
    }
}
