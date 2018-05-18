package tz.core.cluster;

import tz.base.poll.Event;
import tz.core.Connection;

public class ConnectionUpdate implements Event
{
    private final Cluster cluster;
    private final Connection conn;
    private final Connection.Status status;

    public ConnectionUpdate(Cluster cluster, Connection conn, Connection.Status status)
    {
        this.cluster = cluster;
        this.conn    = conn;
        this.status  = status;
    }


    @Override
    public void onEvent()
    {
        cluster.handleConnectionUpdate(conn, status);
    }
}
