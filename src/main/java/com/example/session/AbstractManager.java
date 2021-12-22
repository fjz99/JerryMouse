package com.example.session;

import com.example.Context;
import com.example.life.Lifecycle;
import com.example.life.LifecycleBase;
import com.example.life.LifecycleException;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @date 2021/12/22 11:25
 * todo auth持久化
 */
@Slf4j
public abstract class AbstractManager
        extends LifecycleBase
        implements Manager {

    protected static final Session[] SESSION_ARRAY = new Session[0];
    protected final PropertyChangeSupport support = new PropertyChangeSupport (this);

    /**
     * 用于id生成
     */
    protected String secureRandomClass = null;
    protected String secureRandomAlgorithm = "SHA1PRNG";
    protected SessionIdGenerator sessionIdGenerator = null;
    protected Class<? extends SessionIdGenerator> sessionIdGeneratorClass = StandardSessionIdGenerator.class;

    protected Context context;

    protected Map<String, Session> sessions = new ConcurrentHashMap<> ();

    /**
     * 每隔几次backgroundProcess才执行expire检查
     */
    protected int processExpiresFrequency = 6;
    /**
     * session多久超时
     */
    protected volatile int sessionMaxAliveTime;

    /*
     * 一些配置，-1表示不限制最大个数
     */
    /**
     * 当前活跃的个数，可以大于maxActiveSessions；
     * 只有add session的时候才能大于maxActiveSessions；
     * 如果是创建session的时候超过maxActiveSessions话，就会抛出异常
     * 一个是当前，一个是实际上允许的
     */
    protected volatile int maxActive = 0;
    /**
     * 最多多少活跃的session
     */
    protected int maxActiveSessions = -1;
    private int count = 0;

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener (listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener (listener);
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void setContext(Context context) {
        if (this.context == context) {
            return;
        }
        if (running) {
            throw new IllegalStateException ();
        }

        Context oldContext = this.context;
        this.context = context;
        support.firePropertyChange ("context", oldContext, this.context);
    }

    public String getSecureRandomClass() {
        return this.secureRandomClass;
    }

    public void setSecureRandomClass(String secureRandomClass) {
        String oldSecureRandomClass = this.secureRandomClass;
        this.secureRandomClass = secureRandomClass;
        support.firePropertyChange ("secureRandomClass", oldSecureRandomClass,
                this.secureRandomClass);
    }

    public String getSecureRandomAlgorithm() {
        return secureRandomAlgorithm;
    }

    public void setSecureRandomAlgorithm(String secureRandomAlgorithm) {
        this.secureRandomAlgorithm = secureRandomAlgorithm;
    }

    public SessionIdGenerator getSessionIdGenerator() {
        if (sessionIdGenerator == null) {
            try {
                sessionIdGenerator = sessionIdGeneratorClass.newInstance ();
            } catch (ReflectiveOperationException e) {
                e.printStackTrace ();
                log.error ("SessionIdGenerator 创建失败，class = {}", sessionIdGeneratorClass.getCanonicalName ());
            }
        }
        return sessionIdGenerator;
    }

    public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
        this.sessionIdGenerator = sessionIdGenerator;
        sessionIdGeneratorClass = sessionIdGenerator.getClass ();
    }

    /**
     * 每隔一定频率检查一次超时，基于内存的超时
     * 因为只有一个线程检查这个，所以线程安全
     */
    @Override
    public void backgroundProcess() {
        count = (count + 1) % processExpiresFrequency;
        if (count == 0) {
            processExpires ();
        }
    }

    /**
     * 检查内存中的session超时
     */
    public void processExpires() {
        Session[] sessions = findSessions ();

        for (Session session : sessions) {
            //如果过期就会自动调用expire
            session.isValid ();
        }
    }

    @Override
    public Session[] findSessions() {
        return sessions.values ().toArray (SESSION_ARRAY);
    }

    @Override
    public Session findSession(String id) throws IOException {
        if (id == null) {
            return null;
        }
        return sessions.get (id);
    }

    @Override
    public Session createSession(String sessionId) {
        Session session = createEmptySession ();
        if (sessionId == null) {
            sessionId = generateSessionId ();
        }
        session.setId (sessionId);//会通知context监听器session创建
        session.setMaxInactiveInterval (getSessionMaxAliveTime ());
        session.setCreationTime (System.currentTimeMillis ());
        session.setValid (true);
        session.setNew (true);
        return session;
    }

    @Override
    public void add(Session session) {
        Objects.requireNonNull (session.getId ());

        sessions.put (session.getId (), session);
        if (getSessionCount () > getMaxActive ()) {
            setMaxActive (getSessionCount ());
        }
    }

    @Override
    public int getMaxActive() {
        return maxActive;
    }

    @Override
    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public int getSessionMaxAliveTime() {
        return sessionMaxAliveTime;
    }

    public void setSessionMaxAliveTime(int sessionMaxAliveTime) {
        this.sessionMaxAliveTime = sessionMaxAliveTime;
    }

    @Override
    public Session createEmptySession() {
        if (getMaxActiveSessions () > 0 &&
                getSessionCount () > getMaxActiveSessions ()) {
            throw new IllegalStateException ("session to many");
        }
        return new StandardSession (this);
    }

    public int getMaxActiveSessions() {
        return maxActiveSessions;
    }

    public void setMaxActiveSessions(int maxActiveSessions) {
        int oldMaxActiveSessions = this.maxActiveSessions;
        this.maxActiveSessions = maxActiveSessions;
        support.firePropertyChange ("maxActiveSessions",
                Integer.valueOf (oldMaxActiveSessions),
                Integer.valueOf (this.maxActiveSessions));
    }

    @Override
    public void remove(Session session) {
        //因为create的时候为id=null，而create需要setId，而setId需要remove。。
        if (session.getId () != null) {
            sessions.remove (session.getId ());
        }
    }

    /**
     * 保证不重复
     */
    protected String generateSessionId() {
        String id;
        do {
            id = getSessionIdGenerator ().generateSessionId ();
        } while (sessions.containsKey (id));
        return id;
    }

    @Override
    public int getSessionCount() {
        return sessions.size ();
    }

    @Override
    public synchronized void start() throws LifecycleException {
        super.start ();

        SessionIdGenerator sessionIdGenerator = getSessionIdGenerator ();
        if (sessionIdGenerator instanceof AbstractSessionIdGenerator) {
            AbstractSessionIdGenerator sig = (AbstractSessionIdGenerator) sessionIdGenerator;
            sig.setSecureRandomAlgorithm (getSecureRandomAlgorithm ());
            sig.setSecureRandomClass (getSecureRandomClass ());
        }

        if (sessionIdGenerator instanceof Lifecycle) {
            ((Lifecycle) sessionIdGenerator).start ();
        }

        // Force initialization of the random number generator
        this.sessionIdGenerator.generateSessionId ();
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        super.stop ();

        if (getSessionIdGenerator () instanceof Lifecycle) {
            ((Lifecycle) getSessionIdGenerator ()).stop ();
        }
    }

    @Override
    public void changeSessionId(Session session) {
        changeSessionId (session, generateSessionId ());
    }

    @Override
    public void changeSessionId(Session session, String newId) {
        String id = session.getIdInternal ();
        session.setId (newId, false);
        session.tellChangedSessionId (newId, id, true, true);
    }
}

