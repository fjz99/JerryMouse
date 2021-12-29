package com.example;


import com.example.deploy.ErrorPage;
import com.example.descriptor.FilterDefinition;
import com.example.descriptor.FilterMapping;
import com.example.loader.Loader;
import com.example.resource.AbstractContext;
import com.example.session.Manager;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import java.net.URL;
import java.util.Locale;
import java.util.Set;

/**
 * A <b>Context</b> is a Container that represents a servlet context, and
 * therefore an individual web application, in the Catalina servlet engine.
 * It is therefore useful in almost every deployment of Catalina (even if a
 * Connector attached to a web server (such as Apache) uses the web server's
 * facilities to identify the appropriate Wrapper to handle this request.
 * It also provides a convenient mechanism to use Interceptors that see
 * every request processed by this particular web application.
 * <p>
 * The parent Container attached to a Context is generally a Host, but may
 * be some other implementation, or may be omitted if it is not necessary.
 * <p>
 * The child containers attached to a Context are generally implementations
 * of Wrapper (representing individual servlet definitions).
 * <p>
 *
 * @author Craig R. McClanahan
 */
public interface Context extends Container, ContextBind {


    // ----------------------------------------------------- Manifest Constants

    /**
     * Container event for adding a welcome file.
     */
    String ADD_WELCOME_FILE_EVENT = "addWelcomeFile";

    /**
     * Container event for removing a wrapper.
     */
    String REMOVE_WELCOME_FILE_EVENT = "removeWelcomeFile";

    /**
     * Container event for clearing welcome files.
     */
    String CLEAR_WELCOME_FILES_EVENT = "clearWelcomeFiles";

    /**
     * Container event for changing the ID of a session.
     */
    String CHANGE_SESSION_ID_EVENT = "changeSessionId";


    // ------------------------------------------------------------- Properties

    /**
     * Returns <code>true</code> if requests mapped to servlets without
     * "multipart config" to parse multipart/form-data requests anyway.
     *
     * @return <code>true</code> if requests mapped to servlets without
     * "multipart config" to parse multipart/form-data requests,
     * <code>false</code> otherwise.
     */
    boolean getAllowCasualMultipartParsing();


    /**
     * Set to <code>true</code> to allow requests mapped to servlets that
     * do not explicitly declare @MultipartConfig or have
     * &lt;multipart-config&gt; specified in web.xml to parse
     * multipart/form-data requests.
     *
     * @param allowCasualMultipartParsing <code>true</code> to allow such
     *                                    casual parsing, <code>false</code> otherwise.
     */
    void setAllowCasualMultipartParsing(boolean allowCasualMultipartParsing);


    /**
     * Obtain the registered application event listeners.
     *
     * @return An array containing the application event listener instances for
     * this web application in the order they were specified in the web
     * application deployment descriptor
     */
    Object[] getApplicationEventListeners();


    /**
     * Store the set of initialized application event listener objects,
     * in the order they were specified in the web application deployment
     * descriptor, for this application.
     *
     * @param listeners The set of instantiated listener objects.
     */
    void setApplicationEventListeners(Object[] listeners);


    /**
     * Obtain the registered application lifecycle listeners.
     *
     * @return An array containing the application lifecycle listener instances
     * for this web application in the order they were specified in the
     * web application deployment descriptor
     */
    Object[] getApplicationLifecycleListeners();


    /**
     * Store the set of initialized application lifecycle listener objects,
     * in the order they were specified in the web application deployment
     * descriptor, for this application.
     *
     * @param listeners The set of instantiated listener objects.
     */
    void setApplicationLifecycleListeners(Object[] listeners);


    /**
     * Obtain the character set name to use with the given Locale. Note that
     * different Contexts may have different mappings of Locale to character
     * set.
     *
     * @param locale The locale for which the mapped character set should be
     *               returned
     * @return The name of the character set to use with the given Locale
     */
    String getCharset(Locale locale);


    /**
     * Return the URL of the XML descriptor for this context.
     *
     * @return The URL of the XML descriptor for this context
     */
    URL getConfigFile();


    /**
     * Set the URL of the XML descriptor for this context.
     *
     * @param configFile The URL of the XML descriptor for this context.
     */
    void setConfigFile(URL configFile);


    /**
     * Return the "correctly configured" flag for this Context.
     *
     * @return <code>true</code> if the Context has been correctly configured,
     * otherwise <code>false</code>
     */
    boolean getConfigured();


    /**
     * Set the "correctly configured" flag for this Context.  This can be
     * set to false by startup listeners that detect a fatal configuration
     * error to avoid the application from being made available.
     *
     * @param configured The new correctly configured flag
     */
    void setConfigured(boolean configured);


    /**
     * Return the "use cookies for session ids" flag.
     *忽略，只能是cookie
     *
     * @return <code>true</code> if it is permitted to use cookies to track
     * session IDs for this web application, otherwise
     * <code>false</code>
     */
//    boolean getCookies();


    /**
     * Set the "use cookies for session ids" flag.
     *忽略，只能是cookie
     *
     * @param cookies The new flag
     */
//    void setCookies(boolean cookies);


    /**
     * Gets the name to use for session cookies. Overrides any setting that
     * may be specified by the application.
     *
     * @return The value of the default session cookie name or null if not
     * specified
     */
    String getSessionCookieName();


    /**
     * Sets the name to use for session cookies. Overrides any setting that
     * may be specified by the application.
     *
     * @param sessionCookieName The name to use
     */
    void setSessionCookieName(String sessionCookieName);


    /**
     * Gets the value of the use HttpOnly cookies for session cookies flag.
     *
     * @return <code>true</code> if the HttpOnly flag should be set on session
     * cookies
     */
    boolean getUseHttpOnly();


    /**
     * Sets the use HttpOnly cookies for session cookies flag.
     *
     * @param useHttpOnly Set to <code>true</code> to use HttpOnly cookies
     *                    for session cookies
     */
    void setUseHttpOnly(boolean useHttpOnly);


    /**
     * Gets the domain to use for session cookies. Overrides any setting that
     * may be specified by the application.
     *
     * @return The value of the default session cookie domain or null if not
     * specified
     */
    String getSessionCookieDomain();


    /**
     * Sets the domain to use for session cookies. Overrides any setting that
     * may be specified by the application.
     *
     * @param sessionCookieDomain The domain to use
     */
    void setSessionCookieDomain(String sessionCookieDomain);


    /**
     * Gets the path to use for session cookies. Overrides any setting that
     * may be specified by the application.
     *
     * @return The value of the default session cookie path or null if not
     * specified
     */
    String getSessionCookiePath();


    /**
     * Sets the path to use for session cookies. Overrides any setting that
     * may be specified by the application.
     *
     * @param sessionCookiePath The path to use
     */
    void setSessionCookiePath(String sessionCookiePath);

    /**
     * Return the display name of this web application.
     *
     * @return The display name
     */
    String getDisplayName();


    /**
     * Set the display name of this web application.
     *
     * @param displayName The new display name
     */
    void setDisplayName(String displayName);


    /**
     * Obtain the document root for this Context.
     *
     * @return An absolute pathname or a relative (to the Host's appBase)
     * pathname.
     */
    String getDocBase();


    /**
     * Set the document root for this Context. This can be either an absolute
     * pathname or a relative pathname. Relative pathnames are relative to the
     * containing Host's appBase.
     *
     * @param docBase The new document root
     */
    void setDocBase(String docBase);


    /**
     * Return the URL encoded context path
     *
     * @return The URL encoded (with UTF-8) context path
     */
    String getEncodedPath();


    /**
     * Determine if annotations parsing is currently disabled
     *
     * @return {@code true} if annotation parsing is disabled for this web
     * application
     */
    boolean getIgnoreAnnotations();


    /**
     * Set the boolean on the annotations parsing for this web
     * application.
     *
     * @param ignoreAnnotations The boolean on the annotations parsing
     */
    void setIgnoreAnnotations(boolean ignoreAnnotations);


    /**
     * @return the login configuration descriptor for this web application.
     */
//    LoginConfig getLoginConfig();


    /**
     * Set the login configuration descriptor for this web application.
     *
     * @param config The new login configuration
     */
//    void setLoginConfig(LoginConfig config);


    /**
     * @return the naming resources associated with this web application.
     */
//    NamingResourcesImpl getNamingResources();


    /**
     * Set the naming resources for this web application.
     *
     * @param namingResources The new naming resources
     */
//    void setNamingResources(NamingResourcesImpl namingResources);


    /**
     * @return the context path for this web application.
     */
    String getPath();


    /**
     * Set the context path for this web application.
     *
     * @param path The new context path
     */
    void setPath(String path);


    /**
     * @return the reloadable flag for this web application.
     */
    boolean getReloadable();


    /**
     * Set the reloadable flag for this web application.
     *
     * @param reloadable The new reloadable flag
     */
    void setReloadable(boolean reloadable);


    /**
     * @return the override flag for this web application.
     */
    boolean getOverride();


    /**
     * Set the override flag for this web application.
     *
     * @param override The new override flag
     */
    void setOverride(boolean override);


    /**
     * @return the Servlet context for which this Context is a facade.
     */
    ServletContext getServletContext();


    /**
     * @return the default session timeout (in minutes) for this
     * web application.
     */
    int getSessionTimeout();


    /**
     * Set the default session timeout (in minutes) for this
     * web application.
     *
     * @param timeout The new default session timeout
     */
    void setSessionTimeout(int timeout);


    /**
     * @return the Java class name of the Wrapper implementation used
     * for servlets registered in this Context.
     */
    String getWrapperClass();


    /**
     * Set the Java class name of the Wrapper implementation used
     * for servlets registered in this Context.
     *
     * @param wrapperClass The new wrapper class
     */
    void setWrapperClass(String wrapperClass);


    /**
     * Will the parsing of web.xml and web-fragment.xml files for this Context
     * be performed by a validating parser?
     *
     * @return true if validation is enabled.
     */
    boolean getXmlValidation();


    /**
     * Controls whether the parsing of web.xml and web-fragment.xml files
     * for this Context will be performed by a validating parser.
     *
     * @param xmlValidation true to enable xml validation
     */
    void setXmlValidation(boolean xmlValidation);


    /**
     * Get the Jar Scanner to be used to scan for JAR resources for this
     * context.
     * @return The Jar Scanner configured for this context.
     */
//    JarScanner getJarScanner();

    /**
     * Set the Jar Scanner to be used to scan for JAR resources for this
     * context.
     * @param jarScanner    The Jar Scanner to be used for this context.
     */
//    void setJarScanner(JarScanner jarScanner);

    /**
     * @return the {@link Authenticator} that is used by this context. This is
     *         always non-{@code null} for a started Context
     */
//    Authenticator getAuthenticator();

    /**
     * @return the instance manager associated with this context.
     */
//    InstanceManager getInstanceManager();

    /**
     * Set the instance manager associated with this context.
     *
     * @param instanceManager the new instance manager instance
     */
//    void setInstanceManager(InstanceManager instanceManager);

    /**
     * Obtains the regular expression that specifies which container provided
     * SCIs should be filtered out and not used for this context. Matching uses
     * {@link java.util.regex.Matcher#find()} so the regular expression only has
     * to match a sub-string of the fully qualified class name of the container
     * provided SCI for it to be filtered out.
     *
     * @return The regular expression against which the fully qualified class
     * name of each container provided SCI will be checked
     */
//    String getContainerSciFilter();

    /**
     * Sets the regular expression that specifies which container provided SCIs
     * should be filtered out and not used for this context. Matching uses
     * {@link java.util.regex.Matcher#find()} so the regular expression only has
     * to match a sub-string of the fully qualified class name of the container
     * provided SCI for it to be filtered out.
     *
     * @param containerSciFilter The regular expression against which the fully
     *                           qualified class name of each container provided
     *                           SCI should be checked
     */
//    void setContainerSciFilter(String containerSciFilter);


    // --------------------------------------------------------- Public Methods

    /**
     * Add a new Listener class name to the set of Listeners
     * configured for this application.
     *
     * @param listener Java class name of a listener class
     */
    void addApplicationListener(String listener);


    /**
     * Add a new application parameter for this application.
     *
     * @param parameter The new application parameter
     */
//    void addApplicationParameter(ApplicationParameter parameter);


    /**
     * Add an error page for the specified error or Java exception.
     *
     * @param errorPage The error page definition to be added
     */
    void addErrorPage(ErrorPage errorPage);


    /**
     * Add a filter definition to this Context.
     *
     * @param filterDef The filter definition to be added
     */
    void addFilterDef(FilterDefinition filterDef);


    /**
     * Add a filter mapping to this Context.
     *
     * @param filterMap The filter mapping to be added
     */
    void addFilterMap(FilterMapping filterMap);

    /**
     * Add a filter mapping to this Context before the mappings defined in the
     * deployment descriptor but after any other mappings added via this method.
     *
     * @param filterMap The filter mapping to be added
     * @throws IllegalArgumentException if the specified filter name
     *                                  does not match an existing filter definition, or the filter mapping
     *                                  is malformed
     */
//    void addFilterMapBefore(FilterMapping filterMap);


    /**
     * Add a Locale Encoding Mapping (see Sec 5.4 of Servlet spec 2.4)
     *
     * @param locale   locale to map an encoding for
     * @param encoding encoding to be used for a give locale
     */
    void addLocaleEncodingMappingParameter(String locale, String encoding);


    /**
     * Add a new context initialization parameter, replacing any existing
     * value for the specified name.
     *
     * @param name  Name of the new parameter
     * @param value Value of the new  parameter
     */
    void addParameter(String name, String value);

    /**
     * Add a new servlet mapping, replacing any existing mapping for
     * the specified pattern.
     *
     * @param pattern URL pattern to be mapped. The pattern will be % decoded
     *                using UTF-8
     * @param name    Name of the corresponding servlet to execute
     * @deprecated Will be removed in Tomcat 9. Use
     * {@link #addServletMappingDecoded(String, String)}
     */
    @Deprecated
    void addServletMapping(String pattern, String name);


    /**
     * Add a new servlet mapping, replacing any existing mapping for
     * the specified pattern.
     *
     * @param pattern URL pattern to be mapped
     * @param name    Name of the corresponding servlet to execute
     */
    void addServletMappingDecoded(String pattern, String name);


    /**
     * Add a resource which will be watched for reloading by the host auto
     * deployer. Note: this will not be used in embedded mode.
     *
     * @param name Path to the resource, relative to docBase
     */
    void addWatchedResource(String name);


    /**
     * Add a new welcome file to the set recognized by this Context.
     *
     * @param name New welcome file name
     */
    void addWelcomeFile(String name);


    /**
     * Add the classname of a LifecycleListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a LifecycleListener class
     */
//    void addWrapperLifecycle(String listener);


    /**
     * Add the classname of a ContainerListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a ContainerListener class
     */
//    void addWrapperListener(String listener);


    /**
     * Factory method to create and return a new Wrapper instance, of
     * the Java implementation class appropriate for this Context
     * implementation.  The constructor of the instantiated Wrapper
     * will have been called, but no properties will have been set.
     *
     * @return a newly created wrapper instance that is used to wrap a Servlet
     */
    Wrapper createWrapper();


    /**
     * @return the set of application listener class names configured
     * for this application.
     */
    String[] findApplicationListeners();


    /**
     * @return the set of application parameters for this application.
     */
//    ApplicationParameter[] findApplicationParameters();


    /**
     * @return the error page entry for the specified HTTP error code,
     * if any; otherwise return <code>null</code>.
     *
     * @param errorCode Error code to look up
     */
    ErrorPage findErrorPage(int errorCode);


    /**
     * @param exceptionType Exception type to look up
     * @return the error page entry for the specified Java exception type,
     * if any; otherwise return {@code null}.
     * @deprecated Unused. Will be removed in Tomcat 10.
     * Use {@link #findErrorPage(Throwable)} instead.
     */
    @Deprecated
    ErrorPage findErrorPage(String exceptionType);


    /**
     * Find and return the ErrorPage instance for the specified exception's
     * class, or an ErrorPage instance for the closest superclass for which
     * there is such a definition.  If no associated ErrorPage instance is
     * found, return <code>null</code>.
     *
     * @param throwable The exception type for which to find an ErrorPage
     *
     * @return the error page entry for the specified Java exception type,
     *         if any; otherwise return {@code null}.
     */
    ErrorPage findErrorPage(Throwable throwable);


    /**
     * @return the set of defined error pages for all specified error codes
     * and exception types.
     */
    ErrorPage[] findErrorPages();


    /**
     * @param name Name of the parameter to return
     * @return the value for the specified context initialization
     * parameter name, if any; otherwise return <code>null</code>.
     */
    String findParameter(String name);


    /**
     * @return the names of all defined context initialization parameters
     * for this Context.  If no parameters are defined, a zero-length
     * array is returned.
     */
    String[] findParameters();


    /**
     * @param pattern Pattern for which a mapping is requested
     * @return the servlet name mapped by the specified pattern (if any);
     * otherwise return <code>null</code>.
     */
    String findServletMapping(String pattern);


    /**
     * @return the patterns of all defined servlet mappings for this
     * Context.  If no mappings are defined, a zero-length array is returned.
     */
    String[] findServletMappings();


    /**
     * @param status HTTP status code to look up
     * @return the context-relative URI of the error page for the specified
     * HTTP status code, if any; otherwise return <code>null</code>.
     * @deprecated Unused. Will be removed in Tomcat 10.
     * Use {@link #findErrorPage(int)} instead.
     */
    @Deprecated
    String findStatusPage(int status);


    /**
     * @return the set of HTTP status codes for which error pages have
     * been specified.  If none are specified, a zero-length array
     * is returned.
     * @deprecated Unused. Will be removed in Tomcat 10.
     * Use {@link #findErrorPages()} instead.
     */
    @Deprecated
    int[] findStatusPages();


    /**
     * @return the set of watched resources for this Context. If none are
     * defined, a zero length array will be returned.
     */
    String[] findWatchedResources();


    /**
     * @param name Welcome file to verify
     * @return <code>true</code> if the specified welcome file is defined
     * for this Context; otherwise return <code>false</code>.
     */
    boolean findWelcomeFile(String name);


    /**
     * @return the set of welcome files defined for this Context.  If none are
     * defined, a zero-length array is returned.
     */
    String[] findWelcomeFiles();


    /**
     * @return the set of LifecycleListener classes that will be added to
     * newly created Wrappers automatically.
     */
//    String[] findWrapperLifecycles();


    /**
     * @return the set of ContainerListener classes that will be added to
     * newly created Wrappers automatically.
     */
//    String[] findWrapperListeners();


    /**
     * Notify all {@link javax.servlet.ServletRequestListener}s that a request
     * has started.
     *
     * @param request The request object that will be passed to the listener
     * @return <code>true</code> if the listeners fire successfully, else
     * <code>false</code>
     */
    boolean fireRequestInitEvent(ServletRequest request);

    /**
     * Notify all {@link javax.servlet.ServletRequestListener}s that a request
     * has ended.
     *
     * @param request The request object that will be passed to the listener
     * @return <code>true</code> if the listeners fire successfully, else
     * <code>false</code>
     */
    boolean fireRequestDestroyEvent(ServletRequest request);

    /**
     * Reload this web application, if reloading is supported.
     *
     * @throws IllegalStateException if the <code>reloadable</code>
     *                               property is set to <code>false</code>.
     */
    void reload();


    /**
     * Remove the specified application listener class from the set of
     * listeners for this application.
     *
     * @param listener Java class name of the listener to be removed
     */
    void removeApplicationListener(String listener);


    /**
     * Remove the application parameter with the specified name from
     * the set for this application.
     *
     * @param name Name of the application parameter to remove
     */
    void removeApplicationParameter(String name);


    /**
     * Remove the error page for the specified error code or
     * Java language exception, if it exists; otherwise, no action is taken.
     *
     * @param errorPage The error page definition to be removed
     */
    void removeErrorPage(ErrorPage errorPage);


    /**
     * Remove the specified filter definition from this Context, if it exists;
     * otherwise, no action is taken.
     *
     * @param filterDef Filter definition to be removed
     */
    void removeFilterDef(FilterDefinition filterDef);


    /**
     * Remove a filter mapping from this Context.
     *
     * @param filterMap The filter mapping to be removed
     */
    void removeFilterMap(FilterMapping filterMap);

    /**
     * Remove the context initialization parameter with the specified
     * name, if it exists; otherwise, no action is taken.
     *
     * @param name Name of the parameter to remove
     */
    void removeParameter(String name);


    /**
     * Remove any servlet mapping for the specified pattern, if it exists;
     * otherwise, no action is taken.
     *
     * @param pattern URL pattern of the mapping to remove
     */
    void removeServletMapping(String pattern);


    /**
     * Remove the specified watched resource name from the list associated
     * with this Context.
     *
     * @param name Name of the watched resource to be removed
     */
    void removeWatchedResource(String name);


    /**
     * Remove the specified welcome file name from the list recognized
     * by this Context.
     *
     * @param name Name of the welcome file to be removed
     */
    void removeWelcomeFile(String name);


    /**
     * Remove a class name from the set of LifecycleListener classes that
     * will be added to newly created Wrappers.
     *
     * @param listener Class name of a LifecycleListener class to be removed
     */
//    void removeWrapperLifecycle(String listener);


    /**
     * Remove a class name from the set of ContainerListener classes that
     * will be added to newly created Wrappers.
     *
     * @param listener Class name of a ContainerListener class to be removed
     */
//    void removeWrapperListener(String listener);


    /**
     * @param path The path to the desired resource
     * @return the real path for a given virtual path, if possible; otherwise
     * return <code>null</code>.
     */
    String getRealPath(String path);


    /**
     * Add a ServletContainerInitializer instance to this web application.
     *
     * @param sci     The instance to add
     * @param classes The classes in which the initializer expressed an
     *                interest
     */
    void addServletContainerInitializer(
            ServletContainerInitializer sci, Set<Class<?>> classes);


    /**
     * Is this Context paused whilst it is reloaded?
     *
     * @return <code>true</code> if the context has been paused
     */
    boolean getPaused();

    /**
     * Obtains the list of Servlets that expect a resource to be present.
     *
     * @return A comma separated list of Servlet names as used in web.xml
     */
    String getResourceOnlyServlets();

    /**
     * Sets the (comma separated) list of Servlets that expect a resource to be
     * present. Used to ensure that welcome files associated with Servlets that
     * expect a resource to be present are not mapped when there is no resource.
     *
     * @param resourceOnlyServlets The Servlet names comma separated list
     */
    void setResourceOnlyServlets(String resourceOnlyServlets);

    /**
     * Checks the named Servlet to see if it expects a resource to be present.
     *
     * @param servletName Name of the Servlet (as per web.xml) to check
     * @return <code>true</code> if the Servlet expects a resource,
     * otherwise <code>false</code>
     */
    boolean isResourceOnlyServlet(String servletName);

    /**
     * @return the base name to use for WARs, directories or context.xml files
     * for this context.
     */
    String getBaseName();

    /**
     * @return The version of this web application, used to differentiate
     * different versions of the same web application when using parallel
     * deployment. If not specified, defaults to the empty string.
     */
    String getWebappVersion();

    /**
     * Set the version of this web application - used to differentiate
     * different versions of the same web application when using parallel
     * deployment.
     *
     * @param webappVersion The webapp version associated with the context,
     *                      which should be unique
     */
    void setWebappVersion(String webappVersion);


    /**
     * @return if the context is configured to include a response body as
     * part of a redirect response.
     */
    boolean getSendRedirectBody();

    /**
     * Configures if a response body is included when a redirect response is
     * sent to the client.
     *
     * @param enable <code>true</code> to send a response body for redirects
     */
    void setSendRedirectBody(boolean enable);

    /**
     * @return the Loader with which this Context is associated.
     */
    Loader getLoader();

    /**
     * Set the Loader with which this Context is associated.
     *
     * @param loader The newly associated loader
     */
    void setLoader(Loader loader);

    /**
     * @return the Resources with which this Context is associated.
     */
//    WebResourceRoot getResources();

    /**
     * Set the Resources object with which this Context is associated.
     *
     * @param resources The newly associated Resources
     */
//    void setResources(WebResourceRoot resources);

    /**
     * @return the Manager with which this Context is associated.  If there is
     * no associated Manager, return <code>null</code>.
     */
    Manager getManager();


    /**
     * Set the Manager with which this Context is associated.
     *
     * @param manager The newly associated Manager
     */
    void setManager(Manager manager);

//    /**
//     * @return the flag that indicates if /WEB-INF/classes should be treated like
//     * an exploded JAR and JAR resources made available as if they were in a
//     * JAR.
//     */
//    boolean getAddWebinfClassesResources();
//
//    /**
//     * Sets the flag that indicates if /WEB-INF/classes should be treated like
//     * an exploded JAR and JAR resources made available as if they were in a
//     * JAR.
//     *
//     * @param addWebinfClassesResources The new value for the flag
//     */
//    void setAddWebinfClassesResources(boolean addWebinfClassesResources);

    /**
     * Sets the {@link CookieProcessor} that will be used to process cookies
     * for this Context.
     *
     * @param cookieProcessor   The new cookie processor
     *
     * @throws IllegalArgumentException If a {@code null} CookieProcessor is
     *         specified
     */
//    void setCookieProcessor(CookieProcessor cookieProcessor);

    /**
     * @return the {@link CookieProcessor} that will be used to process cookies
     * for this Context.
     */
//    CookieProcessor getCookieProcessor();

    /**
     * Will client provided session IDs be validated (see {@link
     * #setValidateClientProvidedNewSessionId(boolean)}) before use?
     *
     * @return {@code true} if validation will be applied. Otherwise, {@code
     * false}
     */
    boolean getValidateClientProvidedNewSessionId();

    /**
     * When a client provides the ID for a new session, should that ID be
     * validated? The only use case for using a client provided session ID is to
     * have a common session ID across multiple web applications. Therefore,
     * any client provided session ID should already exist in another web
     * application. If this check is enabled, the client provided session ID
     * will only be used if the session ID exists in at least one other web
     * application for the current host. Note that the following additional
     * tests are always applied, irrespective of this setting:
     * <ul>
     * <li>The session ID is provided by a cookie</li>
     * <li>The session cookie has a path of {@code /}</li>
     * </ul>
     *
     * @param validateClientProvidedNewSessionId {@code true} if validation should be applied
     */
    void setValidateClientProvidedNewSessionId(boolean validateClientProvidedNewSessionId);

//    /**
//     * Will HTTP 1.1 and later location headers generated by a call to
//     * {@link javax.servlet.http.HttpServletResponse#sendRedirect(String)} use
//     * relative or absolute redirects.
//     *
//     * @return {@code true} if relative redirects will be used {@code false} if
//     * absolute redirects are used.
//     * @see #setUseRelativeRedirects(boolean)
//     */
//    boolean getUseRelativeRedirects();
//
//    /**
//     * Controls whether HTTP 1.1 and later location headers generated by a call
//     * to {@link javax.servlet.http.HttpServletResponse#sendRedirect(String)}
//     * will use relative or absolute redirects.
//     * <p>
//     * Relative redirects are more efficient but may not work with reverse
//     * proxies that change the context path. It should be noted that it is not
//     * recommended to use a reverse proxy to change the context path because of
//     * the multiple issues it creates.
//     * <p>
//     * Absolute redirects should work with reverse proxies that change the
//     * context path but may cause issues with the
//     * {@link org.apache.catalina.filters.RemoteIpFilter} if the filter is
//     * changing the scheme and/or port.
//     *
//     * @param useRelativeRedirects {@code true} to use relative redirects and
//     *                             {@code false} to use absolute redirects
//     */
//    void setUseRelativeRedirects(boolean useRelativeRedirects);

//    /**
//     * Are paths used in calls to obtain a request dispatcher expected to be
//     * encoded? This applys to both how Tomcat handles calls to obtain a request
//     * dispatcher as well as how Tomcat generates paths used to obtain request
//     * dispatchers internally.
//     *
//     * @return {@code true} if encoded paths will be used, otherwise
//     * {@code false}
//     */
//    boolean getDispatchersUseEncodedPaths();
//
//    /**
//     * Are paths used in calls to obtain a request dispatcher expected to be
//     * encoded? This affects both how Tomcat handles calls to obtain a request
//     * dispatcher as well as how Tomcat generates paths used to obtain request
//     * dispatchers internally.
//     *
//     * @param dispatchersUseEncodedPaths {@code true} to use encoded paths,
//     *                                   otherwise {@code false}
//     */
//    void setDispatchersUseEncodedPaths(boolean dispatchersUseEncodedPaths);

    /**
     * Get the default request body encoding for this web application.
     *
     * @return The default request body encoding
     */
    String getRequestCharacterEncoding();

    /**
     * Set the default request body encoding for this web application.
     *
     * @param encoding The default encoding
     */
    void setRequestCharacterEncoding(String encoding);

    /**
     * Get the default response body encoding for this web application.
     *
     * @return The default response body encoding
     */
    String getResponseCharacterEncoding();

    /**
     * Set the default response body encoding for this web application.
     *
     * @param encoding The default encoding
     */
    void setResponseCharacterEncoding(String encoding);

    /**
     * When returning a context path from {@link
     * javax.servlet.http.HttpServletRequest#getContextPath()}, is it allowed to
     * contain multiple leading '/' characters?
     *
     * @return <code>true</code> if multiple leading '/' characters are allowed,
     * otherwise <code>false</code>
     */
//    boolean getAllowMultipleLeadingForwardSlashInPath();

    /**
     * Configure if, when returning a context path from {@link
     * javax.servlet.http.HttpServletRequest#getContextPath()}, the return value
     * is allowed to contain multiple leading '/' characters.
     *
     * @param allowMultipleLeadingForwardSlashInPath The new value for the flag
     */
//    void setAllowMultipleLeadingForwardSlashInPath(
//            boolean allowMultipleLeadingForwardSlashInPath);

    /**
     * Will Tomcat attempt to create an upload target used by this web
     * application if it does not exist when the web application attempts to use
     * it?
     *
     * @return {@code true} if Tomcat will attempt to create an upload target
     * otherwise {@code false}
     */
    boolean getCreateUploadTargets();

    /**
     * Configure whether Tomcat will attempt to create an upload target used by
     * this web application if it does not exist when the web application
     * attempts to use it.
     *
     * @param createUploadTargets {@code true} if Tomcat should attempt to
     *                            create the upload target, otherwise {@code false}
     */
    void setCreateUploadTargets(boolean createUploadTargets);

    AbstractContext getResources();

    void setResources(AbstractContext resources);

    /**
     * @param filterName Filter name to look up
     * @return the filter definition for the specified filter name, if any;
     * otherwise return <code>null</code>.
     */
    FilterDefinition findFilterDef(String filterName);


    /**
     * @return the set of defined filters for this Context.
     */
    FilterDefinition[] findFilterDefs();


    /**
     * @return the set of filter mappings for this Context.
     */
    FilterMapping[] findFilterMaps();

    /**
     * Find and return the initialized <code>FilterConfig</code> for the
     * specified filter name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the desired filter
     * @return the filter config object
     */
    FilterConfig findFilterConfig(String name);

    boolean isAvailable();

    void setAvailable(boolean available);

    void incrementInProgressAsyncCount();

    void decrementInProgressAsyncCount();

    /**
     * Remove the MIME mapping for the specified extension, if it exists;
     * otherwise, no action is taken.
     *
     * @param extension Extension to remove the mapping for
     */
    void removeMimeMapping(String extension);

    /**
     * @return the MIME type to which the specified extension is mapped,
     * if any; otherwise return <code>null</code>.
     *
     * @param extension Extension to map to a MIME type
     */
    String findMimeMapping(String extension);


    /**
     * @return the extensions for which MIME mappings are defined.  If there
     * are none, a zero-length array is returned.
     */
    String[] findMimeMappings();

    /**
     * Add a new MIME mapping, replacing any existing mapping for
     * the specified extension.
     *
     * @param extension Filename extension being mapped
     * @param mimeType Corresponding MIME type
     */
    void addMimeMapping(String extension, String mimeType);

    /**
     * Return the "use cookies for session ids" flag.
     *
     * @return <code>true</code> if it is permitted to use cookies to track
     *         session IDs for this web application, otherwise
     *         <code>false</code>
     */
    boolean isCookies();


    /**
     * Set the "use cookies for session ids" flag.
     *
     * @param cookies The new flag
     */
    void setCookies(boolean cookies);
}
