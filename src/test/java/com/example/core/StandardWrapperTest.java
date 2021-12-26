package com.example.core;

import com.example.Context;
import com.example.life.LifecycleException;
import org.junit.jupiter.api.Test;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StandardWrapperTest {
    @Test
    void test() throws ServletException, IOException, LifecycleException {
        StandardWrapper wrapper = new StandardWrapper ();
        Context context = mock (Context.class);
        ClassLoader classLoader = ClassLoader.getSystemClassLoader ();
        when (context.getParentClassLoader ()).thenReturn (classLoader);
        wrapper.setParent (context);
        wrapper.setServletClass ("com.example.core.TestServlet");
        wrapper.start ();

        Servlet servlet = wrapper.allocate ();
        assertTrue (servlet instanceof TestServlet);
        servlet.service (null, null);

        wrapper.stop ();
    }

}
