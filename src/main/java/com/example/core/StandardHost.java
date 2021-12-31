package com.example.core;

import com.example.*;
import com.example.life.LifecycleException;
import com.example.valve.ErrorDispatcherValve;
import com.example.valve.basic.StandardHostValve;
import com.sun.xml.internal.ws.api.pipe.Engine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

/**
 * Standard implementation of the <b>Host</b> interface.  Each
 * child container must be a Context implementation to process the
 * requests directed to a particular web application.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 */
@Slf4j
public final class StandardHost extends AbstractContainer implements Host {

    private static final String mapperClass = "com.example.mapper.StandardHostMapper";
    /**
     * 别名
     */
    private final Set<String> aliases = new ConcurrentSkipListSet<> ();
    /**
     * The application root for this Host.
     * host的base+context的base的string concat必须在工作目录下
     * 路径需要engine、host、context组合
     */
    private String appBase = "webapps";
    private volatile File appBaseFile;//File缓存
    /**
     * The XML root for this Host.
     */
    private String xmlBase = null;
    /**
     * host's default config path
     */
    private volatile File hostConfigBase = null;
    /**
     * The auto deploy flag for this Host.
     */
    private boolean autoDeploy = true;
    /**
     * 部署context的时候使用这个类
     * TODO
     */
    private String configClass = "null";
    private String contextClass = "com.example.core.StandardContext";
    /**
     * host启动时自动部署所有的context
     */
    private boolean deployOnStartup = true;
    /**
     * deploy Context XML config files property.
     */
    private boolean deployXML = true;
    /**
     * 默认的
     */
    private String errorReportValveClass = "com.example.valve.ErrorReportValve";
    private boolean unpackWARs = true;
    private String workDir;
    /**
     * Should we create directories upon startup for appBase and xmlBase
     */
    private boolean createDirs = true;
    private boolean failCtxIfServletStartFails = false;

    public StandardHost() {
        super ();
        pipeline.setBasic (new StandardHostValve ());
    }


    @Override
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException ();
        }
        name = name.toLowerCase (Locale.ENGLISH);// toLowerCase

        super.setName (name);
    }

    @Override
    public String getAppBase() {
        return this.appBase;
    }

    @Override
    public void setAppBase(String appBase) {
        if (appBase.trim ().equals ("")) {
            log.warn ("standardHost.problematicAppBase " + getName ());
        }

        String oldAppBase = this.appBase;
        this.appBase = appBase;
        support.firePropertyChange ("appBase", oldAppBase, this.appBase);
        this.appBaseFile = null;
    }

    /**
     * 即默认情况下，getCatalinaBase + webapps文件夹，就是一个host
     * 即tomcat目录下的webapps文件夹，就是一个host！
     */
    @Override
    public File getAppBaseFile() {
        if (appBaseFile != null) {
            return appBaseFile;
        }

        File file = new File (getAppBase ());
        if (!file.isAbsolute ()) {
            file = new File (getCatalinaBase (), file.getPath ());
        }

        try {
            file = file.getCanonicalFile ();
        } catch (IOException ignored) {

        }

        return appBaseFile = file;
    }


    @Override
    public String getXmlBase() {
        return this.xmlBase;
    }


    @Override
    public void setXmlBase(String xmlBase) {
        String oldXmlBase = this.xmlBase;
        this.xmlBase = xmlBase;
        support.firePropertyChange ("xmlBase", oldXmlBase, this.xmlBase);
    }


    @Override
    public File getConfigBaseFile() {
        if (hostConfigBase != null) {
            return hostConfigBase;
        }
        String path;
        if (getXmlBase () != null) {
            path = getXmlBase ();
        } else {
            StringBuilder xmlDir = new StringBuilder ("conf");
            Container parent = getParent ();
            if (parent instanceof Engine) {
                xmlDir.append ('/');
                xmlDir.append (parent.getName ());
            }
            xmlDir.append ('/');
            xmlDir.append (getName ());
            path = xmlDir.toString ();
        }
        File file = new File (path);
        if (!file.isAbsolute ()) {
            file = new File (getCatalinaBase (), path);
        }
        try {
            file = file.getCanonicalFile ();
        } catch (IOException e) {// ignore
        }
        this.hostConfigBase = file;
        return file;
    }


    @Override
    public boolean getCreateDirs() {
        return createDirs;
    }


    @Override
    public void setCreateDirs(boolean createDirs) {
        this.createDirs = createDirs;
    }


    @Override
    public boolean getAutoDeploy() {
        return this.autoDeploy;
    }


    @Override
    public void setAutoDeploy(boolean autoDeploy) {
        boolean oldAutoDeploy = this.autoDeploy;
        this.autoDeploy = autoDeploy;
        support.firePropertyChange ("autoDeploy", oldAutoDeploy,
                this.autoDeploy);
    }


    @Override
    public String getConfigClass() {
        return this.configClass;
    }


    @Override
    public void setConfigClass(String configClass) {
        String oldConfigClass = this.configClass;
        this.configClass = configClass;
        support.firePropertyChange ("configClass",
                oldConfigClass, this.configClass);
    }


    public String getContextClass() {
        return this.contextClass;
    }


    public void setContextClass(String contextClass) {
        String oldContextClass = this.contextClass;
        this.contextClass = contextClass;
        support.firePropertyChange ("contextClass",
                oldContextClass, this.contextClass);
    }


    @Override
    public boolean getDeployOnStartup() {
        return this.deployOnStartup;
    }


    @Override
    public void setDeployOnStartup(boolean deployOnStartup) {
        boolean oldDeployOnStartup = this.deployOnStartup;
        this.deployOnStartup = deployOnStartup;
        support.firePropertyChange ("deployOnStartup", oldDeployOnStartup,
                this.deployOnStartup);
    }


    public boolean isDeployXML() {
        return deployXML;
    }


    public void setDeployXML(boolean deployXML) {
        this.deployXML = deployXML;
    }


    public String getErrorReportValveClass() {
        return this.errorReportValveClass;
    }

    public void setErrorReportValveClass(String errorReportValveClass) {
        String oldErrorReportValveClassClass = this.errorReportValveClass;
        this.errorReportValveClass = errorReportValveClass;
        support.firePropertyChange ("errorReportValveClass",
                oldErrorReportValveClassClass,
                this.errorReportValveClass);
    }

    public boolean isUnpackWARs() {
        return unpackWARs;
    }

    public void setUnpackWARs(boolean unpackWARs) {
        this.unpackWARs = unpackWARs;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public boolean isFailCtxIfServletStartFails() {
        return failCtxIfServletStartFails;
    }

    public void setFailCtxIfServletStartFails(
            boolean failCtxIfServletStartFails) {
        boolean oldFailCtxIfServletStartFails = this.failCtxIfServletStartFails;
        this.failCtxIfServletStartFails = failCtxIfServletStartFails;
        support.firePropertyChange ("failCtxIfServletStartFails",
                oldFailCtxIfServletStartFails,
                failCtxIfServletStartFails);
    }

    /**
     * 添加别名
     */
    @Override
    public void addAlias(String alias) {
        alias = alias.toLowerCase (Locale.ENGLISH);
        aliases.add (alias);
        fireContainerEvent (ADD_ALIAS_EVENT, alias);
    }


    @Override
    public void addChild(Container child) {
        //只能添加context
        if (!(child instanceof Context)) {
            throw new IllegalArgumentException ();
        }

        super.addChild (child);
    }


    @Override
    public String[] findAliases() {
        return aliases.toArray (new String[0]);
    }

    @Override
    public void removeAlias(String alias) {
        alias = alias.toLowerCase (Locale.ENGLISH);
        aliases.remove (alias);
        fireContainerEvent (REMOVE_ALIAS_EVENT, alias);
    }

    @Override
    public synchronized void start() throws LifecycleException {
        String errorValve = getErrorReportValveClass ();

        if (!StringUtils.isEmpty (errorValve)) {
            try {
                //防止重复添加
                boolean found = false;
                Valve[] valves = getPipeline ().getValves ();
                for (Valve valve : valves) {
                    if (errorValve.equals (valve.getClass ().getName ())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Valve valve =
                            (Valve) Class.forName (errorValve).getConstructor ().newInstance ();
                    getPipeline ().addValve (valve);
                }
            } catch (Throwable e) {
                log.error ("Host" + getName () + " starts failed.", e);
            }
        }

        //TODO
//        getPipeline ().addValve (new ErrorDispatcherValve ());
        addDefaultMapper (mapperClass);

        //多线程启动所有的context
        //顺便启动task thread（如果delay>0）
        super.start ();

        log.info ("Host {} started.", getName ());
    }

}
