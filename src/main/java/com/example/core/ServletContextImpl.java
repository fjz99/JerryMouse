package com.example.core;

import com.example.Container;
import com.example.resource.FileDirContext;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.Globals.SERVER_INFO;


/**
 * @date 2021/12/27 20:12
 */
@Slf4j
public class ServletContextImpl implements ServletContext {

    private final Map<String, String> paramMap = new ConcurrentHashMap<> ();
    private final Map<String, Object> attributes = new ConcurrentHashMap<> ();
    private final StandardContext context;

    /**
     * 如何获得sessionId，是基于url，还是cookie，还是SSL（未实现）
     */
    private Set<SessionTrackingMode> sessionTrackingModes = null;
    private Set<SessionTrackingMode> defaultSessionTrackingModes = null;
    private Set<SessionTrackingMode> supportedSessionTrackingModes = null;

    public ServletContextImpl(StandardContext standardContext) {
        super ();
        this.context = standardContext;

        setSessionTrackingModes ();
    }

    @Override
    public String getContextPath() {
        return context.getPath ();
    }

    /**
     * 根据url获得ServletContext，一般都是获得当前的ServletContext
     * 不支持cross context
     */
    @Override
    public ServletContext getContext(String uri) {
        // Validate the format of the specified argument
        if ((uri == null) || (!uri.startsWith ("/")))
            return (null);

        // Return the current context if requested
        String contextPath = context.getPath ();
        if (!contextPath.endsWith ("/"))
            contextPath = contextPath + "/";
        if (uri.startsWith (contextPath)) {
            return (this);
        }

        return null;
    }

    @Override
    public int getMajorVersion() {
        return 4;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    /**
     * 获得支持的servlet的版本号
     * 暂时不清楚和{@link #getMajorVersion}的区别
     */
    @Override
    public int getEffectiveMajorVersion() {
        return Constants.MAJOR_VERSION;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return Constants.MINOR_VERSION;
    }

    /**
     * 获得数据类型；
     * 即扩展名对应的文件类型（content-type）,比如gif对应image/gif类型
     */
    @Override
    public String getMimeType(String file) {
        return null;
    }

    /**
     * 获得路径下的所有资源，借助{@link com.example.resource.FileDirContext}
     * FIXME 改为相对路径
     */
    @Override
    public Set<String> getResourcePaths(String path) {
        Set<String> set = new HashSet<> ();

        for (Object o : context.getResources ().list (path)) {
            if (o instanceof FileDirContext) {
                set.add (((FileDirContext) o).getFile ().getPath ());
            } else {
                set.add (((FileDirContext.FileResource) o).getFile ().getPath ());
            }
        }
        return set;
    }

    /**
     * 获得路径URL资源
     */
    @Override
    public URL getResource(String path) throws MalformedURLException {
        Object lookup = context.getResources ().lookup (path);
        if (lookup instanceof FileDirContext.FileResource) {
            return ((FileDirContext.FileResource) lookup).getFile ().toURL ();
        } else {
            return ((FileDirContext) lookup).getFile ().toURL ();
        }
    }

    /**
     * 基于{@link com.example.resource.FileDirContext}
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        Object lookup = context.getResources ().lookup (path);
        if (lookup instanceof FileDirContext.FileResource) {
            try {
                return ((FileDirContext.FileResource) lookup).streamContent ();
            } catch (IOException e) {
                log.error ("", e);
            }
        }
        return null;
    }

    /**
     * Return a <code>RequestDispatcher</code> instance that acts as a
     * wrapper for the resource at the given path.  The path must begin
     * with a "/" and is interpreted as relative to the current context root.
     * 获得基于/xx相对路径的RequestDispatcher
     * TODO
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException ("TODO");
    }

    /**
     * 这个指的是规定好了目标servlet的dispatcher，因为dispatcher一般用于forward
     * TODO
     */
    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        throw new UnsupportedOperationException ("TODO");
    }

    /**
     * 根据文档，应该返回null
     */
    @Override
    @Deprecated
    public Servlet getServlet(String name) throws ServletException {
        return null;
    }

    /**
     * 根据文档，应该返回空
     */
    @Override
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        return Collections.emptyEnumeration ();
    }

    /**
     * 根据文档，应该返回空
     */
    @Override
    @Deprecated
    public Enumeration<String> getServletNames() {
        return Collections.emptyEnumeration ();
    }

    /**
     * 具体使用的logger在log4j.xml中配置
     */
    @Override
    public void log(String msg) {
        log.info (msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        log (msg, exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        log.error (message, throwable);
    }

    /**
     * {@inheritDoc}
     * 即把相对路径翻译成绝对路径，带http的那种
     */
    @Override
    public String getRealPath(String path) {
        return context.getRealPath (path);
    }

    @Override
    public String getServerInfo() {
        return SERVER_INFO;
    }

    @Override
    public String getInitParameter(String name) {
        return paramMap.get (name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration (new HashSet<> (paramMap.keySet ()));
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        Objects.requireNonNull (name);
        return paramMap.putIfAbsent (name, value) == null;
    }

    private void mergeParam() {
        paramMap.clear ();

        for (String parameter : context.findParameters ()) {
            paramMap.put (parameter, context.findParameter (parameter));
        }
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get (name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration (new HashSet<> (attributes.keySet ()));
    }

    /**
     * 关键在于触发{@link ServletContextAttributeListener}监听器
     */
    @Override
    public void setAttribute(String name, Object object) {
        Objects.requireNonNull (name);

        if (object == null) {
            removeAttribute (name);
            return;
        }

        boolean replaced = attributes.containsKey (name);
        if (replaced) {
            for (Object o : context.getApplicationEventListeners ()) {
                if (o instanceof ServletContextAttributeListener) {
                    try {
                        ServletContextAttributeEvent event = new ServletContextAttributeEvent (this, name, attributes.get (name));
                        ((ServletContextAttributeListener) o).attributeRemoved (event);
                    } catch (Throwable e) {
                        log.error ("", e);
                    }
                }
            }
        }

        attributes.put (name, object);
        for (Object o : context.getApplicationEventListeners ()) {
            if (o instanceof ServletContextAttributeListener) {

                try {
                    if (replaced) {
                        ServletContextAttributeEvent event = new ServletContextAttributeEvent (this, name, attributes.get (name));
                        ((ServletContextAttributeListener) o).attributeAdded (event);
                    } else {
                        ServletContextAttributeEvent event = new ServletContextAttributeEvent (this, name, object);
                        ((ServletContextAttributeListener) o).attributeReplaced (event);
                    }
                } catch (Throwable e) {
                    log.error ("", e);
                }
            }
        }
    }

    /**
     * 关键在于触发{@link ServletContextAttributeListener}监听器
     */
    @Override
    public void removeAttribute(String name) {
        Object remove = attributes.remove (name);

        if (remove != null) {
            for (Object o : context.getApplicationEventListeners ()) {
                if (o instanceof ServletContextAttributeListener) {
                    try {
                        ServletContextAttributeEvent event = new ServletContextAttributeEvent (this, name, remove);
                        ((ServletContextAttributeListener) o).attributeRemoved (event);
                    } catch (Throwable e) {
                        log.error ("", e);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletContextName() {
        return context.getDisplayName ();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        throw new UnsupportedOperationException ();
    }

    /**
     * TODO
     */
    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        throw new UnsupportedOperationException ("TODO");
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        for (SessionTrackingMode sessionTrackingMode : sessionTrackingModes) {
            if (!supportedSessionTrackingModes.contains (sessionTrackingMode)) {
                throw new IllegalArgumentException ();
            }
        }

        this.sessionTrackingModes = sessionTrackingModes;
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return defaultSessionTrackingModes;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return sessionTrackingModes == null ? defaultSessionTrackingModes : sessionTrackingModes;
    }

    @Override
    public void addListener(String className) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException ();
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public ClassLoader getClassLoader() {
        return context.getLoader ().getClassLoader ();
    }

    /**
     * 没有实现
     */
    @Override
    public void declareRoles(String... roleNames) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public String getVirtualServerName() {
        // Constructor will fail if context or its parent is null
        Container host = context.getParent ();
        Container engine = host.getParent ();
        return engine.getName () + "/" + host.getName ();
    }

    @Override
    public int getSessionTimeout() {
        return context.getSessionTimeout ();
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        context.setSessionTimeout (sessionTimeout);
    }

    @Override
    public String getRequestCharacterEncoding() {
        return context.getRequestCharacterEncoding ();
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        context.setRequestCharacterEncoding (encoding);
    }

    @Override
    public String getResponseCharacterEncoding() {
        return context.getResponseCharacterEncoding ();
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        context.setResponseCharacterEncoding (encoding);
    }

    private void setSessionTrackingModes() {
        defaultSessionTrackingModes = EnumSet.of (SessionTrackingMode.URL);
        supportedSessionTrackingModes = EnumSet.of (SessionTrackingMode.URL);

        if (context.isCookies ()) {
            defaultSessionTrackingModes.add (SessionTrackingMode.COOKIE);
            supportedSessionTrackingModes.add (SessionTrackingMode.COOKIE);
        }
    }
}
