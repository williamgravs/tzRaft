package tz.base.poll;


import tz.base.log.LogOwner;

import java.util.Deque;

/**
 * Owner interface for Poll object. Thread implementations implements this
 * interface mostly.
 */
public interface PollOwner extends LogOwner
{
    /**
     * This is called to inform owner that all events are processed, so caller
     * can take action according to it, ex: handling events in batches
     */
    void handleEvents(Deque<Event> events);
}
