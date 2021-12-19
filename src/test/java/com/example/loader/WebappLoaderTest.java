package com.example.loader;

import com.example.Context;
import com.example.life.LifecycleException;
import com.example.resource.FileDirContext;
import org.junit.jupiter.api.Test;

import static com.example.loader.WebappClassLoaderTest.normalize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebappLoaderTest {
    @Test
    public void test() throws LifecycleException, ClassNotFoundException {
        WebappLoader loader = new WebappLoader ();
        FileDirContext fileDirContext = new FileDirContext ();
        fileDirContext.setDocBase (normalize ("webapps/testClassLoader"));
        Context context = mock (Context.class);
        when (context.getResources ()).thenReturn (fileDirContext);
        loader.setContext (context);
        loader.start ();

        ClassLoader webappClassLoader = loader.getClassLoader ();
        Class<?> aClass = webappClassLoader.loadClass ("com.example.servlet.ModernServlet");
        Class<?> bClass = webappClassLoader.loadClass ("javax.servlet.http.HttpServlet");
        System.out.println (aClass);
        System.out.println (aClass.getClassLoader ());
        assertEquals (aClass.getClassLoader (), webappClassLoader);
        System.out.println (bClass);
        System.out.println (bClass.getClassLoader ());
        assertEquals (bClass.getClassLoader (), ClassLoader.getSystemClassLoader ());
        loader.stop ();
    }

}
