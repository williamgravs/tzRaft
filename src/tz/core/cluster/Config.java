package tz.core.cluster;


import tz.base.common.Util;
import tz.base.transport.TlsConfig;


/**
 * Configuration class
 */
public class Config
{
    public int storeSize;
    public String logLevel;
    public long sendBufSize;
    public long recvBufSize;
    public int clusterWorkerCount;
    public int ioWorkerCount;
    public int snapshotWorkerCount;

    public TlsConfig tlsConfig;


    /**
     * Create new config
     */
    public Config()
    {
        storeSize           = 100 * 1024 * 1024;
        logLevel            = "INFO";
        sendBufSize         = 8192 * 8;
        recvBufSize         = 8192 * 8;
        clusterWorkerCount  = 1;
        ioWorkerCount       = 1;
        snapshotWorkerCount = 1;
    }

    /**
     * Set snapshot worker count
     * @param snapshotWorkerCount snapshot worker count
     */
    public void setSnapshotWorkerCount(int snapshotWorkerCount)
    {
        this.snapshotWorkerCount = snapshotWorkerCount;
    }

    /**
     * Set store size
     * @param storeSize store size
     */
    public void setStoreSize(int storeSize)
    {
        this.storeSize = storeSize;
    }

    /**
     * Set log level
     * @param logLevel log level
     */
    public void setLogLevel(String logLevel)
    {
        this.logLevel = logLevel;
    }

    /**
     * Set send buffer size
     * @param sendBufSize send buffer size
     */
    public void setSendBufSize(long sendBufSize)
    {
        this.sendBufSize = sendBufSize;
    }

    /**
     * Set recv buffer size
     * @param recvBufSize recv buffer size
     */
    public void setRecvBufSize(long recvBufSize)
    {
        this.recvBufSize = recvBufSize;
    }

    /**
     * Set tls config
     * @param tlsConfig tls config
     */
    public void setTlsConfig(TlsConfig tlsConfig)
    {
        this.tlsConfig = tlsConfig;
    }

    /**
     * To string
     * @return configuration synopsis
     */
    @Override
    public String toString()
    {
        final String nl = Util.newLine();

        StringBuilder builder = new StringBuilder(2048);
        builder.append("Config :")                                                                          .append(nl)
               .append("\t store size                       = ").append(storeSize)                          .append(nl)
               .append("\t log level                        = ").append(logLevel)                           .append(nl)
               .append("\t send buffer size                 = ").append(sendBufSize)                        .append(nl)
               .append("\t recv buffer size                 = ").append(recvBufSize)                        .append(nl)
               .append("\t cluster worker count             = ").append(clusterWorkerCount)                 .append(nl)
               .append("\t io worker count                  = ").append(ioWorkerCount)                      .append(nl)
               .append("\t snapshot worker count            = ").append(snapshotWorkerCount)                .append(nl)
               .append("\t tls local keystore               = ").append(tlsConfig.serverKeyStore)           .append(nl)
               .append("\t tls local keystore password      = ").append(tlsConfig.serverKeyStorePassword)   .append(nl)
               .append("\t tls local keystore key password  = ").append(tlsConfig.serverKeyStoreKeyPassword).append(nl)
               .append("\t tls local truststore             = ").append(tlsConfig.serverTrustStore)         .append(nl)
               .append("\t tls local truststore password    = ").append(tlsConfig.serverTrustStorePassword) .append(nl)
               .append("\t tls client keystore              = ").append(tlsConfig.clientKeyStore)           .append(nl)
               .append("\t tls client keystore password     = ").append(tlsConfig.clientKeyStorePassword)   .append(nl)
               .append("\t tls client keystore key password = ").append(tlsConfig.clientKeyStoreKeyPassword).append(nl)
               .append("\t tls client truststore            = ").append(tlsConfig.clientTrustStore)         .append(nl)
               .append("\t tls client truststore password   = ").append(tlsConfig.clientTrustStorePassword) .append(nl);

        builder.append(nl);

        return builder.toString();
    }
}
