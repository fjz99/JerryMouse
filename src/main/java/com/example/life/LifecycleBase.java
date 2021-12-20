package com.example.life;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 简化代码逻辑
 * 同时也提供Support类，因为java是单继承
 *
 * @date 2021/12/19 10:20
 */
public abstract class LifecycleBase implements Lifecycle {
    protected final List<LifecycleListener> listeners = new CopyOnWriteArrayList<> ();
    protected volatile boolean running = false;

    public void addLifecycleListener(LifecycleListener listener) {
        listeners.add (listener);
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        listeners.remove (listener);
    }

    //保护性拷贝
    @Override
    public List<LifecycleListener> findLifecycleListeners() {
        return Collections.unmodifiableList (new ArrayList<> (listeners));
    }

    public void fireLifecycleEvent(EventType type, Object data) {

        final LifecycleEvent event = new LifecycleEvent (this, type, data);
        //copy 避免修改
        final List<LifecycleListener> lifecycleListeners = new ArrayList<> (listeners);
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.lifecycleEvent (event);
        }

    }

    @Override
    public synchronized void start() throws LifecycleException {
        if (running) {
            throw new LifecycleException ();
        }
        running = true;
        fireLifecycleEvent (EventType.START_EVENT, null);

    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (!running) {
            throw new LifecycleException ();
        }
        running = false;
        fireLifecycleEvent (EventType.STOP_EVENT, null);
    }

}