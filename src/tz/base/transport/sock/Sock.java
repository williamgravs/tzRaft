package tz.base.transport.sock;

import tz.base.common.BufferArray;
import tz.base.poll.Fd;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;


/**
 * Abstract class for socket implementations
 */
public abstract class Sock implements Fd
{
    private static final int IOV_MAX  = 512;
    private static final int BUF_SIZE = 5 * 1024 * 1024;

    private final String protocol;
    private String localAddress;
    private String remoteAddress;
    private String str;
    protected final SocketChannel channel;
    protected SockOwner owner;

    SelectionKey key;
    final ByteBuffer recvBuf;
    final ByteBuffer sendBuf;
    final BufferArray outBufs;
    boolean connected;

    long receivedBytes;
    long sentBytes;


    /**
     * Create new Sock
     * @param owner    Owner of Sock object, callbacks will call the owner
     * @param channel  Channel for the Sock
     * @param protocol Protocol string, ex : tcp, tls
     *
     * @throws IOException on any socketchannel error
     */
    protected Sock(SockOwner owner, SocketChannel channel, String protocol)
    {
        try {
            this.owner     = owner;
            this.channel   = ((channel == null) ? SocketChannel.open() : channel);
            this.connected = this.channel.isConnected();
            this.protocol  = protocol;
            this.recvBuf   = ByteBuffer.allocateDirect(BUF_SIZE);
            this.sendBuf   = ByteBuffer.allocateDirect(BUF_SIZE);
            this.outBufs   = new BufferArray(IOV_MAX);

            this.channel.configureBlocking(false);
            this.channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            this.channel.setOption(StandardSocketOptions.SO_RCVBUF, BUF_SIZE);
            this.channel.setOption(StandardSocketOptions.SO_SNDBUF, BUF_SIZE);

            gatherInfo();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public long getReceivedBytes()
    {
        return receivedBytes;
    }

    public long getSentBytes()
    {
        return sentBytes;
    }

    /**
     * Append buf to socket's outgoing list
     * @param buf ByteBuffer to send out via this socket, this buffer should be
     *            DirectByteBuffer
     * @return    true if buffer added to outgoing list, if returns false,
     *            caller should try later (after a send operation)
     */
    public boolean append(ByteBuffer buf)
    {
        return outBufs.add(buf);
    }

    /**
     * Copy a ByteBuffer to outgoing list of this socket.
     *
     * Common reason for this call is to copy data from HeapByteBuffer to
     * DirectByteBuffer. So, caller should call this method with a
     * HeapByteBuffer
     *
     * @param buf ByteBuffer to send
     */
    public void copy(ByteBuffer buf)
    {
        if (outBufs.remaining() == 0 || sendBuf.remaining() == 0) {
            return;
        }

        int min       = Math.min(buf.remaining(), sendBuf.remaining());
        int prevPos   = sendBuf.position();
        int prevLimit = sendBuf.limit();

        //Get a slice from sendBuf which is a DirectByteBuffer
        sendBuf.limit(prevPos + min);
        ByteBuffer copy = sendBuf.slice();
        sendBuf.limit(prevLimit);
        sendBuf.position(prevPos + min);

        copy.put(buf.array(), buf.position(), min);
        copy.flip();
        buf.position(buf.position() + min);

        outBufs.add(copy);
    }

    /**
     * Get receive buffer of the socket
     * @return Buffer holding read data from socket
     */
    public ByteBuffer getRecvBuf()
    {
        return recvBuf;
    }

    /**
     * Get send buffer of the socket
     * @return Outgoing buffer of the socket
     */
    public ByteBuffer getSendBuf()
    {
        return sendBuf;
    }

    /**
     * If the socket has data to be sent
     *
     * @return true if this buffer does not have unsent remaining data
     */
    public boolean hasRemaining()
    {
        return sendBuf.hasRemaining() && outBufs.remaining() != 0;
    }

    /**
     * Recv call
     * @return received byte count
     */
    public abstract int recv();

    /**
     * Send call for scather gather IO
     * @return true if all bufs are sent
     * @throws IOException on any socketchannel error
     */
    public abstract boolean sendAll();

    /**
     * Connect call to remote
     *
     * @param hostname remote hostname
     * @param port     remote port
     * @return         true if connection established immediately
     *
     * @throws IOException on any socketchannel error
     */
    public boolean connect(String hostname, int port)
    {
        try {
            return channel.connect(new InetSocketAddress(hostname, port));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Gather socket info and create string representation
     *
     * @throws IOException on any socketchannel error
     */
    private void gatherInfo()
    {
        try {
            if (connected) {
                InetSocketAddress localAddr, remoteAddr;

                localAddr = (InetSocketAddress) channel.getLocalAddress();
                remoteAddr = (InetSocketAddress) channel.getRemoteAddress();

                localAddress  = protocol + "://" + localAddr.getHostString() + ":"
                                                 + localAddr.getPort();

                remoteAddress = protocol + "://" + remoteAddr.getHostString() + ":"
                                                 + remoteAddr.getPort();
                str = "(Local  : " + localAddress + " - " +
                       "Remote : " + remoteAddress + ")";
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Finalize connect process for non blocking sockets
     *
     * @return true if connection is established
     * @throws IOException on any socketchannel error
     */
    public boolean finishConnect()
    {
        try {
            boolean result = channel.finishConnect();
            if (result) {
                connected = true;
                gatherInfo();
            }

            return result;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Is this sock connected
     * @return true if connection is alive
     */
    public boolean isConnected()
    {
        return connected;
    }

    /**
     * Get remote hostname port pair
     * @return remote end hostname:port string
     */
    public String getRemoteAddress()
    {
        return remoteAddress;
    }

    /**
     * Connected callback
     */
    @Override
    public void onConnect()
    {
        owner.handleConnectEvent(this);
    }

    /**
     * Socket has data to be processed callback
     */
    @Override
    public void onRead()
    {
        owner.handleReadEvent(this);
    }

    /**
     * Socket buffer is available to write more callback
     */
    @Override
    public void onWrite()
    {
        owner.handleWriteEvent(this);
    }

    /**
     * Set owner
     * @param owner owner of the Sock
     */
    public void setOwner(SockOwner owner)
    {
        this.owner = owner;
    }

    /**
     * Change interest ops of the sock
     * @param op Interest ops flag
     *
     * @throws  IllegalArgumentException
     *          If a bit in the set does not correspond to an operation that
     *          is supported by this key's channel, that is, if
     *          {@code (ops & ~channel().validOps()) != 0}
     *
     * @throws CancelledKeyException
     *          If this key has been cancelled
     */
    public void setOp(int op)
    {
        key.interestOps(op);
    }

    /**
     * Closes this channel.
     *
     * @throws  UncheckedIOException
     *          If an I/O error occurs
     */
    public void close()
    {
        try {
            if (key != null) {
                key.cancel();
                key = null;
            }

            channel.close();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Registers this channel with the given selector with given interest ops
     *
     * @param selector Selector to register
     * @param ops      Interest ops
     *
     * @throws  UncheckedIOException on any socket channel error
     *
     */
    public void register(Selector selector, int ops)
    {
        try {
            this.key = channel.register(selector, ops, this);
        }
        catch (ClosedChannelException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Cancel this key, leads to removal of the key from the associated selector
     */
    public void cancel()
    {
        key.cancel();
    }

    /**
     * Read operation
     *
     * @param buf Buffer to read data in
     * @return    Number of bytes read, could be 0, if it s -1, indicates EOF
     *
     * @throws UncheckedIOException on any channel error
     */
    public int read(ByteBuffer buf)
    {
        try {
            int bytes = channel.read(buf);
            if (bytes != -1) {
                receivedBytes += bytes;
            }

            return bytes;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Write operation
     *
     * @param buf Buffer to write to socket
     * @return    Number of bytes written, could be 0
     *
     * @throws UncheckedIOException on any channel error
     */
    public long write(ByteBuffer buf)
    {
        try {
            long bytes = channel.write(buf);
            sentBytes += bytes;

            return bytes;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Write scatter gather operation
     *
     * @param bufs Buffers to write to socket
     * @return     Number of bytes written, could be 0
     *
     * @throws UncheckedIOException on any channel error
     */
    public long write(ByteBuffer[] bufs)
    {
        try {
            long bytes = channel.write(bufs);
            sentBytes += bytes;
            return bytes;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Relative write scatter gather operation
     *
     * @param bufs Buffers to write to socket
     * @return     Number of bytes written, could be 0
     *
     * @throws UncheckedIOException on any channel error
     */
    public long write(ByteBuffer[] bufs, int offset, int len)
    {
        try {
            long bytes = channel.write(bufs, offset, len);
            sentBytes += bytes;

            return bytes;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * toString
     * @return string representation of the socket
     */
    public String toString()
    {
        return str;
    }

    /**
     * Handshake call to finish connection process, used for TLS connections
     */
    public void handshake()
    {

    }
}
