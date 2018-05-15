package tz.core.worker.IOWorker;

import tz.base.poll.Event;
import tz.core.Connection;
import tz.core.msg.Msg;

import java.util.Deque;

public class OutgoingMsg implements Event
{
    private IOWorker worker;
    private Connection conn;
    private Deque<Msg> msgs;

    public OutgoingMsg(IOWorker worker, Connection conn, Deque<Msg> msgs)
    {
        this.worker = worker;
        this.conn   = conn;
        this.msgs   = msgs;
    }

    @Override
    public void onEvent()
    {
        worker.handleOutgoingMsg(conn, msgs);
    }
}
