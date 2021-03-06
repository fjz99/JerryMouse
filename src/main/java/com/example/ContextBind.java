package com.example;

public interface ContextBind {

    /**
     * Change the current thread context class loader to the web application
     * class loader. If no web application class loader is defined, or if the
     * current thread is already using the web application class loader then no
     * change will be made. If the class loader is changed and a
     * {@link org.apache.catalina.ThreadBindingListener} is configured then
     * {@link org.apache.catalina.ThreadBindingListener#bind()} will be called
     * after the change has been made.
     *
     * @param originalClassLoader The current class loader if known to save this method having to
     *                            look it up
     * @return If the class loader has been changed by the method it will return
     * the thread context class loader in use when the method was
     * called. If no change was made then this method returns null.
     */
    ClassLoader bind(ClassLoader originalClassLoader);

    /**
     * Restore the current thread context class loader to the original class
     * loader in used before {@link #bind(boolean, ClassLoader)} was called. If
     * no original class loader is passed to this method then no change will be
     * made. If the class loader is changed and a
     * {@link org.apache.catalina.ThreadBindingListener} is configured then
     * {@link org.apache.catalina.ThreadBindingListener#unbind()} will be called
     * before the change is made.
     *
     * @param originalClassLoader The class loader to restore as the thread context class loader
     */
    void unbind(ClassLoader originalClassLoader);
}
