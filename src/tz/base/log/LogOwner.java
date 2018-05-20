package tz.base.log;


/**
 * Log owner class
 *
 * Each thread must be a log owner to be able to create log lines.
 */
public interface LogOwner
{
    /**
     * Provide log instance
     * There is single log instance which holds desired log level etc.
     *
     * @return  Log instance
     */
    Log getLogger();

    /**
     * Get timestamp of the thread. If threads are running a event loop, they
     * may already have an updated timestamp info, so they might prefer to use
     * it, otherwise they might call System.currentTimeMillis() or
     * System.nanoTime();
     *
     * For debugging purposes, using a unified timestamp of thread
     * is better. So, log reader may understand events occured in one loop
     *
     * @return  Timestamp in millis
     */
    long getTimestamp();

    /**
     * Get current thread's name
     *
     * @return  Current thread's name
     */
    String getName();


    /**
     * Create DEBUG log line
     *
     * @param  args
     *         Variable sized arguments, could be any type, they will be
     *         combined if log level is permitted
     */
    default void logDebug(Object... args)
    {
        getLogger().debug(getTimestamp(), getName(), null, args);
    }

    /**
     * Create INFO log line
     *
     * @param  args
     *         Variable sized arguments, could be any type, they will be
     *         combined if log level is permitted
     */
    default void logInfo(Object... args)
    {
        getLogger().info(getTimestamp(), getName(), null,  args);
    }

    /**
     * Create WARNING log line
     *
     * @param  args
     *         Variable sized arguments, could be any type, they will be
     *         combined if log level is permitted
     */
    default void logWarn(Object... args)
    {
        getLogger().warn(getTimestamp(), getName(), null, args);
    }

    /**
     * Create ERROR log line
     *
     * @param  args
     *         Variable sized arguments, could be any type, they will be
     *         combined if log level is permitted
     */
    default void logError(Object... args)
    {
        getLogger().error(getTimestamp(), getName(), null, args);
    }

    /**
     * Create ERROR log line with a throwable object
     *
     * @param  t
     *         Any throwable to report
     *
     * @param  args
     *         Variable sized arguments, could be any type, they will be
     *         combined if log level is permitted
     */
    default void logError(Throwable t, Object... args)
    {
        getLogger().error(getTimestamp(), getName(), t, args);
    }
}
