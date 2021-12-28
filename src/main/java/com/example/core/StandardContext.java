package com.example.core;

import com.example.Container;
import com.example.Context;
import com.example.Globals;
import com.example.Wrapper;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.descriptor.FilterDefinition;
import com.example.descriptor.FilterMapping;
import com.example.filter.FilterConfigImpl;
import com.example.life.Lifecycle;
import com.example.life.LifecycleException;
import com.example.loader.Loader;
import com.example.loader.WebappLoader;
import com.example.resource.AbstractContext;
import com.example.resource.FileDirContext;
import com.example.session.Manager;
import com.example.session.StandardManager;
import com.example.util.URLEncoder;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.example.life.EventType.*;

/**
 * 主要是get set remove方法多
 *
 * @date 2021/12/27 14:22
 */
@Slf4j
public final class StandardContext extends AbstractContainer implements Context {


    private final ReadWriteLock managerLock = new ReentrantReadWriteLock ();
    private final Map<String, FilterConfigImpl> filterConfigs = new ConcurrentHashMap<> ();
    private final List<FilterMapping> filterMappings = new ArrayList<> ();
    private final Map<String, String> parameters = new ConcurrentHashMap<> ();
    /**
     * The set of filter definitions for this application, keyed by
     * filter name.
     */
    private final Map<String, FilterDefinition> filterDefinitions = new ConcurrentHashMap<> ();
    private final Object applicationListenersLock = new Object ();

    private final ReadWriteLock loaderLock = new ReentrantReadWriteLock ();
    private final ReadWriteLock resourcesLock = new ReentrantReadWriteLock ();
    private final Object watchedResourcesLock = new Object ();
    private final Object welcomeFilesLock = new Object ();
    private final Object pausedLock = new Object ();
    /**
     * The ordered set of ServletContainerInitializers for this web application.
     */
    private final Map<ServletContainerInitializer, Set<Class<?>>> initializers = new LinkedHashMap<> ();
    /**
     * The servlet mappings for this web application, keyed by
     * matching pattern.
     */
    private final Map<String, String> servletMappings = new ConcurrentHashMap<> ();
    /**
     * The watched resources for this application.
     */
    private final List<String> watchedResources = new ArrayList<> ();
    /**
     * The welcome files for this application.
     */
    private final List<String> welcomeFiles = new ArrayList<> ();
    private final Set<String> resourceOnlyServlets = new HashSet<> ();
    /**
     * The set of application listener class names configured for this
     * application, in the order they were encountered in the resulting merged
     * web.xml file.
     */
    private final List<String> applicationListeners = new ArrayList<> ();
    /**
     * The list of instantiated application event listener objects. Note that
     * SCIs and other code may use the pluggability APIs to add listener
     * instances directly to this list before the application starts.
     */
    private final List<Object> applicationEventListeners = new CopyOnWriteArrayList<> ();
    /**
     * The set of instantiated application lifecycle listener objects. Note that
     * SCIs and other code may use the pluggability APIs to add listener
     * instances directly to this list before the application starts.
     */
    private final List<Object> applicationLifecycleListeners = new ArrayList<> ();
    /**
     * 统计正在处理的请求数，由外部valve设置
     */
    private final AtomicLong inProgressAsyncCount = new AtomicLong (0);
    private Manager manager = null;
    /**
     * The ServletContext implementation associated with this Context.
     */
    private ServletContextImpl servletContext = null;
    /**
     * Allow multipart/form-data requests to be parsed even when the
     * target servlet doesn't specify @MultipartConfig or have a
     * &lt;multipart-config&gt; element.
     */
    private boolean allowCasualMultipartParsing = false;
    private String docBase;
    /**
     * The URL of the XML descriptor for this context.
     */
    private URL configFile = null;
    /**
     * The "correctly configured" flag for this Context.
     */
    private boolean configured = false;
    private boolean available = false;
    /**
     * Encoded path.
     */
    private String encodedPath = null;
    /**
     * Unencoded path for this web application.
     */
    private String path = null;
    /**
     * The Loader implementation with which this Container is associated.
     */
    private Loader loader = null;
    /**
     * The request processing pause flag (while reloading occurs)
     */
    private volatile boolean paused = false;
    /**
     * The reloadable flag for this web application.
     */
    private boolean reloadable = false;
    /**
     * Unpack WAR property.
     */
    private boolean unpackWAR = true;
    /**
     * The default context override flag for this web application.
     */
    private boolean override = false;
    /**
     * The original document root for this web application.
     */
    private String originalDocBase = null;
    /**
     * Should the next call to <code>addWelcomeFile()</code> cause replacement
     * of any existing welcome files?  This will be set before processing the
     * web application's deployment descriptor, so that application specified
     * choices <strong>replace</strong>, rather than append to, those defined
     * in the global descriptor.
     */
    private boolean replaceWelcomeFiles = false;
    /**
     * The session timeout (in minutes) for this web application.
     */
    private int sessionTimeout = 30;
    /**
     * Amount of ms that the container will wait for servlets to unload.
     * 假设要stop的话，如果还有正在处理的请求咋办？unloadDelay指定的就是等待的最大时间，超过时间后就会返回
     * 当stop开始调用时，就会拒绝新来的请求了
     */
    private long unloadDelay = 2000;
    /**
     * The pathname to the work directory for this context (relative to
     * the server's home if not absolute).
     */
    private String workDir = null;
    /**
     * Java class name of the Wrapper class implementation we use.
     */
    private String wrapperClassName = StandardWrapper.class.getName ();
    private Class<?> wrapperClass = null;
    private AbstractContext resources;
    /**
     * The name to use for session cookies. <code>null</code> indicates that
     * the name is controlled by the application.
     */
    private String sessionCookieName;
    /**
     * The flag that indicates that session cookies should use HttpOnly
     */
    private boolean useHttpOnly = true;
    /**
     * The domain to use for session cookies. <code>null</code> indicates that
     * the domain is controlled by the application.
     */
    private String sessionCookieDomain;
    /**
     * The path to use for session cookies. <code>null</code> indicates that
     * the path is controlled by the application.
     */
    private String sessionCookiePath;
    private String webappVersion = "";
    private boolean sendRedirectBody = false;
    private boolean validateClientProvidedNewSessionId = true;
    private String requestEncoding = null;
    private String responseEncoding = null;
    private boolean createUploadTargets = false;
    private boolean ignoreAnnotations = false;
    /**
     * The display name of this web application.
     */
    private String displayName = null;
    /**
     * Override the default context xml location.
     */
    private String defaultContextXml;
    /**
     * Override the default web xml location.
     */
    private String defaultWebXml;
    private boolean webXmlValidation = true;

    public String getDefaultWebXml() {
        return defaultWebXml;
    }

    public String getDefaultContextXml() {
        return defaultContextXml;
    }

    public void setDefaultContextXml(String defaultContextXml) {
        this.defaultContextXml = defaultContextXml;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        boolean oldAvailable = this.available;
        this.available = available;
        support.firePropertyChange ("available", oldAvailable, this.available);
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        verifyRunning ();

        fireLifecycleEvent (BEFORE_STOP_EVENT, this);
        log.info ("Stopping {}, waiting for finishing {} request(s).", getDisplayName (), getInProgressAsyncCount ());

        running = false;
        setAvailable (false);//我自己加的，让wrapper的valve发出404

        //等待一段时间，看看是否处理完所有的请求了
        long limit = System.currentTimeMillis () + unloadDelay;
        while (getInProgressAsyncCount () > 0 && System.currentTimeMillis () < limit) {
            try {
                Thread.sleep (50);
            } catch (InterruptedException ignored) {
                break;
            }
        }
        if (getInProgressAsyncCount () == 0) {
            log.info ("All request(s) stopped.");
        } else {
            log.warn ("{} reqs hasn't sopped yet.", getInProgressAsyncCount ());
        }

        running = false;//后面就无法接受新的请求了
        ClassLoader bind = bind (null);
        try {
            super.stopThread ();//先关闭

            for (Container child : findChildren ()) {
                if (child.isRunning ()) {
                    child.stop ();
                }
            }

            //因为可能启动失败，这样可以避免fail loudly
            if (getManager () instanceof Lifecycle &&
                    !((Lifecycle) getManager ()).isRunning ()) {
                ((Lifecycle) getManager ()).stop ();
            }

            if (getPipeline () instanceof Lifecycle &&
                    !((Lifecycle) getPipeline ()).isRunning ()) {
                ((Lifecycle) getPipeline ()).stop ();
            }

            stopListeners ();
            stopFilters ();

            //最后关闭
            if (getLoader () instanceof Lifecycle &&
                    !((Lifecycle) getLoader ()).isRunning ()) {
                ((Lifecycle) getLoader ()).stop ();
            }
        } finally {
            unbind (bind);
        }

        try {
            resetContext ();
        } catch (Exception ex) {
            log.error ("Error resetting context " + this + " " + ex, ex);
        }

        setAvailable (false);
        log.info ("Context {} stopped.", getDisplayName ());
    }

    private void resetContext() {
        for (Container child : findChildren ()) {
            removeChild (child);
        }

        servletContext = null;
        applicationListeners.clear ();
        applicationEventListeners.clear ();
        applicationLifecycleListeners.clear ();

        initializers.clear ();

        log.debug ("reset context {}", getDisplayName ());
    }

    /**
     * 给servletContext设置init param
     */
    private void setParameters() {
        Map<String, String> mergedParams = new HashMap<> ();

        String[] names = findParameters ();
        for (String s : names) {
            mergedParams.put (s, findParameter (s));
        }

//        ApplicationParameter params[] = findApplicationParameters ();
//        for (ApplicationParameter param : params) {
//            if (param.getOverride ()) {
//                if (mergedParams.get (param.getName ()) == null) {
//                    mergedParams.put (param.getName (),
//                            param.getValue ());
//                }
//            } else {
//                mergedParams.put (param.getName (), param.getValue ());
//            }
//        }

        ServletContext sc = getServletContext ();
        for (Map.Entry<String, String> entry : mergedParams.entrySet ()) {
            sc.setInitParameter (entry.getKey (), entry.getValue ());
        }
    }

    /**
     * 先是loader、再是child container、再是pipeline
     * 启动完子组件之后，检查configure标志位，如果没问题，那就启动filter和manager和listener
     * 如果启动失败，那就stop(?)
     */
    @Override
    public synchronized void start() throws LifecycleException {
        verifyStopped ();
        fireLifecycleEvent (BEFORE_START_EVENT, this);

        log.debug ("starting {}", getDisplayName ());
        running = true;
        setConfigured (false);//后面会通过监听器设置为true
        setAvailable (false);
        boolean ok = true;

        postWorkDirectory ();

        if (getResources () == null) {
            log.debug ("Configure FileDirContext Resource.");
            FileDirContext resources = new FileDirContext ();
            resources.setDocBase (getDocBase ());
            setResources (resources);
        }
        if (getLoader () == null) {
            log.debug ("Configure default Loader.");
            setLoader (new WebappLoader (getParentClassLoader ()));
        }


        log.debug ("Processing standard container startup");
        ClassLoader bind = bind (null);
        try {
            if (getLoader () instanceof Lifecycle) {
                ((Lifecycle) getLoader ()).start ();
            }
            unbind (bind);

            bind = bind (null);
            for (Container child : findChildren ()) {
                if (!child.isRunning ()) {
                    child.start ();
                }
            }

            if (pipeline instanceof Lifecycle) {
                ((Lifecycle) pipeline).start ();
            }

            //启动完所有的子组件，就开始检查
            //configured会被监听器设置，所以启动所有的子组件之后，如果configure是false的话，那就启动失败了
            if (!getConfigured ())
                ok = false;

            fireLifecycleEvent (START_EVENT, this);
            if (getManager () == null) {
                setManager (new StandardManager ());//set的时候会start
            }

            if (ok) {
                getServletContext ().setAttribute (Globals.RESOURCES_ATTR, getResources ());
                getServletContext ().setAttribute (Globals.WEBAPP_VERSION, getWebappVersion ());
            }

            setParameters ();

            for (Map.Entry<ServletContainerInitializer, Set<Class<?>>> entry :
                    initializers.entrySet ()) {
                try {
                    entry.getKey ().onStartup (entry.getValue (),
                            getServletContext ());
                } catch (ServletException e) {
                    log.error ("standardContext.ServletContainerInitializerFail", e);
                    ok = false;
                    break;
                }
            }

            ok = ok && loadOnStartUp (findChildren ());
            ok = ok && startListeners () && startFilters ();

            if (ok && (getManager () instanceof Lifecycle) &&
                    !((Lifecycle) getManager ()).isRunning ()) {
                ((Lifecycle) getManager ()).start ();
            }

            if (ok) {
                super.startThread ();
            }
        } finally {
            unbind (bind);
        }

        if (ok) {
            setAvailable (true);
            log.info ("Context {} started.", getDisplayName ());
        } else {
            setAvailable (false);
            try {
                stop ();
            } catch (Throwable e) {
                log.error ("", e);
            }
            log.error ("Context {} started failed.", getDisplayName ());
        }

        fireLifecycleEvent (AFTER_START_EVENT, this);
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
        if (isRunning ()) {
            postWorkDirectory ();
        }
    }

    private void postWorkDirectory() {
        //默认的
        String workDir = getWorkDir ();
        if (workDir == null || workDir.length () == 0) {

            // Retrieve our parent (normally a host) name
            String hostName = null;
            String engineName = null;
            String hostWorkDir = null;
            Container parentHost = getParent ();
            if (parentHost != null) {
                hostName = parentHost.getName ();
                if (parentHost instanceof StandardHost) {
                    hostWorkDir = ((StandardHost) parentHost).getWorkDir ();
                }
                Container parentEngine = parentHost.getParent ();
                if (parentEngine != null) {
                    engineName = parentEngine.getName ();
                }
            }
            if ((hostName == null) || (hostName.length () < 1)) {
                hostName = "_";
            }
            if ((engineName == null) || (engineName.length () < 1)) {
                engineName = "_";
            }

            String temp = getBaseName ();
            if (temp.startsWith ("/")) {
                temp = temp.substring (1);
            }
            temp = temp.replace ('/', '_');
            temp = temp.replace ('\\', '_');
            if (temp.length () < 1) {
                temp = "ROOT";
            }
            if (hostWorkDir != null) {
                workDir = hostWorkDir + File.separator + temp;
            } else {
                workDir = "work" + File.separator + engineName +
                        File.separator + hostName + File.separator + temp;
            }
            setWorkDir (workDir);
        }

        File work = new File (getWorkDir ());
        if (!work.isAbsolute ()) {
            //相对路径
            try {
                String path = getCatalinaBase ().getCanonicalPath ();
                work = new File (path, getDocBase ());
            } catch (IOException e) {
                log.warn ("", e);
            }
        }

        if (servletContext == null) {
            getServletContext ();
        }

        if (!work.mkdirs () && !work.isDirectory ()) {
            log.warn ("创建work dir {} failed.", getWorkDir ());
        }

        servletContext.setAttribute (ServletContext.TEMPDIR, work);//!!!!!!!!temp dir 就是 work dir。。
    }

    /**
     * 创建所有的filterConfig
     */
    private boolean startFilters() {
        synchronized (filterConfigs) {
            filterConfigs.clear ();
            for (FilterDefinition filterDef : findFilterDefs ()) {
                try {
                    FilterConfigImpl config = new FilterConfigImpl (this, filterDef);
                    filterConfigs.put (config.getFilterName (), config);
                } catch (Throwable e) {
                    log.error ("", e);
                    return false;
                }
            }
        }
        log.debug ("context {} filter started.", getDisplayName ());
        return true;
    }

    /**
     * 释放所有的filterConfig
     */
    private boolean stopFilters() {
        synchronized (filterConfigs) {
            for (FilterConfigImpl value : filterConfigs.values ()) {
                try {
                    value.release ();
                } catch (Throwable e) {
                    log.error ("", e);
                    return false;
                }
            }
            filterConfigs.clear ();
        }
        log.debug ("context {} filter stopped.", getDisplayName ());
        return false;
    }

    /**
     * 实例化所有的监听器，并且触发init回调
     */
    private boolean startListeners() {
        ClassLoader classLoader = getLoader ().getClassLoader ();
        List<Object> event = new ArrayList<> ();
        List<Object> lifecycle = new ArrayList<> ();

        for (String applicationListener : findApplicationListeners ()) {
            try {
                Class<?> aClass = classLoader.loadClass (applicationListener);
                Object o = aClass.newInstance ();

                if (o instanceof ServletContextListener ||
                        o instanceof HttpSessionListener) {
                    lifecycle.add (o);
                }
                if ((o instanceof ServletContextAttributeListener)
                        || (o instanceof ServletRequestAttributeListener)
                        || (o instanceof ServletRequestListener)
                        || (o instanceof HttpSessionIdListener)
                        || (o instanceof HttpSessionAttributeListener)) {
                    event.add (o);
                }
            } catch (Throwable e) {
                log.error ("Listener" + applicationListener + "创建失败", e);
                return false;
            }
        }

        for (Object o : lifecycle) {
            if (o instanceof ServletContextListener) {
                try {
                    ServletContextEvent sce = new ServletContextEvent (getServletContext ());
                    ((ServletContextListener) o).contextInitialized (sce);
                } catch (Throwable e) {
                    //因为有外部代码
                    log.error ("", e);
                    return false;
                }
            }
        }

        setApplicationEventListeners (event.toArray ());
        setApplicationLifecycleListeners (lifecycle.toArray ());
        log.debug ("{} Listeners started.", getDisplayName ());
        return true;
    }

    /**
     * 销毁所有的监听器，并且触发destory回调
     */
    private boolean stopListeners() {
        for (Object o : getApplicationLifecycleListeners ()) {
            if (o instanceof ServletContextListener) {
                try {
                    ServletContextEvent sce = new ServletContextEvent (getServletContext ());
                    ((ServletContextListener) o).contextDestroyed (sce);
                } catch (Throwable e) {
                    log.error ("", e);
                    return false;
                }
            }
        }

        setApplicationEventListeners (null);
        setApplicationLifecycleListeners (null);
        log.debug ("{} Listeners stopped.", getDisplayName ());
        return true;
    }

    /**
     * 后台任务
     */
    @Override
    public void backgroundProcess() {
        if (getLoader () != null) {
            try {
                //modified检查功能,如果返回true，那就调用context的reload方法
                getLoader ().backgroundProcess ();
            } catch (Throwable e) {
                log.error ("", e);
            }
        }

        if (getManager () != null) {
            try {
                getManager ().backgroundProcess ();//session清除功能
            } catch (Throwable e) {
                log.error ("", e);
            }
        }

        super.backgroundProcess ();
    }

    /**
     * 判断是否是热加载，如果是，那就加载
     */
    private boolean loadOnStartUp(Container[] containers) {
        TreeMap<Integer, List<Wrapper>> map = new TreeMap<> ();
        for (Container container : containers) {
            if (container instanceof Wrapper) {
                int loadOnStartup = ((Wrapper) container).getLoadOnStartup ();
                if (loadOnStartup < 0) {
                    //懒加载
                    continue;
                }

                List<Wrapper> wrappers = map.computeIfAbsent (loadOnStartup, x -> new ArrayList<> ());
                wrappers.add (((Wrapper) container));
            } else {
                return false;
            }
        }

        for (List<Wrapper> value : map.values ()) {
            for (Wrapper wrapper : value) {
                try {
                    wrapper.load ();
                    log.debug ("Context {} init servlet {}", getDisplayName (), wrapper.getServletClass ());
                } catch (ServletException e) {
                    log.error ("", e);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean getAllowCasualMultipartParsing() {
        return allowCasualMultipartParsing;
    }

    @Override
    public void setAllowCasualMultipartParsing(boolean allowCasualMultipartParsing) {
        this.allowCasualMultipartParsing = allowCasualMultipartParsing;
    }

    @Override
    public Object[] getApplicationEventListeners() {
        return applicationEventListeners.toArray ();
    }

    @Override
    public void setApplicationEventListeners(Object[] listeners) {
        applicationEventListeners.clear ();
        if (listeners != null) {
            applicationEventListeners.addAll (Arrays.asList (listeners));
        }
    }

    @Override
    public Object[] getApplicationLifecycleListeners() {
        return applicationLifecycleListeners.toArray ();
    }

    @Override
    public void setApplicationLifecycleListeners(Object[] listeners) {
        applicationLifecycleListeners.clear ();
        if (listeners != null) {
            applicationLifecycleListeners.addAll (Arrays.asList (listeners));
        }
    }

    @Override
    public String getCharset(Locale locale) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public URL getConfigFile() {
        return configFile;
    }

    @Override
    public void setConfigFile(URL configFile) {
        this.configFile = configFile;
    }

    @Override
    public boolean getConfigured() {
        return configured;
    }

    @Override
    public void setConfigured(boolean configured) {
        boolean oldConfigured = this.configured;
        this.configured = configured;
        support.firePropertyChange ("configured",
                oldConfigured,
                this.configured);
    }

    @Override
    public String getSessionCookieName() {
        return sessionCookieName;
    }

    @Override
    public void setSessionCookieName(String sessionCookieName) {
        String oldSessionCookieName = this.sessionCookieName;
        this.sessionCookieName = sessionCookieName;
        support.firePropertyChange ("sessionCookieName",
                oldSessionCookieName, sessionCookieName);
    }

    @Override
    public boolean getUseHttpOnly() {
        return useHttpOnly;
    }

    @Override
    public void setUseHttpOnly(boolean useHttpOnly) {
        boolean oldUseHttpOnly = this.useHttpOnly;
        this.useHttpOnly = useHttpOnly;
        support.firePropertyChange ("useHttpOnly",
                oldUseHttpOnly,
                this.useHttpOnly);
    }

    @Override
    public String getSessionCookieDomain() {
        return sessionCookieDomain;
    }

    @Override
    public void setSessionCookieDomain(String sessionCookieDomain) {
        String oldSessionCookieDomain = this.sessionCookieDomain;
        this.sessionCookieDomain = sessionCookieDomain;
        support.firePropertyChange ("sessionCookieDomain",
                oldSessionCookieDomain, sessionCookieDomain);
    }

    @Override
    public String getSessionCookiePath() {
        return sessionCookiePath;
    }

    @Override
    public void setSessionCookiePath(String sessionCookiePath) {
        String oldSessionCookiePath = this.sessionCookiePath;
        this.sessionCookiePath = sessionCookiePath;
        support.firePropertyChange ("sessionCookiePath",
                oldSessionCookiePath, sessionCookiePath);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        String oldDisplayName = this.displayName;
        this.displayName = displayName;
        support.firePropertyChange ("displayName", oldDisplayName,
                this.displayName);
    }

    @Override
    public String getDocBase() {
        return docBase;
    }

    @Override
    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    @Override
    public String getEncodedPath() {
        return encodedPath;
    }

    @Override
    public boolean getIgnoreAnnotations() {
        return ignoreAnnotations;
    }

    @Override
    public void setIgnoreAnnotations(boolean ignoreAnnotations) {
        boolean oldIgnoreAnnotations = this.ignoreAnnotations;
        this.ignoreAnnotations = ignoreAnnotations;
        support.firePropertyChange ("ignoreAnnotations", oldIgnoreAnnotations,
                this.ignoreAnnotations);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        boolean invalid = false;
        if (path == null || path.equals ("/")) {
            invalid = true;
            this.path = "";
        } else if (path.isEmpty () || path.startsWith ("/")) {
            this.path = path;
        } else {
            invalid = true;
            this.path = "/" + path;
        }
        if (this.path.endsWith ("/")) {
            invalid = true;
            this.path = this.path.substring (0, this.path.length () - 1);
        }
        if (invalid) {
            log.warn ("standardContext.pathInvalid");
        }
        encodedPath = URLEncoder.DEFAULT.encode (this.path, StandardCharsets.UTF_8);
        if (getName () == null) {
            setName (this.path);
        }
    }

    @Override
    public boolean getReloadable() {
        return reloadable;
    }

    @Override
    public void setReloadable(boolean reloadable) {
        boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
        support.firePropertyChange ("reloadable",
                oldReloadable,
                this.reloadable);
    }

    @Override
    public boolean getOverride() {
        return override;
    }

    @Override
    public void setOverride(boolean override) {
        boolean oldOverride = this.override;
        this.override = override;
        support.firePropertyChange ("override",
                oldOverride,
                this.override);
    }

    @Override
    public ServletContext getServletContext() {
        if (servletContext == null) {
            servletContext = new ServletContextImpl (this);
        }
        return servletContext;
    }

    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public void setSessionTimeout(int timeout) {
        int oldSessionTimeout = this.sessionTimeout;
        /*
         * SRV.13.4 ("Deployment Descriptor"):
         * If the timeout is 0 or less, the container ensures the default
         * behaviour of sessions is never to time out.
         */
        this.sessionTimeout = (timeout == 0) ? -1 : timeout;
        support.firePropertyChange ("sessionTimeout",
                oldSessionTimeout,
                this.sessionTimeout);
    }

    @Override
    public String getWrapperClass() {
        return wrapperClassName;
    }

    @Override
    public void setWrapperClass(String wrapperClassName) {
        this.wrapperClassName = wrapperClassName;

        try {
            wrapperClass = Class.forName (wrapperClassName);
            if (!StandardWrapper.class.isAssignableFrom (wrapperClass)) {
                throw new IllegalArgumentException ();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException (e.getMessage ());
        }
    }


    @Override
    public boolean getXmlValidation() {
        return webXmlValidation;
    }

    @Override
    public void setXmlValidation(boolean xmlValidation) {
        this.webXmlValidation = xmlValidation;
    }

    @Override
    public void addApplicationListener(String listener) {
        synchronized (applicationListenersLock) {
            applicationListeners.add (listener);
        }
        fireContainerEvent ("addApplicationListener", listener);
    }

    @Override
    public void addFilterDef(FilterDefinition filterDef) {
        filterDefinitions.put (filterDef.getFilterName (), filterDef);
    }

    @Override
    public void addFilterMap(FilterMapping filterMap) {
        //todo 验证filterMap
        synchronized (filterMappings) {
            filterMappings.add (filterMap);
        }
    }

    @Override
    public void addLocaleEncodingMappingParameter(String locale, String encoding) {
        //todo
        throw new UnsupportedOperationException ();
    }

    @Override
    public void addParameter(String name, String value) {
        if ((name == null) || (value == null)) {
            throw new IllegalArgumentException ();
        }
        String oldValue = parameters.putIfAbsent (name, value);

        if (oldValue != null) {
            throw new IllegalArgumentException ("key重复");
        }

        fireContainerEvent ("addParameter", name);
    }

    /**
     * 添加url到servlet的映射
     */
    @Override
    public void addServletMapping(String pattern, String name) {
        addServletMappingDecoded (pattern, name);
    }

    /**
     * 添加url到servlet的映射
     */
    @Override
    public void addServletMappingDecoded(String pattern, String name) {
        if (findChild (name) == null) {
            throw new IllegalArgumentException ("Servlet " + name + " not exists.");
        }

        if (!validateURLPattern (pattern)) {
            throw new IllegalArgumentException ("Illegal url " + pattern);
        }

        servletMappings.put (pattern, name);
        fireContainerEvent ("addServletMapping", pattern);
    }


    @Override
    public void addWatchedResource(String name) {
        //不用watchedResources sync，因为可能会被替换。。
        synchronized (watchedResourcesLock) {
            watchedResources.add (name);
        }
        fireContainerEvent ("addWatchedResource", name);
    }

    public void setReplaceWelcomeFiles(boolean replaceWelcomeFiles) {
        this.replaceWelcomeFiles = replaceWelcomeFiles;
    }

    @Override
    public void addWelcomeFile(String name) {
        synchronized (welcomeFilesLock) {
            if (replaceWelcomeFiles) {
                fireContainerEvent (CLEAR_WELCOME_FILES_EVENT, null);
                welcomeFiles.clear ();
                setReplaceWelcomeFiles (false);
            }
            welcomeFiles.add (name);
        }
        fireContainerEvent (ADD_WELCOME_FILE_EVENT, name);
    }

    @Override
    public Wrapper createWrapper() {
        Wrapper wrapper = null;
        if (wrapperClass != null) {
            try {
                wrapper = (Wrapper) wrapperClass.getConstructor ().newInstance ();
            } catch (Throwable e) {
                log.error ("", e);
            }
        } else {
            wrapper = new StandardWrapper ();
        }
        return wrapper;
    }

    @Override
    public String[] findApplicationListeners() {
        return applicationListeners.toArray (new String[0]);
    }

    @Override
    public String findParameter(String name) {
        return parameters.get (name);
    }

    @Override
    public String[] findParameters() {
        return parameters.keySet ().toArray (new String[0]);
    }

    @Override
    public String findServletMapping(String pattern) {
        return servletMappings.get (pattern);
    }

    @Override
    public String[] findServletMappings() {
        return servletMappings.keySet ().toArray (new String[0]);
    }

    @Override
    public String findStatusPage(int status) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public int[] findStatusPages() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public String[] findWatchedResources() {
        //不加锁也行，但是可能会是旧版本
        synchronized (watchedResourcesLock) {
            return watchedResources.toArray (new String[0]);
        }
    }

    @Override
    public boolean findWelcomeFile(String name) {
        synchronized (welcomeFilesLock) {
            for (String welcomeFile : welcomeFiles) {
                if (name.equals (welcomeFile)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String[] findWelcomeFiles() {
        synchronized (welcomeFilesLock) {
            return welcomeFiles.toArray (new String[0]);
        }
    }

    @Override
    public boolean fireRequestInitEvent(ServletRequest request) {
        boolean ok = true;
        for (Object o : getApplicationEventListeners ()) {
            if (o instanceof ServletRequestListener) {
                try {
                    ServletRequestEvent sre = new ServletRequestEvent (getServletContext (), request);
                    ((ServletRequestListener) o).requestInitialized (sre);
                } catch (Throwable e) {
                    request.setAttribute (RequestDispatcher.ERROR_EXCEPTION, e);
                    log.error ("", e);
                    ok = false;
                }
            }
        }
        return ok;
    }

    @Override
    public boolean fireRequestDestroyEvent(ServletRequest request) {
        boolean ok = true;
        for (Object o : getApplicationEventListeners ()) {
            if (o instanceof ServletRequestListener) {
                try {
                    ServletRequestEvent sre = new ServletRequestEvent (getServletContext (), request);
                    ((ServletRequestListener) o).requestDestroyed (sre);
                } catch (Throwable e) {
                    request.setAttribute (RequestDispatcher.ERROR_EXCEPTION, e);
                    log.error ("", e);
                    ok = false;
                }
            }
        }
        return ok;
    }

    @Override
    public synchronized void reload() {
        verifyRunning ();

        setPaused (true);

        try {
            stop ();
        } catch (Throwable e) {
            log.error ("stop err", e);
        }

        try {
            start ();
        } catch (Throwable e) {
            log.error ("start err", e);
        }

        setPaused (false);
        log.info ("context {} reloaded.", getName ());
    }

    @Override
    public void removeApplicationListener(String listener) {
        synchronized (applicationListenersLock) {
            applicationListeners.remove (listener);
        }
        fireContainerEvent ("removeApplicationListener", listener);
    }

    @Override
    public void removeApplicationParameter(String name) {
        fireContainerEvent ("removeApplicationParameter", name);
        throw new UnsupportedOperationException ();
    }

    @Override
    public void removeFilterDef(FilterDefinition filterDef) {
        filterDefinitions.remove (filterDef.getFilterName ());
        fireContainerEvent ("removeFilterDef", filterDef);
    }

    @Override
    public void removeFilterMap(FilterMapping filterMap) {
        synchronized (filterMappings) {
            filterMappings.remove (filterMap);
        }
        fireContainerEvent ("removeFilterMap", filterMap);
    }

    @Override
    public void removeParameter(String name) {
        parameters.remove (name);
        fireContainerEvent ("removeParameter", name);
    }

    @Override
    public void removeServletMapping(String pattern) {
        servletMappings.remove (pattern);
        fireContainerEvent ("removeServletMapping", pattern);
    }

    @Override
    public void removeWatchedResource(String name) {
        synchronized (watchedResources) {
            watchedResources.remove (name);
        }
        fireContainerEvent ("removeWatchedResource", name);
    }

    @Override
    public void removeWelcomeFile(String name) {
        synchronized (welcomeFiles) {
            welcomeFiles.remove (name);
        }
        if (isRunning ()) {
            fireContainerEvent (REMOVE_WELCOME_FILE_EVENT, name);
        }
    }

    @Override
    public String getRealPath(String path) {
        return getServletContext ().getRealPath (path);
    }

    @Override
    public boolean getPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        synchronized (pausedLock) {
            this.paused = paused;
            pausedLock.notifyAll ();
        }
    }

    @Override
    public String getResourceOnlyServlets() {
        return String.join (",", resourceOnlyServlets);
    }

    @Override
    public void setResourceOnlyServlets(String resourceOnlyServlets) {
        this.resourceOnlyServlets.clear ();
        this.resourceOnlyServlets.addAll (Arrays.asList (resourceOnlyServlets.split (",")));
    }

    @Override
    public boolean isResourceOnlyServlet(String servletName) {
        return resourceOnlyServlets.contains (servletName);
    }

    @Override
    public String getBaseName() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public String getWebappVersion() {
        return webappVersion;
    }

    @Override
    public void setWebappVersion(String webappVersion) {
        if (null == webappVersion) {
            this.webappVersion = "";
        } else {
            this.webappVersion = webappVersion;
        }
    }

    @Override
    public boolean getSendRedirectBody() {
        return sendRedirectBody;
    }

    @Override
    public void setSendRedirectBody(boolean enable) {
        this.sendRedirectBody = enable;
    }

    @Override
    public Manager getManager() {
        Lock lock = managerLock.readLock ();
        try {
            lock.lock ();
            return manager;
        } finally {
            lock.unlock ();
        }
    }

    @Override
    public void setManager(Manager manager) {
        Lock lock = managerLock.writeLock ();
        Manager oldManager = null;
        try {
            lock.lock ();

            oldManager = this.manager;
            this.manager = manager;


            if (oldManager instanceof Lifecycle) {
                ((Lifecycle) oldManager).stop ();
            }

            manager.setContext (this);
            manager.setMaxActive (getSessionTimeout ());
            if (manager instanceof Lifecycle) {
                ((Lifecycle) manager).start ();
            }
        } catch (LifecycleException e) {
            log.error ("", e);
        } finally {
            lock.unlock ();
        }
        support.firePropertyChange ("manager", oldManager, manager);
    }

    @Override
    public boolean getValidateClientProvidedNewSessionId() {
        return validateClientProvidedNewSessionId;
    }

    @Override
    public void setValidateClientProvidedNewSessionId(boolean validateClientProvidedNewSessionId) {
        this.validateClientProvidedNewSessionId = validateClientProvidedNewSessionId;
    }

    @Override
    public String getRequestCharacterEncoding() {
        return requestEncoding;
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        this.requestEncoding = encoding;
    }

    @Override
    public String getResponseCharacterEncoding() {
        return responseEncoding;
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        this.responseEncoding = encoding;
    }

    @Override
    public boolean getCreateUploadTargets() {
        return createUploadTargets;
    }

    @Override
    public void setCreateUploadTargets(boolean createUploadTargets) {
        this.createUploadTargets = createUploadTargets;
    }

    @Override
    public AbstractContext getResources() {
        Lock lock = resourcesLock.readLock ();
        try {
            lock.lock ();
            return resources;
        } finally {
            lock.unlock ();
        }
    }

    @Override
    public void setResources(AbstractContext resources) {
        Lock lock = resourcesLock.writeLock ();
        try {
            lock.lock ();
            if (resources == null || resources == this.resources) {
                return;
            }

            AbstractContext oldResources = this.resources;
            this.resources = resources;

//            if (oldResources != null) {
//                oldResources.setContext (null);
//            }
//            if (resources != null) {
//                resources.setContext (this);
//            }

            support.firePropertyChange ("resources", oldResources,
                    resources);
        } finally {
            lock.unlock ();
        }
    }

    @Override
    public FilterDefinition findFilterDef(String filterName) {
        return filterDefinitions.get (filterName);
    }

    @Override
    public FilterDefinition[] findFilterDefs() {
        return filterDefinitions.values ().toArray (new FilterDefinition[0]);
    }

    @Override
    public FilterMapping[] findFilterMaps() {
        return filterMappings.toArray (new FilterMapping[0]);
    }

    @Override
    public FilterConfig findFilterConfig(String name) {
        return filterConfigs.get (name);
    }

    /**
     * 简化版本
     */
    @Override
    public ClassLoader bind(ClassLoader originalClassLoader) {
        if (originalClassLoader != null) {
            throw new IllegalArgumentException ();
        }
        ClassLoader contextClassLoader = Thread.currentThread ().getContextClassLoader ();
        if (getLoader () == null) {
            return null;
        }

        ClassLoader classLoader = getLoader ().getClassLoader ();
        if (classLoader != null) {
            Thread.currentThread ().setContextClassLoader (classLoader);
            return contextClassLoader;
        }

        return null;
    }

    @Override
    public void unbind(ClassLoader originalClassLoader) {
        if (originalClassLoader == null) {
            return;
        }
        Thread.currentThread ().setContextClassLoader (originalClassLoader);
    }

    @Override
    public Loader getLoader() {
        Lock readLock = loaderLock.readLock ();
        readLock.lock ();
        try {
            return loader;
        } finally {
            readLock.unlock ();
        }
    }

    @Override
    public void setLoader(Loader loader) {
        Lock lock = loaderLock.writeLock ();
        lock.lock ();
        Loader oldLoader = null;
        try {
            //stop
            oldLoader = this.loader;
            if (this.loader instanceof Lifecycle) {
                ((Lifecycle) this.loader).stop ();
            }

            this.loader = loader;
            loader.setContext (this);
            if (loader instanceof Lifecycle) {
                ((Lifecycle) loader).start ();
            }
        } catch (LifecycleException e) {
            log.error ("", e);
        } finally {
            lock.unlock ();
        }
        support.firePropertyChange ("loader", oldLoader, loader);
    }

    /**
     * 抄的，验证url合法性
     */
    private boolean validateURLPattern(String urlPattern) {
        if (urlPattern == null) {
            return false;
        }
        if (urlPattern.indexOf ('\n') >= 0 || urlPattern.indexOf ('\r') >= 0) {
            return false;
        }
        if (urlPattern.equals ("")) {
            return true;
        }
        if (urlPattern.startsWith ("*.")) {
            if (urlPattern.indexOf ('/') < 0) {
                checkUnusualURLPattern (urlPattern);
                return true;
            } else {
                return false;
            }
        }
        if (urlPattern.startsWith ("/") && !urlPattern.contains ("*.")) {
            checkUnusualURLPattern (urlPattern);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 抄的，验证url合法性
     */
    private void checkUnusualURLPattern(String urlPattern) {
        if (log.isInfoEnabled ()) {
            // First group checks for '*' or '/foo*' style patterns
            // Second group checks for *.foo.bar style patterns
            if ((urlPattern.endsWith ("*") && (urlPattern.length () < 2 ||
                    urlPattern.charAt (urlPattern.length () - 2) != '/')) ||
                    urlPattern.startsWith ("*.") && urlPattern.length () > 2 &&
                            urlPattern.lastIndexOf ('.') > 1) {
                log.info ("Suspicious url pattern: \"" + urlPattern + "\"" +
                        " in context [" + getName () + "] - see" +
                        " sections 12.1 and 12.2 of the Servlet specification");
            }
        }
    }

    @Override
    public void addServletContainerInitializer(
            ServletContainerInitializer sci, Set<Class<?>> classes) {
        initializers.put (sci, classes);
    }


    public boolean getUnpackWAR() {
        return unpackWAR;
    }

    public void setUnpackWAR(boolean unpackWAR) {
        this.unpackWAR = unpackWAR;
    }

    public void setUnloadDelay(long unloadDelay) {
        long oldUnloadDelay = this.unloadDelay;
        this.unloadDelay = unloadDelay;
        support.firePropertyChange ("unloadDelay",
                oldUnloadDelay,
                this.unloadDelay);
    }

    @Override
    public void incrementInProgressAsyncCount() {
        inProgressAsyncCount.incrementAndGet ();
    }

    @Override
    public void decrementInProgressAsyncCount() {
        inProgressAsyncCount.decrementAndGet ();
    }

    public long getInProgressAsyncCount() {
        return inProgressAsyncCount.get ();
    }

    /**
     * @return the original document root for this Context.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     * Is only set as deployment has change docRoot!
     */
    public String getOriginalDocBase() {
        return this.originalDocBase;
    }

    /**
     * Set the original document root for this Context.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     *
     * @param docBase The original document root
     */
    public void setOriginalDocBase(String docBase) {
        this.originalDocBase = docBase;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        synchronized (pausedLock) {
            while (paused) {
                try {
                    pausedLock.wait ();
                } catch (InterruptedException ignored) {

                }
            }
        }

        super.invoke (request, response);
    }
}
