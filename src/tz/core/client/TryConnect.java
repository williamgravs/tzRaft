package tz.core.client;

import tz.base.poll.TimerEvent;

public class TryConnect extends TimerEvent
{
    private final Client client;

    public TryConnect(Client client, boolean periodic, long interval, long timeout)
    {
        super(periodic, interval, timeout);

        this.client = client;
    }

    @Override
    public void onTimeout()
    {
        client.handleTryConnect();
    }
}
