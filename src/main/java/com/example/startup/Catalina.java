package com.example.startup;

import com.example.Container;
import com.example.Server;
import com.example.life.Lifecycle;
import com.example.life.LifecycleException;
import com.example.startup.rule.ContextRuleSet;
import com.example.startup.rule.EngineRuleSet;
import com.example.startup.rule.HostRuleSet;
import com.example.startup.rule.NamingRuleSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.Security;

/**
 * 负责解析server.xml并启动server
 * TODO socket关闭支持
 *
 * @date 2022/1/1 11:38
 */
@Slf4j
public class Catalina {

    protected Server server;
    /**
     * server.xml配置文件的位置
     */
    protected String configFile = "conf/server.xml";

    protected ClassLoader parentClassLoader = ClassLoader.getSystemClassLoader ();

    public static void main(String[] args) {
        new Catalina ().process (args);
    }

    public void process(String[] args) {
        if (args.length < 1) {
            usage ();
            return;
        }

        setCatalinaHome ();
        setCatalinaBase ();

        boolean config = false;
        for (String arg : args) {
            if (config) {
                configFile = arg;
                config = false;
            } else if (arg.equals ("--config")) {
                config = true;
            } else if (arg.equals ("start")) {
                start ();
                return;
            } else if (arg.equals ("stop")) {
                stop ();
                return;
            } else {
                usage ();
                return;
            }
        }
        usage ();
    }

    protected void usage() {
        System.out.println
                ("usage: java com.example.startup.Catalina"
                        + " [ --config {pathname} ]"
                        + " { start | stop | --help}");
    }

    /**
     * Set the <code>catalina.base</code> System property to the current
     * working directory if it has not been set.
     */
    protected void setCatalinaBase() {
        if (System.getProperty ("catalina.base") != null)
            return;
        System.setProperty ("catalina.base",
                System.getProperty ("catalina.home"));
    }

    public void setParentClassLoader(ClassLoader parentClassLoader) {

        this.parentClassLoader = parentClassLoader;

    }


    public void setServer(Server server) {

        this.server = server;

    }


    /**
     * Set the <code>catalina.home</code> System property to the current
     * working directory if it has not been set.
     */
    protected void setCatalinaHome() {
        if (System.getProperty ("catalina.home") != null)
            return;
        System.setProperty ("catalina.home",
                System.getProperty ("user.dir"));
    }

    protected void start() {
        Digester digester = createStartDigester ();
        File file = configFile ();
        try {
            InputSource is =
                    new InputSource ("file://" + file.getAbsolutePath ());
            FileInputStream fis = new FileInputStream (file);
            is.setByteStream (fis);
            digester.push (this);
            digester.parse (is);
            fis.close ();
        } catch (Exception e) {
            System.out.println ("Catalina.start: " + e);
            e.printStackTrace ();
            System.exit (1);
        }

        Thread shutdownHook = new ShutDownHook ();

        if (server != null) {
            try {
                server.start ();
                try {
                    Runtime.getRuntime ().addShutdownHook (shutdownHook);
                } catch (Throwable t) {
                    // This will fail on JDK 1.2. Ignoring, as Tomcat can run
                    // fine without the shutdown hook.
                }

                server.await ();
            } catch (LifecycleException e) {
                System.out.println ("Catalina.start: " + e);
                e.printStackTrace ();
                if (e.getThrowable () != null) {
                    System.out.println ("----- Root Cause -----");
                    e.getThrowable ().printStackTrace (System.out);
                }
            }
        }

        //如果启动失败，或者await返回，那就关闭服务器
        if (server != null) {
            try {
                try {
                    //防止server的stop执行2次
                    Runtime.getRuntime ().removeShutdownHook (shutdownHook);
                } catch (Throwable ignored) {

                }
                server.stop ();
            } catch (LifecycleException e) {
                System.out.println ("Catalina.stop: " + e);
                e.printStackTrace ();
                if (e.getThrowable () != null) {
                    System.out.println ("----- Root Cause -----");
                    e.getThrowable ().printStackTrace (System.out);
                }
            }
        }
    }

    /**
     * 使用socket发送一个关闭命令给server(因为是跨进程的，就无法修改server的等待标志位)
     * 这样就会从await方法中返回
     * TODO
     */
    protected void stop() {
        Digester digester = createStopDigester ();
        File file = configFile ();
        try {
            InputSource is =
                    new InputSource ("file://" + file.getAbsolutePath ());
            FileInputStream fis = new FileInputStream (file);
            is.setByteStream (fis);
            digester.push (this);
            digester.parse (is);
            fis.close ();
        } catch (Exception e) {
            System.out.println ("Catalina.stop: " + e);
            e.printStackTrace ();
            System.exit (1);
        }

        // Stop the existing server
//        try {
//            Socket socket = new Socket ("127.0.0.1", server.getPort ());
//            OutputStream stream = socket.getOutputStream ();
//            String shutdown = server.getShutdown ();//目的只是为了获得端口和host和command而已
//            for (int i = 0; i < shutdown.length (); i++)
//                stream.write (shutdown.charAt (i));
//            stream.flush ();
//            stream.close ();
//            socket.close ();
//        } catch (IOException e) {
//            System.out.println ("Catalina.stop: " + e);
//            e.printStackTrace ();
//            System.exit (1);
//        }


    }

    protected File configFile() {
        File file = new File (configFile);
        if (!file.isAbsolute ())
            file = new File (System.getProperty ("catalina.base"), configFile);
        return file;
    }

    /**
     * Create and configure the Digester we will be using for startup.
     */
    protected Digester createStartDigester() {
        Digester digester = new Digester ();
        digester.setValidating (false);

        digester.addObjectCreate ("Server",
                "com.example.core.StandardServer",
                "className");
        digester.addSetProperties ("Server");
        digester.addSetNext ("Server",
                "setServer",
                "com.example.Server");

//        digester.addObjectCreate ("Server/GlobalNamingResources",
//                "org.apache.catalina.deploy.NamingResources");
//        digester.addSetProperties ("Server/GlobalNamingResources");
//        digester.addSetNext ("Server/GlobalNamingResources",
//                "setGlobalNamingResources",
//                "org.apache.catalina.deploy.NamingResources");

        digester.addObjectCreate ("Server/Listener",
                null, // MUST be specified in the element
                "className");
        digester.addSetProperties ("Server/Listener");
        digester.addSetNext ("Server/Listener",
                "addLifecycleListener",
                "com.example.life.LifecycleListener");

        digester.addObjectCreate ("Server/Service",
                "com.example.core.StandardService",
                "className");
        digester.addSetProperties ("Server/Service");
        digester.addSetNext ("Server/Service",
                "addService",
                "com.example.Service");

        digester.addObjectCreate ("Server/Service/Listener",
                null, // MUST be specified in the element
                "className");
        digester.addSetProperties ("Server/Service/Listener");
        digester.addSetNext ("Server/Service/Listener",
                "addLifecycleListener",
                "com.example.life.LifecycleListener");

        digester.addObjectCreate ("Server/Service/Connector",
                "com.example.connector.http.HttpConnector",
                "className");
        digester.addSetProperties ("Server/Service/Connector");
        digester.addSetNext ("Server/Service/Connector",
                "addConnector",
                "com.example.connector.Connector");

//        digester.addObjectCreate ("Server/Service/Connector/Factory",
//                "org.apache.catalina.net.DefaultServerSocketFactory",
//                "className");
//        digester.addSetProperties ("Server/Service/Connector/Factory");
//        digester.addSetNext ("Server/Service/Connector/Factory",
//                "setFactory",
//                "org.apache.catalina.net.ServerSocketFactory");

        digester.addObjectCreate ("Server/Service/Connector/Listener",
                null, // MUST be specified in the element
                "className");
        digester.addSetProperties ("Server/Service/Connector/Listener");
        digester.addSetNext ("Server/Service/Connector/Listener",
                "addLifecycleListener",
                "com.example.life.LifecycleListener");

        // Add RuleSets for nested elements
//        digester.addRuleSet (new NamingRuleSet ("Server/GlobalNamingResources/"));
        digester.addRuleSet (new EngineRuleSet ("Server/Service/"));
        digester.addRuleSet (new HostRuleSet ("Server/Service/Engine/"));
//        digester.addRuleSet (new ContextRuleSet ("Server/Service/Engine/Default"));
//        digester.addRuleSet (new NamingRuleSet ("Server/Service/Engine/DefaultContext/"));
//        digester.addRuleSet (new ContextRuleSet ("Server/Service/Engine/Host/Default"));
//        digester.addRuleSet (new NamingRuleSet ("Server/Service/Engine/Host/DefaultContext/"));
        digester.addRuleSet (new ContextRuleSet ("Server/Service/Engine/Host/"));
//        digester.addRuleSet (new NamingRuleSet ("Server/Service/Engine/Host/Context/"));

        digester.addRule ("Server/Service/Engine",
                new SetParentClassLoaderRule (digester, parentClassLoader));

        return digester;
    }


    /**
     * shutdown只需要获得port、host、command就行了
     * 所以只需要解析出server组件就行了
     */
    protected Digester createStopDigester() {
        Digester digester = new Digester ();

        digester.addObjectCreate ("Server",
                "com.example.core.StandardServer",
                "className");
        digester.addSetProperties ("Server");
        digester.addSetNext ("Server",
                "setServer",
                "com.example.Server");

        return digester;
    }

    private final class ShutDownHook extends Thread {

        @Override
        public void run() {
            if (server != null) {
                log.info ("开始执行关闭钩子");
                try {
                    server.stop ();
                } catch (Throwable e) {
                    log.error ("", e);
                }
                log.info ("JerryMouse已关闭");
            }
        }
    }
}

/**
 * Rule that sets the parent class loader for the top object on the stack,
 * which must be a <code>Container</code>.
 */
final class SetParentClassLoaderRule extends Rule {

    ClassLoader parentClassLoader;

    public SetParentClassLoaderRule(Digester digester,
                                    ClassLoader parentClassLoader) {
        super (digester);
        this.parentClassLoader = parentClassLoader;
    }

    public void begin(Attributes attributes) {
        if (digester.getDebug () >= 1)
            digester.log ("Setting parent class loader");

        Container top = (Container) digester.peek ();
        top.setParentClassLoader (parentClassLoader);
    }
}
