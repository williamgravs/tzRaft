package tz.core.client;

import tz.base.poll.TimerEvent;

public class ConnectTimeout extends TimerEvent
{
    private final Client client;

    public ConnectTimeout(Client client, boolean periodic, long interval, long timeout)
    {
        super(periodic, interval, timeout);

        this.client = client;
    }

    @Override
    public void onTimeout()
    {
        client.handleConnectTimeout();
    }
}
