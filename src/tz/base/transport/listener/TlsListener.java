package tz.base.transport.listener;

import tz.base.exception.TlsException;
import tz.base.record.TransportRecord;
import tz.base.transport.TlsConfig;
import tz.base.transport.sock.Sock;
import tz.base.transport.sock.TLSSock;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;

/**
 * TLS Listener
 */
public class TlsListener extends Listener
{
    private static final String PROTOCOL = "tls";

    private final SSLEngine engine;
    private final TlsConfig config;


    /**
     * Create new TLS listener
     *
     * @param owner    owner of the listener object
     * @param selector selector to cache this listener in
     * @param record   local record to bind and listen
     * @param config   TLS config data (keys, certs, CA's)
     *
     * @exception TlsException if tls config is inconsistent
     */
    public TlsListener(ListenerOwner owner, Selector selector,
                       TransportRecord record,
                       TlsConfig config)
    {
        super(owner, selector, record);

        this.config = config;

        try {
            // First initialize the key and trust material
            KeyStore ksKeys = KeyStore.getInstance("JKS");
            ksKeys.load(
                new FileInputStream(config.serverKeyStore),
                                   config.serverKeyStorePassword.toCharArray());

            KeyStore ksTrust = KeyStore.getInstance("JKS");
            ksTrust.load(
                new FileInputStream(config.serverTrustStore),
                                    config.serverTrustStorePassword.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, config.serverKeyStoreKeyPassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ksTrust);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
        }
        catch (Exception e) {
            throw new TlsException(e);
        }
    }

    /**
     * Accept new TLS connection
     * @return Sock object of accepted connection
     *
     * @throws  UncheckedIOException if channel fails to accept
     */
    @Override
    public Sock accept()
    {
        try {
            SocketChannel incoming = channel.accept();
            incoming.configureBlocking(false);

            return new TLSSock(incoming, engine, false);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Inform this listener that it is about to shutdown
     * @param e Related exception caused this FD's shutdown method called
     */
    @Override
    public void shutdown(Exception e)
    {
        owner.handleShutdown(e, this);
    }
}
