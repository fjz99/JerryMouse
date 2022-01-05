package com.example.util;

import com.example.*;
import com.example.connector.Connector;
import com.example.life.LifecycleListener;
import com.example.loader.Loader;
import com.example.loader.WebappLoader;
import com.example.session.Manager;
import com.example.session.PersistentManager;
import com.example.session.StandardManager;
import com.example.session.Store;
import org.apache.commons.beanutils.PropertyUtils;
import sun.security.krb5.Realm;

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.util.Objects;

/**
 * 自动生成并保存server.xml的工具类
 *
 * @date 2021/12/31 19:08
 */
public final class StoreConfigurationUtil {

    private static final String[][] exceptions = {
            {"org.apache.catalina.core.StandardContext", "available"},
            {"org.apache.catalina.core.StandardContext", "configured"},
            {"org.apache.catalina.core.StandardContext", "distributable"},
            {"org.apache.catalina.core.StandardContext", "name"},
            {"org.apache.catalina.core.StandardContext", "override"},
            {"org.apache.catalina.core.StandardContext", "publicId"},
            {"org.apache.catalina.core.StandardContext", "replaceWelcomeFiles"},
            {"org.apache.catalina.core.StandardContext", "sessionTimeout"},
            {"org.apache.catalina.core.StandardContext", "workDir"},
            {"org.apache.catalina.session.StandardManager", "distributable"},
            {"org.apache.catalina.session.StandardManager", "entropy"},
    };

    /**
     * The set of classes that represent persistable properties.
     */
    private static final Class<?>[] persistables = {
            String.class,
            Integer.class, Integer.TYPE,
            Boolean.class, Boolean.TYPE,
            Byte.class, Byte.TYPE,
            Character.class, Character.TYPE,
            Double.class, Double.TYPE,
            Float.class, Float.TYPE,
            Long.class, Long.TYPE,
            Short.class, Short.TYPE,
    };

    /**
     * The set of class names that should be skipped when persisting state,
     * because the corresponding listeners, valves, etc. are configured
     * automatically at startup time.
     */
    private static final String[] skippables = {
            "org.apache.catalina.authenticator.BasicAuthenticator",
            "org.apache.catalina.authenticator.DigestAuthenticator",
            "org.apache.catalina.authenticator.FormAuthenticator",
            "org.apache.catalina.authenticator.NonLoginAuthenticator",
            "org.apache.catalina.authenticator.SSLAuthenticator",
            "org.apache.catalina.core.NamingContextListener",
            "com.example.valve.basic.StandardContextValve",
            "com.example.valve.basic.StandardEngineValve",
            "com.example.valve.basic.StandardHostValve",
            "org.apache.catalina.startup.ContextConfig",
            "org.apache.catalina.startup.EngineConfig",
            "org.apache.catalina.startup.HostConfig",
            "org.apache.catalina.valves.CertificatesValve",
            "com.example.valve.ErrorDispatcherValve",
            "com.example.valve.valves.ErrorReportValve",
    };

    private final static String SERVER_LISTENER_CLASS_NAME =
            "org.apache.catalina.mbeans.ServerLifecycleListener";

    /**
     * Store the specified Server properties.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param server Object to be stored
     * @throws Exception if an exception occurs while storing
     */
    public static void storeServer(PrintWriter writer, int indent,
                                   Server server) throws Exception {

        // Store the beginning of this element
        writer.println ("<?xml version='1.0' encoding='utf-8'?>");
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Server");
        storeAttributes (writer, server);
        writer.println (">");

        // Store nested <Listener> elements
        if (server != null) {
            LifecycleListener[] listeners = server.findLifecycleListeners ().toArray (new LifecycleListener[0]);
            for (LifecycleListener listener : listeners) {
                storeListener (writer, indent + 2, listener);
            }
        }

        // Store nested <GlobalNamingResources> element
//        NamingResources globalNamingResources =
//                server.getGlobalNamingResources ();
//        if (globalNamingResources != null) {
//            for (int i = 0; i < indent + 2; i++) {
//                writer.print (' ');
//            }
//            writer.println ("<GlobalNamingResources>");
//            storeNamingResources (writer, indent + 4, globalNamingResources);
//            for (int i = 0; i < indent + 2; i++) {
//                writer.print (' ');
//            }
//            writer.println ("</GlobalNamingResources>");
//        }

        // Store nested <Service> elements
        Service[] services = server.findServices ();
        for (Service service : services) {
            storeService (writer, indent + 2, service);
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.println ("</Server>");
    }


    /**
     * Store the specified Service properties.
     *
     * @param writer  PrintWriter to which we are storing
     * @param indent  Number of spaces to indent this element
     * @param service Object to be stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeService(PrintWriter writer, int indent,
                                     Service service) throws Exception {

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Service");
        storeAttributes (writer, service);
        writer.println (">");

        // Store nested <Connector> elements
        Connector[] connectors = service.findConnectors ();
        for (Connector connector : connectors) {
            storeConnector (writer, indent + 2, connector);
        }

        // Store nested <Engine> element (or other appropriate container)
        Engine container = service.getContainer ();
        if (container != null) {
            if (container instanceof Context) {
                storeContext (writer, indent + 2, (Context) container);
            } else {
                storeEngine (writer, indent + 2, container);
            }
        }

        // Store nested <Listener> elements
        LifecycleListener[] listeners = service.findLifecycleListeners ().toArray (new LifecycleListener[0]);
        for (LifecycleListener listener : listeners) {
            if (listener.getClass ().getName ().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                continue;
            }
            storeListener (writer, indent + 2, listener);
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.println ("</Service>");
    }


    /**
     * Store the specified Store properties.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param store  Object whose properties are being stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeStore(PrintWriter writer, int indent,
                                   Store store) throws Exception {
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Store");
        storeAttributes (writer, store);
        writer.println ("/>");
    }


    /**
     * Store the specified Valve properties.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param valve  Object whose properties are being valved
     * @throws Exception if an exception occurs while storing
     */
    private static void storeValve(PrintWriter writer, int indent,
                                   Valve valve) throws Exception {
        if (isSkippable (valve.getClass ().getName ())) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Valve");
        storeAttributes (writer, valve);
        writer.println ("/>");
    }

    /**
     * Is the specified class name one that should be skipped because
     * the corresponding component is configured automatically at
     * startup time?
     *
     * @param className Class name to be tested
     */
    private static boolean isSkippable(String className) {
        for (String skippable : skippables) {
            if (Objects.equals (skippable, className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Store the relevant attributes of the specified JavaBean, plus a
     * <code>className</code> attribute defining the fully qualified
     * Java class name of the bean.
     *
     * @param writer PrintWriter to which we are storing
     * @param bean   Bean whose properties are to be rendered as attributes,
     * @throws Exception if an exception occurs while storing
     */
    private static void storeAttributes(PrintWriter writer,
                                        Object bean) throws Exception {
        storeAttributes (writer, true, bean);
    }

    /**
     * Store the relevant attributes of the specified JavaBean.
     *
     * @param writer  PrintWriter to which we are storing
     * @param include Should we include a <code>className</code> attribute?
     * @param bean    Bean whose properties are to be rendered as attributes,
     * @throws Exception if an exception occurs while storing
     */
    private static void storeAttributes(PrintWriter writer, boolean include,
                                        Object bean) throws Exception {
        // Render a className attribute if requested
        if (include) {
            writer.print (" className=\"");
            writer.print (bean.getClass ().getName ());
            writer.print ("\"");
        }
        // Acquire the list of properties for this bean
        PropertyDescriptor[] descriptors =
                PropertyUtils.getPropertyDescriptors (bean);
        if (descriptors == null) {
            descriptors = new PropertyDescriptor[0];
        }

        // Render the relevant properties of this bean
        String className = bean.getClass ().getName ();
        for (PropertyDescriptor descriptor : descriptors) {
            if (descriptor instanceof IndexedPropertyDescriptor) {
                continue; // Indexed properties are not persisted
            }
            if (!isPersistable (descriptor.getPropertyType ()) ||
                    (descriptor.getReadMethod () == null) ||
                    (descriptor.getWriteMethod () == null)) {
                continue; // Must be a read-write primitive or String
            }
            Object value = PropertyUtils.getSimpleProperty (bean,
                    descriptor.getName ());
            if (value == null) {
                continue; // Null values are not persisted
            }
            if (isException (className, descriptor.getName ())) {
                continue; // Skip the specified exceptions
            }
            if (!(value instanceof String)) {
                value = value.toString ();
            }
            writer.print (' ');
            writer.print (descriptor.getName ());
            writer.print ("=\"");
            String strValue = convertStr ((String) value);
            writer.print (strValue);
            writer.print ("\"");
        }

    }

    /**
     * Store the specified Connector properties.
     *
     * @param writer    PrintWriter to which we are storing
     * @param indent    Number of spaces to indent this element
     * @param connector Object whose properties are being stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeConnector(PrintWriter writer, int indent,
                                       Connector connector) throws Exception {
        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Connector");
        storeAttributes (writer, connector);
        writer.println (">");

        // Store nested <Listener> elements
        if (connector != null) {
            LifecycleListener[] listeners = connector.findLifecycleListeners ().toArray (new LifecycleListener[0]);
            for (LifecycleListener listener : listeners) {
                if (listener.getClass ().getName ().equals
                        (SERVER_LISTENER_CLASS_NAME)) {
                    continue;
                }
                storeListener (writer, indent + 2, listener);
            }
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.println ("</Connector>");
    }

    /**
     * Store the specified Context properties.
     *
     * @param writer  PrintWriter to which we are storing
     * @param indent  Number of spaces to indent this element
     * @param context Object whose properties are being stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeContext(PrintWriter writer, int indent,
                                     Context context) throws Exception {
        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Context");
        storeAttributes (writer, context);
        writer.println (">");

        if (context == null) {
            return;
        }

        // Store nested <Listener> elements
        LifecycleListener[] listeners = context.findLifecycleListeners ().toArray (new LifecycleListener[0]);
        for (LifecycleListener listener : listeners) {
            if (listener.getClass ().getName ().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                continue;
            }
            storeListener (writer, indent + 2, listener);
        }

        // Store nested <Loader> element
        Loader loader = context.getLoader ();
        if (loader != null) {
            storeLoader (writer, indent + 2, loader);
        }

        // Store nested <Manager> element
        Manager manager = context.getManager ();
        if (manager != null) {
            storeManager (writer, indent + 2, manager);
        }


//        // Store nested <Realm> element
//        Realm realm = context.getRealm ();
//        if (realm != null) {
//            Realm parentRealm = null;
//            if (context.getParent () != null) {
//                parentRealm = context.getParent ().getRealm ();
//            }
//            if (realm != parentRealm) {
//                storeRealm (writer, indent + 2, realm);
//            }
//        }


        // Store nested <Valve> elements
        if (context instanceof Pipeline) {
            Valve[] valves = ((Pipeline) context).getValves ();
            for (Valve valve : valves) {
                storeValve (writer, indent + 2, valve);
            }
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.println ("</Context>");
    }

    /**
     * Store the specified Engine properties.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param engine Object whose properties are being stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeEngine(PrintWriter writer, int indent,
                                    Engine engine) throws Exception {

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Engine");
        storeAttributes (writer, engine);
        writer.println (">");


        // Store nested <Host> elements (or other relevant containers)
        Container[] children = engine.findChildren ();
        for (Container child : children) {
            if (child instanceof Context) {
                storeContext (writer, indent + 2, (Context) child);
            } else if (child instanceof Engine) {
                storeEngine (writer, indent + 2, (Engine) child);
            } else if (child instanceof Host) {
                storeHost (writer, indent + 2, (Host) child);
            }
        }

        // Store nested <Listener> elements
        LifecycleListener[] listeners = engine.findLifecycleListeners ().toArray (new LifecycleListener[0]);
        for (LifecycleListener listener : listeners) {
            if (listener.getClass ().getName ().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                continue;
            }
            storeListener (writer, indent + 2, listener);
        }

//        // Store nested <Realm> element
//        Realm realm = engine.getRealm ();
//        if (realm != null) {
//            Realm parentRealm = null;
//            if (engine.getParent () != null) {
//                parentRealm = engine.getParent ().getRealm ();
//            }
//            if (realm != parentRealm) {
//                storeRealm (writer, indent + 2, realm);
//            }
//        }

        // Store nested <Valve> elements
        if (engine instanceof Pipeline) {
            Valve[] valves = ((Pipeline) engine).getValves ();
            for (Valve valve : valves) {
                storeValve (writer, indent + 2, valve);
            }
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.println ("</Engine>");
    }

    /**
     * Store the specified Host properties.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param host   Object whose properties are being stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeHost(PrintWriter writer, int indent,
                                  Host host) throws Exception {

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Host");
        storeAttributes (writer, host);
        writer.println (">");

        // Store nested <Alias> elements
        String[] aliases = host.findAliases ();
        for (String alias : aliases) {
            for (int j = 0; j < indent; j++) {
                writer.print (' ');
            }
            writer.print ("<Alias>");
            writer.print (alias);
            writer.println ("</Alias>");
        }

        // Store nested <Cluster> elements

        // Store nested <Context> elements (or other relevant containers)
        Container[] children = host.findChildren ();
        for (Container child : children) {
            if (child instanceof Context) {
                storeContext (writer, indent + 2, (Context) child);
            } else if (child instanceof Engine) {
                storeEngine (writer, indent + 2, (Engine) child);
            } else if (child instanceof Host) {
                storeHost (writer, indent + 2, (Host) child);
            }
        }


        // Store nested <Listener> elements
        LifecycleListener[] listeners = host.findLifecycleListeners ().toArray (new LifecycleListener[0]);
        for (LifecycleListener listener : listeners) {
            if (listener.getClass ().getName ().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                continue;
            }
            storeListener (writer, indent + 2, listener);
        }

//        // Store nested <Realm> element
//        Realm realm = host.getRealm ();
//        if (realm != null) {
//            Realm parentRealm = null;
//            if (host.getParent () != null) {
//                parentRealm = host.getParent ().getRealm ();
//            }
//            if (realm != parentRealm) {
//                storeRealm (writer, indent + 2, realm);
//            }
//        }

        // Store nested <Valve> elements
        if (host instanceof Pipeline) {
            Valve[] valves = ((Pipeline) host).getValves ();
            for (Valve valve : valves) {
                storeValve (writer, indent + 2, valve);
            }
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.println ("</Host>");

    }

    /**
     * Store the specified Listener properties.
     *
     * @param writer   PrintWriter to which we are storing
     * @param indent   Number of spaces to indent this element
     * @param listener Object whose properties are being stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeListener(PrintWriter writer, int indent,
                                      LifecycleListener listener) throws Exception {

        if (isSkippable (listener.getClass ().getName ())) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Listener");
        storeAttributes (writer, listener);
        writer.println ("/>");

    }

    /**
     * Store the specified Loader properties.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param loader Object whose properties are being stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeLoader(PrintWriter writer, int indent,
                                    Loader loader) throws Exception {

        if (isDefaultLoader (loader)) {
            return;
        }
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Loader");
        storeAttributes (writer, loader);
        writer.println ("/>");

    }

    /**
     * Store the specified Manager properties.
     *
     * @param writer  PrintWriter to which we are storing
     * @param indent  Number of spaces to indent this element
     * @param manager Object whose properties are being stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeManager(PrintWriter writer, int indent,
                                     Manager manager) throws Exception {

        if (isDefaultManager (manager)) {
            return;
        }

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Manager");
        storeAttributes (writer, manager);
        writer.println (">");

        // Store nested <Store> element
        if (manager instanceof PersistentManager) {
            Store store = ((PersistentManager) manager).getStore ();
            if (store != null) {
                storeStore (writer, indent + 2, store);
            }
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.println ("</Manager>");

    }

    /**
     * Store the specified Realm properties.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param realm  Object whose properties are being stored
     * @throws Exception if an exception occurs while storing
     */
    private static void storeRealm(PrintWriter writer, int indent,
                                   Realm realm) throws Exception {

        for (int i = 0; i < indent; i++) {
            writer.print (' ');
        }
        writer.print ("<Realm");
        storeAttributes (writer, realm);
        writer.println ("/>");

    }

    /**
     * Given a string, this method replaces all occurrences of
     * '<', '>', '&', and '"'.
     */
    private static String convertStr(String input) {
        StringBuilder filtered = new StringBuilder (input.length ());
        char c;
        for (int i = 0; i < input.length (); i++) {
            c = input.charAt (i);
            if (c == '<') {
                filtered.append ("&lt;");
            } else if (c == '>') {
                filtered.append ("&gt;");
            } else if (c == '\'') {
                filtered.append ("&apos;");
            } else if (c == '"') {
                filtered.append ("&quot;");
            } else if (c == '&') {
                filtered.append ("&amp;");
            } else {
                filtered.append (c);
            }
        }
        return (filtered.toString ());
    }

    /**
     * Is this an instance of the default <code>Loader</code> configuration,
     * with all-default properties?
     *
     * @param loader Loader to be tested
     */
    private static boolean isDefaultLoader(Loader loader) {
        if (!(loader instanceof WebappLoader)) {
            return (false);
        }
        WebappLoader wloader = (WebappLoader) loader;
        return (!wloader.getDelegate ()) &&
                wloader.getLoaderClass ().equals
                        ("com.example.loader.WebappLoader");
    }

    /**
     * Is this an instance of the default <code>Manager</code> configuration,
     * with all-default properties?
     *
     * @param manager Manager to be tested
     */
    private static boolean isDefaultManager(Manager manager) {
        if (!(manager instanceof StandardManager)) {
            return (false);
        }
        StandardManager smanager = (StandardManager) manager;
        return smanager.getFileName ().equals ("SESSIONS.ser") &&
                (smanager.getProcessExpiresFrequency () == 6) &&
                (smanager.getMaxActiveSessions () == -1);
    }

    /**
     * Is the specified class name + property name combination an
     * exception that should not be persisted?
     *
     * @param className The class name to check
     * @param property  The property name to check
     */
    private static boolean isException(String className, String property) {
        for (String[] exception : exceptions) {
            if (className.equals (exception[0]) &&
                    property.equals (exception[1])) {
                return (true);
            }
        }
        return (false);
    }

    /**
     * Is the specified property type one for which we should generate
     * a persistence attribute?
     *
     * @param clazz Java class to be tested
     */
    private static boolean isPersistable(Class<?> clazz) {
        for (Class<?> persistable : persistables) {
            if (persistable == clazz) {
                return (true);
            }
        }
        return (false);
    }
}
