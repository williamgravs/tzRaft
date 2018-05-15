package tz.core.client;

import tz.base.log.LogHandler;
import tz.base.record.ClusterRecord;

import java.nio.ByteBuffer;

public interface ClientListener extends LogHandler
{
    void connectionState(boolean connected);
    void requestCompleted(long sequence, ByteBuffer buf);
    void configChange(ClusterRecord record);
}
