package tz.core.cluster;

import tz.base.common.Buffer;
import tz.base.exception.RaftException;
import tz.core.cluster.state.State;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

/**
 * Snapshot reader
 *
 * Read snapshot from file, also holds reference count to this snapshot if
 * we are sending snapshot to a client. When we want to delete a snapshot, we
 * check the ref count.
 *
 */
public class SnapshotReader extends InputStream
{
    private Cluster cluster;
    private Buffer buf;
    private Path path;
    private Path tmpPath;
    private FileChannel channel;

    private long term;
    private long index;
    private State state;
    private CRC32 crc32;

    private final AtomicInteger refCount;


    public SnapshotReader(Cluster cluster, Path workingDir, String filename, State state)
    {
        this.cluster  = cluster;
        this.state    = state;

        crc32         = new CRC32();
        path          = Paths.get(workingDir + "/" + filename + ".snapshot");
        tmpPath       = Paths.get(workingDir + "/" + filename + ".snapshot.tmp");
        buf           = new Buffer(ByteBuffer.allocateDirect(5 * 1024 * 1024));
        refCount      = new AtomicInteger(0);
    }

    public long getIndex()
    {
        return index;
    }

    public long getTerm()
    {
        return term;
    }

    /**
     * Close snapshot file
     * @throws IOException on any IO error
     */
    private void closeChannel() throws IOException
    {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    /**
     * Increment ref
     */
    public void incrementRef()
    {
        refCount.addAndGet(1);
    }

    /**
     * Decrement ref
     */
    public void decrementRef()
    {
        refCount.decrementAndGet();
    }

    /**
     * Is removable
     * @return true if it s safe to remove
     */
    public boolean isRemovable()
    {
        return refCount.get() == 0;
    }

    /**
     * Delete snapshot
     * @throws IOException on any IO error
     */
    public void delete() throws IOException
    {
        closeChannel();

        if (Files.deleteIfExists(path)) {
            cluster.logInfo("Deleted snapshot file : ", path);
        }

        if (Files.deleteIfExists(tmpPath)) {
            cluster.logInfo("Deleted temp snapshot file : ", tmpPath);
        }
    }

    /**
     * Get snapshot directory
     * @return snapshot directory
     */
    public Path getPath()
    {
        return path;
    }

    /**
     * Read snapshot file
     *
     * @throws IOException on any IO error
     */
    public void readSnapshot() throws IOException
    {
        if (Files.deleteIfExists(tmpPath)) {
            cluster.logInfo("Found tmp snapshot and deleted it : ", tmpPath);
        }

        if (!Files.exists(path)) {
            throw new RaftException("No snapshot");
        }

        try {

            channel = FileChannel.open(path, EnumSet.of(StandardOpenOption.READ,
                                                        StandardOpenOption.WRITE));
            channel.lock();

            crc32.reset();
            buf.clear();

            int bytes = 0;
            while (buf.hasRemaining() && bytes != -1) {
                bytes = channel.read(buf.backend());
            }
            buf.flip();

            long hash = buf.getLong();

            do {
                crc32.update(buf.backend());
                buf.clear();
            } while (channel.read(buf.backend()) != -1);

            if (hash != crc32.getValue()) {
                throw new RaftException("Snapshot is inconsistent");
            }

            buf.position(Long.BYTES);

            state.load(this);

            term  = state.getTerm();
            index = state.getIndex();

            cluster.logInfo("Read snapshot index =  [0, to ", index, "]",
                            " term : ", term);
        }
        catch (Exception e) {
            cluster.logError(e);
            throw new RaftException(e);
        }
        finally {
            crc32.reset();
            channel.close();
        }
    }

    /**
     * Inputstream methods
     *
     * @return             number of bytes read
     * @throws IOException on any IO error
     */
    @Override
    public int read() throws IOException
    {
        if (!buf.hasRemaining()) {
            buf.clear();

            int read = 0;
            while (buf.hasRemaining() && read != -1) {
                read = channel.read(buf.backend());
            }
            buf.flip();

            if (!buf.hasRemaining()) {
                return -1;
            }
        }

        return buf.get();
    }

    /**
     * Inputstream methods
     *
     * @param b            destination array
     * @param off          write offset
     * @param len          write len
     * @return             number of bytes read
     * @throws IOException on any IO error
     */
    @Override
    public int read(byte b[], int off, int len) throws IOException
    {
        int max = len;

        while (len > 0) {
            if (!buf.hasRemaining()) {
                buf.clear();

                int rc = 0;
                while (buf.hasRemaining() && rc != -1) {
                    rc = channel.read(buf.backend());
                }

                buf.flip();

                if (!buf.hasRemaining() && rc == -1) {
                    return -1;
                }
            }

            int read = buf.get(b, off, len);

            len -= read;
            off += read;
        }

        return max - len;
    }
}
