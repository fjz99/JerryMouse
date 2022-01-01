package com.example;


/**
 * Interface defining a listener for significant Container generated events.
 * Note that "container start" and "container stop" events are normally
 * LifecycleEvents, not ContainerEvents.
 *
 * @author Craig R. McClanahan
 */
public interface ContainerListener {


    /**
     * Acknowledge the occurrence of the specified event.
     *
     * @param event ContainerEvent that has occurred
     */
    void containerEvent(ContainerEvent event);


}
