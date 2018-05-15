package tz.core.msg;

import tz.base.common.Buffer;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Protocol utility class, helper for encoding/decoding
 */
public class Encoder
{
    public static int MAX_VAR_INT_LEN = 5;

    /**
     * Get encoded integer length
     * @param i integer
     * @return  encoded integer lenght, its always 4
     */
    public static int intLen(int i)
    {
        return Integer.BYTES;
    }

    /**
     * Get encoded integer length
     * @param l long
     * @return  encoded long lenght, its always 8
     */
    public static int longLen(long l)
    {
        return Long.BYTES;
    }

    /**
     * Get encoded variable integer length
     * @param x integer
     * @return  depends on the integer, at least 1, up to 5 bytes
     */
    public static int varIntLen(int x)
    {
        if ((x & (-1 << 7)) == 0) {
            return 1;
        }
        else if ((x & (-1 << 14)) == 0) {
            return 2;
        }
        else if ((x & (-1 << 21)) == 0) {
            return 3;
        }
        else if ((x & (-1 << 28)) == 0) {
            return 4;
        }

        return 5;
    }

    /**
     * Get encoded variable long length
     * @param x long
     * @return  depends on the integer, at least 1, up to 9 bytes
     */
    public static int varLongLen(long x)
    {
        int i = 1;

        while (true) {
            x >>>= 7;
            if (x == 0) {
                return i;
            }
            i++;
        }
    }

    /**
     * Get encoded integer length
     * @return  encoded byte lenght, its always 1
     */
    public static int byteLen()
    {
        return Byte.BYTES;
    }

    /**
     * Get encoded byte length
     * @return  encoded byte lenght, its always 1
     */
    public static int byteLen(int b)
    {
        return Byte.BYTES;
    }

    /**
     * Get raw encoded string length
     * @param s string
     * @return  raw encoded string length
     */
    public static int stringLen(String s)
    {
        if (s == null) {
            return varIntLen(-1);
        }

        byte[] str = s.getBytes(UTF_8);

        return varIntLen(str.length) + str.length;
    }

    /**
     * Get encoded boolean length
     * @param b boolean
     * @return  encoded boolean lenght, its always 1
     */
    public static int booleanLen(boolean b)
    {
        return 1;
    }


    /**
     * Get encoded buffer length
     * @param buffer buffer
     * @return       encoded length of the buffer
     */
    public static int bufferLen(Buffer buffer)
    {
        return buffer.remaining();
    }

    public static int byteBufferLen(ByteBuffer buffer)
    {
        return varIntLen(buffer.remaining()) + buffer.remaining();
    }
}
