package tz.base.exception;

/**
 * Generic unchecked exception class to use with RAFT implementation error cases
 */
public class RaftException extends RuntimeException
{
    /**
     * Create exception with log info
     *
     * @param   log
     *          Log line to be held in this exception
     */
    public RaftException(String log)
    {
        super(log);
    }

    /**
     * Create Raft Exception
     *
     * @param   e
     *          Any exception to be held in this exception object. This is
     *          used mostly change checked exceptions to unchecked ones.
     */
    public RaftException(Exception e)
    {
        super(e);
    }

}