package tz.app;

import tz.base.log.Level;
import tz.base.record.NodeRecord;
import tz.base.record.TransportRecord;
import tz.core.cluster.Callbacks;
import tz.core.cluster.Cluster;
import tz.core.cluster.Config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

public class App
{

    public static void main(String[] args)
    {

        System.out.println(3/2);
        Config config = new Config();
        config.storeSize = 1024 * 1024 * 1024;
        config.logLevel  = "DEBUG";

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

        MapState state = new MapState();
        Cluster cluster = null;
        /*
        try {
            Files.delete(Paths.get("./cluster0/" + args[0] + "/" + "cluster.conf"));
            Files.delete(Paths.get("./cluster0/" + args[0] + "/" + "cluster0.snapshot"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }*/
        try {
            cluster = new Cluster("cluster0", args[0], "./", config, callbacks, state);

            if (!cluster.isStarted()) {
                NodeRecord record = new NodeRecord("node0", "group0");
                record.addTransport(new TransportRecord("tcp", "127.0.0.1", 9090));
                cluster.addNode(record);

                /*
                NodeRecord record1 = new NodeRecord("node1", "group0");
                record1.addTransport(new TransportRecord("tcp", "127.0.0.1", 9091));
                cluster.addNode(record1);


                NodeRecord record2 = new NodeRecord("node2", "group0");
                record2.addTransport(new TransportRecord("tcp", "127.0.0.1", 9092));
                cluster.addNode(record2);*/
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
