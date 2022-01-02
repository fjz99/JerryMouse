package com.example.startup;

import com.example.util.ClassLoaderFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.util.ClassLoaderFactory.Repository;
import static com.example.util.ClassLoaderFactory.RepositoryType;

@Slf4j
public final class Bootstrap {

    private static final Pattern PATH_PATTERN = Pattern.compile ("(\"[^\"]*\")|(([^,])*)");
    private static final String catalina = "com.example.startup.Catalina";

    private static final File catalinaBaseFile;
    private static final File catalinaHomeFile;
    /**
     * 对应的加载的{@link Catalina}
     * 必须使用反射，因为类加载器不同
     */
    private static Object startupClass;
    private static ClassLoader commonLoader;
    private static ClassLoader catalinaLoader;
    private static ClassLoader sharedLoader;

    static {
        // Will always be non-null
        String userDir = System.getProperty ("user.dir");

        // Home first
        String home = System.getProperty (Constants.CATALINA_HOME_PROP);
        File homeFile = null;

        if (home != null) {
            File f = new File (home);
            try {
                homeFile = f.getCanonicalFile ();
            } catch (IOException ioe) {
                homeFile = f.getAbsoluteFile ();
            }
        }

        if (homeFile == null) {
            // First fall-back. See if current directory is a bin directory
            // in a normal Tomcat install
            File bootstrapJar = new File (userDir, "bootstrap.jar");

            if (bootstrapJar.exists ()) {
                File f = new File (userDir, "..");
                try {
                    homeFile = f.getCanonicalFile ();
                } catch (IOException ioe) {
                    homeFile = f.getAbsoluteFile ();
                }
            }
        }

        if (homeFile == null) {
            // Second fall-back. Use current directory
            File f = new File (userDir);
            try {
                homeFile = f.getCanonicalFile ();
            } catch (IOException ioe) {
                homeFile = f.getAbsoluteFile ();
            }
        }

        catalinaHomeFile = homeFile;
        System.setProperty (
                Constants.CATALINA_HOME_PROP, catalinaHomeFile.getPath ());

        // Then base
        String base = System.getProperty (Constants.CATALINA_BASE_PROP);
        if (base == null) {
            catalinaBaseFile = catalinaHomeFile;
        } else {
            File baseFile = new File (base);
            try {
                baseFile = baseFile.getCanonicalFile ();
            } catch (IOException ioe) {
                baseFile = baseFile.getAbsoluteFile ();
            }
            catalinaBaseFile = baseFile;
        }
        System.setProperty (
                Constants.CATALINA_BASE_PROP, catalinaBaseFile.getPath ());
    }

    public static void main(String[] args) {
        try {
            init ();
        } catch (Throwable t) {
            t.printStackTrace ();
            System.exit (1);
        }

        try {
            String command = "start";
            if (args.length > 0) {
                command = args[args.length - 1];
            }

            if (command.equals ("start")) {
                start ();
            } else if (command.equals ("stop")) {
                stop ();
            } else {
                log.warn ("Bootstrap: command \"" + command + "\" does not exist.");
            }
        } catch (Throwable e) {
            e.printStackTrace ();
            System.exit (1);
        }
    }

    /**
     * 解析配置的属性，即xxx,xxx
     */
    private static String[] getPaths(String value) {

        List<String> result = new ArrayList<> ();
        Matcher matcher = PATH_PATTERN.matcher (value);

        while (matcher.find ()) {
            String path = value.substring (matcher.start (), matcher.end ());

            path = path.trim ();
            if (path.length () == 0) {
                continue;
            }

            char first = path.charAt (0);
            char last = path.charAt (path.length () - 1);

            if (first == '"' && last == '"' && path.length () > 1) {
                path = path.substring (1, path.length () - 1);
                path = path.trim ();
                if (path.length () == 0) {
                    continue;
                }
            } else if (path.contains ("\"")) {
                // Unbalanced quotes
                // Too early to use standard i18n support. The class path hasn't
                // been configured.
                throw new IllegalArgumentException (
                        "The double quote [\"] character can only be used to quote paths. It must " +
                                "not appear in a path. This loader path is not valid: [" + value + "]");
            }

            result.add (path);
        }
        return result.toArray (new String[0]);
    }

    public static String getCatalinaHome() {
        return catalinaHomeFile.getPath ();
    }

    public static String getCatalinaBase() {
        return catalinaBaseFile.getPath ();
    }

    public static File getCatalinaHomeFile() {
        return catalinaHomeFile;
    }

    public static File getCatalinaBaseFile() {
        return catalinaBaseFile;
    }

    public static void init() throws Exception {
        initClassLoaders ();

        log.debug ("Loading startup class");

        //注意启动时加载Catalina类需要手动指定类加载器，保证使用catalina类加载器加载
        //必须通过反射，不能使用class cast，因为那个class又会变成当前类加载器加载的，类加载器就错了！
        Class<?> aClass = catalinaLoader.loadClass (catalina);
        if (aClass != null) {
            Object instance = aClass.getConstructor ().newInstance ();
            Method method = aClass.getMethod ("setParentClassLoader", ClassLoader.class);
            method.invoke (instance, sharedLoader);
            startupClass = instance;
        }

    }

    public static void start() throws Exception {
        Method method = startupClass.getClass ().getMethod ("start");
        method.invoke (startupClass);
    }

    public static void stop() throws Exception {
        Method method = startupClass.getClass ().getMethod ("stop");
        method.invoke (startupClass);
    }

    private static void initClassLoaders() {
        try {
            commonLoader = createClassLoader ("common", null);
            if (commonLoader == null) {
                // no config file, default to this loader - we might be in a 'single' env.
                //设置成加载这个类的类加载器，而不要直接设置成系统类加载器等，因为不一定相等
                commonLoader = Bootstrap.class.getClassLoader ();
            }
            catalinaLoader = createClassLoader ("server", commonLoader);
            sharedLoader = createClassLoader ("shared", commonLoader);
        } catch (Throwable t) {
            log.error ("Class loader creation threw exception", t);
            System.exit (1);
        }
    }

    /**
     * 如果配置文件里没有，就返回父类加载器
     * 否则就读取配置的仓库，然后创建{@link java.net.URLClassLoader}
     */
    private static ClassLoader createClassLoader(String name, ClassLoader parent)
            throws Exception {

        //TODO 加载配置文件
//        String value = CatalinaProperties.getProperty (name + ".loader");
        String value = null;
        if ((value == null) || (value.equals (""))) {
            return parent;
        }

//        value = replace (value);

        List<Repository> repositories = new ArrayList<> ();

        String[] repositoryPaths = getPaths (value);

        for (String repository : repositoryPaths) {
            // Check for a JAR URL repository
            try {
                @SuppressWarnings("unused")
                URL url = new URL (repository);
                repositories.add (new Repository (repository, RepositoryType.URL));
                continue;
            } catch (MalformedURLException e) {
                // Ignore
            }

            // Local repository
            if (repository.endsWith (".jar")) {
                repositories.add (new Repository (repository, RepositoryType.JAR));
            } else {
                repositories.add (new Repository (repository, RepositoryType.DIR));
            }
        }

        return ClassLoaderFactory.createClassLoader (repositories, parent);
    }

}
