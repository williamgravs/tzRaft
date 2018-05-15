package tz.base.poll;

import java.util.PriorityQueue;

public class Timer
{
    private final PriorityQueue<TimerEvent> timers;

    public Timer()
    {
        timers = new PriorityQueue<>(20, new TimerEvent.Compare());
    }

    public void add(TimerEvent event)
    {
        timers.add(event);
    }

    public void remove(TimerEvent event)
    {
        timers.remove(event);
    }

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
