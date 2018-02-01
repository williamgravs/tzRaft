package tz.base.poll;

import java.util.PriorityQueue;

/**
 * Timer class
 */
public class Timer
{
    private final PriorityQueue<TimerEvent> timers;

    /**
     * Create new Timer
     */
    public Timer()
    {
        timers = new PriorityQueue<>(20, new TimerEvent.Compare());
    }

    /**
     * Add timer
     *
     * @param event Timer event
     */
    public void add(TimerEvent event)
    {
        timers.add(event);
    }

    /**
     * Remove timer
     *
     * @param event Timer event
     */
    public void remove(TimerEvent event)
    {
        timers.remove(event);
    }

    /**
     * Execute timers for timestamp, return first timer's timeout for next
     * iteration
     *
     * @param timestamp Current timestamp
     * @return Next timeout
     */
    public long execute(long timestamp)
    {
        while (true) {
            TimerEvent timer = timers.peek();
            if (timer == null) {
                return 0;
            }

            if (timer.timeout > timestamp) {
                break;
            }

            timer = timers.poll();

            if (timer.periodic) {
                timer.updateTimeout(timestamp + timer.interval);
                timers.add(timer);
            }

            timer.onTimeout();
        }

        TimerEvent timer = timers.peek();
        if (timer == null) {
            return 0;
        }

        return timer.timeout - timestamp;
    }
}
