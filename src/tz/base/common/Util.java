package tz.base.common;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods
 */
public class Util
{
    /**
     * Current timestamp
     *
     * This method must not use System.currentTimeMillis() as it does not
     * provide incremental guarantee.
     *
     * @return the current value of the running Java Virtual Machine's
     *         high-resolution time source, in milliseconds.
     *
     */
    public static long time()
    {
        return TimeUnit.MILLISECONDS.convert(System.nanoTime(),
                                             TimeUnit.NANOSECONDS);
    }

    /**
     * Line separators are different among different OS, so this method
     * should be called whenever a line separator needed
     *
     * @return Line separator string
     */
    public static String newLine()
    {
        return System.lineSeparator();
    }

    public static long readLong(InputStream in) throws IOException
    {
        long ch1 = in.read();
        long ch2 = in.read();
        long ch3 = in.read();
        long ch4 = in.read();
        long ch5 = in.read();
        long ch6 = in.read();
        long ch7 = in.read();
        long ch8 = in.read();

        if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
            throw new EOFException();
        }

        return ((ch1 << 56) + (ch2 << 48) + (ch3 << 40) + (ch4 << 32) +
                (ch5 << 24) + (ch6 << 16) + (ch7 << 8)  + (ch8 << 0));
    }

    /**
     * Read little endian integer from inputstream
     *
     * @param in           input stream
     * @return             little integer read from stream
     * @throws IOException If stream has less than 4 bytes exception is thrown
     */
    public static int readInt(InputStream in) throws IOException
    {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();

        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }

        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    /**
     * Read variable sized int from inputstream
     * @param in           stream holding integer value
     * @return             decoded integer
     * @throws IOException if stream has less bytes than required IOException
     *                     is thrown. Variable sized integers represented
     *                     with at least 1 byte, up to 5 bytes at most
     */
    public static int readVarInt(InputStream in) throws IOException
    {
        int b = in.read();
        if (b >= 0) {
            return b;
        }

        int x = b & 0x7f;
        b = in.read();
        if (b >= 0) {
            return x | (b << 7);
        }

        x |= (b & 0x7f) << 7;
        b = in.read();
        if (b >= 0) {
            return x | (b << 14);
        }

        x |= (b & 0x7f) << 14;
        b = in.read();
        if (b >= 0) {
            return x | b << 21;
        }

        x |= ((b & 0x7f) << 21) | (in.read() << 28);

        return x;
    }

}
