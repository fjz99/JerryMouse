package com.example.core;

import com.example.*;
import com.example.life.LifecycleException;
import com.example.valve.basic.StandardEngineValve;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Locale;

/**
 * @date 2021/12/30 20:34
 */
@Slf4j
public final class StandardEngine extends AbstractContainer implements Engine {

    private static final String mapperClass = "com.example.mapper.StandardEngineMapper";
    private Service service;
    private String defaultHost;

    public StandardEngine() {
        getPipeline ().setBasic (new StandardEngineValve ());
        setBackgroundProcessorDelay (10);//task thread的
    }

    @Override
    public void setParent(Container container) {
        throw new IllegalArgumentException ("Engine does not have parent.");
    }

    @Override
    public void addChild(Container child) {
        if (!(child instanceof Host)) {
            throw new IllegalArgumentException ();
        }

        super.addChild (child);
    }

    @Override
    public ClassLoader getParentClassLoader() {
        if (parentClassLoader != null) {
            return parentClassLoader;
        }
        if (getService () != null) {
            return getService ().getParentClassLoader ();
        }
        return ClassLoader.getSystemClassLoader ();
    }

    @Override
    public String getDefaultHost() {
        return defaultHost;
    }

    @Override
    public void setDefaultHost(String host) {
        String oldDefaultHost = this.defaultHost;
        if (host == null) {
            this.defaultHost = null;
        } else {
            this.defaultHost = host.toLowerCase (Locale.ENGLISH);
        }
        support.firePropertyChange ("defaultHost", oldDefaultHost,
                this.defaultHost);
    }

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public void setService(Service service) {
        this.service = service;
    }

    @Override
    public File getCatalinaBase() {
        if (service != null) {
            Server server = service.getServer ();
            if (server != null) {
                File base = server.getCatalinaBase ();
                if (base != null) {
                    return base;
                }
            }
        }
        // Fall-back
        return super.getCatalinaBase ();
    }


    @Override
    public File getCatalinaHome() {
        if (service != null) {
            Server server = service.getServer ();
            if (server != null) {
                File base = server.getCatalinaHome ();
                if (base != null) {
                    return base;
                }
            }
        }
        // Fall-back
        return super.getCatalinaHome ();
    }

    @Override
    public void start() throws LifecycleException {
        log.info ("Engine {} 开始启动", getName ());

        super.start ();//这里会start background task thread

        addDefaultMapper (mapperClass);

        log.info ("Engine {} 启动完成", getName ());
    }
}
