package tz.core.client;

import tz.core.msg.ClientReq;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class FutureRequest extends CompletableFuture<ByteBuffer>
{
    ClientReq request;
    private long index;
    private ByteBuffer response;
    public long ts;

    public FutureRequest(ClientReq request, long index)
    {
        this.request = request;
        this.index   = index;
    }

    public long getSequence()
    {
        return request.getSequence();
    }

    public long getAcknowledge()
    {
        return request.getAcknowledge();
    }

    public ClientReq getRequest()
    {
        return request;
    }

    public void setResponse(ByteBuffer response)
    {
        this.response = response;
    }

    public long getIndex()
    {
        return index;
    }

    public ByteBuffer getResponse()
    {
        return response;
    }

    public void finish(ByteBuffer response)
    {
        this.response = response;

        complete(response);
    }

    public void finishExceptionally(Throwable th)
    {
        this.response = null;
        completeExceptionally(th);
    }
}
