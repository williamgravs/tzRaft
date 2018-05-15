package tz.app;

import tz.base.common.Buffer;
import tz.base.log.Level;
import tz.base.record.ClusterRecord;
import tz.base.record.TransportRecord;
import tz.core.client.Client;
import tz.core.client.ClientListener;
import tz.core.client.FutureRequest;
import tz.core.msg.Encoder;

import java.nio.ByteBuffer;
import java.util.UUID;

public class ClientTest implements ClientListener
{
    private long before;
    private long after;
    private static String str = "21321312321";

    static  {
        for (int i = 0 ; i < 1000; i++) {
            str += "s";
        }
    }

    Client client;

    @Override
    public void onLog(Level level, long timestamp, String threadName,
                      String log, Throwable t)
    {
        System.out.println("[" + timestamp + "] [" + threadName + "] : " + log );
        if (t != null) {
            t.printStackTrace();
        }
    }

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
        client = new Client("cluster0", name, "group0", this, "ERROR");
        client.addTransport(new TransportRecord("tcp", "127.0.0.1", 9090));


        try {
            client.connect(10000);
            Thread.sleep(8000);
            before = System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                String str = UUID.randomUUID().toString();
                FutureRequest put = client.sendRequest(createPut("" + i % 1000, str));
                put.thenAccept(s ->  {
                    requestCompleted(put.getIndex(), put.getResponse());
                });

                FutureRequest get = client.sendRequest(createGet("" + i % 1000));
                get.thenAccept(s ->  {
                    requestCompleted(get.getIndex(), get.getResponse());
                });
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    public static void main(String[] args)
    {
        try {
            new ClientTest().test(args[0]);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void connectionState(boolean connected)
    {
        System.out.println("Connected : " + connected);
    }

    @Override
    public void requestCompleted(long index, ByteBuffer buf)
    {
        //System.out.println("Request completed for index : "+ index +
          //                                  " with size : " + buf.remaining());
        Buffer buffer = new Buffer(buf);

        if (index == 1) {
            after = System.currentTimeMillis();
        }

        if (index == 1999999) {
            System.out.println("Before in " + (System.currentTimeMillis() - before));
            System.out.println("After in  " + (System.currentTimeMillis() - after));
        }

       // System.out.println("Index : " + index + "Read : " + buffer.getString());

    }

    @Override
    public void configChange(ClusterRecord record)
    {
        System.out.println("Current config " + record);
    }
}
