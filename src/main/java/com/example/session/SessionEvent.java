package com.example.session;


import java.util.EventObject;


/**
 * General event for notifying listeners of significant changes on a Session.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.1 $ $Date: 2001/07/29 03:43:54 $
 */

public final class SessionEvent
    extends EventObject {


    /**
     * The event data associated with this event.
     */
    private final Object data;


    /**
     * The Session on which this event occurred.
     */
    private final Session session;


    /**
     * The event type this instance represents.
     */
    private final String type;


    /**
     * Construct a new SessionEvent with the specified parameters.
     *
     * @param session Session on which this event occurred
     * @param type Event type
     * @param data Event data
     */
    public SessionEvent(Session session, String type, Object data) {

        super(session);
        this.session = session;
        this.type = type;
        this.data = data;

    }


    /**
     * Return the event data of this event.
     */
    public Object getData() {

        return (this.data);

    }


    /**
     * Return the Session on which this event occurred.
     */
    public Session getSession() {

        return (this.session);

    }


    /**
     * Return the event type of this event.
     */
    public String getType() {

        return (this.type);

    }


    /**
     * Return a string representation of this event.
     */
    public String toString() {

        return ("SessionEvent['" + getSession() + "','" +
                getType() + "']");

    }


}
