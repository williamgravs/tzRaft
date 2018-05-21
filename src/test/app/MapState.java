package test.app;

import tz.base.common.Buffer;
import tz.core.cluster.state.State;
import tz.core.msg.Encoder;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MapState extends State
{
    private static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);
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

            String s = map.get("dsa");
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
            ret = map.put(buffer.getString(), buffer.getString());
            ByteBuffer b = ByteBuffer.allocate(1);
            b.flip();
            return b;
        }
        else {
            ret = map.get(buffer.getString());
        }

        buffer = new Buffer(Encoder.stringLen(ret));
        buffer.putString(ret);
        buffer.flip();

        return buffer.backend();
    }
}
