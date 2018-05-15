package tz.base.transport;

/**
 * TLS config pod
 */
public class TlsConfig
{
    public final String serverKeyStore;
    public final String serverKeyStorePassword;
    public final String serverKeyStoreKeyPassword;
    public final String serverTrustStore;
    public final String serverTrustStorePassword;

    public final String clientKeyStore;
    public final String clientKeyStorePassword;
    public final String clientKeyStoreKeyPassword;
    public final String clientTrustStore;
    public final String clientTrustStorePassword;

    /**
     * Create new Tls config
     * @param serverKeyStore            server side keystore path
     * @param serverKeyStorePassword    server side keystore password
     * @param serverKeyStoreKeyPassword server side keystore key password
     * @param serverTrustStore          server side truststore path
     * @param serverTrustStorePassword  server side truststore password
     * @param clientKeyStore            client side keystore path
     * @param clientKeyStorePassword    client side keystore password
     * @param clientKeyStoreKeyPassword client side keystore key password
     * @param clientTrustStore          client side truststore path
     * @param clientTrustStorePassword  client side truststore password
     */
    public TlsConfig(String serverKeyStore, String serverKeyStorePassword,
                     String serverKeyStoreKeyPassword,
                     String serverTrustStore, String serverTrustStorePassword,
                     String clientKeyStore, String clientKeyStorePassword,
                     String clientKeyStoreKeyPassword,
                     String clientTrustStore, String clientTrustStorePassword)
    {
        this.serverKeyStore            = serverKeyStore;
        this.serverKeyStorePassword    = serverKeyStorePassword;
        this.serverKeyStoreKeyPassword = serverKeyStoreKeyPassword;
        this.serverTrustStore          = serverTrustStore;
        this.serverTrustStorePassword  = serverTrustStorePassword;
        this.clientKeyStore            = clientKeyStore;
        this.clientKeyStorePassword    = clientKeyStorePassword;
        this.clientKeyStoreKeyPassword = clientKeyStoreKeyPassword;
        this.clientTrustStore          = clientTrustStore;
        this.clientTrustStorePassword  = clientTrustStorePassword;
    }
}
