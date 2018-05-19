package tz.core.cluster;

import tz.base.common.Buffer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

public class SnapshotReceiver
{
    private Cluster cluster;
    private Buffer buf;
    private Path path;
    private FileChannel channel;
    private boolean complete;

    public SnapshotReceiver(Cluster cluster)
    {
        this.cluster = cluster;
        this.path    = cluster.getSnapshotPath();
        this.buf     = new Buffer(ByteBuffer.allocateDirect(5 * 1024 * 1024));
    }

    public void open()
    {
        try {
            if (channel == null) {
                channel = FileChannel.open(path, EnumSet.of(StandardOpenOption.WRITE,
                                                            StandardOpenOption.CREATE));
            }
        }
        catch (IOException e) {
            cluster.logError(e);
            throw new UncheckedIOException(e);
        }
    }

    public void close()
    {
        try {
            channel.close();
        }
        catch (IOException e) {
            cluster.logError(e);
            throw new UncheckedIOException(e);
        }
    }

    public void write(ByteBuffer part, boolean done)
    {
        open();

        try {
            while (part.hasRemaining()) {
                buf.put(part);
                buf.flip();
                if (!buf.hasRemaining() || done) {
                    while (buf.hasRemaining()) {
                        channel.write(buf.backend());
                    }
                }
            }
        }
        catch (IOException e) {
            cluster.logError(e);
            close();
            throw new UncheckedIOException(e);
        }

        complete = done;
        if (complete) {
            close();
        }
    }
}
