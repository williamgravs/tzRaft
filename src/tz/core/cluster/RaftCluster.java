package tz.core.cluster;

import tz.base.record.ClusterRecord;
import tz.base.record.NodeRecord;

/**
 * Raft cluster interface, applications must implement this
 */
public interface RaftCluster
{
    /**
     * Join to cluster
     */
    void join();

    /**
     * Start and run the cluster
     */
    void run();

    /**
     * Is started?
     * If cluster is established once, committed first log, then cluster remains
     * started
     *
     * @return is started
     */
    boolean isStarted();


    /**
     * Add a node to configuration
     * @param nodeRecord record of the node
     */
    void addNode(NodeRecord nodeRecord);


    /**
     * Get current cluster record
     * @return cluster record
     */
    ClusterRecord getClusterRecord();
}
