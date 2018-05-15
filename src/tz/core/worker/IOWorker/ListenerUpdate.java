package tz.core.worker.IOWorker;

import tz.base.poll.Event;
import tz.base.transport.listener.Listener;

public class ListenerUpdate implements Event
{
    private IOOwner owner;
    private Listener listener;
    private boolean active;

    public ListenerUpdate(Listener listener, boolean active)
    {
        this.listener = listener;
        this.active   = active;
    }

    @Override
    public void onEvent()
    {
        owner.handleListenerUpdate(listener, active);
    }
}
