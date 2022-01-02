package com.example.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.List;

import static com.example.util.ClassLoaderFactory.RepositoryType.*;

/**
 * 比较简单，借助于{@link java.net.URLClassLoader}
 * 添加文件夹和url和jar资源
 */
@Slf4j
public final class ClassLoaderFactory {

    /**
     * Create and return a new class loader, based on the configuration
     * defaults and the specified directory paths:
     *
     * @param repositories List of class directories, jar files, jar directories
     *                     or URLS that should be added to the repositories of
     *                     the class loader.
     * @param parent       Parent class loader for the new class loader, or
     *                     <code>null</code> for the system class loader.
     * @return the new class loader
     */
    public static ClassLoader createClassLoader(List<Repository> repositories,
                                                final ClassLoader parent) throws Exception {
        //去重，有序
        LinkedHashSet<URL> set = new LinkedHashSet<> ();

        for (Repository repository : repositories) {
            if (repository.type == URL) {
                URL url = new URL (repository.location);
                set.add (url);
            } else if (repository.type == DIR) {
                File file = new File (repository.location);
                file = file.getCanonicalFile ();
                if (isValid (file, repository.type)) {
                    set.add (file.toURI ().toURL ());
                }
            } else {
                //jar
                File file = new File (repository.location);
                file = file.getCanonicalFile ();
                if (isValid (file, repository.type)) {
                    set.add (file.toURI ().toURL ());
                }
            }
        }

        URL[] array = set.toArray (new URL[0]);
        if (parent == null) {
            return new URLClassLoader (array);
        } else {
            return new URLClassLoader (array, parent);
        }
    }

    private static boolean isValid(File file, RepositoryType type) {
        if (type == JAR) {
            return file.canRead () && file.exists () && file.isFile ();
        } else if (type == DIR) {
            return file.exists () && file.isDirectory () && file.canRead ();
        } else throw new IllegalArgumentException ();
    }


    public enum RepositoryType {
        DIR,
        JAR,
        URL //url资源，甚至可以是网上的jar或者class
    }

    public static class Repository {
        private final String location;
        private final RepositoryType type;

        public Repository(String location, RepositoryType type) {
            this.location = location;
            this.type = type;
        }

        public String getLocation() {
            return location;
        }

        public RepositoryType getType() {
            return type;
        }
    }

}
