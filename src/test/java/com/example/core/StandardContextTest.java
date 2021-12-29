package com.example.core;

import com.example.Context;
import com.example.Wrapper;
import com.example.connector.http.HttpConnector;
import com.example.httpUtil.HttpTestUtil;
import com.example.httpUtil.MatchFailedException;
import com.example.life.EventType;
import com.example.life.LifecycleEvent;
import com.example.life.LifecycleException;
import com.example.life.LifecycleListener;
import com.example.session.AbstractManager;
import com.example.session.FileStore;
import com.example.session.PersistentManager;
import com.example.valve.ErrorReportValve;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void test2() throws LifecycleException, MatchFailedException, IOException, URISyntaxException {
        StandardContext context = new StandardContext ();
        HttpConnector connector = new HttpConnector ();

        Wrapper wrapper = new StandardWrapper ();
        wrapper.setName ("Modern");
        wrapper.setServletClass ("ModernServlet");

        connector.setContainer (context);
        context.setDocBase ("webapps/testContext");
        context.setWorkDir ("/workdir");//和docbase不同
        context.setDisplayName ("Test");
        context.setPath ("/test");
        context.addLifecycleListener (new Listener ());

        context.addChild (wrapper);
        context.addServletMapping ("/servlet", "Modern");

        context.start ();
        new Thread (() -> {
            try {
                connector.start ();
            } catch (LifecycleException e) {
                e.printStackTrace ();
            }
        }).start ();

        //test
        HttpTestUtil.get ("http://localhost:8080/test/servlet")
                .isOk ()
                .printBody ();

//        connector.stop ();
//        context.stop ();
    }

    /**
     * test:
     * ErrPage
     * session
     * servlet等
     */
    @Test
    void runServer() throws LifecycleException {
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

        connector.setContainer (context);
        context.setDocBase ("webapps/testContext");
        context.setWorkDir ("/workdir");//和docbase不同
        context.setDisplayName ("Test");
        context.setPath ("/test");
        context.addLifecycleListener (new Listener ());
        PersistentManager manager = new PersistentManager ();
        manager.setProcessExpiresFrequency (1);
        manager.setStore (new FileStore ());
        manager.setMinIdleSwap (1);
        manager.setMaxIdleSwap (2);
        manager.setMaxIdleBackup (1);
        context.setManager (manager);
        context.setSessionTimeout (1);
//        context.setBackgroundProcessorDelay (5);

        context.addChild (wrapper);
        context.addChild (wrapper2);
        context.addChild (wrapper3);
        context.addChild (wrapper4);
        context.addServletMapping ("/servlet", "Modern");
        context.addServletMapping ("/session", "Session");
        context.addServletMapping ("/pr", "Pr");
        context.addServletMapping ("/err", "ERR");

        context.getPipeline ().addValve (new ErrorReportValve ());

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
