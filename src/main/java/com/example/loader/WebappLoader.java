package com.example.loader;

import com.example.Context;
import com.example.life.LifecycleBase;
import com.example.life.Lifecycle;
import com.example.life.LifecycleException;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;

import static com.example.loader.Constants.WEB_INF_LIB_LOCATION;

/**
 * Loader用于维护一个ClassLoader
 * 给classLoader添加repo等
 * 具体reload在context中
 * Loader会和一个context关联
 * <p>
 * todo 线程上下文类加载器
 *
 * @date 2021/12/19 10:13
 */
@Slf4j
public class WebappLoader extends LifecycleBase
        implements Loader, PropertyChangeListener {

    private final ClassLoader parent;
    protected PropertyChangeSupport support = new PropertyChangeSupport (this);
    /**
     * 关联的context
     */
    private Context context;
    /**
     * 所有的classLoader都必须是WebappClassLoader的子类
     */
    private WebappClassLoader classLoader;
    private boolean reloadable = true;
    /**
     * 双亲委派标志位
     */
    private boolean delegate = false;
    private String loaderClass = WebappClassLoader.class.getName ();

    public WebappLoader() {
        this (null);
    }

    public WebappLoader(ClassLoader parent) {
        this.parent = parent;
    }

    @Override
    public synchronized void start() throws LifecycleException {
        super.start ();

        Objects.requireNonNull (context.getResources ());

        try {
            classLoader = createClassLoader ();
            classLoader.setDelegate (delegate);
            classLoader.setResourceContext (context.getResources ());
            classLoader.setJarPath (WEB_INF_LIB_LOCATION);

            classLoader.start ();
        } catch (LifecycleException e) {
            e.printStackTrace ();
            log.error ("classLoader start失败");
            throw new LifecycleException (e);
        } catch (Exception e) {
            e.printStackTrace ();
            log.error ("classLoader创建失败");
            throw new LifecycleException (e);
        }
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        super.stop ();

        if (classLoader != null)
            ((Lifecycle) classLoader).stop ();//这样可以复用classLoader
        classLoader = null;
    }

    public String getLoaderClass() {
        return loaderClass;
    }

    public void setLoaderClass(String loaderClass) {
        this.loaderClass = loaderClass;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void setContext(Context context) {
        Context old = this.context;
        this.context = context;

        support.firePropertyChange ("context", old, context);

        if (this.context != null) {
            setReloadable (context.getReloadable ());
            context.addPropertyChangeListener (this);
        }
        if (old != null) {
            old.removePropertyChangeListener (this);
        }
    }


    @Override
    public boolean getDelegate() {
        return delegate;
    }

    @Override
    public void setDelegate(boolean delegate) {
        boolean old = this.delegate;
        this.delegate = delegate;

        support.firePropertyChange ("delegate", old, delegate);
    }

    @Override
    public boolean getReloadable() {
        return reloadable;
    }

    @Override
    public void setReloadable(boolean reloadable) {
        boolean old = this.reloadable;
        this.reloadable = reloadable;

        support.firePropertyChange ("reloadable", old, reloadable);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener (listener);
    }

    @Override
    public void addRepository(String repository) {
        ((Reloader) classLoader).addRepository (repository);
    }

    @Override
    public List<String> findRepositories() {
        return ((Reloader) classLoader).findRepositories ();
    }

    @Override
    public boolean modified() {
        return ((Reloader) classLoader).modified ();
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener (listener);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 检查是否需要修改，如果需要就
     */
    @Override
    public void backgroundProcess() {
        if (reloadable && classLoader.modified ()) {
            context.reload ();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder ("WebappLoader[");
        if (context != null)
            sb.append (context.getName ());
        sb.append ("]");
        return (sb.toString ());
    }

    /**
     * 监听context的reload属性变化
     * 这样就不用显式地修改reload了
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (!(event.getSource () instanceof Context))
            return;

        if (event.getPropertyName ().equals ("reloadable")) {
            try {
                setReloadable ((Boolean) event.getNewValue ());
            } catch (NumberFormatException e) {
                e.printStackTrace ();
            }
        }
    }

    private WebappClassLoader createClassLoader() throws Exception {
        Class<?> aClass = Class.forName (loaderClass);

        if (parent == null) {
            return (WebappClassLoader) aClass.newInstance ();
        } else {
            Constructor<?> constructor = aClass.getConstructor (ClassLoader.class);
            return (WebappClassLoader) constructor.newInstance (parent);
        }
    }
}
