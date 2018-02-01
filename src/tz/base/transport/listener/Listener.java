package tz.base.transport.listener;

import tz.base.poll.Fd;
import tz.base.record.TransportRecord;
import tz.base.transport.sock.Sock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * Abstract connection listener class
 *
 * TCP and TLS implementations must extend this class
 */
public abstract class Listener implements Fd
{
    protected final String uri;
    protected final TransportRecord record;
    protected final ServerSocketChannel channel;
    protected final SelectionKey key;
    protected final Selector selector;
    protected final ListenerOwner owner;

    /**
     * Create a listener with the data indicated in TransportRecord
     *
     * @param owner    Owner that will be informed when a connection received
     * @param selector Selector of the owner loop
     * @param record   Record indicated local endpoint
     *
     * @throws UncheckedIOException On any IO error
     */
    protected Listener(ListenerOwner owner,
                       Selector selector, TransportRecord record)
    {
        this.owner    = owner;
        this.selector = selector;
        this.record   = record;

        //              ex : tcp://127.0.0.1:8080
        this.uri      = record.protocol + "://" +
                        record.hostName + ":" + record.port;

        try {
            channel = ServerSocketChannel.open();
            channel.bind(new InetSocketAddress(record.hostName, record.port));
            channel.socket().setReuseAddress(true);
            channel.configureBlocking(false);

            key = channel.register(selector, SelectionKey.OP_ACCEPT, this);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void close()
    {

        try {
            key.cancel();
            channel.close();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Get local transport record
     *
     * @return Local record
     */
    public TransportRecord getRecord()
    {
        return record;
    }

    /**
     *
     * @return Local uri string
     */
    @Override
    public String toString()
    {
        return uri;
    }

    /**
     * This method is called by Poll class object mostly when a connection
     * is accepted
     */
    @Override
    public void onAccept()
    {
        owner.handleAcceptEvent(this);
    }


    /**
     * Accept connection
     * @return Accepted connection's Sock object
     */
    public abstract Sock accept();


    /**
     * Create listener according to protocol ex: TCP, TLS
     * @param owner    Owner of this listener
     * @param selector Selector to cache this listener
     * @param record   Local record to bind and listen
     * @return         Listener object
     */
    public static Listener create(ListenerOwner owner,
                                  Selector selector, TransportRecord record)
    {
        switch (record.protocol) {
            case "tcp":
                return new TcpListener(owner, selector, record);
            case "tls":
                return new TlsListener(owner, selector, record, owner.getTlsConfig());
            default:
                return null;
        }
    }

}
