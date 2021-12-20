package com.example.session;


import java.beans.PropertyChangeListener;
import java.io.IOException;


/**
 * A <b>Store</b> is the abstraction of a Catalina component that provides
 * persistent storage and loading of Sessions and their associated user data.
 * Implementations are free to save and load the Sessions to any media they
 * wish, but it is assumed that saved Sessions are persistent across
 * server or context restarts.
 *
 * @author Craig R. McClanahan
 */
public interface Store {

    // ------------------------------------------------------------- Properties

    /**
     * @return the Manager instance associated with this Store.
     */
    Manager getManager();


    /**
     * Set the Manager associated with this Store.
     *
     * @param manager The Manager which will use this Store.
     */
    void setManager(Manager manager);


    /**
     * @return the number of Sessions present in this Store.
     *
     * @exception IOException if an input/output error occurs
     */
    int getSize() throws IOException;


    // --------------------------------------------------------- Public Methods


    /**
     * Add a property change listener to this component.
     *
     * @param listener The listener to add
     */
    void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * @return an array containing the session identifiers of all Sessions
     * currently saved in this Store.  If there are no such Sessions, a
     * zero-length array is returned.
     *
     * @exception IOException if an input/output error occurred
     */
    String[] keys() throws IOException;


    /**
     * Load and return the Session associated with the specified session
     * identifier from this Store, without removing it.  If there is no
     * such stored Session, return <code>null</code>.
     *
     * @param id Session identifier of the session to load
     *
     * @exception ClassNotFoundException if a deserialization error occurs
     * @exception IOException if an input/output error occurs
     * @return the loaded Session instance
     */
    Session load(String id)
        throws ClassNotFoundException, IOException;


    /**
     * Remove the Session with the specified session identifier from
     * this Store, if present.  If no such Session is present, this method
     * takes no action.
     *
     * @param id Session identifier of the Session to be removed
     *
     * @exception IOException if an input/output error occurs
     */
    void remove(String id) throws IOException;


    /**
     * Remove all Sessions from this Store.
     *
     * @exception IOException if an input/output error occurs
     */
    void clear() throws IOException;


    /**
     * Remove a property change listener from this component.
     *
     * @param listener The listener to remove
     */
    void removePropertyChangeListener(PropertyChangeListener listener);


    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */
    void save(Session session) throws IOException;


}
