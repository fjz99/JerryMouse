package com.example.session;

import java.util.EventListener;


/**
 * Interface defining a listener for significant Session generated events.
 *
 * @author Craig R. McClanahan
 */
public interface SessionListener extends EventListener {


    /**
     * Acknowledge the occurrence of the specified event.
     *
     * @param event SessionEvent that has occurred
     */
    void sessionEvent(SessionEvent event);


}
