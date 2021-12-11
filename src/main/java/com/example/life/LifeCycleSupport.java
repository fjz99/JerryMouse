package com.example.life;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 可能被多个线程add
 * 事实上，目前的事件比较少，不论是add listener还是触发event都很少
 * 所以怎么同步都行
 *
 * @date 2021/12/9 12:56
 */
public final class LifeCycleSupport {
    private final Lifecycle lifecycle;
    private final List<LifecycleListener> listeners;

    public LifeCycleSupport(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        listeners = new CopyOnWriteArrayList<> ();
    }


    public void addLifecycleListener(LifecycleListener listener) {
        listeners.add (listener);
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        listeners.remove (listener);
    }

    public List<LifecycleListener> getListeners() {
        return Collections.unmodifiableList (new ArrayList<> (listeners));
    }

    public void fireLifecycleEvent(EventType type, Object data) {

        final LifecycleEvent event = new LifecycleEvent (lifecycle, type, data);
        //copy 避免修改
        final List<LifecycleListener> lifecycleListeners = new ArrayList<> (listeners);
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.lifecycleEvent (event);
        }

    }
}
