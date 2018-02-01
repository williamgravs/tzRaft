package tz.base.poll;

import tz.base.common.Util;
import tz.base.transport.sock.Sock;
import tz.core.worker.Worker;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Poll
{
    //Owner worker
    final Worker worker;

    //Timestamp in nanoseconds, updated on each loop
    long timestamp;

    //Event holder, mostly events from other threads
    final ArrayBlockingQueue<Event> queue;

    private Deque<Event> events;
    Timer timer;

    boolean stop;

    /**
     * Create new Poll
     * @param worker Worker thread owns this Poll object
     */
    public Poll(Worker worker)
    {
        this.worker = worker;
        this.events = new ArrayDeque<>();
        this.queue  = new ArrayBlockingQueue<>(100000);
        this.timer  = new Timer();
        this.stop   = false;

        timestamp   = Util.time();
    }

    /**
     * Stop the loop
     */
    public void stop()
    {
        stop = true;
    }

    /**
     * Get timestamp
     *
     * @return  latest timestamp of the event loop
     */
    public long getTimestamp()
    {
        return timestamp;
    }

    /**
     * Pass an event to this poll, mostly called by other threads than this
     * one's owner
     *
     * @param event Event object to be processed
     */
    public void addEvent(Event event)
    {
        try {
            queue.put(event);
        }
        catch (InterruptedException e) {
            worker.logError(e);
            throw new IllegalStateException(e);
        }
    }


    /**
     * Process events
     */
    protected void processEvents()
    {
        try {
            queue.drainTo(events);
            worker.handleEvents(events);
        }
        catch (Exception e) {
            worker.logError(e, "Trying to survive ");
        }
    }

    /**
     * Main loop for a thread
     *
     * @throws IOException If selector fails
     */
    public void loop() throws IOException, InterruptedException
    {
        long timeout = 0;

        while (!stop) {
            Event event = queue.poll(timeout, TimeUnit.MILLISECONDS);
            if (event != null) {
                events.add(event);
            }

            timestamp = Util.time();
            processEvents();
            timeout = timer.execute(timestamp);
            if (timeout <= 0) {
                timeout = 2000;
            }
        }
    }

    /**
     * Add Timer
     *
     * @param event Timer to add
     */
    public void addTimer(TimerEvent event)
    {
        timer.add(event);
    }

    /**
     * Remove timer
     *
     * @param event Timer to remove
     */
    public void removeTimer(TimerEvent event)
    {
        timer.remove(event);
    }

    /**
     * Get selector
     *
     * @return Selector
     *
     * @throws UnsupportedOperationException When not overridden
     */
    public Selector getSelector()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Add socket with options to selector of this Poll object
     *
     * @param sock Socket
     * @param ops  Interest Ops
     *
     * @throws UnsupportedOperationException
     *         When not overridden
     */
    public void add(Sock sock, int ops)
    {
        throw new UnsupportedOperationException();
    }
}