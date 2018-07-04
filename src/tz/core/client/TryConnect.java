package tz.core.client;

import tz.base.poll.TimerEvent;

/**
 * Connect attempt timer
 */
public class TryConnect extends TimerEvent
{
    private final Client client;

    /**
     * Connect attempt timer
     * @param client   Client
     * @param periodic Is periodic
     * @param interval Interval
     * @param timeout  First timeout
     */
    public TryConnect(Client client, boolean periodic, long interval, long timeout)
    {
        super(periodic, interval, timeout);

        this.client = client;
    }

    /**
     * Timeout
     */
    @Override
    public void onTimeout()
    {
        client.handleTryConnect();
    }
}
