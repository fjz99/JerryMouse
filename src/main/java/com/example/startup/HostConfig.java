package com.example.startup;

import com.example.Container;
import com.example.Context;
import com.example.Host;
import com.example.core.StandardContext;
import com.example.core.StandardHost;
import com.example.life.EventType;
import com.example.life.LifecycleEvent;
import com.example.life.LifecycleException;
import com.example.life.LifecycleListener;
import com.example.util.ExpandWar;
import com.example.util.UriUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 用于部署web app
 * deployed key == app.name == context name
 */
@Slf4j
public class HostConfig implements LifecycleListener {
    /**
     * The resolution, in milliseconds, of file modification times.
     */
    protected static final long FILE_MODIFICATION_RESOLUTION_MS = 1000;
    /**
     * Map of deployed applications.
     * key是文件夹或文件名，但是不一定等于context的name，因为context的name可以在context.xml中写出来
     */
    protected final Map<String, DeployedApplication> deployed =
            new ConcurrentHashMap<> ();

    /**
     * The list of Wars in the appBase to be ignored because they are invalid
     * (e.g. contain /../ sequences).
     */
    protected final Set<String> invalidWars = new HashSet<> ();
    private final Object digesterLock = new Object ();
    /**
     * Set of applications which are being serviced, and shouldn't be
     * deployed/undeployed/redeployed at the moment.
     * 正在服务？？？，不能deployed/undeployed/redeployed
     */
    private final Set<String> servicedSet = Collections.newSetFromMap (new ConcurrentHashMap<> ());
    /**
     * The Java class name of the Context implementation we should use.
     */
    protected String contextClass = "com.example.core.StandardContext";
    /**
     * The Host we are associated with.
     */
    protected Host host;
    /**
     * Should we deploy XML Context config files packaged with WAR files and
     * directories?
     * 是否部署context.xml文件，可以不部署，即忽略context.xml，但是wrapper仍然好使
     */
    protected boolean deployXML = false;
    /**
     * Should we unpack WAR files when auto-deploying applications in the
     * <code>appBase</code> directory?
     */
    protected boolean unpackWARs = false;
    /**
     * The <code>Digester</code> instance used to parse context descriptors.
     */
    protected Digester digester = createDigester (contextClass);

    /**
     * 只会set properties，具体的wrapper由web.xml创建
     */
    protected static Digester createDigester(String contextClassName) {
        Digester digester = new Digester ();
        digester.setValidating (false);
        // Add object creation rule
        digester.addObjectCreate ("Context", contextClassName, "className");
        // Set the properties on that object (it doesn't matter if extra
        // properties are set)
        digester.addSetProperties ("Context");
        return digester;
    }


    public String getContextClass() {
        return this.contextClass;
    }


    public void setContextClass(String contextClass) {
        String oldContextClass = this.contextClass;
        this.contextClass = contextClass;

        if (!oldContextClass.equals (contextClass)) {
            synchronized (digesterLock) {
                digester = createDigester (getContextClass ());
            }
        }
    }


    public boolean isDeployXML() {
        return this.deployXML;
    }


    public void setDeployXML(boolean deployXML) {
        this.deployXML = deployXML;
    }


    public boolean isUnpackWARs() {
        return this.unpackWARs;
    }


    public void setUnpackWARs(boolean unpackWARs) {
        this.unpackWARs = unpackWARs;
    }

    /**
     * Process the START event for an associated Host.
     *
     * @param event The lifecycle event that has occurred
     */
    @Override
    public void lifecycleEvent(LifecycleEvent event) {

        try {
            host = (Host) event.getLifecycle ();
            if (host instanceof StandardHost) {
                setDeployXML (((StandardHost) host).isDeployXML ());
                setUnpackWARs (((StandardHost) host).isUnpackWARs ());
                setContextClass (((StandardHost) host).getContextClass ());
            }
        } catch (ClassCastException e) {
            log.error ("", e);
            return;
        }

        if (event.getType ().equals (EventType.PERIODIC_EVENT)) {
            check ();
        } else if (event.getType ().equals (EventType.BEFORE_START_EVENT)) {
            beforeStart ();
        } else if (event.getType ().equals (EventType.START_EVENT)) {
            start ();
        }
    }

    /**
     * 创建文件夹
     */
    public void beforeStart() {
        if (host.getCreateDirs ()) {
            File[] dirs = new File[]{host.getAppBaseFile (), host.getConfigBaseFile ()};
            for (File dir : dirs) {
                if (!dir.mkdirs () && !dir.isDirectory ()) {
                    log.error ("创建文件夹失败{}", dir);
                }
            }
        }
    }

    public void start() {
        //如果不是文件夹，那就不部署
        if (!host.getAppBaseFile ().isDirectory ()) {
            log.error ("Host {} 的appbase {} 不是文件夹", host.getName (), host.getAppBaseFile ().getAbsolutePath ());
            host.setDeployOnStartup (false);
            host.setAutoDeploy (false);
        }

        //如果启动的时候需要部署
        //部署了之后不用start，会在host start的时候start
        if (host.getDeployOnStartup ()) {
            deployApps ();
        }
    }


    /**
     * Get the instant where an application was deployed.
     *
     * @param name the context name
     * @return 0L if no application with that name is deployed, or the instant
     * on which the application was deployed
     */
    public long getDeploymentTime(String name) {
        DeployedApplication app = deployed.get (name);
        if (app == null) {
            return 0L;
        }

        return app.timestamp;
    }


    /**
     * Has the specified application been deployed? Note applications defined
     * in server.xml will not have been deployed.
     *
     * @param name the context name
     * @return <code>true</code> if the application has been deployed and
     * <code>false</code> if the application has not been deployed or does not
     * exist
     */
    public boolean isDeployed(String name) {
        return deployed.containsKey (name);
    }

    protected File returnCanonicalPath(String path) {
        File file = new File (path);
        if (!file.isAbsolute ()) {
            file = new File (host.getCatalinaBase (), path);
        }
        try {
            return file.getCanonicalFile ();
        } catch (IOException e) {
            return file;
        }
    }


    /**
     * Get the name of the configBase.
     * For use with JMX management.
     *
     * @return the config base
     */
    public String getConfigBaseName() {
        return host.getConfigBaseFile ().getAbsolutePath ();
    }


    /**
     * Deploy applications for any directories or WAR files that are found
     * in our "application root" directory.
     */
    protected void deployApps() {
        File appBase = host.getAppBaseFile ();
        File configBase = host.getConfigBaseFile ();
        String[] filteredAppPaths = appBase.list ();
        if (filteredAppPaths == null) {
            return;
        }

        // Deploy XML descriptors from configBase
//        部署Context.xml，即给context添加valve等
//        TODO
//        deployDescriptors (configBase, configBase.list ());
        deployWARs (appBase, filteredAppPaths);
        deployDirectories (appBase, filteredAppPaths);
    }

    /**
     * Deploy exploded webapp.
     * <p>
     * Note: It is expected that the caller has successfully added the app
     * to servicedSet before calling this method.
     *
     * @param contextName The context name,文件夹名
     * @param dir         The path to the root folder of the weapp
     */
    protected void deployDirectory(ContextName contextName, File dir) throws Exception {
        log.info ("Host {} 开始deploy {}", host.getName (), dir.getAbsolutePath ());

        File xml = new File (dir, Constants.ApplicationContextXml);
        File webXml = new File (host.getAppBaseFile (),
                contextName.getDocBase () + "/" + Constants.ApplicationWebXml);
        Context context;
        if (xml.exists () && deployXML) {
            //解析
            synchronized (digesterLock) {
                try {
                    context = (Context) digester.parse (xml);
                } catch (IOException | SAXException e) {
                    log.error ("context.xml解析失败", e);
                    return;
                } finally {
                    digester = createDigester (contextClass);//??必要吗？
                }
            }
        } else if (xml.exists () && !deployXML) {
            log.error ("部署 {} 时配置文件 {} 存在，但是deployXML=false", dir.getAbsolutePath (), xml.getAbsolutePath ());
            return;
        } else {
            //如果文件不存在或者deployXML=false（忽略xml文件），就创建一个空的
            context = (Context) Class.forName (contextClass).getConstructor ().newInstance ();
        }

        //设置contextConfig类
        Class<?> clazz = Class.forName (host.getConfigClass ());
        LifecycleListener listener = (LifecycleListener) clazz.getConstructor ().newInstance ();
        context.addLifecycleListener (listener);

        //添加context
        contextName.setContextDefaultName (context, false, dir);
//        context.setParent (host);
        //add child的时候自动start,同步启动
        if (host.findChild (context.getName ()) != null) {
            log.warn ("{} 已存在，放弃部署", context.getName ());
            return;
        }

        host.addChild (context);

        if (!context.isAvailable ()) {
            log.error ("Host {} deploy {} 失败", host.getName (), dir.getAbsolutePath ());
            return;
        }

        //添加监听的资源,用path
        DeployedApplication deployedApp = new DeployedApplication (context.getName (), false);
        //也监控war，万一新的同名war被添加了
        //简单处理
        deployedApp.redeployResources.put (dir.getAbsolutePath () + ".war", 0L);//war
        deployedApp.redeployResources.put (dir.getAbsolutePath (), dir.lastModified ());//文件夹
        if (deployXML) {
            deployedApp.redeployResources.put (xml.getAbsolutePath (), xml.lastModified ());//context.xml
        }
        //监听web.xml
        deployedApp.reloadResources.put (webXml.getAbsolutePath (), webXml.lastModified ());

        deployed.put (context.getName (), deployedApp);//数组使用path
        log.info ("Host {} deploy {} 成功", host.getName (), dir.getAbsolutePath ());
    }


    /**
     * Deploy packed WAR.
     * 麻烦的地方在于，可能war ，文件夹都存在。。很麻烦
     * <p>
     * Note: It is expected that the caller has successfully added the app
     * to servicedSet before calling this method.
     *
     * @param contextName The context name，其实就是war包文件的名字。。
     * @param war         The WAR file
     */
    protected void deployWAR(ContextName contextName, File war) throws Exception {
        log.info ("Host {} 开始部署war {} ", host.getName (), war.getAbsolutePath ());

        //因为可能war和dir都存在。。
        File xmlInDir = new File (host.getAppBaseFile (),
                contextName.getDocBase () + "/" + Constants.ApplicationContextXml);
        File webXml = new File (host.getAppBaseFile (),
                contextName.getDocBase () + "/" + Constants.ApplicationWebXml);

        boolean xmlInWar = false;
        try (JarFile jar = new JarFile (war)) {
            JarEntry entry = jar.getJarEntry (Constants.ApplicationContextXml);
            if (entry != null) {
                //有context.xml文件
                xmlInWar = true;
            }
        } catch (IOException ignored) {

        }

        Context context = null;
        //假如文件夹和war都存在，而且这个context还没有部署过，说明用户复制的war懒得删除了
        //此时优先文件夹
        if (deployXML && xmlInDir.exists ()) {
            synchronized (digesterLock) {
                try {
                    context = (Context) digester.parse (xmlInDir);
                } catch (Exception e) {
                    log.error ("xml解析失败", e);
                } finally {
                    digester = createDigester (contextClass);//??
                }
            }
            if (context != null) {
                context.setConfigFile (xmlInDir.toURI ().toURL ());
            }
        } else if (deployXML && xmlInWar) {
            synchronized (digesterLock) {
                try (JarFile jar = new JarFile (war)) {
                    JarEntry entry = jar.getJarEntry (Constants.ApplicationContextXml);
                    try (InputStream istream = jar.getInputStream (entry)) {
                        context = (Context) digester.parse (istream);
                    }
                } catch (Exception e) {
                    log.error ("xml解析失败", e);
                    return;
                } finally {
                    digester = createDigester (contextClass);//??
                    if (context != null) {
                        context.setConfigFile (UriUtil.buildJarUrl (war, Constants.ApplicationContextXml));
                    }
                }
            }
        } else if (!deployXML && xmlInWar) {
            log.error ("部署 {} 时配置文件context.xml在war{}中，但是deployXML=false", contextName.getName (), war.getAbsolutePath ());
            return;
        } else {
            //如果文件不存在或者deployXML=false（忽略xml文件），就创建一个空的
            context = (Context) Class.forName (contextClass).getConstructor ().newInstance ();
        }

        //设置contextConfig类
        Class<?> clazz = Class.forName (host.getConfigClass ());
        LifecycleListener listener = (LifecycleListener) clazz.getConstructor ().newInstance ();
        context.addLifecycleListener (listener);

        //添加context
        contextName.setContextDefaultName (context, true, war);
        if (host.findChild (context.getName ()) != null) {
            log.warn ("{} 已存在，放弃部署", context.getName ());
            return;
        }
//        context.setParent (host);
        host.addChild (context);

        if (!context.isAvailable ()) {
            log.error ("Host {} deploy {} 失败", host.getName (), war.getAbsolutePath ());
            return;
        }

        //添加监听的资源
        //监听war
        DeployedApplication deployedApp = new DeployedApplication (context.getName (), false);
        deployedApp.redeployResources.put (war.getAbsolutePath (), war.lastModified ());

        boolean unpackWAR = unpackWARs;
        if (unpackWAR && context instanceof StandardContext) {
            unpackWAR = ((StandardContext) context).getUnpackWAR ();
        }
        if (unpackWAR && context.getDocBase () != null) {
            File docBase = new File (host.getAppBaseFile (), context.getDocBase ());
            //监听解压的文件夹,修改就要重新部署
            deployedApp.redeployResources.put (docBase.getAbsolutePath (), docBase.lastModified ());
            if (deployXML && (xmlInWar || xmlInDir.exists ())) {
                //监听解压的xml
                deployedApp.redeployResources.put (xmlInDir.getAbsolutePath (), xmlInDir.lastModified ());
            }
            //监听web.xml
            deployedApp.reloadResources.put (webXml.getAbsolutePath (), webXml.lastModified ());
        }

        deployed.put (context.getName (), deployedApp);
        log.info ("Host {} deploy {} 成功", host.getName (), war.getAbsolutePath ());
    }


    protected void deployDirectories(File appBase, String[] files) {
        ExecutorService executor = host.getStartStopExecutor ();
        List<Future<?>> jobs = new ArrayList<> ();

        for (String file : files) {
            File dir = new File (appBase, file);
            ContextName contextName = new ContextName (file);
            String name = contextName.getName ();

            if (servicedSet.add (name)) {
                //已经部署了,但是判断不准确，因为key==context.name,而context的name没有解析就无法确定
                //所以在deployDirectory内还会去重
                if (deploymentExists (name)) {
                    servicedSet.remove (name);
                    continue;
                }

                Future<?> future = executor.submit (() -> {
                    try {
                        deployDirectory (contextName, dir);
                    } catch (Exception e) {
                        log.error ("", e);
                    } finally {
                        servicedSet.remove (name);
                    }
                });
                jobs.add (future);
            }

        }

        for (Future<?> job : jobs) {
            try {
                job.get ();
            } catch (Exception e) {
                log.error ("", e);
            }
        }
    }


    /**
     * 周期检查任务
     */
    protected void check() {
        if (host.getAutoDeploy ()) {
            //先处理undeploy和reload(检查资源修改)
            DeployedApplication[] applications = deployed.values ().toArray (new DeployedApplication[0]);
            for (DeployedApplication app : applications) {
                if (servicedSet.add (app.name)) {
                    try {
                        checkResources (app);
                    } finally {
                        servicedSet.remove (app.name);
                    }
                }
            }

            //再统一deploy，这样新加的也会deploy
            deployApps ();
        }
    }

    /**
     * 注意是有序的LinkedHashMap，这个和添加的顺序有关，检查的时候只要检查到一个改变了，就会删除后面的！
     * 重新部署需要删除旧的资源，优先级war大于文件夹
     * 而reload则不需要
     * <p>
     * 简单处理,文件夹优先级高
     * 文件被修改文件夹的修改时间不变
     */
    protected synchronized void checkResources(DeployedApplication app) {
        //redeploy
        log.trace ("check {}", app.name);
        String[] array = app.redeployResources.keySet ().toArray (new String[0]);
        for (int i = 0; i < array.length; i++) {
            File file = new File (array[i]);
            if (!file.exists ()) {
                //删除了资源无所谓，因为正在用到的资源没法删除。。
                continue;
            }

            long lastTime = app.redeployResources.get (array[i]);
            if (lastTime != file.lastModified ()) {
                //FIXME ??看源码
                //修改了
                undeploy (app);

                for (int j = i + 1; j < array.length; j++) {
                    File current = new File (array[j]);
                    ExpandWar.delete (current);
                }
                return;
            }
        }

        //reload,其实不会用到，因为context reload需要重置状态，即recycle
        array = app.reloadResources.keySet ().toArray (new String[0]);
        for (String resource : array) {
            File file = new File (resource);
            if (!file.exists ()) {
                continue;
            }

            long lastTime = app.reloadResources.get (resource);
            if (lastTime != file.lastModified ()) {
                //修改了
                app.reloadResources.put (resource, file.lastModified ());
                reload (app);
                return;
            }
        }

        log.trace ("check {} 未修改", app.name);
    }

    private void reload(DeployedApplication app) {
        if (log.isInfoEnabled ()) {
            log.info ("reloading {} ...", app.name);
        }
        Context context = (Context) host.findChild (app.name);
        if (context.isRunning ()) {
            context.reload ();
        } else {
            try {
                context.start ();
            } catch (Exception e) {
                log.error ("", e);
            }
        }
    }

    private void undeploy(DeployedApplication app) {
        if (log.isInfoEnabled ()) {
            log.info ("undeploy {} ...", app.name);
        }

        Container context = host.findChild (app.name);
        try {
            host.removeChild (context);
        } catch (Throwable t) {
            log.warn ("", t);
        }
        deployed.remove (app.name);
    }


    protected void deployWARs(File appBase, String[] files) {
        ExecutorService executor = host.getStartStopExecutor ();
        List<Future<?>> jobs = new ArrayList<> ();

        for (String file : files) {
            File war = new File (appBase, file);
            ContextName contextName = new ContextName (file);
            String name = contextName.getName ();

            if (!(file.toLowerCase (Locale.ENGLISH).endsWith (".war")
                    && war.isFile () && war.canRead ())) {
                continue;
            }

            if (servicedSet.add (name)) {
                //已经部署了,但是判断不准确，因为key==context.name,而context的name没有解析就无法确定
                //所以在deployWAR内还会去重
                if (deploymentExists (name)) {
                    servicedSet.remove (name);
                    continue;
                }

                Future<?> future = executor.submit (() -> {
                    try {
                        deployWAR (contextName, war);
                    } catch (Exception e) {
                        log.error ("", e);
                    } finally {
                        servicedSet.remove (name);
                    }
                });
                jobs.add (future);
            }

        }

        for (Future<?> job : jobs) {
            try {
                job.get ();
            } catch (Exception e) {
                log.error ("", e);
            }
        }
    }


    /**
     * Check if a webapp is already deployed in this host.
     *
     * @param name 文件夹名或context name
     */
    protected boolean deploymentExists(String name) {
        //检查文件夹名
        for (Container child : host.findChildren ()) {
            Context context = (Context) child;
            String docBase = context.getDocBase ();
            if (fixPath (docBase).equals (fixPath (name))) {
                return true;
            }
        }
        return deployed.containsKey (name) || (host.findChild (name) != null);
    }

    private String fixPath(String docBase) {
        while (docBase.contains ("\\")) {
            docBase = docBase.replace ('\\', '/');
        }
        while (docBase.startsWith ("/")) {
            docBase = docBase.substring (1);
        }
        return docBase;
    }


    /**
     * 实体类，表示被监控的部署的context
     */
    protected static final class DeployedApplication {
        /**
         * Application context path. The assertion is that
         * (host.getChild(name) != null).
         */
        public final String name;
        /**
         * Does this application have a context.xml descriptor file on the
         * host's configBase?
         * 是否有context.xml，context.xml即为描述符，部署描述符就是解析context.xml，加载多个context
         * 因为context可能要配置属性和valve，所有单独使用web.xml不行
         */
        public final boolean hasDescriptor;
        /**
         * 保存监控的资源，任何资源被移除后，会undeploy
         * 被修改后会redeploy
         * 例如添加war包，context.xml文件
         * 0表示文件不存在，所以没有上次修改时间
         * <p>注意是有序的LinkedHashMap，这个和添加的顺序有关，检查的时候只要检查到一个改变了，就会删除后面的！</p>
         * 这个会delete原来的资源
         * <p>
         * Any modification of the specified (static) resources will cause a
         * redeployment of the application. If any of the specified resources is
         * removed, the application will be undeployed. Typically, this will
         * contain resources like the context.xml file, a compressed WAR path.
         * The value is the last modification time.
         */
        public final LinkedHashMap<String, Long> redeployResources =
                new LinkedHashMap<> ();
        /**
         * 保存监控的资源，如果修改就会被reload，和redeploy不一样，reload更轻量级，是context组件的重启
         * 而redeploy更重量级，是context组件的重新构建
         * 而context.xml修改只能redeploy，但是web.xml修改可以reload（因为有contextConfig类）
         * 0表示文件不存在，所以没有上次修改时间
         * 这个是无序的，因为不会delete原来的资源
         * <p>
         * Any modification of the specified (static) resources will cause a
         * reload of the application. This will typically contain resources
         * such as the web.xml of a webapp, but can be configured to contain
         * additional descriptors.
         * The value is the last modification time.
         */
        public final HashMap<String, Long> reloadResources = new HashMap<> ();
        /**
         * Instant where the application was last put in service.
         */
        public long timestamp = System.currentTimeMillis ();

        public DeployedApplication(String name, boolean hasDescriptor) {
            this.name = name;
            this.hasDescriptor = hasDescriptor;
        }
    }

    /**
     * 实体类，封装了context的path、name、doc base
     * 可以理解为默认的名字，如果context.xml没有指定的话，就需要通过文件夹路径生成path和name
     */
    @Getter
    @AllArgsConstructor
    public final class ContextName {

        public static final String ROOT_NAME = "ROOT";
        private static final String VERSION_MARKER = "##";
        private static final char FWD_SLASH_REPLACEMENT = '#';

        private String docBase;
        private String path;
        /**
         * 对应文件夹的名字，可能是多层文件夹;或者war包的文件名
         */
        private String name;

        public ContextName(String name) {
            // Strip off any leading "/"
            if (name.startsWith ("/")) {
                name = name.substring (1);
            }

            // Replace any remaining /
            name = name.replace ('/', FWD_SLASH_REPLACEMENT);

            // Insert the ROOT name if required
            if (name.startsWith (VERSION_MARKER) || name.isEmpty ()) {
                name = ROOT_NAME + name;
            }

            // Remove any file extensions
            if (name.toLowerCase (Locale.ENGLISH).endsWith (".war") ||
                    name.toLowerCase (Locale.ENGLISH).endsWith (".xml")) {
                name = name.substring (0, name.length () - 4);
            }

            docBase = name;
            path = "/" + docBase.replace (FWD_SLASH_REPLACEMENT, '/');
            this.name = path;
        }

        void setContextDefaultName(Context context, boolean isWar, File file) {
            if (context.getDocBase () == null ||
                    context.getName () == null ||
                    context.getPath () == null) {

                log.warn ("host {} deploy dir/war {} 没有配置显式的context.xml，故此context" +
                                "将会被映射到一个默认contextPath={}，docBase={}，name={}",
                        host.getName (), file.getAbsolutePath (), path, docBase, name);


                context.setDocBase (docBase);
                context.setPath (path);
                context.setName (name);
            }

            if (isWar && !context.getDocBase ().endsWith (".war")) {
                context.setDocBase (context.getDocBase () + ".war");
            }

            //修正为正确的值
            name = context.getName ();
            path = context.getPath ();
            docBase = context.getDocBase ();
        }

        @Override
        public String toString() {
            StringBuilder tmp = new StringBuilder ();
            if ("".equals (path)) {
                tmp.append ('/');
            } else {
                tmp.append (path);
            }

            return tmp.toString ();
        }
    }


}
