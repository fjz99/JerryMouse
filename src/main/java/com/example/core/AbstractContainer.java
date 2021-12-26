package com.example.core;

import com.example.*;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.life.Lifecycle;
import com.example.life.LifecycleBase;
import com.example.life.LifecycleException;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @date 2021/12/23 19:54
 */
@Slf4j
public abstract class AbstractContainer extends LifecycleBase implements Container {

    protected final PropertyChangeSupport support = new PropertyChangeSupport (this);
    /**
     * name，唯一id，用在children上
     */
    protected String name = "AbstractContainer";
    protected Pipeline pipeline = new StandardPipeline (this);
    protected Map<String, Container> children = new ConcurrentHashMap<> ();
    protected Container parent;
    protected ClassLoader parentClassLoader;
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
    private volatile int backgroundProcessorDelay = -1;
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
     * 命名有问题，本质就是获得classLoader而已
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
    }

    @Override
    public void addChild(Container child) {
        if (children.containsKey (child.getName ())) {
            throw new IllegalArgumentException ();
        }

        child.setParent (this);
        children.put (child.getName (), child);

        if (startChildren) {
            try {
                child.start ();
            } catch (LifecycleException e) {
                e.printStackTrace ();
            }
        }
        fireContainerEvent (ADD_CHILD_EVENT, child);
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
            } catch (LifecycleException e) {
                e.printStackTrace ();
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
        super.start ();

        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).start ();
        }

        startStopChildren (false);
        //后start thread，因为thread用到了children
        startThread ();
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
     *
     * @param isStop true代表是stop
     */
    private void startStopChildren(boolean isStop) {
        ExecutorService executor = getExecutor ();
        List<Future<?>> jobs = new ArrayList<> ();
        for (Container value : children.values ()) {
            Future<?> future = executor.submit (() -> {
                try {
                    if (isStop) {
                        value.stop ();
                    } else {
                        value.start ();
                    }
                } catch (LifecycleException e) {
                    log.error ("start " + value.getName () + " 失败", e);
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
    }

    /**
     * 注意这个线程池设计
     */
    private ExecutorService getExecutor() {
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

        private String threadName() {
            return Thread.currentThread ().getName ();
        }

        @Override
        public void run() {
            if (getBackgroundProcessorDelay () <= 0) {
                log.warn (threadName () + ":Delay<=0, return");
                return;
            }
            log.debug (threadName () + ":background task started.");

            while (!stopThread) {
                try {
                    Thread.sleep (getBackgroundProcessorDelay () * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace ();
                    //可能是被stopThread方法唤醒了
                    log.debug (threadName () + ":background task stopped.");
                    return;
                }
                if (stopThread) {
                    log.debug (threadName () + ":background task stopped.");
                    return;
                }

                log.debug (threadName () + ":开始级联处理background task");
                process (root);
                log.debug (threadName () + ":处理background task结束");
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
                e.printStackTrace ();
                log.error (threadName () + ":线程异常", e);
            } finally {
                if (container instanceof Context) {
                    //把类加载器换回来
                    ((Context) container).unbind (origin);
                }
            }
        }
    }
}
