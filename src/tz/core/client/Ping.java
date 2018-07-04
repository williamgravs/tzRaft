package tz.core.client;

import tz.base.poll.TimerEvent;

/**
 * Ping timer
 */
public class Ping extends TimerEvent
{
    private final Client client;

    /**
     * Ping timer
     * @param client   Client
     * @param periodic Is periodic
     * @param interval Timeout interval
     * @param timeout  First timeout
     */
    public Ping(Client client, boolean periodic, long interval, long timeout)
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
        client.sendPing();
    }
}
