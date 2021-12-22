package com.example.session;

import com.example.Context;
import com.example.life.LifecycleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StandardManagerTest {
    Context context;
    ServletContext servletContext;

    @BeforeEach
    void b() {
        context = mock (Context.class);
        servletContext = mock (ServletContext.class);
        when (context.getServletContext ()).thenReturn (servletContext);
    }

    @Test
    void test() throws LifecycleException {
        StandardManager standardManager = new StandardManager ();
        standardManager.setContext (context);
        Session session = standardManager.createSession (null);
        standardManager.start ();
        standardManager.add (session);
        standardManager.stop ();
        standardManager.start ();
        assertEquals (standardManager.getSessionCount (), 1);
    }
}
