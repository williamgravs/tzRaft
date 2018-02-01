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
     * @param level      Log level
     * @param timestamp  Log timestamp
     * @param threadName Log producer thread's name
     * @param log        Log line
     * @param t          Exception if any
     */
    void onLog(Level level, long timestamp,
               String threadName, String log, Throwable t);
}
