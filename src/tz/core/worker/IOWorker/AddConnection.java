package tz.core.worker.IOWorker;

import tz.base.poll.Event;
import tz.core.Connection;

/**
 * Add connection event
 *
 * Informs IO worker to execute connection
 */
public class AddConnection implements Event
{
    private final IOWorker worker;
    private final Connection conn;

    /**
     * Create new AddConnectionEvent
     *
     * @param worker IO worker
     * @param conn   connection
     */
    public AddConnection(IOWorker worker, Connection conn)
    {
        this.worker = worker;
        this.conn   = conn;
    }

    /**
     * Handle callback
     */
    @Override
    public void onEvent()
    {
        worker.handleAddConnection(conn);
    }
}
