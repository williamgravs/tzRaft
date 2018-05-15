package tz.core.cluster.request;

import tz.core.cluster.Cluster;
import tz.core.cluster.state.Response;
import tz.core.msg.Entry;

import java.nio.ByteBuffer;

public class PeerRegister implements Request
{
    private String name;

    public PeerRegister(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public void handle(Cluster cluster, Entry entry, Response response)
    {
        cluster.handlePeerRegisterCompleted(this, entry, response);
    }
}
