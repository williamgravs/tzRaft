package tz.core.cluster.request;

import tz.core.cluster.Cluster;
import tz.core.cluster.state.Response;
import tz.core.msg.Entry;


public interface Request
{
    void handle(Cluster cluster, Entry entry, Response response);
}
