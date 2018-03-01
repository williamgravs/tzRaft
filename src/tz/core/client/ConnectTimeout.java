package tz.core.client;

import tz.base.poll.TimerEvent;

/**
 * Connection attempt timeout
 */
public class ConnectTimeout extends TimerEvent
{
    private final Client client;

    /**
     * Connection attempt timer
     * @param client   Client
     * @param periodic Is periodic
     * @param interval Interval
     * @param timeout  Timeout (mostly currentTimestamp() + interval)
     */
    public ConnectTimeout(Client client, boolean periodic, long interval, long timeout)
    {
        super(periodic, interval, timeout);

        this.client = client;
    }

    /**
     * Timeout occured
     */
    @Override
    public void onTimeout()
    {
        client.handleConnectTimeout();
    }
}
