package com.example.core;

import com.example.Engine;
import com.example.Host;
import com.example.Service;
import com.example.Wrapper;
import com.example.connector.http.HttpConnector;
import com.example.life.LifecycleException;
import com.example.session.FileStore;
import com.example.session.PersistentManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StandardServiceTest {
    @Test
    void runServer() throws LifecycleException, InterruptedException, IOException {
        StandardContext context = new StandardContext ();
        HttpConnector connector = new HttpConnector ();
        connector.setPort (8080);

        Wrapper wrapper = new StandardWrapper ();
        wrapper.setName ("Modern");
        wrapper.setServletClass ("ModernServlet");

        Wrapper wrapper2 = new StandardWrapper ();
        wrapper2.setName ("Session");
        wrapper2.setServletClass ("SessionServlet");

        Wrapper wrapper3 = new StandardWrapper ();
        wrapper3.setName ("Pr");
        wrapper3.setServletClass ("PrimitiveServlet");

        Wrapper wrapper4 = new StandardWrapper ();
        wrapper4.setName ("ERR");
        wrapper4.setServletClass ("com.example.servlet.ExceptionServlet");

        context.setDocBase ("testContext");
        context.setWorkDir ("/workdir");//和docbase不同
        context.setDisplayName ("Test");
        context.setPath ("/test");
        context.addLifecycleListener (new StandardContextTest.Listener ());
        PersistentManager manager = new PersistentManager ();
        manager.setProcessExpiresFrequency (1);
        manager.setStore (new FileStore ());
        manager.setMinIdleSwap (1);
        manager.setMaxIdleSwap (2);
        manager.setMaxIdleBackup (1);
        context.setManager (manager);
        context.setSessionTimeout (1);

        context.addChild (wrapper);
        context.addChild (wrapper2);
        context.addChild (wrapper3);
        context.addChild (wrapper4);
        context.addServletMapping ("/servlet", "Modern");
        context.addServletMapping ("/session", "Session");
        context.addServletMapping ("/pr", "Pr");
        context.addServletMapping ("/err", "ERR");

        Host host = new StandardHost ();
        host.setName ("localhost");
        host.addChild (context);
        host.setAppBase ("webapps");//组合！

        Engine engine = new StandardEngine ();
        engine.setName ("engine1");
        engine.addChild (host);

        connector.setContainer (engine);

        Service service = new StandardService ();
        service.setName ("service");
        service.setContainer (engine);
        service.addConnector (connector);

        service.start ();

        System.in.read ();
//        Thread.sleep (1000);

        service.stop ();
    }
}
