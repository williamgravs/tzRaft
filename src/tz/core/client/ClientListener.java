package tz.core.client;

import tz.base.log.LogHandler;
import tz.base.record.ClusterRecord;


/**
 * Client callbacks
 */
public interface ClientListener extends LogHandler
{
    /**
     * Update on connection state
     * @param connected True if reconnected, false if cannot connect to cluster
     */
    void connectionState(boolean connected);

    /**
     * Update on cluster configuration
     * @param record Cluster configuration
     */
    void configChange(ClusterRecord record);
}
