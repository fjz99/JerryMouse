package com.example.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileDirContextTest {

    @Test
    void testDocBase() {
        FileDirContext fileDirContext = new FileDirContext ();
        assertThrows (IllegalArgumentException.class, () -> fileDirContext.setDocBase ("fff"));
        System.out.println (System.getProperty ("user.dir"));
        fileDirContext.setDocBase ("webapps/testFileDirContext");
    }

    @Test
    void lookup() {
        FileDirContext fileDirContext = new FileDirContext ();
        System.out.println (System.getProperty ("user.dir"));
        fileDirContext.setDocBase ("webapps/testFileDirContext");
        FileDirContext.FileResource lookup = ((FileDirContext.FileResource) fileDirContext.lookup ("abc.txt"));
        System.out.println (lookup);
        System.out.println (fileDirContext.getAttributes ("abc.txt"));
    }

    @Test
    void lookup2() {
        FileDirContext fileDirContext = new FileDirContext ();
        System.out.println (System.getProperty ("user.dir"));
        fileDirContext.setDocBase ("webapps/testFileDirContext");
        FileDirContext.FileResource lookup = ((FileDirContext.FileResource) fileDirContext.lookup ("abc.txt"));
        String absolutePath = lookup.getFile ().getAbsolutePath ();
        Object o = fileDirContext.lookup (absolutePath);
    }

    @Test
    void list() {
        FileDirContext fileDirContext = new FileDirContext ();
        System.out.println (System.getProperty ("user.dir"));
        fileDirContext.setDocBase ("webapps/testFileDirContext");
        for (Object o : fileDirContext.list ("/")) {
            System.out.println (o);
        }
    }
}
