package tz.core.worker.IOWorker;

import tz.base.poll.Event;
import tz.base.record.TransportRecord;

public class CancelEndpoint implements Event
{
    private IOWorker ioWorker;
    private TransportRecord transportRecord;

    public CancelEndpoint(IOWorker ioWorker, TransportRecord transportRecord)
    {
        this.ioWorker = ioWorker;
        this.transportRecord = transportRecord;
    }

    @Override
    public void onEvent()
    {
        ioWorker.handleCancelEndpoint(transportRecord);
    }
}
