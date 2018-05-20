package tz.base.log;

/**
 * Interface to get log info out of the logger, implementations must implement
 * this interface and pass it to Log class constructor
 */
public interface LogHandler
{
    /**
     * This method will be called whenever a log line created, it is not thread
     * safe, multiple threads can call simultaneously,
     * so implementation should use proper locking
     *
     * @param  level       log level
     * @param  timestamp   log timestamp
     * @param  threadName  log producer thread's name
     * @param  log         log line
     * @param  t           exception if any
     */
    void onLog(Level level, long timestamp,
               String threadName, String log, Throwable t);
}
