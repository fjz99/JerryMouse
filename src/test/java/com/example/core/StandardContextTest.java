package com.example.core;

import com.example.Context;
import com.example.Wrapper;
import com.example.connector.http.HttpConnector;
import com.example.life.*;
import com.example.loader.Loader;
import com.example.loader.WebappLoader;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StandardContextTest {
    @Test
    void testBasic() throws LifecycleException, ServletException, IOException {
        StandardContext context = new StandardContext ();

        Wrapper wrapper = new StandardWrapper ();
        wrapper.setName ("Modern");
        wrapper.setServletClass ("com.example.servlet.ModernServlet");

        context.setDocBase ("webapps/testContext");
        context.setWorkDir ("/workdir");//和docbase不同
        context.setDisplayName ("Test");
        context.setPath ("/test");
        context.addLifecycleListener (new Listener ());

        context.addChild (wrapper);
        context.addServletMapping ("/test", "Modern");

        context.start ();
        assertTrue (context.isRunning ());

//        context.invoke (null, null);

        context.stop ();
    }

    @Test
    void test2() throws LifecycleException {
        StandardContext context = new StandardContext ();
        HttpConnector connector = new HttpConnector ();

        Wrapper wrapper = new StandardWrapper ();
        wrapper.setName ("Modern");
        wrapper.setServletClass ("com.example.servlet.ModernServlet");

        connector.setContainer (context);
        context.setDocBase ("webapps/testContext");
        context.setWorkDir ("/workdir");//和docbase不同
        context.setDisplayName ("Test");
        context.setPath ("/test");
        context.addLifecycleListener (new Listener ());

        context.addChild (wrapper);
        context.addServletMapping ("/servlet", "Modern");

        context.start ();
        connector.start ();
    }

    static class Listener implements LifecycleListener {
        public void lifecycleEvent(LifecycleEvent event) {
            if (EventType.START_EVENT.equals (event.getType ())) {
                Context context = (Context) event.getLifecycle ();
                context.setConfigured (true);
            }
        }
    }
}
