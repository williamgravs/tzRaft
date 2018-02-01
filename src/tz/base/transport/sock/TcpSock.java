package tz.base.transport.sock;

import java.io.UncheckedIOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


/**
 * TCP socket implementation
 */
public class TcpSock extends Sock
{
    private static final String PROTOCOL = "tcp";

    /**
     * Create new TCP sock
     *
     * @param owner   Socket owner
     * @param channel Channel of the socket
     *
     * @throws UncheckedIOException on any channel error
     */
    public TcpSock(SockOwner owner, SocketChannel channel)
    {
        super(owner, channel, PROTOCOL);
    }

    /**
     * Set owner
     * @param owner owner of the Sock
     */
    @Override
    public void setOwner(SockOwner owner)
    {
        super.setOwner(owner);
    }

    /**
     * Receive data to this socket's buffer from OS socket
     *
     * @return Number of bytes received
     * @throws UncheckedIOException On any channel error
     */
    @Override
    public int recv()
    {
        recvBuf.clear();

        int n = read(recvBuf);
        if (n == -1) {
            connected = false;
        }

        return n;
    }

    /**
     * Send call for scatter gather IO
     *
     * @return True if all buffers are written to socket's buffer
     * @throws UncheckedIOException On any channel error
     */
    @Override
    public boolean sendAll()
    {
        write(outBufs.getArray(), outBufs.getOffset(), outBufs.getCount());
        if (!outBufs.popEmpties()) {
            setOp(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            return false;
        }

        sendBuf.clear();

        return true;
    }



    /**
     * Shutdown this socket
     *
     * @param e Related exception caused this FD's shutdown method called
     */
    @Override
    public void shutdown(Exception e)
    {
        owner.handleShutdown(e, this);
    }
}
