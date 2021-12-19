package com.example.loader;

import com.example.life.*;
import com.example.resource.AbstractContext;
import com.example.resource.FileDirContext;
import com.example.resource.ResourceAttributes;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.example.loader.Constants.WEB_INF_CLASSES_LOCATION;
import static com.example.loader.Constants.WEB_INF_LIB_LOCATION;

/**
 * 线程安全的类加载器
 * 外部提供的路径必须是平台相关的，即必须是对应的/或者\
 * 这个类可能被继承
 * <p>
 * todo filter实现
 * todo notFoundResources在jar，repository等改变的时候会错误，仍然认为找不到
 *
 * @date 2021/12/18 16:10
 */
public class WebappClassLoader
        extends URLClassLoader
        implements Reloader, Lifecycle {
    private static final Logger log = LoggerFactory.getLogger (WebappClassLoader.class);

    /**
     * 找不到的资源，下次再找也找不到，加快速度。
     * key是org.apache.tools.ant.Executor
     */
    protected final Set<String> notFoundResources = Collections.newSetFromMap (new ConcurrentHashMap<> ());
    /**
     * ResourceEntry缓存，给findResource方法使用的
     */
    protected final Map<String, ResourceEntry> resourceEntries = new ConcurrentHashMap<> ();
    /**
     * 类仓库，类的查找位置
     */
    protected final List<ClassRepository> classRepositories = new ArrayList<> ();
    /**
     * jar仓库
     */
    protected final Map<String, JarRepository> jarRepositories = new HashMap<> ();
    protected final LifeCycleSupport lifeCycleSupport = new LifeCycleSupport (this);
    /**
     * jar的根路径，所有的jar都在这个根路径中
     * 典型值 /WEB-INF/lib
     */
    protected String jarPath = null;
    /**
     * 关联的context，实现资源查找
     */
    protected AbstractContext resourceContext;
    /**
     * delegate代表是否遵循双亲委派还是破坏双亲委派
     * 破坏双亲委派的话，就是自己加载，如果自己加载不了，才让父类加载器加载
     */
    protected volatile boolean delegate = false;
    /**
     * 用于LifeCycle接口
     */
    protected volatile boolean running = false;
    /**
     * Has external repositories.
     * 似乎是用于加载外部资源，即非classes、lib的repository
     */
    protected boolean hasExternalRepositories = false;
    /**
     * 层次化的类加载器，parent即父类加载器；null表示父类加载器是sys classloader
     */
    protected ClassLoader parent;

    /**
     * 系统类加载器
     */
    protected ClassLoader systemClassLoader;

    public WebappClassLoader(ClassLoader parent) {
        super (new URL[0], parent);
        this.parent = parent;
        systemClassLoader = getSystemClassLoader ();
    }

    public WebappClassLoader() {
        super (new URL[0]);
        parent = null;
        systemClassLoader = getSystemClassLoader ();
    }

    public String getJarPath() {
        return jarPath;
    }

    /**
     * @param jarPath 如/WEB-INF/lib
     */
    public void setJarPath(String jarPath) {
        if (!jarPath.endsWith (File.separator)) {
            jarPath = jarPath + File.separator;
        }
        this.jarPath = jarPath;
    }

    public boolean isDelegate() {
        return delegate;
    }

    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }

    public void setResourceContext(AbstractContext resourceContext) {
        this.resourceContext = resourceContext;
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifeCycleSupport.addLifecycleListener (listener);
    }

    @Override
    public List<LifecycleListener> findLifecycleListeners() {
        return lifeCycleSupport.getListeners ();
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifeCycleSupport.removeLifecycleListener (listener);
    }

    /**
     * 会自己添加WEB-INF/classes和WEB-INF/lib
     */
    @Override
    public synchronized void start() throws LifecycleException {
        if (running) {
            throw new LifecycleException ();
        }
        running = true;
        lifeCycleSupport.fireLifecycleEvent (EventType.START_EVENT, null);

        //自己添加WEB-INF/classes和WEB-INF/lib
        if (resourceContext == null) {
            throw new IllegalStateException ();
        }

        addRepositoryInternal (WEB_INF_CLASSES_LOCATION);
        log.info ("添加Class Repository：" + WEB_INF_CLASSES_LOCATION);

        Collection<Object> list = resourceContext.list (jarPath);
        if (list == null) {
            //合法的
            System.out.println (jarPath + " 不存在");
            return;
        }

        for (Object o : list) {
            if (o instanceof FileDirContext.FileResource) {
                try {
                    FileDirContext.FileResource resource = (FileDirContext.FileResource) o;
                    File file = resource.getFile ();
                    String fileName = file.getName ();

                    addJar (fileName, new JarFile (file), file);
                } catch (IOException e) {
                    e.printStackTrace ();
                }
            }
        }


    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (!running) {
            throw new LifecycleException ();
        }
        running = false;
        lifeCycleSupport.fireLifecycleEvent (EventType.STOP_EVENT, null);

        //因为可以再次启动，所以。。这个就相当于recycle
        try {
            for (JarRepository jarRepository : jarRepositories.values ()) {
                jarRepository.jarFile.close ();
            }
        } catch (IOException e) {
            e.printStackTrace ();
        }

        jarRepositories.clear ();
        resourceEntries.clear ();
        classRepositories.clear ();
        notFoundResources.clear ();
        delegate = false;
    }

    /**
     * addRepository，给用户使用，只能加入lib和classes之外的
     * <p>
     * lib和classes由addRepository加入
     * <p>
     * 调用这个方法之后hasExternalRepositories就为true了
     */
    @Override
    public void addRepository(String repository) {
        //这两个是内部资源
        if (repository.startsWith (WEB_INF_LIB_LOCATION)
                || repository.startsWith (WEB_INF_CLASSES_LOCATION))
            return;

        try {
            URL url = new URL (repository);
            super.addURL (url); //父类的这个方法是线程安全的
            hasExternalRepositories = true;
            log.info ("添加外部资源 {}", repository);
        } catch (MalformedURLException e) {
            e.printStackTrace ();
            log.error ("添加外部资源 {} 失败", repository);
        }
    }

    public URL findResource(final String name) {
        URL url = null;
        ResourceEntry entry = resourceEntries.get (name);
        if (entry == null) {
            entry = findResourceInternal (name, name);
        }
        if (entry != null) {
            url = entry.source;
        }
        if ((url == null) && hasExternalRepositories)
            url = super.findResource (name);

        return (url);
    }

    /**
     * 添加jar包，保证线程安全
     * <p>
     * 这个方法给Loader使用
     * <p>
     * public for test use
     * 外部调用须线程安全
     *
     * @param name    jar name，比如xxx.jar
     * @param file    jar路径，从工作路径开始，如webapp/WEB-INF/lib/xx.jar
     * @param jarFile 通过file创建的 new JarFile (file)
     */
    void addJar(String name, JarFile jarFile, File file) throws IOException {
        if (name == null ||
                jarFile == null ||
                file == null) {
            throw new IllegalArgumentException (String.format ("%s,%s,%s", name, jarFile, file));
        }

        if (jarPath == null ||
                !file.getCanonicalPath ().endsWith (getJarPath () + name)) {
            throw new IllegalArgumentException ("不在jar path: " + getJarPath () + "下");
        }

        JarRepository jarRepository = new JarRepository ();
        jarRepository.jarFile = jarFile;
        jarRepository.jarName = name;
        jarRepository.originalJarFile = file;

        //获得修改时间
        String p = getJarPath () + name;//所以jar不能嵌套目录
        FileDirContext.FileResourceAttributes attributes =
                (FileDirContext.FileResourceAttributes) resourceContext.getAttributes (p);
        jarRepository.lastModifyTime = attributes.getLastModified ();

        jarRepositories.put (name, jarRepository);

        //todo 验证jar包中是否含有黑名单的类
        log.info ("添加jar {}", file.getPath ());
    }

    /**
     * 内部使用的addRepository
     * <p>
     * 加入的repository是内部的，即/WEB-INF/lib和/WEB-INF/classes
     * <p>
     * 这个方法给Loader使用
     * <p>
     * public for test use
     * 文件，需要能定位到，如new File ("webapps/testClassLoader/WEB-INF/classes"))
     * 外部调用须线程安全
     *
     * @param repository 以docbase为基础的绝对路径，示例："/WEB-INF/classes"
     */
    void addRepositoryInternal(String repository) {
        if (StringUtils.isEmpty (repository) || resourceContext == null) {
            throw new IllegalArgumentException ();
        }

        Object lookup = resourceContext.lookup (repository);
        if (!(lookup instanceof FileDirContext)) {
            throw new IllegalArgumentException ("必须是文件夹");
        }

        File docBase = new File (((FileDirContext) lookup).getDocBase ());
        File file = new File (docBase, repository);
        //保证结尾有/
        if (!repository.endsWith (File.separator)) {
            repository = repository + File.separator;
        }

        ClassRepository classRepository = new ClassRepository ();
        classRepository.pathFile = file;
        classRepository.pathName = repository;

        classRepositories.add (classRepository);
    }

    @Override
    public synchronized List<String> findRepositories() {
        List<String> list = new ArrayList<> ();
        for (ClassRepository classRepository : classRepositories) {
            list.add (classRepository.pathName);
        }
        return list;
    }

    /**
     * 用于检测类是否修改，是否需要重新加载，具体的加载逻辑在context中
     * 只检查jar
     */
    @Override
    public boolean modified() {
        for (JarRepository jarRepository : jarRepositories.values ()) {
            String jar = getJarPath () + jarRepository.jarName;
            ResourceAttributes attributes = resourceContext.getAttributes (jar);
            if (attributes == null) {
                //说明删除了
                return true;
            }

            long lastModified = attributes.getLastModified ();
            if (lastModified != jarRepository.lastModifyTime) {
                System.out.println ("jar " + jarRepository.jarName + " 被修改了");
                return true;
            }
        }

        //判断jar的数目改没改
        for (Object o : resourceContext.list (jarPath)) {
            if (o instanceof FileDirContext.FileResource) {
                FileDirContext.FileResource fileResource = (FileDirContext.FileResource) o;
                String name = fileResource.getFile ().getName ();
                JarRepository jarRepository = jarRepositories.get (name);

                if (jarRepository == null) {
                    //说明新增了
                    System.out.println ("新增jar " + name);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 查找资源,线程安全的
     *
     * @param name 类名 例如org.apache.tools.ant.Executor
     * @param path 例如 org/apache/tools/ant/Executor.class
     */
    protected ResourceEntry findResourceInternal(String name, String path) {
        if (!running) {
            return null;
        }

        //检查缓存
        if (resourceEntries.containsKey (name)) {
            return resourceEntries.get (name);
        }

        // 如果上次也没找到，就说明本地没有
        // 除非external
        if (notFoundResources.contains (name)) {
            return null;
        }


        InputStream stream = null;
        ResourceEntry resourceEntry = null;
        int length = -1;

        //先查找repository
        for (ClassRepository classRepository : classRepositories) {
            //pathName是仓库path，path是仓库下的path
            //这里pathName结尾肯定是/
            String fullName = classRepository.pathName + path;
            Object o = resourceContext.lookup (fullName);

            //顺便检验了null（即存不存在）
            if (o instanceof FileDirContext.FileResource) {
                FileDirContext.FileResource lookup = (FileDirContext.FileResource) o;
                try {
                    resourceEntry = new ResourceEntry ();
                    resourceEntry.source = file2URL (new File (classRepository.pathFile, path));//仓库路径+path
                    resourceEntry.codeBase = resourceEntry.source;

                    ResourceAttributes attributes = lookup.getAttribute ();
                    resourceEntry.lastModified = attributes.getLastModified ();

                    stream = lookup.streamContent ();
                    length = (int) attributes.getContentLength ();
                } catch (IOException e) {
                    e.printStackTrace ();
                    return null;
                }
            }
        }

        if (resourceEntry == null) {
            //没找到,找jar
            for (JarRepository jarRepository : jarRepositories.values ()) {
                String jarPath = path.replace ('\\', '/');//jar 内必须是/
                JarEntry jarEntry = jarRepository.jarFile.getJarEntry (jarPath);

                if (jarEntry != null) {
                    resourceEntry = new ResourceEntry ();

                    try {
                        resourceEntry.codeBase = file2URL (jarRepository.originalJarFile);
                        String jarFakeUrl = resourceEntry.codeBase.toString ();
                        //组合成jar:file:/C:/Users/dell/Desktop/HowTomcatWorks/myApp/WEB-INF/lib/ant-1.6.5.jar!/org/apache/tools/ant/Executor.class
                        jarFakeUrl = "jar:" + jarFakeUrl + "!/" + path;
                        resourceEntry.source = new URL (jarFakeUrl);
                    } catch (IOException e) {
                        e.printStackTrace ();
                    }

                    try {
                        resourceEntry.manifest = jarRepository.jarFile.getManifest ();
                        stream = jarRepository.jarFile.getInputStream (jarEntry);
                        length = (int) jarEntry.getSize ();
                    } catch (IOException e) {
                        return null;
                    }

                    resourceEntry.lastModified = jarRepository.lastModifyTime;
                    break;
                }
            }
        }

        if (resourceEntry == null) {
            //找不到
            notFoundResources.add (name);
            log.debug ("定位不到资源 {}", name);
            return null;
        }

        //把stream读出来
        try {
            byte[] buf = new byte[length];
            IOUtils.readFully (stream, buf);
            resourceEntry.binaryContent = buf;
        } catch (IOException e) {
            e.printStackTrace ();
            return null;
        }

        //添加完成任务缓存
        //虽然map是线程安全的，但是仍然可能覆盖，我们希望put只会put一次
        //否则可能返回的entry不是map中的entry
        if (!resourceEntries.containsKey (name)) {
            synchronized (resourceEntries) {
                if (!resourceEntries.containsKey (name)) {
                    resourceEntries.put (name, resourceEntry);
                }
            }
        }
        log.debug ("定位到resource {}", resourceEntries.get (name));
        return resourceEntries.get (name);
    }

    /**
     * 内部加载类
     * 根据找到的resource entry，来加载class，并加载package
     */
    protected Class<?> findClassInternal(String name)
            throws ClassNotFoundException {
        if (name == null) {
            throw new NullPointerException ();
        }

        String tempPath = name.replace ('.', '/');
        String classPath = tempPath + ".class";
        ResourceEntry entry = findResourceInternal (name, classPath);

        if (entry == null) {
            throw new ClassNotFoundException ();
        }

        if (entry.loadedClass != null) {
            return entry.loadedClass;
        }

        // define package，作用未知。。
        String packageName = null;
        int pos = name.lastIndexOf ('.');//xxx.class去掉.号
        if (pos != -1) {
            //即org.apache.tools.ant.Executor 的 org.apache.tools.ant
            packageName = name.substring (0, pos);
        }

        Package pkg;
        if (packageName != null) {
            pkg = getPackage (packageName);
            if (pkg == null) {
                if (entry.manifest == null) {
                    definePackage (packageName, null, null, null, null, null,
                            null, null);
                } else {
                    definePackage (packageName, entry.manifest, entry.codeBase);
                }
            }
        }

        //加载类
        //entry是引用，所以loadedClass可能不是null了
        if (entry.loadedClass == null) {
            synchronized (resourceEntries) {
                if (entry.loadedClass == null) {
                    entry.loadedClass = defineClass (name, entry.binaryContent, 0, entry.binaryContent.length);
                }

            }
        }
        return entry.loadedClass;
    }

    /**
     * 相比findClassInternal，这个方法既加载内部资源，又加载外部资源
     */
    public Class<?> findClass(String name) throws ClassNotFoundException {
        //先尝试内部加载
        Class<?> clazz = null;
        try {
            clazz = findClassInternal (name);
        } catch (ClassNotFoundException e) {
            if (!hasExternalRepositories) {
                throw new ClassNotFoundException ();
            }
        }

        if (clazz == null && hasExternalRepositories) {
            clazz = super.loadClass (name);
        }

        if (clazz == null) {
            throw new ClassNotFoundException ();
        }

        return clazz;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        log.info ("类加载 {}", name);
        //判断缓存是否存在
        Class<?> clazz = findLoadedClass0 (name);
        if (clazz != null) {
            if (resolve) {
                resolveClass (clazz);
            }
            return clazz;
        }

        //判断classLoader缓存，这个似乎是classLoader本身不会多次加载类的原因
        clazz = findLoadedClass (name);
        if (clazz != null) {
            if (resolve) {
                resolveClass (clazz);
            }
            return clazz;
        }

        //委托给system classloader，避免覆盖它加载的类
        try {
            clazz = systemClassLoader.loadClass (name);
            if (clazz != null) {
                if (resolve) {
                    resolveClass (clazz);
                }
                return clazz;
            }
        } catch (ClassNotFoundException ignored) {

        }


        //如果双亲委派，那就委派
        //todo 添加filter功能
        if (delegate) {
            ClassLoader loader = parent;
            if (parent == null) {
                loader = getSystemClassLoader ();
            }

            try {
                clazz = loader.loadClass (name);
            } catch (ClassNotFoundException ignored) {

            }
            if (clazz != null) {
                if (resolve) {
                    resolveClass (clazz);
                }
                return clazz;
            }
        }

        //自己加载
        try {
            clazz = findClass (name);
            if (clazz != null) {
                if (resolve) {
                    resolveClass (clazz);
                }
                return clazz;
            }
        } catch (ClassNotFoundException ignored) {

        }


        //如果不双亲委派，还加载不到，那就尝试使用双亲加载
        if (!delegate) {
            ClassLoader loader = parent;
            if (parent == null) {
                loader = getSystemClassLoader ();
            }

            try {
                clazz = loader.loadClass (name);
            } catch (ClassNotFoundException ignored) {

            }
            if (clazz != null) {
                if (resolve) {
                    resolveClass (clazz);
                }
                return clazz;
            }
        }

        log.error ("类 {} 加载失败", name);
        throw new ClassNotFoundException (name);
    }

    /**
     * 检查本地类缓存
     */
    protected Class<?> findLoadedClass0(String name) {
        return Optional
                .ofNullable (resourceEntries)
                .map (x -> x.get (name))
                .map (x -> x.loadedClass) //获得加载的类
                .orElse (null);
    }

    private URL file2URL(File file) throws IOException {
        return file.getCanonicalFile ().toURL ();
    }

    /**
     * jar包的位置等信息
     */
    private static class JarRepository {

        /**
         * 对应的jar文件
         */
        JarFile jarFile;

        /**
         * 对应的jar文件 jarFile = new JarFile(originalJarFile)
         */
        File originalJarFile;

        /**
         * 用于检测是否修改
         */
        long lastModifyTime;

        String jarName;


    }

    /**
     * class
     */
    private static class ClassRepository {

        /**
         * 文件夹位置
         */
        File pathFile;

        String pathName;


    }


}
