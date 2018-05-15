package tz.base.transport.listener;


import tz.base.transport.TlsConfig;

/**
 * Listener owner class
 *
 * Any class holds a listener must implement this interface
 */
public interface ListenerOwner
{
    /**
     *
     * @param e        Exception caused this listener to be shutdown
     * @param listener Listener object which is about to shutdown
     */
    void handleShutdown(Exception e, Listener listener);

    /**
     * Handle newly accepted connection
     * @param listener Listener object which has accepted the connection
     */
    void handleAcceptEvent(Listener listener);

    /**
     *  If listener is TLS listener, socket creation and validation might
     *  require TLS configuration
     *
     * @return TLS config holder
     */
    TlsConfig getTlsConfig();
}
