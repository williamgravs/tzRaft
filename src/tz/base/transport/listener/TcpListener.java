package tz.base.transport.listener;

import tz.base.poll.Fd;
import tz.base.record.TransportRecord;
import tz.base.transport.sock.TcpSock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * TCP Listener
 */
public class TcpListener extends Listener implements Fd
{
    private static final String PROTOCOL = "tcp";


    /**
     * Create new TCP listener
     * @param owner    owner of the listener
     * @param selector selector to cache this listener in
     * @param record   record indicates bind and listen endpoint
     */
    public TcpListener(ListenerOwner owner,
                       Selector selector, TransportRecord record)
    {
        super(owner, selector, record);
    }

    /**
     * Stop listening and release resources
     *
     * @exception   UncheckedIOException If an I/O error occurs
     *
     */
    public void close()
    {
        try {
            channel.close();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Accept TCP connection
     *
     * @return new Sock object for accepted connection
     *
     * @exception UncheckedIOException if channel fails to accept
     */
    public TcpSock accept()
    {
        try {
            SocketChannel incoming = channel.accept();
            incoming.configureBlocking(false);
            incoming.socket().setTcpNoDelay(true);

            return new TcpSock(null, incoming);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Inform this listener is about to shutdown
     *
     * @param e Related exception caused this FD's shutdown method called
     */
    @Override
    public void shutdown(Exception e)
    {
        owner.handleShutdown(e, this);
    }

    /**
     * Inform this listener there is an incoming connection
     */
    @Override
    public void onAccept()
    {
        owner.handleAcceptEvent(this);
    }
}
