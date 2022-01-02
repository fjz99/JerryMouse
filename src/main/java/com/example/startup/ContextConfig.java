package com.example.startup;


import com.example.Container;
import com.example.Context;
import com.example.core.StandardContext;
import com.example.deploy.ErrorPage;
import com.example.descriptor.FilterDefinition;
import com.example.descriptor.FilterMapping;
import com.example.life.EventType;
import com.example.life.LifecycleEvent;
import com.example.life.LifecycleListener;
import com.example.resource.AbstractContext;
import com.example.resource.FileDirContext;
import com.example.startup.rule.TldRuleSet;
import com.example.startup.rule.WebRuleSet;
import org.apache.commons.digester.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public final class ContextConfig implements LifecycleListener {

//    private static final Logger log = LoggerFactory.getLogger (Constants.ParserLogName);

    private static final Logger log = LoggerFactory.getLogger (ContextConfig.class);

    /**
     * The <code>Digester</code> we will use to process tag library
     * descriptor files.
     */
    private static final Digester tldDigester = createTldDigester ();
    /**
     * The <code>Digester</code> we will use to process web application
     * deployment descriptor files.
     */
    private static final Digester webDigester = createWebDigester ();
    /**
     * The Context we are associated with.
     */
    private Context context = null;
    /**
     * Track any fatal errors during startup configuration processing.
     */
    private boolean ok = false;


    /**
     * Create (if necessary) and return a Digester configured to process a tag
     * library descriptor, looking for additional listener classes to be
     * registered.
     */
    private static Digester createTldDigester() {
        Digester tldDigester = new Digester ();
        tldDigester.setValidating (true);
        tldDigester.addRuleSet (new TldRuleSet ());
        return (tldDigester);
    }

    /**
     * Create (if necessary) and return a Digester configured to process the
     * web application deployment descriptor (web.xml).
     */
    private static Digester createWebDigester() {
        URL url;
        Digester webDigester = new Digester ();
        webDigester.setValidating (true);
        url = ContextConfig.class.getResource (Constants.WebDtdResourcePath_22);
        webDigester.register (Constants.WebDtdPublicId_22, url.toString ());
        url = ContextConfig.class.getResource (Constants.WebDtdResourcePath_23);
        webDigester.register (Constants.WebDtdPublicId_23, url.toString ());
        webDigester.addRuleSet (new WebRuleSet ());
        return (webDigester);
    }


    /**
     * Process the START event for an associated Context.
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        // Identify the context we are associated with
        try {
            context = (Context) event.getLifecycle ();
        } catch (ClassCastException e) {
            log.error ("contextConfig.cce", e);
            return;
        }

        // Process the event that has occurred
        if (event.getType ().equals (EventType.START_EVENT))
            start ();
        else if (event.getType ().equals (EventType.STOP_EVENT))
            stop ();

    }

    /**
     * Process the application configuration file, if it exists.
     */
    private void applicationConfig() {

        // Open the application web.xml file, if it exists
        InputStream stream = null;
        ServletContext servletContext = context.getServletContext ();
        if (servletContext != null)
            stream = servletContext.getResourceAsStream (Constants.ApplicationWebXml);
        if (stream == null) {
//            standardLog.debug ("contextConfig.applicationMissing");
            log.debug ("contextConfig.applicationMissing");
            return;
        }

        // Process the application web.xml file
        synchronized (webDigester) {
            try {
                URL url = servletContext.getResource (Constants.ApplicationWebXml);

                InputSource is = new InputSource (url.toExternalForm ());
                is.setByteStream (stream);
                webDigester.setDebug (0);
                if (context instanceof StandardContext) {
                    ((StandardContext) context).setReplaceWelcomeFiles (true);
                }
                webDigester.clear ();
                webDigester.push (context);
                webDigester.parse (is);
            } catch (SAXParseException e) {
                log.error ("contextConfig.applicationParse", e);
                log.error ("contextConfig.applicationPosition " + "" + e.getLineNumber () + "" + e.getColumnNumber ());
                ok = false;
            } catch (Exception e) {
                log.error ("contextConfig.applicationParse", e);
                ok = false;
            } finally {
                try {
                    stream.close ();
                } catch (IOException e) {
                    log.error ("contextConfig.applicationClose", e);
                }
            }
        }

    }

//    /**
//     * Set up an Authenticator automatically if required, and one has not
//     * already been configured.
//     */
//    private synchronized void authenticatorConfig() {
//
//        // Does this Context require an Authenticator?
//        SecurityConstraint[] constraints = context.findConstraints ();
//        if ((constraints == null) || (constraints.length == 0))
//            return;
//        LoginConfig loginConfig = context.getLoginConfig ();
//        if (loginConfig == null) {
//            loginConfig = new LoginConfig ("NONE", null, null, null);
//            context.setLoginConfig (loginConfig);
//        }
//
//        // Has an authenticator been configured already?
//        if (context instanceof Authenticator)
//            return;
//        if (context instanceof AbstractContainer) {
//            Pipeline pipeline = ((AbstractContainer) context).getPipeline ();
//            if (pipeline != null) {
//                Valve basic = pipeline.getBasic ();
//                if ((basic instanceof Authenticator))
//                    return;
//                Valve[] valves = pipeline.getValves ();
//                for (int i = 0; i < valves.length; i++) {
//                    if (valves[i] instanceof Authenticator)
//                        return;
//                }
//            }
//        } else {
//            return;     // Cannot install a Valve even if it would be needed
//        }
//
//        // Has a Realm been configured for us to authenticate against?
//        if (context.getRealm () == null) {
//            log (sm.getString ("contextConfig.missingRealm"));
//            ok = false;
//            return;
//        }
//
//        // Load our mapping properties if necessary
//        if (authenticators == null) {
//            try {
//                authenticators = ResourceBundle.getBundle
//                        ("org.apache.catalina.startup.Authenticators");
//            } catch (MissingResourceException e) {
//                log (sm.getString ("contextConfig.authenticatorResources"), e);
//                ok = false;
//                return;
//            }
//        }
//
//        // Identify the class name of the Valve we should configure
//        String authenticatorName = null;
//        try {
//            authenticatorName =
//                    authenticators.getString (loginConfig.getAuthMethod ());
//        } catch (MissingResourceException ignored) {
//
//        }
//        if (authenticatorName == null) {
//            log (sm.getString ("contextConfig.authenticatorMissing",
//                    loginConfig.getAuthMethod ()));
//            ok = false;
//            return;
//        }
//
//        // Instantiate and install an Authenticator of the requested class
//        Valve authenticator = null;
//        try {
//            Class authenticatorClass = Class.forName (authenticatorName);
//            authenticator = (Valve) authenticatorClass.newInstance ();
//            if (context instanceof AbstractContainer) {
//                Pipeline pipeline = ((AbstractContainer) context).getPipeline ();
//                if (pipeline != null) {
//                    ((AbstractContainer) context).addValve (authenticator);
//                    log (sm.getString ("contextConfig.authenticatorConfigured",
//                            loginConfig.getAuthMethod ()));
//                }
//            }
//        } catch (Throwable t) {
//            log (sm.getString ("contextConfig.authenticatorInstantiate",
//                    authenticatorName), t);
//            ok = false;
//        }
//
//    }

    /**
     * Create and deploy a Valve to expose the SSL certificates presented
     * by this client, if any.  If we cannot instantiate such a Valve
     * (because the JSSE classes are not available), silently continue.
     * This is only instantiated for those Contexts being served by
     * a Connector with secure set to true.
     */
    private void certificatesConfig() {

        // Only install this valve if there is a Connector installed
        // which has secure set to true.
//        boolean secure = false;
//        Container container = context.getParent ();
//        if (container instanceof Host) {
//            container = container.getParent ();
//        }
//        if (container instanceof Engine) {
//            Service service = ((Engine) container).getService ();
//            // The service can be null when Tomcat is run in embedded mode
//            if (service == null) {
//                secure = true;
//            } else {
//                Connector[] connectors = service.findConnectors ();
//                for (Connector connector : connectors) {
//                    secure = connector.getSecure ();
//                    if (secure) {
//                        break;
//                    }
//                }
//            }
//        }
//        if (!secure) {
//            return;
//        }
//

        // Instantiate a new CertificatesValve if possible
//        Valve certificates;
//        try {
//            Class<?> clazz = Class.forName ("org.apache.catalina.valves.CertificatesValve");
//            certificates = (Valve) clazz.newInstance ();
//        } catch (Throwable t) {
//            return;     // Probably JSSE classes not present
//        }

        // Add this Valve to our Pipeline
//        try {
//            if (context instanceof AbstractContainer) {
//                Pipeline pipeline = context.getPipeline ();
//                if (pipeline != null) {
//                    context.getPipeline ().addValve (certificates);
//                    log.debug (sm.getString ("contextConfig.certificatesConfig.added"));
//                }
//            }
//        } catch (Throwable t) {
//            log.error (sm.getString ("contextConfig.certificatesConfig.error"), t);
//            ok = false;
//        }

    }

    /**
     * Process the default configuration file, if it exists.
     * 解析默认配置文件,conf/web.xml
     */
    private void defaultConfig() {

        File file = new File (Constants.DefaultWebXml);
        if (!file.isAbsolute ())
            file = new File (System.getProperty ("catalina.base"), Constants.DefaultWebXml);
        FileInputStream stream;
        try {
            stream = new FileInputStream (file.getCanonicalPath ());
            stream.close ();
            stream = null;
        } catch (FileNotFoundException e) {
            log.error ("contextConfig.defaultMissing");
            return;
        } catch (IOException e) {
            log.error ("contextConfig.defaultMissing", e);
            return;
        }

        // Process the default web.xml file
        synchronized (webDigester) {
            try {
                InputSource is = new InputSource ("file://" + file.getAbsolutePath ());
                stream = new FileInputStream (file);
                is.setByteStream (stream);
                webDigester.setDebug (0);
                if (context instanceof StandardContext)
                    ((StandardContext) context).setReplaceWelcomeFiles (true);
                webDigester.clear ();
                webDigester.push (context);
                webDigester.parse (is);
            } catch (SAXParseException e) {
                log.error ("contextConfig.defaultParse", e);
                log.error ("contextConfig.defaultPosition" + "" + e.getLineNumber () + "" + e.getColumnNumber ());
                ok = false;
            } catch (Exception e) {
                log.error ("contextConfig.defaultParse", e);
                ok = false;
            } finally {
                try {
                    if (stream != null) {
                        stream.close ();
                    }
                } catch (IOException e) {
                    log.error ("contextConfig.defaultClose", e);
                }
            }
        }

    }

    /**
     * Process a "start" event for this Context.
     */
    synchronized void start() {
        log.debug ("contextConfig.start");
        context.setConfigured (false);
        ok = true;

        // Process the default and application web.xml files
        defaultConfig ();
        applicationConfig ();

        // Scan tag library descriptor files for additional listener classes
        if (ok) {
            try {
                tldScan ();
            } catch (Exception e) {
                log.error (e.getMessage (), e);
                ok = false;
            }
        }

        // Configure a certificates exposer valve, if required
//        if (ok)
//            certificatesConfig ();

        // Configure an authenticator if we need one
//        if (ok)
//            authenticatorConfig ();

        // Dump the contents of this pipeline if requested
//        if (context instanceof AbstractContainer) {
//            log.debug ("Pipline Configuration:");
//            Pipeline pipeline = context.getPipeline ();
//            Valve[] valves = null;
//            if (pipeline != null)
//                valves = pipeline.getValves ();
//            if (valves != null) {
//                for (Valve valve : valves) {
//                    log.debug ("  " + valve);
//                }
//            }
//            log.debug ("======================");
//        }

        // Make our application available if no problems were encountered
        if (ok)
            context.setConfigured (true);
        else {
            log.debug ("contextConfig.unavailable");
            context.setConfigured (false);
        }

    }


    /**
     * Process a "stop" event for this Context.
     */
    synchronized void stop() {
        log.debug ("contextConfig.stop");

        int i;

        // Removing children
        Container[] children = context.findChildren ();
        for (i = 0; i < children.length; i++) {
            context.removeChild (children[i]);
        }

        // Removing application listeners
        String[] applicationListeners = context.findApplicationListeners ();
        for (i = 0; i < applicationListeners.length; i++) {
            context.removeApplicationListener (applicationListeners[i]);
        }

        // Removing application parameters
//        ApplicationParameter[] applicationParameters =
//                context.findApplicationParameters ();
//        for (i = 0; i < applicationParameters.length; i++) {
//            context.removeApplicationParameter
//                    (applicationParameters[i].getName ());
//        }


        // Removing errors pages
        ErrorPage[] errorPages = context.findErrorPages ();
        for (i = 0; i < errorPages.length; i++) {
            context.removeErrorPage (errorPages[i]);
        }

        // Removing filter defs
        FilterDefinition[] filterDefs = context.findFilterDefs ();
        for (i = 0; i < filterDefs.length; i++) {
            context.removeFilterDef (filterDefs[i]);
        }

        // Removing filter maps
        FilterMapping[] filterMaps = context.findFilterMaps ();
        for (i = 0; i < filterMaps.length; i++) {
            context.removeFilterMap (filterMaps[i]);
        }


        // Removing Mime mappings
        String[] mimeMappings = context.findMimeMappings ();
        for (i = 0; i < mimeMappings.length; i++) {
            context.removeMimeMapping (mimeMappings[i]);
        }

        // Removing parameters
        String[] parameters = context.findParameters ();
        for (i = 0; i < parameters.length; i++) {
            context.removeParameter (parameters[i]);
        }

        // Removing servlet mappings
        String[] servletMappings = context.findServletMappings ();
        for (i = 0; i < servletMappings.length; i++) {
            context.removeServletMapping (servletMappings[i]);
        }

        // Removing welcome files
        String[] welcomeFiles = context.findWelcomeFiles ();
        for (i = 0; i < welcomeFiles.length; i++) {
            context.removeWelcomeFile (welcomeFiles[i]);
        }

        ok = true;

    }


    /**
     * Scan for and configure all tag library descriptors found in this
     * web application.
     * 给loader添加repo
     *
     * @throws Exception if a fatal input/output or parsing error occurs
     */
    private void tldScan() throws Exception {

        // Acquire this list of TLD resource paths to be processed
        Set<String> resourcePaths = tldScanResourcePaths ();

        // Scan each accumulated resource paths for TLDs to be processed
        for (String resourcePath : resourcePaths) {
            if (resourcePath.endsWith (".jar")) {
                tldScanJar (resourcePath);
            } else {
                tldScanTld (resourcePath);
            }
        }

    }


    /**
     * Scan the JAR file at the specified resource path for TLDs in the
     * <code>META-INF</code> subdirectory, and scan them for application
     * event listeners that need to be registered.
     *
     * @param resourcePath Resource path of the JAR file to scan
     * @throws Exception if an exception occurs while scanning this JAR
     */
    private void tldScanJar(String resourcePath) throws Exception {
        log.debug (" Scanning JAR at resource path '" + resourcePath + "'");

        JarFile jarFile;
        String name;
        InputStream inputStream;
        try {
            URL url = context.getServletContext ().getResource (resourcePath);
            if (url == null) {
                throw new IllegalArgumentException ("contextConfig.tldResourcePath " + resourcePath);
            }
            url = new URL ("jar:" + url + "!/");
            JarURLConnection conn = (JarURLConnection) url.openConnection ();
            conn.setUseCaches (false);
            jarFile = conn.getJarFile ();
            Enumeration<JarEntry> entries = jarFile.entries ();
            while (entries.hasMoreElements ()) {
                JarEntry entry = entries.nextElement ();
                name = entry.getName ();
                if (!name.startsWith ("META-INF/")) {
                    continue;
                }
                if (!name.endsWith (".tld")) {
                    continue;
                }
                log.debug ("  Processing TLD at '" + name + "'");
                inputStream = jarFile.getInputStream (entry);
                tldScanStream (inputStream);
                inputStream.close ();
            }
        } catch (Exception e) {
            throw new ServletException ("contextConfig.tldJarException", e);
        }

    }


    /**
     * Scan the TLD contents in the specified input stream, and register
     * any application event listeners found there.  <b>NOTE</b> - It is
     * the responsibility of the caller to close the InputStream after this
     * method returns.
     *
     * @param resourceStream InputStream containing a tag library descriptor
     * @throws Exception if an exception occurs while scanning this TLD
     */
    private void tldScanStream(InputStream resourceStream)
            throws Exception {

        synchronized (tldDigester) {
            tldDigester.clear ();
            tldDigester.push (context);
            tldDigester.parse (resourceStream);
        }

    }


    /**
     * Scan the TLD contents at the specified resource path, and register
     * any application event listeners found there.
     *
     * @param resourcePath Resource path being scanned
     * @throws Exception if an exception occurs while scanning this TLD
     */
    private void tldScanTld(String resourcePath) throws Exception {
        log.debug (" Scanning TLD at resource path '" + resourcePath + "'");

        InputStream inputStream = null;
        try {
            inputStream =
                    context.getServletContext ().getResourceAsStream (resourcePath);
            if (inputStream == null) {
                throw new IllegalArgumentException ();
            }
            tldScanStream (inputStream);
            inputStream.close ();
            inputStream = null;
        } catch (Exception e) {
            throw new ServletException ();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close ();
                } catch (Throwable ignored) {

                }
            }
        }

    }


    /**
     * Accumulate and return a Set of resource paths to be analyzed for
     * tag library descriptors.  Each element of the returned set will be
     * the context-relative path to either a tag library descriptor file,
     * or to a JAR file that may contain tag library descriptors in its
     * <code>META-INF</code> subdirectory.
     * 给loader添加repo
     */
    private Set<String> tldScanResourcePaths() {
        log.debug (" Accumulating TLD resource paths");
        Set<String> resourcePaths = new HashSet<> ();

        // Scan TLDs in the /WEB-INF subdirectory of the web application
        log.debug ("Scanning TLDs in /WEB-INF subdirectory");
        AbstractContext resources = context.getResources ();
        Collection<Object> items = resources.list ("/WEB-INF");
        for (Object item : items) {
            String name;
            if (item instanceof FileDirContext.FileResource) {
                name = ((FileDirContext.FileResource) item).getFile ().getName ();
            } else {
                name = ((FileDirContext) item).getFile ().getName ();
            }
            String resourcePath = "/WEB-INF/" + name;
            // scan subdirectories of /WEB-INF for TLDs also
            if (!resourcePath.endsWith (".tld")) {
                continue;
            }
            log.debug ("   Adding path '" + resourcePath + "'");
            resourcePaths.add (resourcePath);
        }

        // Scan JARs in the /WEB-INF/lib subdirectory of the web application
        log.debug ("Scanning JARs in /WEB-INF/lib subdirectory");
        items = resources.list ("/WEB-INF/lib");
        for (Object item : items) {
            String name;
            if (item instanceof FileDirContext.FileResource) {
                name = ((FileDirContext.FileResource) item).getFile ().getName ();
            } else {
                name = ((FileDirContext) item).getFile ().getName ();
            }
            String resourcePath = "/WEB-INF/lib/" + name;
            if (!resourcePath.endsWith (".jar")) {
                continue;
            }
            log.debug ("   Adding path '" + resourcePath + "'");
            resourcePaths.add (resourcePath);
        }

        // Return the completed set
        return resourcePaths;
    }

}
