package tz.base.common;

import java.nio.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Buffer class
 *
 * This is actually a wrapper around ByteBuffer and it provides extra
 * functionality over ByteBuffer.
 */
public class Buffer
{
    private ByteBuffer buf; //buf can be heap, direct or mapped
    private int offset;
    public Buffer next;


    /**
     * This constructor copies bytes of input src to a newly allocated ByteBuffer
     * @param src byte array to copy into this buffer
     */
    public Buffer(byte[] src)
    {
        buf = ByteBuffer.wrap(src);
    }

    /**
     * Allocate buffer with indicated size
     * @param size size of buffer
     */
    public Buffer(int size)
    {
        this.buf = ByteBuffer.allocate(size);
    }

    /**
     * Shares the byte array behind but position and limit values are
     * independent
     * @param buf Buffer object to share byte array with new object
     */
    public Buffer(Buffer buf)
    {
        this.buf = buf.buf.duplicate();
    }

    /**
     * Wrap ByteBuffer with Buffer object
     * @param buf ByteBuffer backend for Buffer object
     */
    public Buffer(ByteBuffer buf)
    {
        this.buf = buf;
    }

    public Buffer()
    {

    }


    boolean isMapped()
    {
        return buf instanceof MappedByteBuffer;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    public int getOffset()
    {
        return offset;
    }

    /**
     * Set this objects buffer
     * @param buf ByteBuffer backend for Buffer object
     */
    public void setBuf(ByteBuffer buf)
    {
        this.buf = buf;
    }

    /**
     * Returns this buffer's capacity
     * @return capacity of this buffer
     */
    public int cap()
    {
        return buf.capacity();
    }

    /**
     * Creates a new byte buffer whose content is a shared subsequence of
     * this buffer's content.
     * @return slice of this buffer
     */
    public Buffer slice()
    {
        return new Buffer(buf.slice());
    }

    /**
     * Creates a slice of this buffer from 'pos' with lenght 'len'
     * @param pos position to head of the slice
     * @param len length of slice
     * @return    Buffer object with position and limit data is set according
     *            to method arguments
     *
     *@throws  IllegalArgumentException
     *          If the preconditions do not hold
     */
    public Buffer slice(int pos, int len)
    {
        int currLimit  = buf.limit();
        int currentPos = buf.position();

        buf.position(pos);
        buf.limit(pos + len);

        Buffer slice = new Buffer((buf.slice()));
        slice.setOffset(pos);

        buf.limit(currLimit);
        buf.position(currentPos);

        return slice;
    }

    /**
     * Acquire a duplicate of this Buffer
     * @return Duplicate of this Buffer
     */
    public Buffer duplicate()
    {
        return new Buffer(this);
    }

    /**
     * Get the backend of this object, mostly required to pass this buffer
     * to JDK methods
     * @return Backend ByteBuffer of this Buffer
     */
    public ByteBuffer backend()
    {
        return buf;
    }

    /**
     * Absolute put operation
     * @param i     Position to put byte
     * @param value Value of the byte
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit
     *
     * @throws ReadOnlyBufferException
     *         If this buffer is read-only
     */
    public void put(int i, byte value)
    {
        buf.put(i, value);
    }

    /**
     * Absolute put operation
     * @param value Value of the byte
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit
     *
     * @throws ReadOnlyBufferException
     *         If this buffer is read-only
     */
    public void put(byte value)
    {
        buf.put(value);
    }

    /**
     * Absolute put operation
     * @param value Value of the byte
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit
     *
     * @throws ReadOnlyBufferException
     *         If this buffer is read-only
     */
    public void put(int value)
    {
        buf.put((byte) value);
    }


    /**
     * Relative <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes the given byte into this buffer at the current
     * position, and then increments the position. </p>
     *
     * @param  b
     *         The byte to be written
     *
     * @throws BufferOverflowException
     *          If this buffer's current position is not smaller than its limit
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putBoolean(boolean b)
    {
        buf.put((byte) (b ? 1 : 0));
    }

    /**
     * Relative <i>put</i> method for writing a short
     * value&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes two bytes containing the given short value, in the
     * current byte order, into this buffer at the current position, and then
     * increments the position by two.  </p>
     *
     * @param  value
     *         The short value to be written
     *
     * @throws  BufferOverflowException
     *          If there are fewer than two bytes
     *          remaining in this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putShort(short value)
    {
        buf.putShort(value);
    }


    /**
     * Relative <i>put</i> method for writing an int
     * value&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes four bytes containing the given int value, in the
     * current byte order, into this buffer at the current position, and then
     * increments the position by four.  </p>
     *
     * @param  value
     *         The int value to be written
     *
     * @throws  BufferOverflowException
     *          If there are fewer than four bytes
     *          remaining in this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putInt(int value)
    {
        buf.putInt(value);
    }

    /**
     * Absolute <i>put</i> method for writing an int
     * value&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes four bytes containing the given int value, in the
     * current byte order, into this buffer at the given index.  </p>
     *
     * @param  index
     *         The index at which the bytes will be written
     *
     * @param  value
     *         The int value to be written
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit,
     *          minus three
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putInt(int index, int value)
    {
        buf.putInt(index, value);
    }

    /**
     * Relative <i>put</i> method for writing a long
     * value&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes eight bytes containing the given long value, in the
     * current byte order, into this buffer at the current position, and then
     * increments the position by eight.  </p>
     *
     * @param  value
     *         The long value to be written
     *
     * @throws  BufferOverflowException
     *          If there are fewer than eight bytes
     *          remaining in this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putLong(long value)
    {
        buf.putLong(value);
    }


    /**
     * Absolute <i>put</i> method for writing a long
     * value&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes eight bytes containing the given long value, in the
     * current byte order, into this buffer at the given index.  </p>
     *
     * @param  index
     *         The index at which the bytes will be written
     *
     * @param  value
     *         The long value to be written
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit,
     *          minus seven
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putLong(int index, long value)
    {
        buf.putLong(index, value);
    }

    /**
     * Absolute <i>put</i> method for writing an int
     * value&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes at least one byte, up to five bytes containing the given int
     * value, in the current byte order, into this buffer at the given index.
     * </p>
     *
     * @param  value
     *         The byte to be written
     *
     * @throws  BufferOverflowException
     *          If this buffer's current position is not smaller than its limit
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putVarInt(int value)
    {
        while ((value & ~0x7f) != 0) {
            buf.put((byte) (0x80 | (value & 0x7f)));
            value >>>= 7;
        }
        buf.put((byte) value);
    }

    /**
     * Absolute <i>put</i> method for writing a long
     * value&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes at least one byte, up to nine bytes containing the given int
     * value, in the current byte order, into this buffer at the given index.
     * </p>
     *
     * @param  value
     *         The byte to be written
     *
     * @throws  BufferOverflowException
     *          If this buffer's current position is not smaller than its limit
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putVarLong(long value)
    {
        while ((value & ~0x7f) != 0) {
            buf.put((byte) (0x80 | (value & 0x7f)));
            value >>>= 7;
        }
        buf.put((byte) value);
    }

    /**
     * Absolute <i>put</i> method for writing a string
     * value&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * Length of string is put as an int value, and string bytes are appended
     *
     * @param  value
     *         The byte to be written
     *
     * @throws  BufferOverflowException
     *          If this buffer's current position is not smaller than its limit
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putString(String value)
    {
        if (value == null) {
            putVarInt(-1);
            return;
        }

        byte str[] = value.getBytes(UTF_8);
        putVarInt(str.length);
        buf.put(str);
    }


    /**
     * Relative bulk <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> This method transfers the entire content of the given source
     * ByteBuffer into this buffer.
     *
     * <pre>
     *     dst.put(a, 0, a.length) </pre>
     *
     * @param   buf
     *          The source ByteBuffer
     *
     *
     * @throws  BufferOverflowException
     *          If there is insufficient space in this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void putByteBuffer(ByteBuffer buf)
    {
        putVarInt(buf.remaining());
        this.buf.put(buf);
    }

    /**
     * Relative bulk <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> This method transfers the entire content of the given source
     * byte array into this buffer.  An invocation of this method of the
     * form <tt>dst.put(a)</tt> behaves in exactly the same way as the
     * invocation
     *
     * <pre>
     *     dst.put(a, 0, a.length) </pre>
     *
     * @param   value
     *          The source array
     *
     *
     * @throws  BufferOverflowException
     *          If there is insufficient space in this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void put(byte[] value)
    {
        buf.put(value);
    }

    /**
     * Relative <i>get</i> method.  Reads the byte at this buffer's
     * current position, and then increments the position.
     *
     * @return  The byte at the buffer's current position
     *
     * @throws BufferUnderflowException
     *          If the buffer's current position is not smaller than its limit
     */
    public byte get()
    {
        return buf.get();
    }

    /**
     * Relative <i>get</i> method.  Reads a byte, interprets it as boolean
     *
     * @return  The byte at the buffer's current position
     *
     * @throws  BufferUnderflowException
     *          If the buffer's current position is not smaller than its limit
     */
    public boolean getBoolean()
    {
        return buf.get() == 1;
    }

    /**
     * Relative <i>get</i> method for reading a short value.
     *
     * <p> Reads the next two bytes at this buffer's current position,
     * composing them into a short value according to the current byte order,
     * and then increments the position by two.  </p>
     *
     * @return  The short value at the buffer's current position
     *
     * @throws  BufferUnderflowException
     *          If there are fewer than two bytes
     *          remaining in this buffer
     */
    public short getShort()
    {
        return buf.getShort();
    }

    /**
     * Relative <i>get</i> method for reading an int value.
     *
     * <p> Reads the next four bytes at this buffer's current position,
     * composing them into an int value according to the current byte order,
     * and then increments the position by four.  </p>
     *
     * @return  The int value at the buffer's current position
     *
     * @throws  BufferUnderflowException
     *          If there are fewer than four bytes
     *          remaining in this buffer
     */
    public int getInt()
    {
        return buf.getInt();
    }


    /**
     * Relative <i>get</i> method for reading an variable int value.
     *
     * <p> Reads at least one, up to the next five bytes at this buffer's
     * current position, composing them into an int value according to the
     * current byte order  </p>
     *
     * @return  The int value at the buffer's current position
     *
     * @throws  BufferUnderflowException
     *          If there are fewer than four bytes
     *          remaining in this buffer
     */
    public int getVarInt()
    {
        int b = buf.get();
        if (b >= 0) {
            return b;
        }

        int x = b & 0x7f;
        b = buf.get();
        if (b >= 0) {
            return x | (b << 7);
        }

        x |= (b & 0x7f) << 7;
        b = buf.get();
        if (b >= 0) {
            return x | (b << 14);
        }

        x |= (b & 0x7f) << 14;
        b = buf.get();
        if (b >= 0) {
            return x | b << 21;
        }

        x |= ((b & 0x7f) << 21) | (buf.get() << 28);

        return x;
    }

    /**
     * Relative <i>get</i> method for reading an variable int value.
     *
     * <p> Reads at least one, up to the next 9 bytes at this buffer's
     * current position, composing them into an int value according to the
     * current byte order  </p>
     *
     * @return  The int value at the buffer's current position
     *
     * @throws  BufferUnderflowException
     *          If there are fewer than four bytes
     *          remaining in this buffer
     */
    public long getVarLong()
    {
        long x = buf.get();
        if (x >= 0) {
            return x;
        }

        x &= 0x7f;
        for (int s = 7; s < 64; s += 7) {
            long b = buf.get();
            x |= (b & 0x7f) << s;
            if (b >= 0) {
                break;
            }
        }

        return x;
    }

    /**
     * Relative <i>get</i> method for reading a long value.
     *
     * <p> Reads the next eight bytes at this buffer's current position,
     * composing them into a long value according to the current byte order,
     * and then increments the position by eight.  </p>
     *
     * @return  The long value at the buffer's current position
     *
     * @throws  BufferUnderflowException
     *          If there are fewer than eight bytes
     *          remaining in this buffer
     */
    public long getLong()
    {
        return buf.getLong();
    }


    /**
     * Absolute <i>put</i> method for reading a string
     * value&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     *
     * @throws  BufferOverflowException
     *          If this buffer's current position is not smaller than its limit
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public String getString()
    {
        int len = getVarInt();
        if (len == -1) {
            return null;
        }

        byte[] strBuf = new byte[len];

        buf.get(strBuf);

        return new String(strBuf, UTF_8);
    }


    /**
     * Get buffer with length
     * @param len length of buffer
     * @return    Buffer as slice
     */
    public Buffer getBuffer(int len)
    {
        return new Buffer(getByteBuffer(len));
    }


    /**
     * Get slice of this buffer with length of 'len'
     * @param len len of the slice
     * @return    new bytebuffer which shares array with this buffer
     *
     * @throws  IllegalArgumentException
     *          If the preconditions do not hold
     */
    public ByteBuffer getByteBuffer(int len)
    {
        ByteBuffer dup = buf.duplicate();

        dup.limit(dup.position() + len);
        advance(len);

        return dup.slice();
    }

    public ByteBuffer getByteBuffer()
    {
        return getByteBuffer(getVarInt());
    }

    public ByteBuffer getByteBufferCopy()
    {
        ByteBuffer copy = ByteBuffer.allocate(getVarInt());
        copy.put(buf.array(), buf.arrayOffset(), copy.capacity());

        return copy;
    }

    /**
     * Tells whether there are any elements between the current position and
     * the limit.
     *
     * @return  <tt>true</tt> if, and only if, there is at least one element
     *          remaining in this buffer
     */
    public boolean hasRemaining()
    {
        Buffer current = this;
        while (current != null) {
            if (current.buf.hasRemaining()) {
                return true;
            }

            current = current.next;
        }

        return false;
    }

    /**
     * Returns the number of elements between the current position and the
     * limit.
     *
     * @return  The number of elements remaining in this buffer
     */
    public int remaining()
    {
        return buf.remaining();
    }

    /**
     * Rewinds this buffer.  The position is set to zero and the mark is
     * discarded.
     *
     * <p> Invoke this method before a sequence of channel-write or <i>get</i>
     * operations, assuming that the limit has already been set
     * appropriately.  For example:
     *
     * <blockquote><pre>
     * out.write(buf);    // Write remaining data
     * buf.rewind();      // Rewind buffer
     * buf.get(array);    // Copy data into array</pre></blockquote>
     *
     */
    public void rewind()
    {
        buf.rewind();
    }

    /**
     * Returns this buffer's position.
     *
     * @return  The position of this buffer
     */
    public int position()
    {
        return buf.position();
    }

    /**
     * Sets this buffer's position.  If the mark is defined and larger than the
     * new position then it is discarded.
     *
     * @param  pos
     *         The new position value; must be non-negative
     *         and no larger than the current limit
     *
     * @throws  IllegalArgumentException
     *          If the preconditions on <tt>newPosition</tt> do not hold
     */
    public void position(int pos)
    {
        buf.position(pos);
    }

    /**
     * Absolute <i>get</i> method.  Reads the byte at the given
     * index.
     *
     * @param  index
     *         The index from which the byte will be read
     *
     * @return  The byte at the given index
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit
     */
    public byte get(int index)
    {
        return buf.get(index);
    }

    /**
     * Advance this buffer's position value
     *
     * @param  i
     *         Advance amount
     *
     * @throws  IllegalArgumentException
     *          If the preconditions on <tt>newPosition</tt> do not hold
     */
    public void advance(int i)
    {
        buf.position(buf.position() + i);
    }

    /**
     * Returns the byte array that backs this
     * buffer&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Modifications to this buffer's content will cause the returned
     * array's content to be modified, and vice versa.
     *
     * @return  The array that backs this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is backed by an array but is read-only
     *
     * @throws  UnsupportedOperationException
     *          If this buffer is not backed by an accessible array
     */
    public byte[] array()
    {
        return buf.array();
    }

    /**
     * Flips this buffer.  The limit is set to the current position and then
     * the position is set to zero.  If the mark is defined then it is
     * discarded.
     *
     * <p> After a sequence of channel-read or <i>put</i> operations, invoke
     * this method to prepare for a sequence of channel-write or relative
     * <i>get</i> operations.  For example:
     *
     * <blockquote><pre>
     * buf.put(magic);    // Prepend header
     * in.read(buf);      // Read data into rest of buffer
     * buf.flip();        // Flip buffer
     * out.write(buf);    // Write header + data to channel</pre></blockquote>
     *
     * <p> This method is often used in conjunction with the {@link
     * ByteBuffer#compact compact} method when transferring data from
     * one place to another.  </p>
     *
     * @return  This buffer
     */
    public void flip()
    {
        buf.flip();
    }

    /**
     * Compacts this buffer&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> The bytes between the buffer's current position and its limit,
     * if any, are copied to the beginning of the buffer.  That is, the
     * byte at index <i>p</i>&nbsp;=&nbsp;<tt>position()</tt> is copied
     * to index zero, the byte at index <i>p</i>&nbsp;+&nbsp;1 is copied
     * to index one, and so forth until the byte at index
     * <tt>limit()</tt>&nbsp;-&nbsp;1 is copied to index
     * <i>n</i>&nbsp;=&nbsp;<tt>limit()</tt>&nbsp;-&nbsp;<tt>1</tt>&nbsp;-&nbsp;<i>p</i>.
     * The buffer's position is then set to <i>n+1</i> and its limit is set to
     * its capacity.  The mark, if defined, is discarded.
     *
     * <p> The buffer's position is set to the number of bytes copied,
     * rather than to zero, so that an invocation of this method can be
     * followed immediately by an invocation of another relative <i>put</i>
     * method. </p>
     *

     *
     * <p> Invoke this method after writing data from a buffer in case the
     * write was incomplete.  The following loop, for example, copies bytes
     * from one channel to another via the buffer <tt>buf</tt>:
     *
     * <blockquote><pre>{@code
     *   buf.clear();          // Prepare buffer for use
     *   while (in.read(buf) >= 0 || buf.position != 0) {
     *       buf.flip();
     *       out.write(buf);
     *       buf.compact();    // In case of partial write
     *   }
     * }</pre></blockquote>
     *

     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public void compact()
    {
        buf.compact();
    }

    /**
     * Clears this buffer.  The position is set to zero, the limit is set to
     * the capacity, and the mark is discarded.
     *
     * <p> Invoke this method before using a sequence of channel-read or
     * <i>put</i> operations to fill this buffer.  For example:
     *
     * <blockquote><pre>
     * buf.clear();     // Prepare buffer for reading
     * in.read(buf);    // Read data</pre></blockquote>
     *
     * <p> This method does not actually erase the data in the buffer, but it
     * is named as if it did because it will most often be used in situations
     * in which that might as well be the case. </p>
     *
     */
    public void clear()
    {
        buf.clear();
    }

    /**
     * Returns this buffer's limit.
     *
     * @return  The limit of this buffer
     */
    public int limit()
    {
        return buf.limit();
    }

    /**
     * Sets this buffer's limit.  If the position is larger than the new limit
     * then it is set to the new limit.  If the mark is defined and larger than
     * the new limit then it is discarded.
     *
     * @param  limit
     *         The new limit value; must be non-negative
     *         and no larger than this buffer's capacity
     *
     * @return  This buffer
     *
     * @throws  IllegalArgumentException
     *          If the preconditions on <tt>newLimit</tt> do not hold
     */
    public void limit(int limit)
    {
        buf.limit(limit);
    }

    /**
     * Get data to dest buffer from this buffer. This call might not drain
     * this buffer if dest buffer does not have enough capacity
     *
     * @param    dest
     *           ByteBuffer to get copy of the data
     *
     *
     * @throws  IllegalArgumentException
     *          If the preconditions on <tt>newLimit</tt> do not hold
     */
    public void get(ByteBuffer dest)
    {
        int prevLimit = dest.limit();
        dest.limit(dest.position() + Math.min(buf.remaining(), dest.remaining()));
        dest.put(buf);
        dest.limit(prevLimit);
    }

    /**
     * Relative bulk <i>get</i> method.
     *
     * <p> This method transfers bytes from this buffer into the given
     * destination array.
     *
     * This method copies <tt>length</tt> bytes from this
     * buffer into the given array, starting at the current position of this
     * buffer and at the given offset in the array.  The position of this
     * buffer is then incremented by <tt>length</tt>.
     *
     * <p> In other words, an invocation of this method of the form
     * <tt>src.get(dst,&nbsp;off,&nbsp;len)</tt> has exactly the same effect as
     * the loop
     *
     * <pre>{@code
     *     for (int i = off; i < off + len; i++)
     *         dst[i] = src.get():
     * }</pre>
     *
     * except that it first checks that there are sufficient bytes in
     * this buffer and it is potentially much more efficient.
     *
     * @param  dst
     *         The array into which bytes are to be written
     *
     * @param  offset
     *         The offset within the array of the first byte to be
     *         written; must be non-negative and no larger than
     *         <tt>dst.length</tt>
     *
     * @param  len
     *         The maximum number of bytes to be written to the given
     *         array; must be non-negative and no larger than
     *         <tt>dst.length - offset</tt>
     *
     * @return  Copied byte count
     *
     * @throws  IndexOutOfBoundsException
     *          If the preconditions on the <tt>offset</tt> and <tt>length</tt>
     *          parameters do not hold
     */
    public int get(byte[] dst, int offset, int len)
    {
        int min = Math.min(len, buf.remaining());
        buf.get(dst, offset, min);

        return min;
    }

    /**
     * Put data to dest buffer from this buffer. This call might not drain
     * src buffer if this buffer does not have enough capacity
     *
     * @param    src
     *           ByteBuffer to get data
     *
     *
     * @throws  IllegalArgumentException
     *          If the preconditions do not hold
     */
    public void put(ByteBuffer src)
    {
        int prevLimit = src.limit();
        src.limit(src.position() + Math.min(buf.remaining(), src.remaining()));
        buf.put(src);
        src.limit(prevLimit);
    }

    /**
     * Put data to dest buffer from this buffer. This call might not drain
     * src buffer if this buffer does not have enough capacity
     *
     * @param    src
     *           ByteBuffer to get data
     *
     *
     * @throws  IllegalArgumentException
     *          If the preconditions do not hold
     */
    public void put(Buffer src)
    {
        int prevLimit = src.limit();
        src.limit(src.position() + Math.min(buf.remaining(), src.remaining()));
        buf.put(src.backend());
        src.limit(prevLimit);
    }


    /**
     * Relative bulk <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> This method transfers bytes into this buffer from the given
     * source array.
     *
     * <p> This method copies <tt>length</tt> bytes from the
     * given array into this buffer, starting at the given offset in the array
     * and at the current position of this buffer.  The position of this buffer
     * is then incremented by <tt>len</tt>.
     *
     * @param  src
     *         The array from which bytes are to be read
     *
     * @param  offset
     *         The offset within the array of the first byte to be read;
     *         must be non-negative and no larger than <tt>array.length</tt>
     *
     * @param  len
     *         The number of bytes to be read from the given array;
     *         must be non-negative and no larger than
     *         <tt>array.length - offset</tt>
     *
     * @return  This buffer
     *
     * @throws  IndexOutOfBoundsException
     *          If the preconditions on the <tt>offset</tt> and <tt>length</tt>
     *          parameters do not hold
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public int put(byte[] src, int offset, int len)
    {
        int min = Math.min(len, buf.remaining());
        buf.put(src, offset, min);

        return min;
    }

    /**
     * This is a flush call for MappedByteBuffer backed Buffer objects.
     */
    public void force()
    {
        if (buf instanceof MappedByteBuffer) {
            ((MappedByteBuffer) buf).force();
        }
    }
}
