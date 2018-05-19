package tz.core.cluster;

import tz.base.common.Buffer;
import tz.core.cluster.state.State;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.zip.CRC32;

/**
 * Snapshot writer
 *
 * Serialize states to a file, first we serialize meta data, then calling a
 * callback to application so they can serialize their state machines
 *
 * Snapshots are taken in a nonblocking fashion to prevent cluster waiting
 * snapshots.
 */
public class SnapshotWriter extends OutputStream
{
    private final Cluster cluster;
    private Buffer buf;
    private final Path path;
    private final Path tmpPath;
    private FileChannel channel;

    private State state;
    private CRC32 crc32;


    public SnapshotWriter(Cluster cluster, State state,
                          Path workingDir, String name) throws IOException
    {
        this.cluster = cluster;
        this.state   = state;

        path         = Paths.get(workingDir + "/" + name + ".snapshot");
        tmpPath      = Paths.get(workingDir + "/" + name + ".snapshot.tmp");
        buf          = new Buffer(ByteBuffer.allocateDirect(5 * 1024 * 1024));
        crc32        = new CRC32();
    }

    public void takeSnapshot()
    {
        try (FileChannel channel = FileChannel.open(tmpPath,
                                                    EnumSet.of(StandardOpenOption.WRITE,
                                                               StandardOpenOption.CREATE,
                                                               StandardOpenOption.TRUNCATE_EXISTING))){
            this.channel = channel;

            this.channel.lock();
            crc32.reset();
            buf.clear();

            this.channel.position(Long.BYTES);

            state.save(this);
            flush(true);

            buf.clear();
            buf.putLong(crc32.getValue());
            buf.flip();

            this.channel.position(0);
            while (buf.hasRemaining()) {
                this.channel.write(buf.backend());
            }

            Files.deleteIfExists(path);
            Files.move(tmpPath, path);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void flush(boolean force) throws IOException
    {
        if (!buf.hasRemaining() || force) {
            buf.flip();

            crc32.update(buf.duplicate().backend());
            while (buf.hasRemaining()) {
                channel.write(buf.backend());
            }

            buf.clear();
        }
    }

    /**
     * Outputstream method
     * @param b            byte to write to file
     * @throws IOException on any IO error
     */
    @Override
    public void write(int b) throws IOException
    {
        flush(false);
        buf.put((byte) b);
    }

    /**
     * Outputstream method
     *
     * @param b            byte array to write to file
     * @param offset       array offset
     * @param len          data len
     * @throws IOException on any IO error
     */
    @Override
    public void write(byte b[], int offset, int len) throws IOException
    {
        while (len > 0) {
            int written = buf.put(b, offset, len);

            flush(false);

            len    -= written;
            offset += written;
        }
    }
}
