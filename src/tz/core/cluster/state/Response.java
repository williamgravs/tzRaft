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
        return Encoder.varLongLen(sequence) +
               Encoder.booleanLen(success) + Encoder.byteBufferLen(data);
    }

    public void encodeTo(OutputStream out) throws IOException
    {
        Buffer buf = new Buffer(Encoder.varLongLen(sequence) +
                                Encoder.booleanLen(success) +
                                Encoder.varIntLen(data.remaining()));

        buf.putVarLong(sequence);
        buf.putBoolean(success);
        buf.putVarInt(data.remaining());
        buf.flip();

        out.write(buf.array());
        out.write(data.array());
    }
}
