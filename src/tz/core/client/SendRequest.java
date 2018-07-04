package tz.core.client;

import tz.base.poll.Event;

/**
 * Send request event
 * Application to client worker
 */
public class SendRequest implements Event
{
    private final Client client;
    private final FutureRequest req;

    /**
     * SendRequest
     *
     * @param client Client
     * @param req    Request
     */
    public SendRequest(Client client, FutureRequest req)
    {
        this.client = client;
        this.req = req;
    }

    /**
     * On event
     */
    @Override
    public void onEvent()
    {
        client.handleSendRequestEvent(req);
    }
}
