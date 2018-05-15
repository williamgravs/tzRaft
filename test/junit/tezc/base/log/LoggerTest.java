package junit.tezc.base.log;

import org.junit.Assert;
import org.junit.Test;
import tezc.base.log.Level;
import tezc.base.log.Log;
import tezc.base.log.LogHandler;
import tezc.base.log.LogOwner;

public class LoggerTest implements LogHandler, LogOwner
{
    private Log log;
    private int logLines;

    public LoggerTest()
    {
        log = new Log(this);
    }

    @Test
    public void run()
    {
        log.setLevel(Level.ERROR);
        int prevLogLine = logLines;
        this.logInfo("Print log");
        Assert.assertEquals(prevLogLine, logLines);

        prevLogLine = logLines;
        log.setLevel(Level.INFO);
        this.logInfo("Print log");
        Assert.assertNotEquals(prevLogLine, logLines);

        log.setLevel(Level.DEBUG);
        prevLogLine = logLines;
        this.logWarn("Print log");
        Assert.assertNotEquals(prevLogLine, logLines);

    }

    @Override
    public void onLog(Level level, long timestamp, String threadName,
                      String log, Throwable t)
    {
        logLines++;
        System.out.println(log);
    }

    @Override
    public Log getLogger()
    {
        return log;
    }

    @Override
    public long getTimestamp()
    {
        return System.currentTimeMillis();
    }

    @Override
    public String getName()
    {
        return "loggerTest";
    }
}
