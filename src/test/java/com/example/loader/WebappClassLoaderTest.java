package com.example.loader;

import com.example.life.LifecycleException;
import com.example.resource.FileDirContext;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class WebappClassLoaderTest {
    WebappClassLoader webappClassLoader;

    public static String normalize(String p) {
        return new File (p).getPath ();
    }

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
    public void testJar() throws ClassNotFoundException, IOException, LifecycleException, InterruptedException {
        Class<?> aClass = webappClassLoader.loadClass ("org.apache.tools.ant.Executor");
        System.out.println (aClass);
        System.out.println (aClass.getClassLoader ());
        assertEquals (aClass.getClassLoader (), webappClassLoader);

        aClass = webappClassLoader.loadClass ("org.apache.tools.ant.FileScanner");
        System.out.println (aClass);
        System.out.println (aClass.getClassLoader ());
        assertEquals (aClass.getClassLoader (), webappClassLoader);
//        Thread.sleep (11111111);
    }

    @Test
    public void testModified() throws ClassNotFoundException, IOException, LifecycleException {
//        Class<?> aClass = webappClassLoader.loadClass ("org.apache.tools.ant.Executor");
//        System.out.println (aClass);
//        System.out.println (aClass.getClassLoader ());
        assertFalse (webappClassLoader.modified ());

        File base = new File ("webapps/testClassLoader/WEB-INF");
        File jar = new File (base, "lib/ant-1.6.5.jar");
        FileUtils.copyFile (new File ("webapps/ant-1.6.5.jar"), jar);
        assertTrue (webappClassLoader.modified ());
        webappClassLoader.stop ();
        webappClassLoader.start ();
        assertTrue (jar.exists ());
        Files.touch (jar);
        assertTrue (webappClassLoader.modified ());
        webappClassLoader.stop ();
        webappClassLoader.start ();
        jar.delete ();
        assertTrue (webappClassLoader.modified ());
        webappClassLoader.stop ();
    }
}
