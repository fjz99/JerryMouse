package com.example.session;

import com.example.life.Lifecycle;
import com.example.life.LifecycleException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 增加了真正的持久化:内存与磁盘的换入换出功能
 * 换入时使用换入锁
 * 换出时使用session sync
 *
 * @date 2021/12/22 11:25
 */
@Slf4j
public class PersistentManager
        extends AbstractManager
        implements StoreManager {

    protected static final String NAME = "PersistentManager";
    /**
     * 换入锁,private防止被子类破坏线程安全性
     */
    private final Map<String, Object> sessionSwapInLocks = new HashMap<> ();
    protected Store store;

    /**
     * 即是否在start在stop时使用load和unload方法
     */
    protected boolean saveOnRestart = true;
    /**
     * 多长时间不活跃才能备份，
     * -1表示根本不会备份
     */
    protected int maxIdleBackup = -1;
    /**
     * 用于将session总数降低到小于maxActiveSessions。
     * 这个指的是将还在活跃active的session换出，因为总数太多了。
     * -1表示根本不会换出
     */
    protected int minIdleSwap = -1;
    /**
     * 用于将session总数降低到小于maxActiveSessions。
     * 这个指的是将不活跃inactive的session换出，因为太长时间不活跃。
     * -1表示根本不会换出
     */
    protected int maxIdleSwap = -1;

    private int count = 0;

    /**
     * 查看是否加载到内存
     */
    public boolean isLoaded(String id) {
        try {
            if (super.findSession (id) != null) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace ();
            log.error ("isLoaded ERR {}", e.toString ());
        }
        return false;
    }

    /**
     * 须加锁
     * 因为可能查找的同时正在换出
     * 正在换入的话，swapIn内部有锁，所以是安全的
     */
    @Override
    public Session findSession(String id) throws IOException {
        Session session = super.findSession (id);//先在内存中找

        if (session != null) {
            synchronized (session) {
                //加锁，因为可能正在换出
                session = super.findSession (id);
            }
        }

        //换入
        if (session == null) {
            //换入内部加锁保证一致性
            session = swapIn (id);
        }

        return session;
    }

    /**
     * 可能被多线程访问，比如findSession的时候，如果发现内存没有session
     * 那就会swapIn
     */
    protected Session swapIn(String id) throws IOException {
        if (id == null || getStore () == null) {
            return null;
        }

        //这里有很多原因，首先，为什么要移除换出锁，假设有3个线程，第一个进入临界区，第二个阻塞在锁上，
        //然后第一个做完了，此时第三个线程到达了
        //假设不remove 锁，那就第2 3 线程要互斥，如果remove，那2 3线程就不互斥。
        //！！但是这种问题不会发生，因为get是concurrentHashMap，是线程安全的，只要有一个完成swapIn了，那就绝对没问题了
        //我理解他这么做是为了防止内存泄漏
        Object lock;
        synchronized (this) {
            sessionSwapInLocks.computeIfAbsent (id, k -> new Object ());
            lock = sessionSwapInLocks.get (id);
        }

        Session session;
        synchronized (lock) {
            session = sessions.get (id);
            if (session != null) {
                //已经换进来了
                return session;
            }

            try {
                session = store.load (id);
            } catch (ClassNotFoundException e) {
                e.printStackTrace ();
                log.error ("store.load err {}", e.toString ());
            }

            if (session != null) {
                //检查是否已经过期了,isValid内部会expire
                if (!session.isValid ()) {
                    store.remove (id);
                } else {
                    session.setManager (this);
                    ((StandardSession) session).tellNew ();//????源码是这样的
                    ((StandardSession) session).activate ();
                    add (session);
                    session.access ();//????源码是这样的
                    session.endAccess ();//????源码是这样的
                }
            }
        }

        synchronized (this) {
            sessionSwapInLocks.remove (id);
        }

        return session;
    }

    /**
     * 换出，而且remove了
     */
    /*
     * swapOut有风险，因为findSessions返回的是快照，可能在swapOut的时候已经remove了一个session
     * 这就导致把删除的会话换到外存了;
     * 这个是安全的，因为remove只会在session.expire中被调用，而那个方法中，必须加synchronized(this)
     */
    protected void swapOut(Session session) throws IOException {
        //判断isValid，防止失效的session被换出
        //例如，在判断换出的方法里，先get了一个array，但是array可能被并发修改
        //array中的session可能已经被expire了，从而导致被remove
        //但是因为expire加锁了，所以只要swapout加锁，再判断isValid，那就可以保证是有效的
        if (getStore () == null) {
            return;
        }
        synchronized (session) {
            if (!session.isValid ()) {
                return;
            }

            store.save (session);
            ((StandardSession) session).passivate ();
            super.remove (session);
            session.recycle ();//因为可能实现了对象池，还可以顺便告知expire方法：当前session已经换出了
        }
    }

    /**
     * 清除所有store的数据
     */
    public void clearStore() {
        if (getStore () == null) {
            throw new IllegalStateException ();
        }

        try {
            store.clear ();
        } catch (IOException e) {
            e.printStackTrace ();
            log.error ("clearStore err {}", e.toString ());
        }
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
        verifyRunning ();
        if (store == null) {
            return;
        }

        sessions.clear ();

        for (String key : store.keys ()) {
            swapIn (key);
        }
    }

    @Override
    public void unload() throws IOException {
        verifyStopped ();
        if (store == null) {
            return;
        }

        for (Session session : findSessions ()) {
            swapOut (session);
        }

        super.unload ();
    }


    /**
     * 换出，并且保证不超过最大值的90%
     */
    protected void processMaxActiveSwaps() {
        verifyRunning ();
        if (getMinIdleSwap () <= 0 || getStore () == null) {
            return;
        }

        //TODO LRU

        Session[] sessions = findSessions ();
        int n = sessions.length - (int) (getMaxActiveSessions () * 0.9);//!

        for (Session session : sessions) {
            if (n <= 0) return;

            long gap = (System.currentTimeMillis () - session.getThisAccessedTime ()) / 1000L;
            if (gap >= getMinIdleSwap ()) {
                //换出
                try {
                    String id = session.getId ();
                    swapOut (session);
                    log.debug ("processMaxActiveSwaps 换出session {}", id);
                    n--;
                } catch (IOException e) {
                    e.printStackTrace ();
                    log.error ("swapOut failed in processMaxActiveSwaps,{}", e.toString ());
                }
            }
        }
    }

    /**
     * 备份
     */
    protected void processMaxIdleBackups() {
        verifyRunning ();
        if (getMaxIdleBackup () <= 0 || getStore () == null) {
            return;
        }

        for (Session session : findSessions ()) {
            long gap = (System.currentTimeMillis () - session.getThisAccessedTime ()) / 1000L;
            if (gap >= getMaxIdleBackup ()) {
                //备份
                try {
                    store.save (session);
                    log.debug ("processMaxIdleBackups 备份session {}", session.getId ());
                } catch (IOException e) {
                    e.printStackTrace ();
                    log.error ("save failed in processMaxIdleBackups,{}", e.toString ());
                }
            }
        }
    }


    protected void processMaxIdleSwaps() {
        verifyRunning ();
        if (getMaxIdleSwap () <= 0 || getStore () == null) {
            return;
        }

        for (Session session : findSessions ()) {
            long gap = (System.currentTimeMillis () - session.getThisAccessedTime ()) / 1000L;
            if (gap >= getMaxIdleSwap ()) {
                //换出
                try {
                    String id = session.getId ();
                    swapOut (session);
                    log.debug ("processMaxIdleSwaps 换出session {}", id);
                } catch (IOException e) {
                    e.printStackTrace ();
                    log.error ("swapOut failed in processMaxIdleSwaps,{}", e.toString ());
                }
            }
        }
    }

    public void checkSwapAndBackup() {
        processMaxIdleSwaps ();
        processMaxActiveSwaps ();
        processMaxIdleBackups ();
    }

    @Override
    public void remove(Session session) {
        super.remove (session);
        if (getStore () == null || session.getId () == null) {
            return;
        }

        try {
            store.remove (session.getIdInternal ());
        } catch (IOException ignored) {

        }
    }

    @Override
    public void removeSuper(Session session) {
        super.remove (session);
    }

    @Override
    public synchronized void start() throws LifecycleException {
        super.start ();

        if (getStore () instanceof Lifecycle) {
            ((Lifecycle) getStore ()).start ();
        }

        warnConfiguration ();

        if (getStore () != null && saveOnRestart) {
            try {
                load ();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace ();
                log.error ("start:load err {}", e.toString ());
            }
        }
        log.debug ("{} started", NAME);
    }

    private void warnConfiguration() {
        if (getStore () == null) {
            log.warn ("没有配置store，持久化功能将失效");
        }
        if (getMaxIdleBackup () <= 0) {
            log.warn ("备份功能未开启");
        }
        if (getMaxIdleSwap () <= 0) {
            log.warn ("空闲会话换出功能未开启");
        }
        if (getMinIdleSwap () <= 0) {
            log.warn ("会话超量换出功能未开启");
        }
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        super.stop ();

        //store可能是null，这样就会自动忽略
        if (getStore () != null && saveOnRestart) {
            try {
                unload ();
            } catch (IOException e) {
                e.printStackTrace ();
                log.error ("stop:unload ERR {}", e.toString ());
            }
        }

        if (getStore () instanceof Lifecycle) {
            ((Lifecycle) getStore ()).stop ();
        }

        log.debug ("{} stopped", NAME);
    }

    /**
     * 父类的backgroundProcess会调用这个的
     */
    @Override
    public void processExpires() {
        log.debug ("{} 开始一轮 processExpires", NAME);
        super.processExpires ();
        checkSwapAndBackup ();
        if (getStore () instanceof AbstractStore) {
            ((AbstractStore) getStore ()).processExpires ();
        }
        log.debug ("{} 完成一轮 processExpires", NAME);
    }

    public int getMaxIdleBackup() {
        return maxIdleBackup;
    }

    public void setMaxIdleBackup(int maxIdleBackup) {
        int oldBackup = this.maxIdleBackup;
        this.maxIdleBackup = maxIdleBackup;
        support.firePropertyChange ("maxIdleBackup",
                Integer.valueOf (oldBackup),
                Integer.valueOf (this.maxIdleBackup));
    }

    public int getMaxIdleSwap() {
        return maxIdleSwap;
    }

    public void setMaxIdleSwap(int maxIdleSwap) {
        int oldMaxIdleSwap = this.maxIdleSwap;
        this.maxIdleSwap = maxIdleSwap;
        support.firePropertyChange ("maxIdleSwap",
                Integer.valueOf (oldMaxIdleSwap),
                Integer.valueOf (this.maxIdleSwap));
    }

    public int getMinIdleSwap() {
        return minIdleSwap;
    }

    public void setMinIdleSwap(int minIdleSwap) {
        int oldMinIdleSwap = this.minIdleSwap;
        this.minIdleSwap = minIdleSwap;
        support.firePropertyChange ("minIdleSwap",
                Integer.valueOf (oldMinIdleSwap),
                Integer.valueOf (this.minIdleSwap));
    }

    @Override
    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
        this.store.setManager (this);
    }

    public boolean isSaveOnRestart() {
        return saveOnRestart;
    }

    public void setSaveOnRestart(boolean saveOnRestart) {
        boolean oldSaveOnRestart = this.saveOnRestart;
        this.saveOnRestart = saveOnRestart;
        support.firePropertyChange ("saveOnRestart",
                Boolean.valueOf (oldSaveOnRestart),
                Boolean.valueOf (this.saveOnRestart));
    }
}
