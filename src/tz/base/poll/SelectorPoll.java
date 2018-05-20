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


/**
 * Poller with Selector backed
 */
public class SelectorPoll extends Poll
{
    private final Selector selector;
    private final AtomicBoolean awake;
    private final AtomicBoolean wakenUp;


    /**
     * Create a new SelectorPoll
     *
     * @param  worker Worker thread for this poller
     */
    public SelectorPoll(Worker worker)
    {
        super(worker);

        try {
            this.selector = Selector.open();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.awake   = new AtomicBoolean(false);
        this.wakenUp = new AtomicBoolean(false);
        timestamp    = Util.time();
    }

    /**
     * Get selector
     *
     * @return  Selector
     */
    @Override
    public Selector getSelector()
    {
        return selector;
    }

    /**
     * Register socket with interest ops
     *
     * @param  sock  Socket
     * @param  ops   Interest Ops
     *
     */
    public void add(Sock sock, int ops)
    {
        sock.register(selector, ops);
    }

    /**
     * Add event
     *
     * @param  event  Event object to be processed
     */
    @Override
    public void addEvent(Event event)
    {
        try {
            queue.put(event);
            if (!awake.get() && wakenUp.compareAndSet(false, true)) {
                selector.wakeup();
            }
        }
        catch (InterruptedException e) {
            worker.logError(e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Process selected keys
     *
     * @param  readyKeys  Keys ready to be processed
     */
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


    /**
     * Main loop of the poller
     *
     * @throws  IOException On any IO exception
     */
    @Override
    public void loop() throws IOException
    {
        long timeout = 1;

        while (!stop) {
            awake.set(false);
            wakenUp.set(false);
            processEvents();
            selector.select(timeout);
            awake.set(true);
            timestamp = Util.time();
            processSelect(selector.selectedKeys());
            processEvents();
            timeout = timer.execute(timestamp);
        }
    }
}