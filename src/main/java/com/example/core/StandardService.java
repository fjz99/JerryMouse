package com.example.core;

import com.example.Engine;
import com.example.Server;
import com.example.Service;
import com.example.connector.Connector;
import com.example.life.LifecycleBase;
import com.example.life.LifecycleException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


@Slf4j
public final class StandardService extends LifecycleBase implements Service {

    private final PropertyChangeSupport support = new PropertyChangeSupport (this);
    private final List<Connector> connectors = new ArrayList<> ();
    private String name;
    private Server server;
    private Engine engine;
    private ClassLoader parentClassLoader;

    private CountDownLatch countDownLatch;


    @Override
    public Engine getContainer() {
        return engine;
    }


    @Override
    public void setContainer(Engine engine) {
        Engine oldEngine = this.engine;
        if (oldEngine != null) {
            oldEngine.setService (null);
        }

        this.engine = engine;
        if (this.engine != null) {
            this.engine.setService (this);
        }

        if (isRunning ()) {
            if (this.engine != null) {
                try {
                    this.engine.start ();
                } catch (LifecycleException e) {
                    log.error ("standardService.engine.startFailed", e);
                }
            }

            if (oldEngine != null) {
                try {
                    oldEngine.stop ();
                } catch (LifecycleException e) {
                    log.error ("standardService.engine.stopFailed", e);
                }
            }
        }

        support.firePropertyChange ("container", oldEngine, this.engine);
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public void setName(String name) {
        this.name = name;
    }


    @Override
    public Server getServer() {
        return this.server;
    }

    @Override
    public void setServer(Server server) {
        this.server = server;
    }


    @Override
    public void addConnector(Connector connector) {
        if (connectors.contains (connector)) {
            return;
        }

        synchronized (connectors) {
            connectors.add (connector);

            if (isRunning ()) {
                try {
                    connector.start ();
                } catch (LifecycleException e) {
                    log.error ("standardService.connector.startFailed", e);
                }
            }

            support.firePropertyChange ("connector", null, connector);
        }

    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener (listener);
    }


    @Override
    public Connector[] findConnectors() {
        return connectors.toArray (new Connector[0]);
    }


    @Override
    public void removeConnector(Connector connector) {
        //保证remove操作互斥
        synchronized (connectors) {
            if (connectors.contains (connector)) {
                try {
                    connector.stop ();
                } catch (LifecycleException e) {
                    log.error ("standardService.connector.stopFailed", e);
                }
            }

            connectors.remove (connector);
        }

        support.firePropertyChange ("connector", connector, null);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener (listener);
    }


    @Override
    public String toString() {
        return "StandardService[" +
                getName () +
                ']';
    }

    /**
     * 肯定是先启动engine再启动connector
     */
    @Override
    public void start() throws LifecycleException {
        super.start ();

        if (StringUtils.isEmpty (getName ())) {
            throw new IllegalStateException ("service的name不能为空");
        }

        log.info ("开始启动service {}", getName ());

        if (engine != null) {
            engine.start ();
        }

        for (Connector connector : connectors) {
            connector.start ();
        }

        log.info ("service {} 启动完成", getName ());
    }


    // TODO: 2021/12/31 先pause connector再关闭engine
    @Override
    public void stop() throws LifecycleException {
        super.stop ();

        log.info ("service {} 开始关闭", getName ());

        if (engine != null) {
            engine.stop ();//context会等待一段时间，执行剩余的任务
        }

        for (Connector connector : connectors) {
            if (connector.isRunning ()) {
                try {
                    connector.stop ();
                } catch (Exception e) {
                    log.error ("standardService.connector.stopFailed", e);
                }
            }
        }

        log.info ("service {} 关闭完成", getName ());

    }


    @Override
    public ClassLoader getParentClassLoader() {
        if (parentClassLoader != null) {
            return parentClassLoader;
        }
        if (server != null) {
            return server.getParentClassLoader ();
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

}
