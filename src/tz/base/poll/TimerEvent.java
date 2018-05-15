package tz.base.poll;

import java.util.Comparator;

/**
 * Timer abstract class, any timer implementation must extend this and
 * implement onTimeout() method.
 */
public abstract class TimerEvent
{
    public boolean periodic;
    public long interval;
    public long timeout;

    /**
     * Create new timer
     *
     * @param periodic is this timer recurring
     * @param interval interval of each period
     * @param timeout  next timeout
     */
    public TimerEvent(boolean periodic, long interval, long timeout)
    {
        this.periodic = periodic;
        this.interval = interval;
        this.timeout  = timeout;
    }

    /**
     * Update timeout for periodic timers
     *
     * @param timeout next timeout
     */
    public void updateTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    /**
     * Comparator according to timeout values
     */
    public static class Compare implements Comparator<TimerEvent>
    {
        @Override
        public int compare(TimerEvent o1, TimerEvent o2)
        {
            return (int) (o1.timeout - o2.timeout);
        }
    }

    /**
     * Timeout occured
     */
    public abstract void onTimeout();
}
