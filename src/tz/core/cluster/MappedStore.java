package tz.core.cluster;

import tz.base.common.Buffer;
import tz.base.exception.RaftException;
import tz.core.msg.Entry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;


/**
 * MappedByteBuffer backed log store
 *
 * When snapshot is taken, and log file is not required, it will be deleted.
 * Before deleting a mapped file, we have to be sure that it's not being sent
 * via another thread or OS. Otherwise, it would cause undefined behaviour.
 * (similar to sendfile call on linux, look for : DMA, linux kernel)
 *
 * One way to be sure if OS is not using mapped file is waiting for response
 * from receiver side, if receiver sends response "I got it", then we can be
 * sure OS is not using it. As Raft algorithm requires receivers to send
 * response for each request, we can guarantee it. Check for Cluster class
 * for details
 *
 * Log files are allocated with a fixed size, so they are not growing or
 * shrinking. So, we preallocate space for that file. There could be multiple
 * files though.
 *
 */
public class MappedStore
{
    private static final int END_FLAG       = 0;
    private static final int END_FLAG_LEN   = 1;
    private static final int CRC32_HASH_LEN = 8;
    private final Cluster cluster;
    private FileChannel channel;
    private Buffer buf;

    private final Path path;
    private final List<Entry> entries;
    private long prevIndex;
    private boolean needFlush;

    private final CRC32 crc32;


    public MappedStore(Cluster cluster, Path path)
    {
        this.cluster      = cluster;
        this.path         = path;
        this.prevIndex    = -1;

        entries = new ArrayList<>();
        crc32   = new CRC32();

        crc32.reset();
        read();
    }

    public MappedStore(Cluster cluster,
                       Path path, long prevIndex, long size)
    {
        this.cluster   = cluster;
        this.prevIndex = prevIndex;
        this.path      = Paths.get(path + "/" + "log-" + System.nanoTime() + ".store");

        entries        = new ArrayList<>();
        crc32          = new CRC32();

        crc32.reset();
        init(size);
    }

    private void init(long size)
    {
        try {
            channel = FileChannel.open(this.path, EnumSet.of(StandardOpenOption.CREATE,
                                                             StandardOpenOption.READ,
                                                             StandardOpenOption.WRITE));

            buf = new Buffer(channel.map(FileChannel.MapMode.READ_WRITE, 0, size));

            buf.putLong(prevIndex);
            updateMeta(0, Long.BYTES);

            // Windows requires this to allocate space on disk
            buf.put((int) size - 1, (byte) 0);
            buf.force();
        }
        catch (IOException e) {
            cluster.logError(e);
            delete();
            throw new RaftException(e);
        }
    }

    private void updateMeta(int pos, int len)
    {
        //End flag
        buf.put(END_FLAG);

        //Hash value
        crc32.update(buf.slice(pos, len).backend());
        buf.putLong(crc32.getValue());
        buf.position(buf.position() - END_FLAG_LEN - CRC32_HASH_LEN);
    }

    private void recalculateMeta()
    {
        crc32.reset();
        updateMeta(0, buf.position());
    }

    /**
     * Read entries from the mapped buffer
     */
    private void read()
    {
        try {
            EnumSet<StandardOpenOption> options;
            options = EnumSet.of(StandardOpenOption.CREATE,
                                 StandardOpenOption.READ,
                                 StandardOpenOption.WRITE);

            channel = FileChannel.open(this.path, options);

            final FileChannel.MapMode mode = FileChannel.MapMode.READ_WRITE;
            buf = new Buffer(channel.map(mode, 0, channel.size()));

            prevIndex = buf.getLong();
            if (prevIndex == -1) {
                throw new RaftException("Empty log file");
            }

            while (true) {
                //0 is a sentinel to end of file
                byte b = buf.get(buf.position());
                if (b == END_FLAG) {
                    break;
                }

                entries.add(new Entry(buf));
            }

            final int pos   = buf.position();

            buf.advance(END_FLAG_LEN);
            final long hash = buf.getLong();

            buf.position(0);
            buf.limit(pos);

            crc32.update(buf.backend());

            if (crc32.getValue() != hash) {
                throw new RaftException("Store file is inconsistent : " + path);
            }

            buf.position(pos);
            buf.limit((int) channel.size());
        }
        catch (Exception e) {
            cluster.logError(e);
            delete();
            throw new RaftException(e);
        }
    }

    /**
     * Append entry to log file
     * @param entry entry
     */
    public void add(Entry entry)
    {
        final int pos       = buf.position();
        final int headerLen = entry.headerLen();
        final int dataLen   = entry.dataLen();

        entry.encode(buf);
        entry.setHeader(buf.slice(pos, headerLen));
        entry.setData(buf.slice(pos + headerLen, dataLen));

        updateMeta(pos, headerLen + dataLen);

        entries.add(entry);
        entry.setIndex(prevIndex + entries.size());

        needFlush = true;
    }

    /**
     * Get the entry record
     * @param index index of the entry
     * @return      EntryRecord object if exists
     */
    public Entry getEntry(long index)
    {
        int pos = (int) (index - prevIndex - 1);
        if (pos < 0 || pos >= entries.size()) {
            return null;
        }

        return entries.get(pos);
    }

    /**
     * Remove logs from index to end of the current log file
     * @param index index to start deleting(inclusive)
     */
    public void removeFrom(long index)
    {
        int pos = (int) (index - prevIndex - 1);
        if (pos < 0 || pos >= entries.size()) {
            return;
        }

        buf.position(entries.get(pos).getOffset());
        recalculateMeta();
        entries.subList(pos, entries.size()).clear();
    }

    /**
     * Get path
     * @return path of the log file
     */
    public Path getPath()
    {
        return path;
    }

    /**
     * Get base log index
     * @return base index of the log file
     */
    public long getPrevIndex()
    {
        return prevIndex;
    }

    /**
     * Get last log index
     * @return last log index
     */
    public long getLastIndex()
    {
        return prevIndex + entries.size();
    }

    public long getLastTerm()
    {
        Entry entry = getEntry(getLastIndex());
        return entry == null ? 0 : entry.getTerm();
    }

    /**
     * Get remaining entry count starting from index(inclusive),
     * if its lower than the base index, total count will be returned
     * @param index from index
     * @return      entry count starting from 'index'
     */
    public int getEntriesCount(long index)
    {
        if (index < prevIndex + 1) {
            index = prevIndex + 1;
        }

        int pos = (int) (index - prevIndex - 1);
        if (pos < 0 || pos >= entries.size()) {
            return 0;
        }

        return entries.size() - pos;
    }

    /**
     * Get multiple entry buffer as one buffer
     * if its lower than the base index, buffer includes all entries
     * @param index start index
     * @return      entry buffer starting from 'index'
     */
    public Buffer rawEntriesFrom(long index)
    {
        if (index < prevIndex + 1) {
            index = prevIndex + 1;
        }

        int pos = (int) (index - prevIndex - 1);
        if (pos < 0 || pos >= entries.size()) {
            return null;
        }

        Entry entry = entries.get(pos);
        int begin = entry.getOffset();
        int end   = buf.position();

        return buf.slice(begin, end - begin);
    }


    /**
     * Flush mapped buffer to disk
     */
    public void flush()
    {
        if (needFlush) {
            needFlush = false;
            buf.force();
        }
    }

    /**
     * Remaining space in this store
     * @return remaining bytes count
     */
    public int remaining()
    {
        return buf.remaining() - END_FLAG_LEN - CRC32_HASH_LEN;
    }

    /**
     * Close this mapped store
     *
     * We have to use reflection here as JDK does not provide unmap method for
     * mapped files.
     *
     * TODO: We must implement java9 check here as java9 forbids unsafe and
     * TODO: JDK internal class access
     *
     * @throws IOException
     */
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
        }
    }

    /**
     * Delete file
     * @throws IOException on any IO error
     */
    public void delete()
    {


        try {
            close();
            Files.deleteIfExists(path);
        }
        catch (IOException e) {
            cluster.logError(e);
            throw new UncheckedIOException(e);
        }
    }
}
