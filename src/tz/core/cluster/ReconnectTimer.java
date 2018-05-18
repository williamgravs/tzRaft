package tz.core.cluster;

import tz.base.poll.TimerEvent;

public class ReconnectTimer extends TimerEvent
{
    private Cluster cluster;
    private Node node;

    public ReconnectTimer(Cluster cluster, Node node,
                          boolean periodic, long interval, long timeout)
    {
        super(periodic, interval, timeout);

        this.cluster = cluster;
        this.node = node;
    }

    @Override
    public void onTimeout()
    {
        cluster.handleReconnectTimer(node);
    }
}
