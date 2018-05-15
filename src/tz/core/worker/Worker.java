package tz.core.worker;

import tz.base.log.*;
import tz.base.poll.*;
import tz.base.transport.sock.Sock;


/**
 * Abstract worker
 */
public abstract class Worker implements Runnable, LogOwner, PollOwner
{
    protected final Log log;
    private String name;
    private Thread thread;
    protected Poll poll;
    private boolean stop;

    /**
     * Create new worker
     * @param log  log object, mostly there is one in each instance of this library
     * @param name worker name
     */
    public Worker(Log log, String name, boolean ioWorker)
    {
        this.log    = log;
        this.name   = name;
        this.thread = new Thread(this, name);
        this.poll   = ioWorker ? new SelectorPoll(this) : new Poll(this);
    }

    public void stop()
    {
        stop = true;
        thread = new Thread(this, thread.getName());
    }

    /**
     * Start thread
     */
    public void start()
    {
        logInfo(toString() + " starting..");
        thread.start();
    }

    /**
     * Run this worker
     */
    @Override
    public void run()
    {
        while (!stop) {
            try {
                poll.loop();
            }
            catch (Exception e) {
                logError(e, "Trying to survive.. ");

                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * Get worker timestamp
     * @return timestamp
     */
    public long timestamp()
    {
        return poll.getTimestamp();
    }

    /**
     * Add an event to this worker, called by other workers
     * @param event event
     */
    public void addEvent(Event event)
    {
        poll.addEvent(event);
    }

    /**
     * Get logger
     * @return logger
     */
    @Override
    public Log getLogger()
    {
        return log;
    }

    /**
     * Get timestamp
     * @return timestamp
     */
    @Override
    public long getTimestamp()
    {
        return timestamp();
    }

    /**
     * Get name
     * @return worker's name
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Add a timer
     * @param timer
     */
    public void addTimer(TimerEvent timer)
    {
        poll.addTimer(timer);
    }

    /**
     * Remove a timer
     * @param timer
     */
    public void removeTimer(TimerEvent timer)
    {
        poll.removeTimer(timer);
    }

    /**
     * Register a socket to worker's loop
     * @param sock socket
     * @param ops  interest ops(read, write, connect)
     */
    public void register(Sock sock, int ops)
    {
        poll.add(sock, ops);
    }

    /**
     * Get string representation
     * @return worker's name
     */
    @Override
    public String toString()
    {
        return name;
    }
}
