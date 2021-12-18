package com.example.loader;

import com.example.life.LifecycleException;
import com.example.resource.FileDirContext;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class WebappClassLoaderTest {

    @Test
    public void testClass() throws ClassNotFoundException, IOException, LifecycleException {
        WebappClassLoader webappClassLoader = new WebappClassLoader ();
        webappClassLoader.setJarPath (normalize ("/WEB-INF/lib"));
        FileDirContext fileDirContext = new FileDirContext ();
        fileDirContext.setDocBase (normalize ("webapps/testClassLoader"));
        webappClassLoader.setResourceContext (fileDirContext);
        webappClassLoader.addRepository (normalize ("/WEB-INF/classes"), new File ("webapps/testClassLoader/WEB-INF/classes"));
        File file = new File ("webapps/testClassLoader/WEB-INF/lib/ant-1.6.5.jar");
        webappClassLoader.addJar ("ant-1.6.5.jar", new JarFile (file), file);
        webappClassLoader.start ();

        Class<?> aClass = webappClassLoader.loadClass ("com.example.servlet.ModernServlet", false);
        Class<?> bClass = webappClassLoader.loadClass ("javax.servlet.http.HttpServlet", false);
        System.out.println (aClass);
        System.out.println (aClass.getClassLoader ());
        assertEquals (aClass.getClassLoader (), webappClassLoader);
        System.out.println (bClass);
        System.out.println (bClass.getClassLoader ());
        assertEquals (bClass.getClassLoader (), ClassLoader.getSystemClassLoader ());

        //test cache
        webappClassLoader.loadClass ("com.example.servlet.ModernServlet", false);
    }

    @Test
    public void testJar() throws ClassNotFoundException, IOException, LifecycleException {
        WebappClassLoader webappClassLoader = new WebappClassLoader ();
        webappClassLoader.setJarPath (normalize ("/WEB-INF/lib"));
        FileDirContext fileDirContext = new FileDirContext ();
        fileDirContext.setDocBase (normalize ("webapps/testClassLoader"));
        webappClassLoader.setResourceContext (fileDirContext);
        webappClassLoader.addRepository (normalize ("/WEB-INF/classes"), new File ("webapps/testClassLoader/WEB-INF/classes"));
        File file = new File ("webapps/testClassLoader/WEB-INF/lib/ant-1.6.5.jar");
        webappClassLoader.addJar ("ant-1.6.5.jar", new JarFile (file), file);
        webappClassLoader.start ();

        Class<?> aClass = webappClassLoader.loadClass ("org.apache.tools.ant.Executor");
        System.out.println (aClass);
        System.out.println (aClass.getClassLoader ());
        assertEquals (aClass.getClassLoader (), webappClassLoader);
    }

    private String normalize(String p) {
        return new File (p).getPath ();
    }
}
