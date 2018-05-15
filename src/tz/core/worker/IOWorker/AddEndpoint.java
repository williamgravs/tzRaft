package tz.core.worker.IOWorker;

import tz.base.poll.Event;
import tz.base.record.TransportRecord;

/**
 * Add endpoint event
 *
 * Informs IO worker to execute endpoint (listen and accept connections)
 */
public class AddEndpoint implements Event
{
    private final IOWorker worker;
    private final TransportRecord transportRecord;

    /**
     * Create new AddPointEvent
     *
     * @param worker           IO worker
     * @param transportRecord  transport record contains endpoints info
     */
    public AddEndpoint(IOWorker worker, TransportRecord transportRecord)
    {
        this.worker          = worker;
        this.transportRecord = transportRecord;
    }

    @Override
    public void onEvent()
    {
        worker.handleAddEndpoint(transportRecord);
    }
}
