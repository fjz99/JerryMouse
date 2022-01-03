package com.example.life;

import com.example.core.AbstractContainer;

public enum EventType {

    /**
     * 周期任务 {@link AbstractContainer#backgroundProcess()}
     */
    PERIODIC_EVENT ("periodic"),

    /**
     * The LifecycleEvent type for the "component start" event.
     */
    START_EVENT ("start"),


    /**
     * The LifecycleEvent type for the "component before start" event.
     */
    BEFORE_START_EVENT ("before_start"),


    /**
     * The LifecycleEvent type for the "component after start" event.
     */
    AFTER_START_EVENT ("after_start"),


    /**
     * The LifecycleEvent type for the "component stop" event.
     */
    STOP_EVENT ("stop"),


    /**
     * The LifecycleEvent type for the "component before stop" event.
     */
    BEFORE_STOP_EVENT ("before_stop"),


    /**
     * The LifecycleEvent type for the "component after stop" event.
     */
    AFTER_STOP_EVENT ("after_stop");

    private final String message;

    EventType(String e) {
        message = e;
    }

    public String getMessage() {
        return message;
    }

}
