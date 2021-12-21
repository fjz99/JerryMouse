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
@Slf4j
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
     * 只获得过期的key,默认实现为获得所有的key<p>
     * 此方法用于processExpires<p>
     * <b>如果要适用LRU那么就在此方法改动，保证返回的是LRU顺序的数组</b>
     *
     * @return list of session keys, that are to be expired
     * @throws IOException if an input-/output error occurred
     */
    public String[] expiredKeys() throws IOException {
        return keys ();
    }

    /**
     * 处理store的session过期检查<p>
     * 这个方法只会和访问manager的线程冲突，而用户不会swapOut，<p>
     * 所以在这个方法调用的时候，只要在内存中的map查询是否存在session即可
     * todo
     */
    public void processExpires() {
        String[] strings;
        try {
            strings = expiredKeys ();
        } catch (IOException e) {
            e.printStackTrace ();
            log.error ("processExpires中，获得expiredKeys失败");
            return;
        }

        for (String string : strings) {
            //检查是否过期，因为不一定过期(默认实现就是返回所有的key)
            Session session;
            try {
                session = load (string);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace ();
                log.error ("processExpires中，load {} 失败，err {}", strings, e.toString ());
                return;
            }
        }
    }


    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener (listener);
    }

}
