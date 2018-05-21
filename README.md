# Raft Consensus Algorithm implementation for Java

Raft paper : https://raft.github.io/

### ***Project is in early stages of development. Not ready for any kind of use cases!***


[![Build Status](https://travis-ci.org/tezc/tzRaft.svg?branch=master)](https://travis-ci.org/tezc/tzRaft)
[![Coverage Status](https://coveralls.io/repos/github/tezc/tzraft/badge.svg?branch=master)](https://coveralls.io/github/tezc/tzraft?branch=master)

**Goal :** Although there are some cluster communication tools/libraries/systems whether they are implementing Raft or not, yet, 
none of them is consistent, easy to use and provide high performance. This projects aims to create a simple library to provide cluster communication/replication facility/high availability to existing/ongoing projects. I believe high availability and consistency can be done
in a simple and performant manner.

**Dependencies :** There are not third party dependencies. Pure java, no native code yet(aim is not to use any). Requires Java8 at least.

**Design :** Raft defines a strong consistency algorithm. I will try to follow it, so no plan for stale reads/reads from followers.
             We don't have write command - read command difference, all of them are commands, goes to leader and gets answer from leader.
             
**Architecture :**
![Architecture](docs/image/arch.jpg?raw=true "Architecture")


You are either peer or client. Clients are only required when you want to send commands to a remote cluster. There is nothing static in the code so you can make a single node part of multiple clusters. Sharding/distributed clusters can be built this way.

![Sharding](docs/image/shard.jpg?raw=true "Sharding")


**Code Example:**
**Peer:**
```Java
import tz.base.log.Level;
import tz.base.record.NodeRecord;
import tz.base.record.TransportRecord;
import tz.core.cluster.Callbacks;
import tz.core.cluster.Cluster;
import tz.core.cluster.Config;

import java.io.*;

public class App
{
    public static void main(String[] args)
    {
        //Config paramaters for cluster
        Config config = new Config();
        config.storeSize = 1024 * 1024 * 1024;
        config.logLevel  = "ERROR";

        //Callbacks including log, config change, current cluster info
        Callbacks callbacks = new Callbacks()
        {
            @Override
            public void onLog(Level level, long timestamp, String threadName,
                              String log, Throwable t)
            {
                System.out.println(timestamp + " " + threadName + " : " + log );
                if (t != null) {
                    t.printStackTrace();
                }
            }
        };

        try {
            //This is your state machine, must extend tz.core.cluster State
            MapState state = new MapState();
            
            Cluster cluster = new Cluster("cluster0", "node1", "./workingDir/", 
                                           config, callbacks, state);
            //If cluster is already started, you must first connect to cluster and
            //pull the latest config
            if (!cluster.isStarted()) {
                NodeRecord node1 = new NodeRecord("node0", "group0");
                node1.addTransport(new TransportRecord("tcp", "127.0.0.1", 9091));
                cluster.addNode(node1);

                NodeRecord node2 = new NodeRecord("node1", "group0");
                node2.addTransport(new TransportRecord("tcp", "127.0.0.1", 9092));
                cluster.addNode(node2);

                NodeRecord node3 = new NodeRecord("node3", "group0");
                node3.addTransport(new TransportRecord("tcp", "127.0.0.1", 9093));
                cluster.addNode(node3);
            }

            cluster.join();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
  ```
  
  **State in peer :**
  ```Java
import tz.base.common.Buffer;
import tz.core.cluster.state.State;
import tz.core.msg.Encoder;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MapState extends State
{
    private Map<String, String> map = new HashMap<>();

    @Override
    public void clear()
    {
        map.clear();
    }

    @Override
    public void saveState(OutputStream out) throws IOException
    {
        try {
            ObjectOutputStream obj = new ObjectOutputStream(out);
            obj.writeObject(map);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadState(InputStream in) throws IOException
    {
        try {
            ObjectInputStream obj = new ObjectInputStream(in);
            map = (Map<String, String>) obj.readObject();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ByteBuffer onCommand(long index, ByteBuffer buf)
    {
        String ret = null;
        Buffer buffer = new Buffer(buf);
        boolean put = buffer.getBoolean();
        if (put) {
            ret = map.put(buffer.getString(), 
                          buffer.getString());
        }
        else {
            ret = map.get(buffer.getString());
        }

        buffer = new Buffer(Encoder.stringLen(ret));
        buffer.putString(ret);
        buffer.flip();

        return buffer.backend();
    }
  ```
  
  **Client:**
  ```Java
import tz.base.common.Buffer;
import tz.base.log.Level;
import tz.base.record.ClusterRecord;
import tz.base.record.TransportRecord;
import tz.core.client.Client;
import tz.core.client.ClientListener;
import tz.core.client.FutureRequest;
import tz.core.msg.Encoder;

import java.nio.ByteBuffer;

public class ClientTest implements ClientListener
{
    public ByteBuffer createPut(String key, String value)
    {
        Buffer buffer = new Buffer(Encoder.booleanLen(true) +
                                   Encoder.stringLen(key) +
                                   Encoder.stringLen(value));

        buffer.putBoolean(true);
        buffer.putString(key);
        buffer.putString(value);
        buffer.flip();

        return buffer.backend();
    }

    public ByteBuffer createGet(String key)
    {
        Buffer buffer = new Buffer(Encoder.booleanLen(false) +
                                   Encoder.stringLen(key));

        buffer.putBoolean(false);
        buffer.putString(key);
        buffer.flip();

        return buffer.backend();
    }


    public void test(String name) throws InterruptedException
    {
        Client client = new Client("cluster0", name, "group0", this, "ERROR");
        client.addTransport(new TransportRecord("tcp", "127.0.0.1", 9090));
        client.addTransport(new TransportRecord("tcp", "127.0.0.1", 9091));
        
        try {
            client.connect(10000);

            for (int i = 0; i < 10000; i++) {
                ByteBuffer bb = createPut(UUID.randomUUID().toString(),
                                          UUID.randomUUID().toString());
                FutureRequest req = client.sendRequest(bb);
                req.thenAccept(s ->  {
                    System.out.println("Request completed : " + put.getSequence());
                });
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void configChange(ClusterRecord record)
    {
        System.out.println("Current config " + record);
    }
    
    @Override
    public void onLog(Level level, long timestamp, String threadName,
                      String log, Throwable t)
    {
        System.out.println("[" + timestamp + "] [" + threadName + "] : " + log );
        if (t != null) {
            t.printStackTrace();
        }
    }
}

  ```
