package tz.core.client;

import tz.core.msg.ClientReq;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * CompletableFuture for client request
 */
public class FutureRequest extends CompletableFuture<ByteBuffer>
{
    //Original request
    private ClientReq request;

    //Response
    private ByteBuffer response;

    /**
     * Create a new FutureRequest
     * @param request Client request
     */
    public FutureRequest(ClientReq request)
    {
        this.request = request;
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
