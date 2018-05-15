package tz.core.cluster;

import tz.base.common.Buffer;
import tz.base.common.Util;
import tz.base.exception.RaftException;
import tz.core.cluster.command.Command;
import tz.core.cluster.command.NoOPCommand;
import tz.core.msg.Entry;
import tz.core.worker.Worker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Store
{
    private final Cluster cluster;
    private final int pageSize;
    private final int MAX_ENTRY_SIZE;
    private final Deque<MappedStore> pages;
    private final Path path;
    private long lastIndex;
    private long lastTerm;


    public Store(Cluster cluster, Path path, int pageSize)
    {
        this.cluster   = cluster;
        this.path      = path;
        this.pageSize  = pageSize;
        this.pages     = new ArrayDeque<>();
        this.lastIndex = 0;
        this.lastTerm  = 0;

        MAX_ENTRY_SIZE = pageSize - 128;
    }

    public void open(long snapshotIndex) throws IOException
    {
        List<Path> paths = Files.walk(path)
                                .filter(Files::isRegularFile)
                                .filter(p -> p.toString().endsWith(".store"))
                                .collect(Collectors.toList());

        List<MappedStore> tmp = new ArrayList<>();

        for (Path path : paths) {
            try {
                tmp.add(new MappedStore(cluster, path));
            }
            catch (Exception e) {
                cluster.logError(e);
            }
        }

        /*
         * Store files should be sequential, if we detect files out of other,
         * sequential ones will be kept, rest will be deleted. As we will start
         * as follower, leader will send us the missing log anyway.
         */
        tmp.sort(Comparator.comparingLong(MappedStore::getPrevIndex));

        if (tmp.size() > 0) {
            if (tmp.get(0).getPrevIndex() > snapshotIndex) {
                for (MappedStore store : tmp) {
                    store.delete();
                }

                tmp.clear();
            }
            else {
                long index = tmp.get(0).getPrevIndex();
                for (int i = 1; i < tmp.size(); i++) {
                    long prev = tmp.get(i).getPrevIndex();
                    if (prev != index) {
                        for (int j = i; j < tmp.size(); j++) {
                            tmp.get(j).delete();
                        }
                        break;
                    }

                    index = tmp.get(i).getEndIndex();
                }
            }
        }

        pages.addAll(tmp);

        if (pages.size() == 0) {
            pages.add(new MappedStore(cluster, path, snapshotIndex, pageSize));
        }

        for (MappedStore page : pages) {
            cluster.logInfo("Opened page at " + page.getPath() +
                            " entries : (" + page.getPrevIndex() +
                            " to " + page.getEndIndex() + "]");
        }
    }

    public void close()
    {
        for (MappedStore page : pages) {
            try {
                page.close();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public void deleteFirst()
    {
        MappedStore store = pages.removeFirst();
        store.delete();
    }

    /**
     * Delete log stores.
     *
     * There might be multiple log stores, this method deletes all,
     * mostly called prior to deleting a cluster
     *
     * @throws IOException on any IO error
     */
    public void deleteAll()
    {
        if (pages.size() > 0) {
            for (MappedStore page : pages) {
                page.delete();
            }

            pages.clear();
        }
        else {
            try {
                List<Path> paths = Files.walk(path)
                                        .filter(Files::isRegularFile)
                                        .filter(p -> p.toString().endsWith(".store"))
                                        .collect(Collectors.toList());

                for (Path path : paths) {
                    Files.deleteIfExists(path);
                }
            }
            catch (IOException e) {
                cluster.logError(e);
                throw new UncheckedIOException(e);
            }
        }
    }

    public long getLastIndex()
    {
        return lastIndex;
    }

    public long getLastTerm()
    {
        return lastTerm;
    }

    public boolean isStarted()
    {
        return lastIndex != 0;
    }

    public boolean isSnapshotPossible()
    {
        return pages.size() > 1;
    }

    public void flush()
    {
        for (MappedStore page : pages) {
            page.flush();
        }
    }

    public void add(Entry entry)
    {
        if (entry.encodedLen() > MAX_ENTRY_SIZE) {
            throw new RaftException("Entry's size exceeds store size " +
                                        entry.encodedLen());
        }

        MappedStore store = pages.peekLast();
        if (store.remaining() < entry.encodedLen()) {
            store = new MappedStore(cluster, path, lastIndex, pageSize);
            pages.add(store);
            cluster.checkCompaction();
        }

        store.add(entry);
        lastIndex = entry.getIndex();
        lastTerm  = entry.getTerm();
    }

    public void removeFrom(long index)
    {
        for (MappedStore page : pages) {
            page.removeFrom(index);
        }
    }

    public Entry get(long index)
    {
        if (index > lastIndex) {
            return null;
        }

        for (MappedStore page : pages) {
            Entry entry = page.getEntry(index);
            if (entry != null) {
                return entry;
            }
        }

        return null;
    }

    public long getFirstPageEnd()
    {
        return pages.peekFirst().getEndIndex();
    }

    public Buffer rawEntriesFrom(long index)
    {
        Buffer out = new Buffer();
        for (MappedStore page : pages) {
            Buffer buf = page.rawEntriesFrom(index);
            if (buf != null) {
                out.next = buf;
                out = buf;
            }
        }

        return out.next;
    }
}
