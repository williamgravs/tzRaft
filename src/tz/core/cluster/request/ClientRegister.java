package tz.core.cluster.request;

import tz.core.cluster.Cluster;
import tz.core.cluster.Node;
import tz.core.cluster.state.Response;
import tz.core.msg.Entry;

public class ClientRegister implements Request
{
    private Node node;

    public ClientRegister(Node node)
    {
        this.node = node;
    }

    public Node getNode()
    {
        return node;
    }

    @Override
    public void handle(Cluster cluster, Entry entry, Response response)
    {
        cluster.handleClientRegisterCompleted(this, entry, response);
    }
}
