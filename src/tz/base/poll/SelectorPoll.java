package tz.base.poll;

import tz.base.common.Util;
import tz.base.transport.sock.Sock;
import tz.core.worker.Worker;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class SelectorPoll extends Poll
{
    private final Selector selector;
    private final AtomicBoolean awake;
    private final AtomicBoolean willWakeUp;


    public SelectorPoll(Worker worker)
    {
        super(worker);

        try {
            this.selector = Selector.open();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.awake      = new AtomicBoolean(false);
        this.willWakeUp = new AtomicBoolean(false);
        timestamp  = Util.time();
    }

    @Override
    public Selector getSelector()
    {
        return selector;
    }

    public void add(Sock sock, int ops)
    {
        sock.register(selector, ops);
    }

    @Override
    public void addEvent(Event event)
    {
        try {
            queue.put(event);
            if (!awake.get() && !willWakeUp.getAndSet(true)) {
                selector.wakeup();
            }
        }
        catch (InterruptedException e) {
            worker.logError(e);
            throw new IllegalStateException(e);
        }
    }


    private void processSelect(Set<SelectionKey> readyKeys)
    {
        Iterator<SelectionKey> it = readyKeys.iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            Fd fdEvent = (Fd) key.attachment();
            it.remove();

            if (!key.isValid()) {
                continue;
            }

            try {
                if (key.isValid() && key.isConnectable()) {
                    fdEvent.onConnect();
                }
                
                if (key.isValid() && key.isAcceptable()) {
                    fdEvent.onAccept();
                }

                if (key.isValid() && key.isReadable()) {
                    fdEvent.onRead();
                }

                if (key.isValid() && key.isWritable()) {
                    fdEvent.onWrite();
                }
            }
            catch (Exception e) {
                try {
                    fdEvent.shutdown(e);
                }
                catch (Exception e1) {
                    worker.logError(e1, "Trying to survive ");
                }
            }
        }
    }


    @Override
    public void loop() throws IOException
    {
        long timeout = 0;

        while (!stop) {

            timeout = timer.execute(timestamp);
            willWakeUp.set(false);
            awake.set(false);
            selector.select(timeout);
            awake.set(true);
            willWakeUp.set(true);
            timestamp = Util.time();

            processSelect(selector.selectedKeys());
            processEvents();
        }
    }
}

