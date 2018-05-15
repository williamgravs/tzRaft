package tz.core.cluster.request;

import tz.core.cluster.Cluster;
import tz.core.cluster.state.Response;
import tz.core.msg.Entry;

public class TermStart implements Request
{
    @Override
    public void handle(Cluster cluster, Entry entry, Response response)
    {
        cluster.handleTermStart(entry, response);
    }
}
