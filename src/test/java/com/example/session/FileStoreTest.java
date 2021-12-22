package com.example.session;

import com.example.Context;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStoreTest {
    static FileStore fileStore;

    static {
        fileStore = new FileStore ();
        String property = System.getProperty ("user.dir");
        File file = new File (property);
        File test = new File (file, "test");
        System.out.println (test.getPath ());
        fileStore.setDir (test.getAbsolutePath ());
    }

    Manager manager;
    Context context;

    @AfterAll
    static void f() throws IOException {
        FileUtils.delete (new File (fileStore.getDir ()));
    }

    @BeforeEach
    void b() {
        manager = mock (Manager.class);
        context = mock (Context.class);
        when (manager.getContext ()).thenReturn (context);
    }

    @Test
    void test() throws IOException, ClassNotFoundException {
        StandardSession session = new StandardSession (manager);
        session.setValid (true);
        session.setId ("112");

        session.putValue ("a", "b");
        fileStore.save (session);
        assertEquals (fileStore.getSize (), 1);
        StandardSession load = (StandardSession) fileStore.load (session.getId ());
        assertNotNull (load);
        assertEquals (load.getAttribute ("a"), "b");

        assertEquals (fileStore.getSize (), 1);
        System.out.println (Arrays.toString (fileStore.keys ()));

        fileStore.clear ();
        assertEquals (fileStore.getSize (), 0);
    }

}
