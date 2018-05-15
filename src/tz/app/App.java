package tz.app;

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

        Config config = new Config();
        config.storeSize = 50 * 1024 * 1024;
        config.logLevel  = "ERROR";

        Callbacks callbacks = new Callbacks()
        {
            @Override
            public void onLog(Level level, long timestamp, String threadName,
                              String log, Throwable t)
            {
                System.out.println(threadName + " : " + log );
                if (t != null) {
                    t.printStackTrace();
                }
            }
        };

        MapState state = new MapState();
        Cluster cluster = null;
        try {
            cluster = new Cluster("cluster0", "node0", "./",
                                  config, callbacks, state);

            if (!cluster.isStarted()) {
                NodeRecord record = new NodeRecord("node0", "group0");
                record.addTransport(new TransportRecord("tcp", "127.0.0.1", 9090));

                cluster.addNode(record);
            }

            cluster.join();
        }
        catch (IOException e) {
            e.printStackTrace();
        }





        try {
            Thread.sleep(100000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



}
