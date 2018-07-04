package tz.core.client;

import tz.base.poll.Event;

/**
 * Stop timer
 */
public class Stop implements Event
{
    private final Client client;

    /**
     * Create new timer
     * @param client Client
     */
    public Stop(Client client)
    {
        this.client = client;
    }


    /**
     * On event
     */
    @Override
    public void onEvent()
    {
        client.handleStop();
    }
}
