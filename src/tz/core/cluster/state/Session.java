package tz.core.cluster.state;

import tz.base.common.ArrayQueue;
import tz.base.common.Buffer;
import tz.base.common.Util;
import tz.base.exception.RaftException;
import tz.core.msg.Encoder;
import tz.core.msg.Entry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Session
 * Clients and peers each holds a session in the internal state machine
 */
public class Session
{
    final String name;
    final int id;

    private long sequence;
    private long acknowledge;
    private Map<Long, Response> responses;

    public Session(String name, int id)
    {
        this.name   = name;
        this.id     = id;

        responses   = new HashMap<>();
        sequence    = -1;
        acknowledge = -1;
    }

    public Session(InputStream in) throws IOException
    {
        byte[] b = new byte[Util.readInt(in)];

        int bytes = in.read(b);
        if (bytes != b.length) {
            throw new IOException("Snapshot is corrupt");
        }

        Buffer buf  = new Buffer(b);

        name        = buf.getString();
        id          = buf.getInt();
        sequence    = buf.getLong();
        acknowledge = buf.getLong();

        int responseCount = buf.getInt();
        responses = new HashMap<>(responseCount);
        for (int i = 0; i < responseCount; i++) {
            Response response = new Response(buf);
            responses.put(response.sequence, response);
        }
    }

    public int getId()
    {
        return id;
    }

    public long getAcknowledge()
    {
        return acknowledge;
    }

    public long getSequence()
    {
        return sequence;
    }

    public Response getResponse(long sequence)
    {
        Response response = responses.get(sequence);
        if (response != null) {
            return response.dup();
        }

        return null;
    }

    public void clearTo(long sequence)
    {
        if (sequence < acknowledge) {
            return;
        }

        for (long i = acknowledge; i <= sequence; i++) {
            responses.remove(i);
        }

        acknowledge = sequence;
    }

    public void cache(Entry entry, Response response)
    {
        Response dup = response.dup();

        responses.put(dup.sequence, dup);
        clearTo(entry.getAcknowledge());
        sequence = entry.getSequence();
    }

    /**
     * Get encoded len of the session
     * @return encoded len
     */
    public int encodedLen()
    {
        int len = Encoder.stringLen(name) +
                  Encoder.intLen(id) +
                  Encoder.longLen(sequence) +
                  Encoder.longLen(acknowledge) +
                  Encoder.intLen(responses.size());

        for (Response response : responses.values()) {
            len += response.encodedLen();
        }

        return len;
    }


    public void encode(OutputStream out) throws IOException
    {
        int len = Encoder.stringLen(name) +
                  Encoder.intLen(id) +
                  Encoder.longLen(sequence) +
                  Encoder.longLen(acknowledge) +
                  Encoder.intLen(responses.size());

        int total = len;
        for (Response response : responses.values()) {
            total += response.encodedLen();
        }

        len += Encoder.intLen(total);
        Buffer buf = new Buffer(len);

        buf.putInt(total);
        buf.putString(name);
        buf.putInt(id);
        buf.putLong(sequence);
        buf.putLong(acknowledge);
        buf.putInt(responses.size());
        buf.flip();

        out.write(buf.array());
        for (Response response : responses.values()) {
            response.encodeTo(out);
        }
    }
}
