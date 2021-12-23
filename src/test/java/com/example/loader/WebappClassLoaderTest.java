package com.example.loader;

import com.example.life.LifecycleException;
import com.example.resource.FileDirContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebappClassLoaderTest {
    WebappClassLoader webappClassLoader;

    @BeforeEach
    public void b() throws LifecycleException {
        webappClassLoader = new WebappClassLoader ();
        webappClassLoader.setJarPath (normalize ("/WEB-INF/lib"));
        FileDirContext fileDirContext = new FileDirContext ();
        fileDirContext.setDocBase (normalize ("webapps/testClassLoader"));
        webappClassLoader.setResourceContext (fileDirContext);
        webappClassLoader.start ();
    }

    @Test
    public void testClass() throws ClassNotFoundException, IOException, LifecycleException {
        Class<?> aClass = webappClassLoader.loadClass ("com.example.servlet.ModernServlet");
        Class<?> bClass = webappClassLoader.loadClass ("javax.servlet.http.HttpServlet");
        System.out.println (aClass);
        System.out.println (aClass.getClassLoader ());
        assertEquals (aClass.getClassLoader (), webappClassLoader);
        System.out.println (bClass);
        System.out.println (bClass.getClassLoader ());
        assertEquals (bClass.getClassLoader (), ClassLoader.getSystemClassLoader ());

        //test cache
        webappClassLoader.loadClass ("com.example.servlet.ModernServlet");
    }

    @Test
    public void testJar() throws ClassNotFoundException, IOException, LifecycleException {
        Class<?> aClass = webappClassLoader.loadClass ("org.apache.tools.ant.Executor");
        System.out.println (aClass);
        System.out.println (aClass.getClassLoader ());
        assertEquals (aClass.getClassLoader (), webappClassLoader);
    }

    @Test
    public void testModified() throws ClassNotFoundException, IOException, LifecycleException {
        Class<?> aClass = webappClassLoader.loadClass ("org.apache.tools.ant.Executor");
        System.out.println (aClass);
        System.out.println (aClass.getClassLoader ());
        assertFalse (webappClassLoader.modified ());
    }

    public static String normalize(String p) {
        return new File (p).getPath ();
    }
}
