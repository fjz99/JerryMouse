package com.example.loader;


import com.example.Context;

import java.beans.PropertyChangeListener;
import java.util.List;


/**
 * A <b>Loader</b> represents a Java ClassLoader implementation that can
 * be used by a Container to load class files (within a repository associated
 * with the Loader) that are designed to be reloaded upon request, as well as
 * a mechanism to detect whether changes have occurred in the underlying
 * repository.
 * <p>
 * In order for a <code>Loader</code> implementation to successfully operate
 * with a <code>Context</code> implementation that implements reloading, it
 * must obey the following constraints:
 * <ul>
 * <li>Must implement <code>Lifecycle</code> so that the Context can indicate
 *     that a new class loader is required.
 * <li>The <code>start()</code> method must unconditionally create a new
 *     <code>ClassLoader</code> implementation.
 * <li>The <code>stop()</code> method must throw away its reference to the
 *     <code>ClassLoader</code> previously utilized, so that the class loader,
 *     all classes loaded by it, and all objects of those classes, can be
 *     garbage collected.
 * <li>Must allow a call to <code>stop()</code> to be followed by a call to
 *     <code>start()</code> on the same <code>Loader</code> instance.
 * <li>Based on a policy chosen by the implementation, must call the
 *     <code>Context.reload()</code> method on the owning <code>Context</code>
 *     when a change to one or more of the class files loaded by this class
 *     loader is detected.
 * </ul>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.6 $ $Date: 2002/09/19 22:55:47 $
 */

public interface Loader {

    ClassLoader getClassLoader();


    Context getContext();


    void setContext(Context context);


    /**
     * Return the DefaultContext with which this Manager is associated.
     */
//    public DefaultContext getDefaultContext();


    /**
     * Set the DefaultContext with which this Manager is associated.
     *
     * @param defaultContext The newly associated DefaultContext
     */
//    public void setDefaultContext(DefaultContext defaultContext);

    /**
     * Return the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     */
    boolean getDelegate();


    /**
     * Set the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     *
     * @param delegate The new flag
     */
    void setDelegate(boolean delegate);


    /**
     * Return the reloadable flag for this Loader.
     */
    boolean getReloadable();


    /**
     * Set the reloadable flag for this Loader.
     *
     * @param reloadable The new reloadable flag
     */
    void setReloadable(boolean reloadable);


    /**
     * Add a property change listener to this component.
     *
     * @param listener The listener to add
     */
    void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * Add a new repository to the set of repositories for this class loader.
     *
     * @param repository Repository to be added
     */
    void addRepository(String repository);


    /**
     * Return the set of repositories defined for this class loader.
     * If none are defined, a zero-length array is returned.
     */
    List<String> findRepositories();


    /**
     * Has the internal repository associated with this Loader been modified,
     * such that the loaded classes should be reloaded?
     */
    boolean modified();


    /**
     * Remove a property change listener from this component.
     *
     * @param listener The listener to remove
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Execute a periodic task, such as reloading, etc. This method will be
     * invoked inside the classloading context of this container. Unexpected
     * throwables will be caught and logged.
     * <p>
     * 会在AbstractContainer处被调用，统一添加一个检测线程，
     * 并级联递归调用所有的子组件的backgroundProcess
     */
    void backgroundProcess();
}
