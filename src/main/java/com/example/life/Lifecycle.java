package com.example.life;


import java.util.List;

/**
 * Common interface for component life cycle methods.  Catalina components
 * may, but are not required to, implement this interface (as well as the
 * appropriate interface(s) for the functionality they support) in order to
 * provide a consistent mechanism to start and stop the component.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.6 $ $Date: 2002/06/09 02:10:50 $
 */
public interface Lifecycle {
    // --------------------------------------------------------- Public Methods


    /**
     * Add a LifecycleEvent listener to this component.
     *
     * @param listener The listener to add
     */
    void addLifecycleListener(LifecycleListener listener);


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    List<LifecycleListener> findLifecycleListeners();


    /**
     * Remove a LifecycleEvent listener from this component.
     *
     * @param listener The listener to remove
     */
    void removeLifecycleListener(LifecycleListener listener);


    /**
     * Prepare for the beginning of active use of the public methods of this
     * component.  This method should be called before any of the public
     * methods of this component are utilized.  It should also send a
     * LifecycleEvent of type START_EVENT to any registered listeners.
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that prevents this component from being used
     */
    void start() throws LifecycleException;


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.  It should also send a LifecycleEvent
     * of type STOP_EVENT to any registered listeners.
     * <p>
     * 停止类似于recycle，所以一般都是clear，还需要能再次start
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that needs to be reported
     */
    void stop() throws LifecycleException;

}
