package com.example.core;

import com.example.*;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.life.EventType;
import com.example.life.Lifecycle;
import com.example.life.LifecycleBase;
import com.example.life.LifecycleException;
import com.example.loader.Loader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @date 2021/12/23 19:54
 */
@Slf4j
public abstract class AbstractContainer extends LifecycleBase implements Container {

    protected final PropertyChangeSupport support = new PropertyChangeSupport (this);
    /**
     * The set of Mappers associated with this Container, keyed by protocol.
     */
    protected final Map<String, Mapper> mappers = new ConcurrentHashMap<> ();
    protected final List<Container> failures = new ArrayList<> ();
    /**
     * name，唯一id，用在children上
     */
    protected String name = "";
    protected Pipeline pipeline = new StandardPipeline (this);
    protected Map<String, Container> children = new ConcurrentHashMap<> ();
    protected Container parent;
    protected ClassLoader parentClassLoader;
    protected Loader loader;
    /**
     * 务必使用CopyOnWriteArrayList！<p>
     * 例如effective java上说的，for each listener的时候，如果listener remove或add一个listener，
     * 就会出现ConcurrentModificationException（因为sync是可重入的），
     * 所以需要使用CopyOnWriteArrayList
     */
    protected List<ContainerListener> containerListeners = new CopyOnWriteArrayList<> ();
    /**
     * 是否在运行时add了child之后直接start它
     */
    protected volatile boolean startChildren = true;
    /**
     * -1表示后台线程不会启动（根本不会start这个线程），单位秒
     */
    protected volatile int backgroundProcessorDelay = -1;
    /**
     * 停止线程的标志位
     */
    private volatile boolean stopThread = false;
    private Thread thread;
    /**
     * 用于子组件start stop的线程池
     */
    private ThreadPoolExecutor startStopExecutor;
    /**
     * 线程池线程数
     */
    private volatile int startStopThreads;

    public void setStartChildren(boolean startChildren) {
        this.startChildren = startChildren;
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public int getBackgroundProcessorDelay() {
        return backgroundProcessorDelay;
    }

    @Override
    public void setBackgroundProcessorDelay(int delay) {
        backgroundProcessorDelay = delay;
    }

    @Override
    public String getName() {
//        if (StringUtils.isEmpty (name)) {
//            throw new IllegalStateException ("每个组件必须设置name");
//        }

        return name;
    }

    @Override
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException ();
        }

        String oldName = this.name;
        this.name = name;
        support.firePropertyChange ("name", oldName, this.name);
    }

    public Loader getLoader() {
        return loader == null ?
                (getParent () == null ? null : getParent ().getLoader ())
                : loader;
    }

    public void setLoader(Loader loader) {
        this.loader = loader;
    }

    @Override
    public Container getParent() {
        return parent;
    }

    @Override
    public void setParent(Container container) {
        Container oldParent = this.parent;
        this.parent = container;
        support.firePropertyChange ("parent", oldParent, this.parent);
    }

    /**
     * 有啥用？？
     */
    @Override
    public ClassLoader getParentClassLoader() {
        if (parentClassLoader != null) {
            return parentClassLoader;
        }
        if (parent != null) {
            return getParent ().getParentClassLoader ();
        }
        return ClassLoader.getSystemClassLoader ();
    }

    @Override
    public void setParentClassLoader(ClassLoader parent) {
        ClassLoader oldParentClassLoader = this.parentClassLoader;
        this.parentClassLoader = parent;
        support.firePropertyChange ("parentClassLoader", oldParentClassLoader,
                this.parentClassLoader);
    }

    @Override
    public void backgroundProcess() {
        if (getPipeline () == null) {
            return;
        }

        Valve first = getPipeline ().getFirst ();
        while (first != null) {
            first.backgroundProcess ();
            first = first.getNext ();
        }

        //默认触发这个，HostConfig类用到了这个回调，从而进行deploy webapps的检查
        fireLifecycleEvent (EventType.PERIODIC_EVENT, null);
    }

    @Override
    public void addChild(Container child) {
        if (children.containsKey (child.getName ())) {
            throw new IllegalArgumentException ();
        }

        child.setParent (this);
        children.put (child.getName (), child);

        //如果是正在运行就启动，否则就等待一起启动
        if (startChildren && isRunning ()) {
            try {
                child.start ();
            } catch (LifecycleException e) {
                e.printStackTrace ();
            }
        }
        fireContainerEvent (ADD_CHILD_EVENT, child);
    }

    @Override
    public Container map(Request request, boolean update) {
        Mapper mapper = findMapper (request.getRequest ().getProtocol ().toLowerCase ());//lower case
        return Optional.ofNullable (mapper)
                .map (x -> x.map (request, update))
                .orElse (null);
    }

    protected final void addDefaultMapper(String mapperClass) {
        if (mapperClass == null) {
            return;
        }
        if (mappers.size () >= 1)
            return;

        try {
            Class<?> clazz = Class.forName (mapperClass);
            Mapper mapper = (Mapper) clazz.newInstance ();
            mapper.setProtocol ("http");
            addMapper (mapper);
            log.info ("{} 添加默认mapper {}", getName (), mapperClass);
        } catch (Exception e) {
            log.error ("containerBase.addDefaultMapper", e);
        }
    }

    public void addMapper(Mapper mapper) {
        synchronized (mappers) {
            if (mappers.get (mapper.getProtocol ()) != null)
                throw new IllegalArgumentException ("addMapper:  Protocol '" +
                        mapper.getProtocol () +
                        "' is not unique");
            mapper.setContainer (this);      // May throw IAE
            if (isRunning () && (mapper instanceof Lifecycle)) {
                try {
                    ((Lifecycle) mapper).start ();
                } catch (LifecycleException e) {
                    log.error ("ContainerBase.addMapper: start: ", e);
                    throw new IllegalStateException ("ContainerBase.addMapper: start: " + e);
                }
            }
            mappers.put (mapper.getProtocol (), mapper);
            fireContainerEvent (ADD_MAPPER_EVENT, mapper);
        }
    }

    @Override
    public Mapper findMapper(String protocol) {
        return mappers.get (protocol);
    }

    @Override
    public Mapper[] findMappers() {
        return mappers.values ().toArray (new Mapper[0]);
    }

    @Override
    public void addContainerListener(ContainerListener listener) {
        containerListeners.add (listener);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener (listener);
    }

    @Override
    public Container findChild(String name) {
        //避免发生空指针异常
        if (name == null) {
            return null;
        }

        return children.get (name);
    }

    @Override
    public Container[] findChildren() {
        return children.values ().toArray (new Container[0]);
    }

    @Override
    public ContainerListener[] findContainerListeners() {
        return containerListeners.toArray (new ContainerListener[0]);
    }

    @Override
    public void removeChild(Container child) {
        if (child == null) {
            return;
        }

        Container remove = children.remove (child.getName ());
        if (remove != null) {
            try {
                remove.stop ();
            } catch (LifecycleException ignored) {
                //忽略，因为children可能根本没有启动
            }
        }

        fireContainerEvent (REMOVE_CHILD_EVENT, child);
    }

    @Override
    public void removeContainerListener(ContainerListener listener) {
        containerListeners.remove (listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener (listener);
    }

    @Override
    public void fireContainerEvent(String type, Object data) {
        ContainerEvent event = new ContainerEvent (this, type, data);
        for (ContainerListener listener : containerListeners) {
            listener.containerEvent (event);
        }
    }

    @Override
    public void logAccess(Request request, Response response, long time, boolean useDefault) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public int getStartStopThreads() {
        return startStopThreads;
    }

    @Override
    public void setStartStopThreads(int startStopThreads) {
        this.startStopThreads = startStopThreads;

        if (startStopExecutor != null) {
            int poolSize = getRealPoolSize ();
            startStopExecutor.setCorePoolSize (poolSize);
            startStopExecutor.setMaximumPoolSize (poolSize);
        }
    }

    //FIXME 见tomcat8
    private int getRealPoolSize() {
        if (getStartStopThreads () <= 0) {
            return 1;
        } else {
            return Math.min (getStartStopThreads (), Runtime.getRuntime ().availableProcessors ());
        }
    }

    @Override
    public File getCatalinaBase() {
        return Optional.ofNullable (getParent ())
                .map (Container::getCatalinaBase)
                .orElse (null);
    }

    @Override
    public File getCatalinaHome() {
        return Optional.ofNullable (getParent ())
                .map (Container::getCatalinaHome)
                .orElse (null);
    }


    @Override
    public void start() throws LifecycleException {
        if (StringUtils.isEmpty (getName ())) {
            throw new IllegalStateException ("每个组件必须设置name");
        }

        if (running) {
            throw new LifecycleException ();
        }
        running = true;

        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).start ();
        }

        startStopChildren (false);
        //后start thread，因为thread用到了children
        startThread ();

        fireLifecycleEvent (EventType.START_EVENT, this);//为了HostConfig
    }

    @Override
    public void stop() throws LifecycleException {
        super.stop ();

        //先stop thread，因为thread用到了children
        stopThread ();
        startStopChildren (true);

        if (startStopExecutor != null) {
            startStopExecutor.shutdown ();
            startStopExecutor = null;
        }

        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).stop ();
        }
    }

    /**
     * 并行处理子组件的start和stop
     * 因为是多线程，所以当某个组件启动失败的时候，父组件不会启动失败
     *
     * @param isStop true代表是stop
     */
    private void startStopChildren(final boolean isStop) {
        ExecutorService executor = getStartStopExecutor ();
        List<Future<?>> jobs = new ArrayList<> ();
        failures.clear ();

        for (Container value : children.values ()) {
            Future<?> future = executor.submit (() -> {
                try {
                    if (isStop) {
                        value.stop ();
                    } else {
                        value.start ();
                    }
                } catch (Throwable e) {
                    log.error ("组件 " + value.getName () + " 启动失败", e);
                    synchronized (failures) {
                        failures.add (value);
                    }
                }
            });
            jobs.add (future);
        }

        for (Future<?> job : jobs) {
            try {
                job.get ();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace ();
            }
        }

        if (Wrapper.class.isAssignableFrom (getClass ())) {
            return;
        }

        String s = isStop ? "关闭" : "启动";

        if (failures.size () == 0) {
            log.info ("{} 的子组件全部{}成功", getName (), s);
        } else {
            StringBuilder sb = new StringBuilder ();
            for (Container failure : failures) {
                sb.append (failure.getName ()).append (',');
            }
            sb.deleteCharAt (sb.length () - 1);

            log.error ("{} 的子组件 {} {}失败", getName (), sb, s);
        }
    }

    /**
     * 注意这个线程池设计
     */
    public ExecutorService getStartStopExecutor() {
        if (startStopExecutor != null) {
            return startStopExecutor;
        }

        int poolSize = getRealPoolSize ();

        startStopExecutor =
                new ThreadPoolExecutor (
                        poolSize,
                        poolSize,
                        10,
                        TimeUnit.SECONDS,
                        new LinkedBlockingDeque<> (),
                        new SimpleThreadFactory ());
        startStopExecutor.allowCoreThreadTimeOut (true);

        return startStopExecutor;
    }

    protected void startThread() {
        verifyRunning ();
        if (thread != null && thread.isAlive ()) {
            throw new IllegalStateException ();
        }
        if (getBackgroundProcessorDelay () <= 0) {
            return;
        }

        stopThread = false;

        //不用future，因为要精确控制setDaemon等
        String threadName = "ProcessorThread[" + this + "]";
        thread = new Thread (new ProcessorThread (this));
        thread.setDaemon (true);
        thread.setName (threadName);
        thread.start ();
    }

    protected void stopThread() {
        verifyStopped ();

        if (thread == null) {
            return;
        }

        stopThread = true;
        thread.interrupt ();
        try {
            thread.join ();
        } catch (InterruptedException e) {
            e.printStackTrace ();
        }
        if (thread.isAlive ()) {
            log.error ("thread {} 停止失败", thread.getName ());
            throw new IllegalStateException ();
        }
        thread = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder ();
        Container parent = getParent ();
        if (parent != null) {
            sb.append (parent);
            sb.append ('.');
        }
        sb.append (this.getClass ().getSimpleName ());
        sb.append ('[');
        sb.append (getName ());
        sb.append (']');
        return sb.toString ();
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        getPipeline ().invoke (request, response);
    }

    private static final class SimpleThreadFactory implements ThreadFactory {
        private final AtomicInteger count = new AtomicInteger (0);

        @Override
        public Thread newThread(Runnable r) {
            //todo thread group
            String name = "startStopThread-" + count.incrementAndGet ();
            Thread thread = new Thread (r, name);
            thread.setDaemon (true);
            return thread;
        }
    }

    /**
     * 线程调用的层级不定，可能是context，可能是engine，所以要保证通用性
     */
    private class ProcessorThread implements Runnable {

        private final Container root;

        public ProcessorThread(Container root) {
            this.root = root;
        }

        @Override
        public void run() {
            if (getBackgroundProcessorDelay () <= 0) {
                log.warn ("Delay<=0, return");
                return;
            }
            log.info ("background task started.");

            while (!stopThread) {
                try {
                    Thread.sleep (getBackgroundProcessorDelay () * 1000L);
                } catch (InterruptedException e) {
                    //可能是被stopThread方法唤醒了
                    log.debug ("background task stopped.", e);
                    return;
                }
                if (stopThread) {
                    log.debug ("background task stopped.");
                    return;
                }

                log.debug ("开始级联处理background task");
                process (root);
                log.debug ("处理background task结束");
            }
        }

        /**
         * 处理某个子容器
         */
        private void process(Container container) {
            ClassLoader origin = null;
            try {
                if (container instanceof Context) {
                    //将类加载器设置为上下文类加载器，万一需要用到的话可以用
                    //origin就是原本的类加载器
                    origin = ((Context) container).bind (null);
                }

                //处理
                container.backgroundProcess ();
                for (Container child : container.findChildren ()) {
                    //!!这里要注意！，如果子节点有delay>0的话，就会在start里给子节点也启动一个thread
                    //所以必须子节点关闭background thread之后，才能级联处理
                    if (child.getBackgroundProcessorDelay () <= 0) {
                        process (child);
                    }
                }

            } catch (Throwable e) {
                log.error ("线程异常", e);
            } finally {
                if (container instanceof Context) {
                    //把类加载器换回来
                    ((Context) container).unbind (origin);
                }
            }
        }
    }
}
