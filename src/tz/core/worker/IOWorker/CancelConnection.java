package tz.core.worker.IOWorker;

import tz.base.poll.Event;
import tz.core.Connection;

public class CancelConnection implements Event
{
    private IOWorker worker;
    private Connection conn;

    public CancelConnection(IOWorker worker, Connection conn)
    {
        this.worker = worker;
        this.conn   = conn;
    }

    @Override
    public void onEvent()
    {
        worker.handleCancelConnection(conn);
    }
}
