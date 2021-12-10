package com.example;



/**
 * Interface defining a listener for significant Session generated events.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.1 $ $Date: 2001/07/29 03:43:54 $
 */

public interface SessionListener {


    /**
     * Acknowledge the occurrence of the specified event.
     *
     * @param event SessionEvent that has occurred
     */
    public void sessionEvent(SessionEvent event);


}
