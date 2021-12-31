package com.example.core;

import com.example.Globals;
import com.example.Server;
import com.example.Service;
import com.example.life.LifecycleBase;
import com.example.life.LifecycleException;
import com.example.util.StoreConfigurationUtil;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


@Slf4j
public final class StandardServer extends LifecycleBase implements Server {

    private final Object servicesLock = new Object ();
    private final PropertyChangeSupport support = new PropertyChangeSupport (this);
    private final List<Service> services = new ArrayList<> ();
    private final Object waitLock = new Object ();
    //    private Catalina catalina;
    private ClassLoader parentClassLoader;
    private File catalinaHome;
    private File catalinaBase;


    /**
     * Report the current Tomcat Server Release number
     *
     * @return Tomcat release identifier
     */
    public String getServerInfo() {
        return Globals.SERVER_INFO;
    }

    /**
     * Return the current server built timestamp
     */
    public String getServerBuilt() {
        return "0";
    }

    /**
     * Return the current server's version number.
     */
    public String getServerNumber() {
        return "0.1.alpha";
    }

//    /**
//     * Return the outer Catalina startup/shutdown component if present.
//     */
//    @Override
//    public Catalina getCatalina() {
//        return catalina;
//    }
//
//    /**
//     * Set the outer Catalina startup/shutdown component if present.
//     */
//    @Override
//    public void setCatalina(Catalina catalina) {
//        this.catalina = catalina;
//    }


    @Override
    public void addService(Service service) {
        service.setServer (this);

        synchronized (servicesLock) {
            services.add (service);
            support.firePropertyChange ("service", null, service);
        }
    }

    /**
     * 不支持基于端口socket命令的关闭方式，只能线程异步关闭，或者SIGINT等信号关闭
     */
    @Override
    public void await() {
        try {
            synchronized (waitLock) {
                waitLock.wait ();
            }
        } catch (InterruptedException ignored) {

        }
    }

    @Override
    public Service findService(String name) {
        if (name == null) {
            return null;
        }

        synchronized (servicesLock) {
            for (Service service : services) {
                if (name.equals (service.getName ())) {
                    return service;
                }
            }
        }
        return null;
    }

    @Override
    public Service[] findServices() {
        return services.toArray (new Service[0]);
    }

    public void store(String path) throws Exception {
        File file = new File (path);
        if (file.exists ()) {
            file.delete ();
        }
        file.createNewFile ();
        PrintWriter writer = new PrintWriter (new FileWriter (file));

        StoreConfigurationUtil.storeServer (writer, 4, this);
        writer.close ();
    }

    @Override
    public void removeService(Service service) {
        synchronized (servicesLock) {
            if (services.contains (service)) {
                if (isRunning ()) {
                    try {
                        service.stop ();
                    } catch (LifecycleException ignored) {

                    }
                }
            }

            services.remove (service);

            support.firePropertyChange ("service", service, null);
        }
    }

    @Override
    public File getCatalinaBase() {
        if (catalinaBase != null) {
            return catalinaBase;
        }

        catalinaBase = getCatalinaHome ();
        return catalinaBase;
    }

    @Override
    public void setCatalinaBase(File catalinaBase) {
        this.catalinaBase = catalinaBase;
    }


    @Override
    public File getCatalinaHome() {
        return catalinaHome;
    }

    @Override
    public void setCatalinaHome(File catalinaHome) {
        this.catalinaHome = catalinaHome;
    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener (listener);
    }


    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener (listener);
    }

    @Override
    public synchronized void start() throws LifecycleException {
        super.start ();

        log.info ("server开始启动");

        synchronized (servicesLock) {
            for (Service service : services) {
                service.start ();
            }
        }
        log.info ("server启动完成");
    }


    @Override
    public synchronized void stop() throws LifecycleException {
        super.stop ();

        for (Service service : services) {
            service.stop ();
        }

        synchronized (waitLock) {
            waitLock.notifyAll ();
        }
    }


    @Override
    public ClassLoader getParentClassLoader() {
        if (parentClassLoader != null) {
            return parentClassLoader;
        }
//        if (catalina != null) {
//            return catalina.getParentClassLoader ();
//        }
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
