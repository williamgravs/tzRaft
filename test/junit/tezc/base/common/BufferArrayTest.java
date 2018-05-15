package junit.tezc.base.common;

import org.junit.Test;
import tezc.base.common.BufferArray;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class BufferArrayTest
{

    private BufferArray array;

    public BufferArrayTest()
    {
        array = new BufferArray(100);
    }


    @Test
    public void run()
    {
        int i = 0;
        ByteBuffer b;
        do {
            b = ByteBuffer.allocate(100);
            b.putInt(i);
            b.flip();
            i++;

        } while (array.add(b));

        for (i = 0; i < array.getArray().length; i++) {
            assertEquals(array.getArray()[i].getInt(), i);
        }

        array.popEmpties();

        assertEquals(array.getCount(), 0);
    }
}