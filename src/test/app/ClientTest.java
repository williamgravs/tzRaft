package test.app;

import tz.base.common.Buffer;
import tz.base.log.Level;
import tz.base.record.ClusterRecord;
import tz.base.record.TransportRecord;
import tz.core.client.Client;
import tz.core.client.ClientListener;
import tz.core.client.FutureRequest;
import tz.core.msg.Encoder;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class ClientTest implements ClientListener
{
    private long before;
    private long after;
    private long latency;
    private long latency0;
    private long latency1;
    private long latency2;
    private long latency3;
    private static String str = "";

    static  {
        StringBuilder builder = new StringBuilder(1024 * 1024);
        for (int i = 0 ; i < 1214; i++) {
            builder.append("1");
        }

        str = builder.toString();
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
        client.addTransport(new TransportRecord("tcp", "127.0.0.1", 9091));
        try {
            client.connect(20000);
            before = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                //String str = UUID.randomUUID().toString();
                ByteBuffer bb = createPut("" + i % 1000, str);
                long ts = System.nanoTime();
                FutureRequest put = client.sendRequest(bb);
                put.ts = ts;
                put.thenAccept(s ->  {
                    //requestCompleted(put.getSequence(), put.getResponse());
                    //System.out.println(name + " " + put.getSequence() + " Latency : " + TimeUnit.MICROSECONDS.convert((System.nanoTime() - put.ts), TimeUnit.NANOSECONDS));
                    System.out.println(name + " " + put.getSequence() + " Latency : " + TimeUnit.MICROSECONDS.convert((System.nanoTime() - put.ts), TimeUnit.NANOSECONDS));
                    if (put.getSequence() == 9999) {
                        System.out.println(name + " " + put.getSequence() + " Total : " + TimeUnit.MICROSECONDS.convert((System.nanoTime() - before), TimeUnit.NANOSECONDS));

                    }
                });




                //Thread.sleep(5);
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

            new Thread(() -> {
                try {
                    new ClientTest().test(args[0]);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();


            Thread.sleep(1000000000);
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
       // System.out.println("Index : " + index + "Read : " + buffer.getString());

    }

    @Override
    public void configChange(ClusterRecord record)
    {
        System.out.println("Current config " + record);
    }
}
