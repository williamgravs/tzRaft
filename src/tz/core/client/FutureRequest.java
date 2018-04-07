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

    /**
     * Get sequence
     * @return Sequence of the request
     */
    public long getSequence()
    {
        return request.getSequence();
    }

    /**
     * Get acknowledge
     * @return Get acknowledge sent with this request
     */
    public long getAcknowledge()
    {
        return request.getAcknowledge();
    }

    /**
     * Get request
     * @return Request
     */
    public ClientReq getRequest()
    {
        return request;
    }

    /**
     * Set response
     * @param response Encoded response
     */
    public void setResponse(ByteBuffer response)
    {
        this.response = response;
    }

    /**
     * Get response
     * @return Encoded response
     */
    public ByteBuffer getResponse()
    {
        return response;
    }

    /**
     * Response received, finish the future
     * @param response Response
     */
    public void finish(ByteBuffer response)
    {
        this.response = response;
        complete(response);
    }

    /**
     * Request failed
     * @param th Any throwable to report to caller of the future
     */
    public void finishExceptionally(Throwable th)
    {
        this.response = null;
        completeExceptionally(th);
    }
}
