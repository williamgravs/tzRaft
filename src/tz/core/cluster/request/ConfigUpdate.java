package tz.core.cluster.request;

import tz.base.record.ClusterRecord;
import tz.core.cluster.Cluster;
import tz.core.cluster.state.Response;
import tz.core.msg.Entry;

public class ConfigUpdate implements Request
{
    private ClusterRecord record;

    public ConfigUpdate(ClusterRecord record)
    {
        this.record = record;
    }

    @Override
    public void handle(Cluster cluster, Entry entry, Response response)
    {
        cluster.handleConfigUpdate(record, entry, response);
    }
}
