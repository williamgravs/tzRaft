package junit.tezc.base.common;

import org.junit.Test;
import tezc.base.common.Buffer;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BufferTest
{
    private Buffer buf;

    public BufferTest()
    {
        buf = new Buffer(512);
    }

    @Test
    public void run()
    {
        varIntTest();
        varLongTest();
        stringTest();
        byteBufferTest();
    }

    private void varIntTest()
    {
        buf.clear();
        buf.putVarInt(100);
        buf.flip();

        assertEquals(buf.getVarInt(), 100);
    }

    private void varLongTest()
    {
        buf.clear();
        buf.putVarLong(-13);
        buf.flip();

        assertEquals(buf.getVarLong(), -13);
    }

    private void stringTest()
    {
        buf.clear();
        buf.putString("testSTring%3dA''S");
        buf.flip();

        assertEquals(buf.getString(), "testSTring%3dA''S");
    }

    private void byteBufferTest()
    {
        ByteBuffer tmp = ByteBuffer.allocate(buf.cap());
        tmp.putInt(3);
        tmp.putInt(4);
        tmp.putInt(5);
        tmp.flip();

        buf.clear();
        buf.putByteBuffer(tmp.slice());
        buf.flip();

        int len = buf.getVarInt();
        assertEquals(buf.getByteBuffer(len), tmp);
    }
}
