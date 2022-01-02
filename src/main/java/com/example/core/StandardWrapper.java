package com.example.core;

import com.example.Container;
import com.example.ContainerServlet;
import com.example.Context;
import com.example.Wrapper;
import com.example.life.LifecycleException;
import com.example.loader.Loader;
import com.example.valve.basic.StandardWrapperValve;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.core.Constants.SYSTEM_PREFIX;

/**
 * 不支持{@link javax.servlet.SingleThreadModel}
 * 这个类可能被多线程访问
 * 和{@link com.example.filter.FilterConfigImpl}类似
 *
 * @date 2021/12/26 18:27
 */
@Slf4j
public final class StandardWrapper extends AbstractContainer implements Wrapper, ServletConfig {

    /**
     * The initialization parameters for this servlet, keyed by
     * parameter name.
     */
    private final Map<String, String> parameters = new ConcurrentHashMap<> ();
    /**
     * The date and time at which this servlet will become available (in
     * milliseconds since the epoch), or zero if the servlet is available.
     * If this value equals Long.MAX_VALUE, the unavailability of this
     * servlet is considered permanent.
     * 对应的是时间戳，可以和{@link System#currentTimeMillis()}比较
     */
    private long available = 0L;
    /**
     * The (single) initialized instance of this servlet.
     */
    private volatile Servlet instance = null;
    /**
     * The load-on-startup order value (negative value means load on
     * first call) for this servlet.
     * 默认懒加载
     * 具体在context处完成加载
     */
    private int loadOnStartup = -1;
    /**
     * The run-as identity for this servlet.
     */
    private String runAs = null;
    /**
     * The fully qualified servlet class name for this servlet.
     */
    private String servletClass = null;


    public StandardWrapper() {
        pipeline.setBasic (new StandardWrapperValve ());
    }

    @Override
    public void setParent(Container container) {
        if (!(container instanceof Context)) {
            throw new IllegalArgumentException ();
        }

        super.setParent (container);
    }

    @Override
    public void addChild(Container child) {
        throw new IllegalStateException ("Wrapper doesn't have child.");
    }

    @Override
    public long getAvailable() {
        return available;
    }

    @Override
    public void setAvailable(long available) {
        long oldAvailable = this.available;
        if (available > System.currentTimeMillis ())
            this.available = available;
        else
            this.available = 0L;
        support.firePropertyChange ("available", oldAvailable, this.available);
    }

    @Override
    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    @Override
    public void setLoadOnStartup(int value) {
        int oldLoadOnStartup = this.loadOnStartup;
        this.loadOnStartup = value;
        support.firePropertyChange ("loadOnStartup",
                oldLoadOnStartup, this.loadOnStartup);
    }

    /**
     * 为了兼容web.xml解析
     */
    public void setLoadOnStartupString(String value) {
        try {
            setLoadOnStartup (Integer.parseInt (value));
        } catch (NumberFormatException e) {
            setLoadOnStartup (0);
        }
    }

    @Override
    public String getRunAs() {
        return runAs;
    }

    @Override
    public void setRunAs(String runAs) {
        String oldRunAs = this.runAs;
        this.runAs = runAs;
        support.firePropertyChange ("runAs", oldRunAs, this.runAs);
    }

    @Override
    public String getServletClass() {
        return servletClass;
    }

    @Override
    public void setServletClass(String servletClass) {
        String oldServletClass = this.servletClass;
        this.servletClass = servletClass;
        support.firePropertyChange ("servletClass", oldServletClass,
                this.servletClass);
    }

    @Override
    public boolean isUnavailable() {
        return available > System.currentTimeMillis ();
    }

    @Override
    public void addInitParameter(String name, String value) {
        parameters.put (name, value);
        fireContainerEvent ("addInitParameter", name);
    }

    @Override
    public void addSecurityReference(String name, String link) {
        throw new UnsupportedOperationException ();
    }

    /**
     * double check
     */
    @Override
    public Servlet allocate() throws ServletException {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = loadServlet ();
                }
            }
        }
        return instance;
    }

    /**
     * 获得类加载器、加载类、实例化、init
     */
    private Servlet loadServlet() throws ServletException {
        if (servletClass == null) {
            unavailable (null);
            log.error ("loadServlet:class = null");
            throw new ServletException ();
        }

        //loader
        ClassLoader classLoader = getLoader ().getClassLoader ();
        if (isSystem (servletClass)) {
            classLoader = getClass ().getClassLoader ();
        }

        Class<?> clazz;
        try {
            clazz = classLoader.loadClass (servletClass);
        } catch (Throwable e) {
            unavailable (null);
            log.error ("", e);
            throw new ServletException ();
        }

        Servlet servlet;
        try {
            servlet = (Servlet) clazz.newInstance ();
        } catch (ClassCastException e) {
            unavailable (null);
            log.error ("类" + servletClass + "不是servlet", e);
            throw new ServletException ();
        } catch (Throwable e) {
            unavailable (null);
            log.error ("", e);
            throw new ServletException ();
        }

        if (isSystem (servletClass) && (servlet instanceof ContainerServlet)) {
            ((ContainerServlet) servlet).setWrapper (this);
        }

        try {
            servlet.init (this);
        } catch (UnavailableException e) {
            unavailable (e);
            log.error ("", e);
            throw e;
        } catch (Throwable e) {
            unavailable (null);
            log.error ("", e);
            throw new ServletException ();
        }

        fireContainerEvent ("load", this);
        return servlet;
    }

    /**
     * 判断是否是系统的servlet或者filter
     */
    private boolean isSystem(String servletClass) {
        return servletClass.startsWith (SYSTEM_PREFIX);
    }

    /**
     * 因为不是{@link SingleThreadModel}所以这个方法啥也不做
     *
     * @param servlet The servlet to be returned
     */
    @Override
    public void deallocate(Servlet servlet) {

    }

    @Override
    public String findInitParameter(String name) {
        return parameters.get (name);
    }

    @Override
    public String[] findInitParameters() {
        return parameters.keySet ().toArray (new String[0]);
    }

    /**
     * 和{@link #allocate()}不同，这个是一定load一个Servlet
     */
    @Override
    public void load() throws ServletException {
        instance = loadServlet ();
    }

    @Override
    public void removeInitParameter(String name) {
        parameters.remove (name);
        fireContainerEvent ("removeInitParameter", name);
    }

    /**
     * 也可能是内部发生了异常，这样就永久不可用
     */
    @Override
    public void unavailable(UnavailableException unavailable) {
        if (unavailable == null || unavailable.isPermanent ()) {
            setAvailable (Long.MAX_VALUE);
            return;
        }

        int seconds = unavailable.getUnavailableSeconds ();//秒
        if (seconds <= 0) {
            seconds = 60;
        }
        setAvailable (System.currentTimeMillis () + seconds * 1000L);
    }

    @Override
    public void unload() throws ServletException {
        if (instance == null) {
            return;
        }

        try {
            instance.destroy ();
        } catch (Throwable e) {
            log.error ("stop:unload失败", e);
            throw new ServletException ();
        } finally {
            instance = null;
        }
        fireContainerEvent ("unload", this);
    }

    @Override
    public String getServletName() {
        return getName ();
    }

    public void setServletName(String name) {
        setName (name);
    }

    @Override
    public ServletContext getServletContext() {
        return getParent () instanceof Context ?
                ((Context) getParent ()).getServletContext ()
                : null;
    }

    @Override
    public String getInitParameter(String name) {
        return findInitParameter (name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration (parameters.keySet ());
    }

    @Override
    public void stop() throws LifecycleException {
        super.stop ();

        try {
            unload ();
        } catch (Throwable e) {
            log.error ("stop:unload失败", e);
        }
    }
}
