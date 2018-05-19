package tz.core.cluster.state;

import tz.base.common.Buffer;
import tz.core.msg.Encoder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Response
{
    public long sequence;
    public boolean success;
    public ByteBuffer data;

    public Response(long sequence, boolean success, ByteBuffer data)
    {
        this.sequence = sequence;
        this.success  = success;
        this.data     = data;
    }

    public Response(Buffer buffer)
    {
        sequence = buffer.getLong();
        success  = buffer.getBoolean();
        data     = buffer.getByteBufferCopy();
    }

    public boolean isSuccess()
    {
        return success;
    }

    public Response dup()
    {
        return new Response(sequence, success, data.slice());
    }

    public int encodedLen()
    {
        return Encoder.longLen(sequence) + Encoder.booleanLen(success)
                                         + Encoder.byteBufferLen(data);
    }

    public void encodeTo(OutputStream out) throws IOException
    {
        Buffer buf = new Buffer(Encoder.longLen(sequence) +
                                Encoder.booleanLen(success) +
                                Encoder.varIntLen(data.remaining()));

        buf.putLong(sequence);
        buf.putBoolean(success);
        buf.putVarInt(data.remaining());
        buf.flip();

        out.write(buf.array(), buf.getOffset(), buf.remaining());
        out.write(data.array(), data.arrayOffset(), data.remaining());
    }
}
