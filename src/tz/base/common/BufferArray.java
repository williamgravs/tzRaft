package tz.base.common;

import java.nio.ByteBuffer;

/**
 * Simple class for Array of ByteBuffer's
 *
 * This implementation is similar to a ring buffer implementation. Adds to end,
 * pops from head if buffers dont have remaining data
 */
public class BufferArray
{
    private final int len;
    private final ByteBuffer[] array;
    private int index;
    private int offset;

    /**
     * Create fixed length array of ByteBuffer's
     *
     * @param len max count of the arrays
     */
    public BufferArray(int len)
    {
        this.len   = len;
        this.array = new ByteBuffer[len];
    }

    /**
     * Append bytebuffer to this array
     * @param buf ByteBuffer to append
     * @return    false if there is no space for a new ByteBuffer,
     *            caller must try again when a read operation on this array is
     *            done
     */
    public boolean add(ByteBuffer buf)
    {
        if (index == len) {
            return false;
        }

        array[index] = buf;
        index++;
        return true;
    }

    /**
     * Remove empty bytebuffers
     *
     * @return true if the array is fully empty
     */
    public boolean popEmpties()
    {
        while (offset < index) {
            if (array[offset].remaining() != 0) {
                break;
            }

            array[offset++] = null;
        }

        if (offset == index) {
            index  = 0;
            offset = 0;

            return true;
        }

        return false;
    }

    /**
     * Array backend of this object, mostly used for JDK calls
     * @return ByteBuffer array
     */
    public ByteBuffer[] getArray()
    {
        return array;
    }

    /**
     * Get remaining space in the array
     * @return count of remaining spots in the array
     */
    public int remaining()
    {
        return len - index;
    }

    /**
     * Get first index of ring logic
     * @return value of the first ByteBuffer in backend array
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * Get byteBuffers count
     * @return count of byteBuffers held in this array
     */
    public int getCount()
    {
        return index - offset;
    }

    /**
     * Clear the array, let the GC do its job
     */
    public void clear()
    {
        for (int i = 0; i < len; i++) {
            array[i] = null;
        }

        index  = 0;
        offset = 0;
    }


}
