package com.example.loader;

import java.net.URL;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

/**
 * Resource entry.
 *
 * @author Remy Maucherat
 * @version $Revision: 1.1 $ $Date: 2002/07/22 19:52:40 $
 */
public class ResourceEntry {


    /**
     * The "last modified" time of the origin file at the time this class
     * was loaded, in milliseconds since the epoch.
     */
    public long lastModified = -1;


    /**
     * Binary content of the resource.
     */
    public byte[] binaryContent = null;


    /**
     * Loaded class.
     */
    public volatile Class<?> loadedClass = null;


    /**
     * URL source from where the object was loaded.
     */
    public URL source = null;


    /**
     * URL of the codebase from where the object was loaded.
     */
    public URL codeBase = null;


    /**
     * Manifest (if the resource was loaded from a JAR).
     */
    public Manifest manifest = null;


    /**
     * Certificates (if the resource was loaded from a JAR).
     */
//    public Certificate[] certificates = null;


    @Override
    public String toString() {
        return "ResourceEntry{" +
                "source=" + source +
                '}';
    }
}

