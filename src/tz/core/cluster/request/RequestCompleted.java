package tz.core.cluster.request;

import tz.core.cluster.Cluster;
import tz.core.cluster.Node;
import tz.core.cluster.state.Response;
import tz.core.msg.Entry;

import java.nio.ByteBuffer;

public class RequestCompleted implements Request
{
    private Node node;

    public RequestCompleted(Node node)
    {
        this.node = node;
    }

    @Override
    public void handle(Cluster cluster, Entry entry, Response response)
    {
        cluster.handleRequestCompleted(node, entry, response);
    }
}
