package tz.base.poll;

/**
 * Interface for poll items.
 */
public interface Fd
{
    /**
     * This method is called when Fd needs to be shutdown, indicates error
     * mostly
     * @param e Related exception caused this FD's shutdown method called
     */
    void shutdown(Exception e);

    /**
     * When remote connection attempt succeeds, onConnect is called
     */
    default void onConnect()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * When there is connection attempt to us, this method is called
     */
    default void onAccept()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * When there is data in FD to be read, this method is called
     */
    default void onRead()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * When there is space in FD's related buffer, this method is called
     */
    default void onWrite()
    {
        throw new UnsupportedOperationException();
    }
}
