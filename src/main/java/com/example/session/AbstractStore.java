package com.example.session;

import com.example.life.LifecycleBase;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

/**
 * @date 2021/12/20 21:09
 */
@ToString
public abstract class AbstractStore
        extends LifecycleBase
        implements Store {

    /**
     * used for logging.
     */
    protected static final String storeName = "AbstractStore";

    protected final PropertyChangeSupport support = new PropertyChangeSupport (this);
    protected Manager manager;

    @Override
    public Manager getManager() {
        return manager;
    }

    @Override
    public void setManager(Manager manager) {
        Manager oldManager = this.manager;
        this.manager = manager;
        support.firePropertyChange ("manager", oldManager, this.manager);
    }

    public String getStoreName() {
        return storeName;
    }

    @Override
    public int getSize() throws IOException {
        return 0;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    /**
     * Get only those keys of sessions, that are saved in the Store and are to
     * be expired.
     * <p>
     * 只获得过期的key,默认实现为获得所有的key
     * 此方法用于processExpires
     *
     * @return list of session keys, that are to be expired
     * @throws IOException if an input-/output error occurred
     */
    public String[] expiredKeys() throws IOException {
        return keys ();
    }

    /**
     * 处理store的session过期检查
     * <p>
     * 这个方法只会和访问manager的线程冲突，而用户不会swapOut，
     * 所以在这个方法调用的时候，只要在内存中的map查询是否存在session即可
     */
    public void processExpires() {

    }


    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener (listener);
    }

}
