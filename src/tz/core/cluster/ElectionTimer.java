package tz.core.cluster;

import tz.base.poll.TimerEvent;

/**
 * Election timer
 */
public class ElectionTimer extends TimerEvent
{
    private final Cluster cluster;

    /**
     * Create new Election timer
     *
     * @param cluster   cluster
     * @param periodic  is periodic
     * @param interval  interval
     * @param timeout   first timeout
     */
    public ElectionTimer(Cluster cluster,
                         boolean periodic, long interval, long timeout)
    {
        super(periodic, interval, timeout);

        this.cluster = cluster;
    }

    /**
     * Timeout callback
     */
    @Override
    public void onTimeout()
    {
        cluster.onElectionTimeout();
    }
}
