package com.example.session;

import com.example.Context;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletContext;
import javax.servlet.http.*;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.lang.reflect.Method;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简化版本的tomcat8 session<p>
 * 主要功能就是序列化机制、expire方法（包括sync同步）、Facade模式、
 * CrawlerSessionManagerValve（防止网络爬虫创建过多的session，鉴别爬虫的依据是user-agent）<p>
 * 复杂的地方在于各种listener，有内部使用的，也有servlet标准的<p>
 * Principal暂时没有实现
 * todo containerCall back
 *
 * @date 2021/12/21 16:06
 */
@Slf4j
public class StandardSession implements Session, HttpSession, Serializable {
    protected static final boolean ACTIVITY_CHECK;

    /**
     * 缓存，用于toArray方法
     */
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];
    protected static final SessionListener[] EMPTY_SESSION_LISTENER_ARRAY = new SessionListener[0];

    static {
        String activityCheck = System.getProperty (
                "org.apache.catalina.session.StandardSession.ACTIVITY_CHECK");
        if (activityCheck == null) {
            ACTIVITY_CHECK = true;
        } else {
            ACTIVITY_CHECK = Boolean.parseBoolean (activityCheck);
        }
    }

    /**
     * 没必要序列化，须反序列化后重新set
     */
    protected transient PropertyChangeSupport support =
            new PropertyChangeSupport (this);
    /**
     * 这个是内部使用的listener
     */
    protected transient List<SessionListener> listeners = new ArrayList<> ();
    /**
     * session存储的内容
     */
    protected transient Map<String, Object> attributes = new ConcurrentHashMap<> ();
    /**
     * The authentication type used to authenticate our cached Principal,
     * if any.  NOTE:  This value is not included in the serialized
     * version of this object.
     */
    protected transient String authType = null;
    /**
     * The time this session was created, in milliseconds since midnight,
     * January 1, 1970 GMT.
     */
    protected long creationTime = 0L;
    /**
     * We are currently processing a session expiration, so bypass
     * certain IllegalStateException tests.  NOTE:  This value is not
     * included in the serialized version of this object.
     */
    protected transient volatile boolean expiring = false;
    /**
     * 外观类
     */
    protected transient StandardSessionFacade facade = null;
    /**
     * The session identifier of this Session.
     */
    protected String id = null;
    /**
     * The last accessed time for this Session.
     */
    protected volatile long lastAccessedTime = creationTime;
    /**
     * The Manager with which this Session is associated.
     */
    protected transient Manager manager = null;
    /**
     * session超时时间，单位秒<p>
     * 如果是负值，则代表永不超时
     */
    protected volatile int maxInactiveInterval = -1;
    /**
     * Flag indicating whether this session is new or not.
     * 代表这是不是刚创建的，即因为没有session所以创建的，而不是复用的
     */
    protected volatile boolean isNew = false;
    /**
     * Flag indicating whether this session is valid or not.
     * 表示session是否过期（有效）
     */
    protected volatile boolean isValid = false;
    /**
     * Internal notes associated with this session by Catalina components
     * and event listeners.  <b>IMPLEMENTATION NOTE:</b> This object is
     * <em>not</em> saved and restored across session serializations!
     */
    protected transient Map<String, Object> notes = new ConcurrentHashMap<> ();
    /**
     * The authenticated Principal associated with this session, if any.
     * <b>IMPLEMENTATION NOTE:</b>  This object is <i>not</i> saved and
     * restored across session serializations!
     * 用于登录功能，暂时不用
     */
    protected transient Principal principal = null;
    /**
     * The current accessed time for this session.
     * 即保存2次访问时间，这次的和上次的
     */
    protected volatile long thisAccessedTime = creationTime;
    /**
     * 主要是保底测试用，因为不确定是否线程安全
     */
    protected transient AtomicInteger accessCount = null;

    public StandardSession(Manager manager) {
        super ();
        this.manager = manager;
    }

    @Override
    public String getAuthType() {
        return authType;
    }

    @Override
    public void setAuthType(String authType) {
        String oldAuthType = this.authType;
        this.authType = authType;
        support.firePropertyChange ("authType", oldAuthType, this.authType);
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public void setCreationTime(long time) {
        lastAccessedTime = time;
        creationTime = time;
        thisAccessedTime = time;
    }

    @Override
    public long getCreationTimeInternal() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        setId (id, true);
    }

    /**
     * internal和不是internal的区别在于：是否校验isvalidInternal
     * isvalid让用户校验
     */
    @Override
    public String getIdInternal() {
        return id;
    }

    /**
     * 删除旧的，然后添加新的<p>
     * 事实上，manager创建session之后id=null,
     * 所以manager也是通过setId来设置id的,
     * 所以setId对应的就是createSession<p>
     * <b>为什么不在构造器中？因为要recycle！<b/><p>
     * 创建时的setId会通知监听器，但是改动id的setId不会通知监听器
     */
    @Override
    public void setId(String id, boolean notify) {
        if (id == null || getManager () == null || id.equals (getId ())) {
            return;
        }

        manager.remove (this);

        this.id = id;
        manager.add (this);

        if (notify) {
            tellNew ();
        }
    }

    /**
     * 用于{@link HttpSessionListener}，通知session创建
     */
    public void tellNew() {
        fireSessionEvent (Session.SESSION_CREATED_EVENT, null);

        Object[] listeners = manager.getContext ().getApplicationLifecycleListeners ();
        if (validListeners (listeners)) {
            HttpSessionEvent event = new HttpSessionEvent (getSession ());//getSession()获得Facade
            for (Object listener : listeners) {
                if (listener instanceof HttpSessionListener) {
                    callback (() -> {
                        ((HttpSessionListener) listener).sessionCreated (event);
                    }, "tellNew");
                }
            }
        }

    }

    /**
     * 这个是内部使用的listener，所以传递的是Session对象
     */
    public void fireSessionEvent(String type, Object data) {
        if (listeners.size () == 0) {
            return;
        }

        SessionListener[] sessionListeners;
        SessionEvent event = new SessionEvent (this, type, data);
        //获得一个副本
        synchronized (listeners) {
            sessionListeners = listeners.toArray (EMPTY_SESSION_LISTENER_ARRAY);
        }

        for (SessionListener sessionListener : sessionListeners) {
            sessionListener.sessionEvent (event);
        }
    }

    @Override
    public long getThisAccessedTime() {
        if (!isValidInternal ()) {
            throw new IllegalStateException ("not valid");
        }

        return thisAccessedTime;
    }

    @Override
    public long getThisAccessedTimeInternal() {
        return thisAccessedTime;
    }

    @Override
    public long getLastAccessedTime() {
        if (!isValidInternal ()) {
            throw new IllegalStateException ("not valid");
        }

        return lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        if (manager == null) {
            return null;
        }
        Context context = manager.getContext ();
        return context.getServletContext ();
    }

    @Override
    public long getLastAccessedTimeInternal() {
        return lastAccessedTime;
    }

    @Override
    public long getIdleTime() {
        if (!isValidInternal ()) {
            throw new IllegalStateException ("not valid");
        }

        return getIdleTimeInternal ();
    }

    /**
     * internal和不是internal的区别在于：是否校验isvalidInternal
     * isvalid让用户校验
     */
    @Override
    public long getIdleTimeInternal() {
        long timeNow = System.currentTimeMillis ();
        return timeNow - getThisAccessedTimeInternal ();
    }

    @Override
    public Manager getManager() {
        return manager;
    }

    @Override
    public void setManager(Manager manager) {
        this.manager = manager;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        throw new AssertionError ();
    }

    @Override
    public Object getAttribute(String name) {
        if (!isValidInternal ()) {
            throw new IllegalStateException ("not valid");
        }

        return attributes.get (name);
    }

    @Override
    public Object getValue(String name) {
        return getAttribute (name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        if (!isValidInternal ()) {
            throw new IllegalStateException ("not valid");
        }

        return Collections.enumeration (attributes.keySet ());
    }

    @Override
    public String[] getValueNames() {
        if (!isValidInternal ()) {
            throw new IllegalStateException ("not valid");
        }

        return attributes.keySet ().toArray (EMPTY_STRING_ARRAY);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (!isValidInternal ()) {
            throw new IllegalStateException ("not valid");
        }

        if (name == null) {
            return;
        }

        if (value == null) {
            removeAttributeInternal (name, true);
        }

        Object o = attributes.get (name);
        if (o instanceof HttpSessionBindingListener) {
            HttpSessionBindingEvent event = new HttpSessionBindingEvent (getSession (), name, o);
            callback (() -> {
                ((HttpSessionBindingListener) o).valueUnbound (event);
            }, "setAttribute:valueUnbound");
        }

        attributes.put (name, value);
        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingEvent event = new HttpSessionBindingEvent (getSession (), name, value);
            callback (() -> {
                ((HttpSessionBindingListener) value).valueBound (event);
            }, "setAttribute:valueBound");
        }

        Object[] listeners = getManager ().getContext ().getApplicationEventListeners ();
        if (validListeners (listeners)) {
            for (Object listener : listeners) {
                if (listener instanceof HttpSessionAttributeListener) {
                    HttpSessionBindingEvent event = new HttpSessionBindingEvent (getSession (), name);
                    callback (() -> {
                        if (o == null) {
                            ((HttpSessionAttributeListener) listener).attributeAdded (event);
                        } else {
                            ((HttpSessionAttributeListener) listener).attributeReplaced (event);
                        }
                    }, "setAttribute:attributeReplaced");
                }
            }
        }
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute (name, value);
    }

    @Override
    public void removeAttribute(String name) {
        if (!isValidInternal ()) {
            throw new IllegalStateException ("not valid");
        }

        removeAttributeInternal (name, true);
    }

    @Override
    public void removeValue(String name) {
        removeAttribute (name);
    }

    @Override
    public void invalidate() {
        expire ();
    }

    @Override
    public boolean isNew() {
        if (!isValidInternal ()) {
            throw new IllegalStateException ("not valid");
        }

        return isNew;
    }

    @Override
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    /**
     * 给内部用的get方法不需要加isValid验证
     */
    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public void setPrincipal(Principal principal) {
        Principal oldPrincipal = this.principal;
        this.principal = principal;
        support.firePropertyChange ("principal", oldPrincipal, this.principal);
    }

    @Override
    public HttpSession getSession() {
        if (facade == null) {
            facade = new StandardSessionFacade (this);
        }
        return facade;
    }

    /**
     * 给用户使用，真的检验是否valid
     */
    @Override
    public boolean isValid() {
        if (!isValid) {
            return false;
        }

        if (maxInactiveInterval > 0) {
            int timeIdle = (int) (getIdleTimeInternal () / 1000L);
            if (timeIdle >= maxInactiveInterval) {
                expire (true);
            }
        }

        return isValid;
    }

    @Override
    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    /**
     * 其实主要逻辑就是通知回调而已
     * 使当前session过期，会通知回调
     */
    public void expire(boolean notify) {
        if (!isValid) {
            return;
        }

        /*
            配合swapIn，swapOut使用，所以要sync
         */
        synchronized (this) {
            //可能已经被swapOut了
            if (!isValid) {
                return;
            }

            if (manager == null) {
                return;
            }

            log.trace ("expire session {}", getIdInternal ());
            Object[] listeners = getManager ().getContext ().getApplicationLifecycleListeners ();
            if (validListeners (listeners)) {
                for (Object listener : listeners) {
                    if (listener instanceof HttpSessionListener) {
                        HttpSessionEvent event = new HttpSessionEvent (getSession ());
                        callback (() -> {
                            ((HttpSessionListener) listener).sessionDestroyed (event);
                        }, "expire:sessionDestroyed");
                    }
                }
            }

            if (notify) {
                fireSessionEvent (SESSION_DESTROYED_EVENT, null);
            }

            setValid (false);
            manager.remove (this);

            //还需要unbound回调。。
            for (String key : keys ()) {
                removeAttributeInternal (key, notify);
            }

            recycle ();//??
        }
    }

    /**
     * 用于session swapOut时的回调通知
     */
    public void passivate() {
        fireSessionEvent (SESSION_PASSIVATED_EVENT, null);

        HttpSessionEvent event = new HttpSessionEvent (getSession ());
        for (String key : keys ()) {
            Object o = attributes.get (key);
            if (o instanceof HttpSessionActivationListener) {
                callback (() -> {
                    ((HttpSessionActivationListener) o).sessionWillPassivate (event);
                }, "passivate:sessionWillPassivate");
            }
        }
    }

    /**
     * session换入后的激活
     * 会被外部manager调用，以激活session
     */
    public void activate() {
        fireSessionEvent (SESSION_ACTIVATED_EVENT, null);

        HttpSessionEvent event = new HttpSessionEvent (getSession ());
        for (String key : keys ()) {
            Object o = attributes.get (key);
            if (o instanceof HttpSessionActivationListener) {
                callback (() -> {
                    ((HttpSessionActivationListener) o).sessionDidActivate (event);
                }, "passivate:sessionDidActivate");
            }
        }
    }

    protected String[] keys() {
        return attributes.keySet ().toArray (EMPTY_STRING_ARRAY);
    }

    /**
     * remove并且根据情况决定是否执行回调
     * 关键是回调
     * 如果没有key，那就不会通知
     */
    protected void removeAttributeInternal(String name, boolean notify) {
        if (name == null) {
            return;
        }

        Object value = attributes.remove (name);

        if (!notify || value == null) {
            return;
        }

        //value的回调
        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingEvent httpSessionBindingEvent = new HttpSessionBindingEvent (getSession (), name, value);
            callback (() -> {
                ((HttpSessionBindingListener) value).valueUnbound (httpSessionBindingEvent);
            }, "removeAttributeInternal:valueUnbound");
        }

        //attributes减少的回调
        Object[] listeners = getManager ().getContext ().getApplicationEventListeners ();
        if (validListeners (listeners)) {
            for (Object listener : listeners) {
                if (listener instanceof HttpSessionAttributeListener) {
                    HttpSessionBindingEvent event = new HttpSessionBindingEvent (getSession (), name, value);
                    callback (() -> {
                        ((HttpSessionAttributeListener) listener).attributeRemoved (event);
                    }, "removeAttributeInternal:attributeRemoved");
                }
            }
        }
    }

    /**
     * 内部使用的，其实就是返回isValid而已，和isValid()相比，不会检验超时
     */
    public boolean isValidInternal() {
        return isValid;
    }

    /**
     * 先调用access，在给用户的servlet
     */
    @Override
    public void access() {
        thisAccessedTime = System.currentTimeMillis ();
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        synchronized (listeners) {
            listeners.add (listener);
        }
    }

    /**
     * 用户执行完之后，再执行这个endAccess
     */
    @Override
    public void endAccess() {
        isNew = false;

        thisAccessedTime = System.currentTimeMillis ();
    }

    @Override
    public void expire() {
        expire (true);
    }

    @Override
    public Object getNote(String name) {
        return notes.get (name);
    }

    @Override
    public Iterator<String> getNoteNames() {
        return notes.keySet ().iterator ();
    }

    @Override
    public void recycle() {
        attributes.clear ();
        setAuthType (null);
        creationTime = 0L;
        expiring = false;
        id = null;
        lastAccessedTime = 0L;
        maxInactiveInterval = -1;
        notes.clear ();
        setPrincipal (null);
        isNew = false;
        isValid = false;
        manager = null;
    }

    @Override
    public void removeNote(String name) {
        notes.remove (name);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        synchronized (listeners) {
            listeners.remove (listener);
        }
    }

    @Override
    public void setNote(String name, Object value) {
        notes.put (name, value);
    }

    /**
     * 通知回调
     * todo notifyContainerListeners
     */
    @Override
    public void tellChangedSessionId(String newId, String oldId, boolean notifySessionListeners, boolean notifyContainerListeners) {
        if (notifySessionListeners) {
            Object[] listeners = getManager ().getContext ().getApplicationEventListeners ();
            if (validListeners (listeners)) {
                for (Object listener : listeners) {
                    if (listener instanceof HttpSessionIdListener) {
                        HttpSessionEvent event = new HttpSessionEvent (getSession ());

                        callback (() -> {
                            ((HttpSessionIdListener) listener).sessionIdChanged (event, oldId);
                        }, "tellChangedSessionId");
                    }
                }
            }
        }
    }

    /**
     * 因为是回调，所以可能有异常,确保安全
     */
    private void callback(Runnable runnable, String msg) {
        try {
            //因为是回调，所以可能有异常
            runnable.run ();
        } catch (Throwable e) {
            e.printStackTrace ();
            log.error ("在执行 {} 回调时，发生异常 {}", msg, e.toString ());
        }
    }

    /**
     * 内部使用的回调通知方法 <p>
     * 此方法只根据方法名区分方法，不会考虑签名
     *
     * @param callbackType 回调类类型
     * @param methodName   回调方法名
     * @param isLifecycle  true 代表是false代表是getApplicationLifecycleListeners，
     *                     false代表是getApplicationEventListeners
     * @throws ReflectiveOperationException 表示callbackType中没有methodName，或者有多个methodName
     * @deprecated 不适合使用反射，因为event type也不同
     */
    @Deprecated
    protected void notifyServletListeners(Class<?> callbackType, String methodName, boolean isLifecycle)
            throws ReflectiveOperationException {
        Object[] listeners;
        if (isLifecycle) {
            listeners = getManager ().getContext ().getApplicationLifecycleListeners ();
        } else {
            listeners = getManager ().getContext ().getApplicationEventListeners ();
        }
        if (!(listeners != null && listeners.length > 0)) {
            return;
        }

        //获得方法
        Method method = null;
        for (Method declaredMethod : callbackType.getDeclaredMethods ()) {
            if (declaredMethod.getName ().equals (methodName)) {
                if (method != null) {
                    throw new ReflectiveOperationException ();
                } else {
                    method = declaredMethod;
                }
            }
        }
        if (method == null) {
            throw new ReflectiveOperationException ();
        }

        for (Object listener : listeners) {
            //父类 isAssignableFrom 子类
            if (callbackType.isAssignableFrom (listener.getClass ())) {
                //拷贝一个session，避免被修改
                HttpSessionEvent event = new HttpSessionEvent (getSession ());
                method.invoke (listener, event);
            }
        }
    }

    private boolean validListeners(Object[] listeners) {
        return listeners != null && listeners.length > 0;
    }

    /**
     * 序列化方法,自动调用
     */
    private void readObject(ObjectInputStream stream)
            throws ClassNotFoundException, IOException {
        doReadObject (stream);
    }

    /**
     * 反序列化方法,自动调用
     * 其实标记不标记transient也无所谓了，因为手动序列化
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        doWriteObject (stream);
    }

    protected void doReadObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        creationTime = (long) stream.readObject ();
        lastAccessedTime = (long) stream.readObject ();
        maxInactiveInterval = (int) stream.readObject ();
        isNew = (boolean) stream.readObject ();
        isValid = (boolean) stream.readObject ();
        thisAccessedTime = (long) stream.readObject ();
        id = (String) stream.readObject ();

        int n = stream.readInt ();
        if (attributes == null) {
            attributes = new ConcurrentHashMap<> (n);
        }

        for (int i = 0; i < n; i++) {
            String key = (String) stream.readObject ();
            try {
                Object value = stream.readObject ();
                attributes.put (key, value);
            } catch (NotSerializableException e) {
                e.printStackTrace ();
                log.warn ("反序列化session value错误，err:{}", e.toString ());
            }
        }

        listeners = new ArrayList<> ();
        support = new PropertyChangeSupport (this);
        notes = new ConcurrentHashMap<> ();
        log.trace ("反序列化session {} 完成", getIdInternal ());
    }

    /**
     * todo序列化auth信息<p>
     * 序列化map的时候，为了减少重建开销，选择自定义中间逻辑表示
     * 即size，entry[]
     */
    protected void doWriteObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject (creationTime);
        stream.writeObject (lastAccessedTime);
        stream.writeObject (maxInactiveInterval);
        stream.writeObject (isNew);
        stream.writeObject (isValid);
        stream.writeObject (thisAccessedTime);
        stream.writeObject (id);

        stream.writeInt (attributes.size ());
        for (Map.Entry<String, Object> entry : attributes.entrySet ()) {
            stream.writeObject (entry.getKey ());
            try {
                stream.writeObject (entry.getValue ());
            } catch (NotSerializableException e) {
                e.printStackTrace ();
                log.warn ("序列化session的时候，对象 {} 无法序列化，err:{}", entry.getValue (), e.toString ());
            }
        }

        log.trace ("序列化session {} 完成", getIdInternal ());
    }

    @Override
    public String toString() {
        return "StandardSession{" +
                "creationTime=" + new Date (creationTime) +
                ", id='" + id + '\'' +
                ", isValid=" + isValid +
                ", thisAccessedTime=" + new Date (thisAccessedTime) +
                '}';
    }
}
