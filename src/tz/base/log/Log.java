package tz.base.log;

/**
 * Simple log handler
 * As this project is a library, we dont do much with logging, just passing
 * log lines to application.
 *
 * For performance reasons, log lines are created in a lazy manner, only if
 * level matches the current logger level
 */
public class Log
{
    private final LogHandler handler;
    private Level logLevel;

    /**
     * Log handler is supposed to be application always. So, it must implement
     * LogHandler interface
     *
     * @param handler  Log handler
     * @param logLevel Log level
     *
     */
    public Log(LogHandler handler, String logLevel)
    {
        this.handler  = handler;
        this.logLevel = Level.valueOf(logLevel);
    }

    /**
     * Set log level. Lesser level logs will not be passed to log handler
     *
     * @param level New level for logging
     */
    public void setLevel(Level level)
    {
        this.logLevel = level;
    }

    /**
     * Main logging method with variable length arguments
     *
     * @param level     Log level
     * @param timestamp Log timestamp
     * @param owner     Thread name of the callee
     * @param t         Exception logging if there is any
     * @param args      Log items to build a log line
     */
    private void log(Level level,
                     long timestamp, String owner, Throwable t, Object ...args)
    {
        if (logLevel.ordinal() > level.ordinal()) {
            return;
        }

        StringBuilder builder = new StringBuilder(1024);
        for (Object o : args) {
            builder.append(o.toString());
        }

        synchronized (this) {
            if (handler != null) {
                handler.onLog(level, timestamp, owner, builder.toString(), t);
            }
        }
    }


    /**
     * Print DEBUG level log
     *
     * @param timestamp Log timestamp
     * @param thread    Caller thread's name
     * @param t         Exception loggin if there is any
     * @param args      Log items to build a log line
     */
    public void debug(long timestamp, String thread, Throwable t, Object ...args)
    {
        log(Level.DEBUG, timestamp, thread, t, args);
    }

    /**
     * Print INFO level log
     *
     * @param timestamp  Log timestamp
     * @param thread     Caller thread's name
     * @param t          Exception loggin if there is any
     * @param args       Log items to build a log line
     */
    public void info(long timestamp, String thread, Throwable t, Object ...args)
    {
        log(Level.INFO, timestamp, thread, t, args);
    }

    /**
     * Print WARNING level log
     *
     * @param timestamp Log timestamp
     * @param thread    Caller thread's name
     * @param t         Exception loggin if there is any
     * @param args      Log items to build a log line
     */
    public void warn(long timestamp, String thread, Throwable t, Object ...args)
    {
        log(Level.WARN, timestamp, thread, t, args);
    }

    /**
     * Print ERROR level log
     *
     * @param timestamp Log timestamp
     * @param thread    Caller thread's name
     * @param t         Exception loggin if there is any
     * @param args      Log items to build a log line
     */
    public void error(long timestamp, String thread, Throwable t, Object ...args)
    {
        log(Level.ERROR, timestamp, thread, t, args);
    }
}
