package tz.base.poll;


/**
 * Event interface to be used in inter thread communication in a non blocking
 * manner
 */
public interface Event
{
    /**
     * This method will be called when event is ready to be processed
     */
    void onEvent();
}
