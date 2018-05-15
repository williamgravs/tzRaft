package tz.core.client;

import tz.base.poll.Event;

import java.nio.ByteBuffer;

public class SendRequest implements Event
{
    public final Client client;
    public final FutureRequest req;

    public SendRequest(Client client, FutureRequest req )
    {
        this.client = client;
        this.req = req;
    }

    @Override
    public void onEvent()
    {
        client.handleSendRequestEvent(req);
    }
}
