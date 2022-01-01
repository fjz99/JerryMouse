package com.example.startup;

import com.example.Context;
import com.example.Engine;
import com.example.Host;
import com.example.Service;
import com.example.connector.http.HttpConnector;
import com.example.core.*;
import com.example.life.LifecycleException;
import org.junit.jupiter.api.Test;

class ContextConfigTest {
    @Test
    void test() throws LifecycleException {
        Context context = new StandardContext ();
        ContextConfig contextConfig = new ContextConfig ();
        context.setDocBase ("webapps/testContext");
        context.setPath ("/test");
        context.setDisplayName ("Context");
        System.out.println (context.getName ());
        context.addLifecycleListener (contextConfig);

        context.start ();
    }

    @Test
    void runServer() throws Exception {
        StandardContext context = new StandardContext ();
        HttpConnector connector = new HttpConnector ();
        connector.setPort (8080);
        ContextConfig contextConfig = new ContextConfig ();
        context.addLifecycleListener (contextConfig);
        context.setDocBase ("/testContext");
        context.setPath ("/test");
        context.setDisplayName ("Context");

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

        StandardServer server = new StandardServer ();
        server.addService (service);

        server.start ();

//        server.store ("/server.xml");
        server.await ();

//        System.in.read ();
//        Thread.sleep (1000);

//        server.stop ();
    }
}
