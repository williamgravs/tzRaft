package tz.base.exception;


/**
 * TLS connection related exception class. Extends RuntimeException so it is
 * unchecked itself.
 */
public class TlsException extends RuntimeException
{
    /**
     * Create new TLS exception with info line
     * @param log log line to be held in this exception obj
     */
    public TlsException(String log)
    {
        super(log);
    }

    /**
     * Create new TLS exception with a caught previous exception
     * @param e Exception arg to this object. Mostly used to make checked
     *          exceptions to unchecked.
     */
    public TlsException(Exception e)
    {
        super(e);
    }
}
