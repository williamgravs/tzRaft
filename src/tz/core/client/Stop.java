package tz.core.client;

import tz.base.poll.Event;

public class Stop implements Event
{
    public final Client client;

    public Stop(Client client)
    {
        this.client = client;
    }


    @Override
    public void onEvent()
    {
        client.handleStop();
    }
}
