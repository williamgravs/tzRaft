package tz.base.transport.sock;


/**
 * Socket owner callbacks
 */
public interface SockOwner
{
    /**
     * Callback for socket shutdown, indicates an error
     * @param e    Exception that caused this call
     * @param sock Sock obj about to shutdown
     */
    void handleShutdown(Exception e, Sock sock);

    /**
     * Callback for remote connection establishment
     * @param sock Connected socket
     */
    void handleConnectEvent(Sock sock);

    /**
     * Callback that indicates socket is ready to be read
     * @param sock Sock has unread data
     */
    void handleReadEvent(Sock sock);

    /**
     * Callback that indicates socket is ready to be written
     * @param sock Sock has space in its write buffer
     */
    void handleWriteEvent(Sock sock);
}
