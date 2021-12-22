package com.example.session;

import com.example.Context;
import com.example.life.LifecycleException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistentManagerTest {
    FileStore fileStore;
    Context context;
    ServletContext servletContext;

    @BeforeEach
    void b() throws IOException {
        context = mock (Context.class);
        servletContext = mock (ServletContext.class);
        when (context.getServletContext ()).thenReturn (servletContext);

        fileStore = new FileStore ();
        String property = System.getProperty ("user.dir");
        File f = new File (property);
        File test = new File (f, "test");
        System.out.println (test.getPath ());
        fileStore.setDir (test.getAbsolutePath ());

        File file = new File (fileStore.getDir ());
        FileUtils.deleteQuietly (file);
    }

    @AfterEach
    void f() {
        File file = new File (fileStore.getDir ());
        FileUtils.deleteQuietly (file);
    }

    @Test
    void noStore() throws LifecycleException {
        PersistentManager manager = new PersistentManager ();
        manager.setContext (context);
        manager.start ();

        manager.processExpires ();
        Session session = manager.createSession (null);
        manager.add (session);

        manager.stop ();

        manager.start ();
        manager.stop ();
    }

    //测试内存的expire检查
    @Test
    void testExpireCheck() throws LifecycleException, InterruptedException {
        PersistentManager manager = new PersistentManager ();
        fileStore.setManager (manager);
        manager.setStore (fileStore);
        manager.setContext (context);
//        manager.setMaxIdleBackup (1);
//        manager.setMaxIdleSwap (1);
//        manager.setMinIdleSwap (1);
        manager.setSessionMaxAliveTime (1);
        manager.start ();

        Session session = manager.createSession (null);
        manager.add (session);
        assertEquals (manager.getSessionCount (), 1);
        Thread.sleep (1200);
        manager.processExpires ();
        assertEquals (manager.getSessionCount (), 0);

        manager.stop ();
    }

    //测试fileStore的expire检查
    @Test
    void testExpireCheckInStore() throws LifecycleException, InterruptedException, IOException {
        PersistentManager manager = new PersistentManager ();
        fileStore.setManager (manager);
        manager.setStore (fileStore);
        manager.setContext (context);
//        manager.setMaxIdleBackup (1);
//        manager.setMaxIdleSwap (1);
//        manager.setMinIdleSwap (1);
        manager.setSessionMaxAliveTime (1);
        manager.start ();

        Session session = manager.createSession (null);
        manager.add (session);
        assertEquals (manager.getSessionCount (), 1);
        manager.swapOut (session);
        assertEquals (manager.getSessionCount (), 0);

        Thread.sleep (1200);
        manager.processExpires ();
        assertNull (manager.findSession (session.getId ()));

        manager.stop ();
    }

    //测试换出
    @Test
    void testSwapOut1() throws LifecycleException, InterruptedException, IOException {
        PersistentManager manager = new PersistentManager ();
        fileStore.setManager (manager);
        manager.setStore (fileStore);
        manager.setContext (context);
//        manager.setMaxIdleBackup (1);
//        manager.setMaxIdleSwap (1);
        manager.setMinIdleSwap (1);
        manager.start ();

        Session session = manager.createSession (null);
        manager.add (session);
        assertEquals (manager.getSessionCount (), 1);
        Thread.sleep (1200);
        manager.processExpires ();
        assertEquals (manager.getSessionCount (), 0);

        manager.stop ();
    }

    //测试换出
    @Test
    void testSwapOut2() throws LifecycleException, InterruptedException, IOException {
        PersistentManager manager = new PersistentManager ();
        fileStore.setManager (manager);
        manager.setStore (fileStore);
        manager.setContext (context);
//        manager.setMaxIdleBackup (1);
        manager.setMaxIdleSwap (1);
//        manager.setMinIdleSwap (1);
        manager.start ();

        Session session = manager.createSession (null);
        manager.add (session);
        assertEquals (manager.getSessionCount (), 1);
        Thread.sleep (1200);
        manager.processExpires ();
        assertEquals (manager.getSessionCount (), 0);

        manager.stop ();
    }

    //测试备份
    @Test
    void testSwapBackup() throws LifecycleException, InterruptedException, IOException {
        PersistentManager manager = new PersistentManager ();
        fileStore.setManager (manager);
        manager.setStore (fileStore);
        manager.setContext (context);
        manager.setMaxIdleBackup (1);
//        manager.setMaxIdleSwap (1);
//        manager.setMinIdleSwap (1);
        manager.start ();

        Session session = manager.createSession (null);
        manager.add (session);
        assertEquals (manager.getSessionCount (), 1);
        Thread.sleep (1200);
        manager.processExpires ();
        assertEquals (manager.getSessionCount (), 1);
        assertEquals (fileStore.getSize (), 1);

        manager.remove (session);
        assertEquals (manager.getSessionCount (), 0);
        assertEquals (fileStore.getSize (), 0);

        manager.stop ();
    }
}
