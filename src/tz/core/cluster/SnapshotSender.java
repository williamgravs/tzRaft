package tz.core.cluster;

import tz.base.common.Buffer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class SnapshotSender
{
    private static final long MAX_MAP_SIZE = 2L * 1024 * 1024 * 1024;
    private Cluster cluster;
    private Buffer buf;
    private Path path;
    private FileChannel channel;
    private long term;
    private long index;
    private long offset;
    private boolean allSent;
    private int inFlight;

    public SnapshotSender(Cluster cluster, Path path, long term, long index)
    {
        this.cluster = cluster;
        this.path    = path;
        this.term    = term;
        this.index   = index;

        open();
    }

    private boolean open()
    {
        try {
            if (channel == null) {
                channel = FileChannel.open(path, EnumSet.of(StandardOpenOption.READ));

                if (offset == channel.size()) {
                    allSent = true;
                    return false;
                }

                long actualLen = Math.min(channel.size() - offset, MAX_MAP_SIZE);
                buf = new Buffer(channel.map(FileChannel.MapMode.READ_ONLY, offset, actualLen));
                offset += actualLen;
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return true;
    }

    public long getIndex()
    {
        return index;
    }

    public long getTerm()
    {
        return term;
    }

    public boolean isComplete()
    {
        return allSent;
    }

    private boolean remap()
    {
        try {
            close();
            return open();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean isAllAcked(long offset)
    {
        inFlight--;
        return this.offset == offset;
    }

    public Buffer nextSlice()
    {
        int len = Math.min(buf.remaining(), 50 * 1024 * 1024);
        if (len == 0) {
            if (!remap()) {
                return null;
            }
        }

        Buffer slice = buf.slice(buf.position(), len);
        buf.position(buf.position() + len);
        inFlight++;

        try {
            if (offset == channel.size()) {
                allSent = true;
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return slice;
    }

    public void close() throws IOException
    {
        try {
            if (buf != null) {
                cluster.logDebug("Deleting store at ", path);
                buf.force();

                MappedByteBuffer mappedBuf = (MappedByteBuffer) buf.backend();
                buf.setBuf(null);
                buf = null;

                boolean useSystemGc = true;

                try {
                    Method cleanerMethod = mappedBuf.getClass().getMethod("cleaner");
                    cleanerMethod.setAccessible(true);
                    Object cleaner = cleanerMethod.invoke(mappedBuf);
                    if (cleaner != null) {
                        Method clearMethod = cleaner.getClass().getMethod("clean");
                        clearMethod.invoke(cleaner);
                    }
                    useSystemGc = false;
                }
                catch (Throwable e) {
                    cluster.logWarn(
                        "Cannot get Sun's cleaner method, we will use GC");
                }
                finally {
                    if (!useSystemGc) {
                        mappedBuf = null;
                    }
                }

                if (useSystemGc) {
                    cluster.logWarn("Unmap will use GC");
                    WeakReference<MappedByteBuffer> ref = new WeakReference<>(mappedBuf);
                    mappedBuf = null;

                    long start = System.nanoTime();
                    while (ref.get() != null) {
                        if (System.nanoTime() - start > TimeUnit.MILLISECONDS.toNanos(10000)) {
                            throw new IOException(
                                "Timeout (10000) ms reached while" +
                                    " trying to GC mapped buffer"
                            );
                        }
                        System.gc();
                        Thread.yield();
                    }
                }
            }
        }
        catch (IOException e) {
            cluster.logError(e);
            throw new UncheckedIOException(e);
        }
        finally {
            channel.close();
            channel = null;
        }
    }
}
