package com.example;

import lombok.extern.slf4j.Slf4j;

/**
 * Global constants that are applicable to multiple packages within Catalina.
 *
 * @author Craig R. McClanahan
 */
@Slf4j
public final class Globals {

    public static final String JERRY_MOUSE_BASE;
    /**
     * The request attribute under which the original context path is stored
     * on an included dispatcher request.
     */
    public static final String CONTEXT_PATH_ATTR =
            "javax.servlet.include.context_path";
    /**
     * The request attribute under which we forward a Java exception
     * (as an object of type Throwable) to an error page.
     */
    public static final String EXCEPTION_ATTR =
            "javax.servlet.error.exception";
    /**
     * The request attribute under which we forward the request URI
     * (as an object of type String) of the page on which an error occurred.
     */
    public static final String EXCEPTION_PAGE_ATTR =
            "javax.servlet.error.request_uri";
    /**
     * The request attribute under which we forward a Java exception type
     * (as an object of type Class) to an error page.
     */
    public static final String EXCEPTION_TYPE_ATTR =
            "javax.servlet.error.exception_type";
    /**
     * The request attribute under which we forward an HTTP status message
     * (as an object of type STring) to an error page.
     */
    public static final String ERROR_MESSAGE_ATTR =
            "javax.servlet.error.message";
    /**
     * The name of the cookie used to pass the session identifier back
     * and forth with the client.
     */
    public static final String SESSION_COOKIE_NAME = "JSESSIONID";
    /**
     * The name of the path parameter used to pass the session identifier
     * back and forth with the client.
     */
    public static final String SESSION_PARAMETER_NAME = "jsessionid";
    public static final String SERVER_INFO_WITH_NO_VERSION = "JerryMouse";
    public static final String SERVER_INFO = SERVER_INFO_WITH_NO_VERSION + "/0.1.alpha";
    public static final String SERVER_INFO_WITH_AUTHOR = SERVER_INFO + " - by Fjz";
    // ------------------------------------------------- Request attribute names
    public static final String ASYNC_SUPPORTED_ATTR = "org.apache.catalina.ASYNC_SUPPORTED";
    public static final String GSS_CREDENTIAL_ATTR = "org.apache.catalina.realm.GSS_CREDENTIAL";
    /**
     * Request dispatcher state.
     */
    public static final String DISPATCHER_TYPE_ATTR = "org.apache.catalina.core.DISPATCHER_TYPE";
    /**
     * Request dispatcher path.
     */
    public static final String DISPATCHER_REQUEST_PATH_ATTR = "org.apache.catalina.core.DISPATCHER_REQUEST_PATH";
    /**
     * The request attribute under which we store the servlet name on a
     * named dispatcher request.
     */
    public static final String NAMED_DISPATCHER_ATTR = "org.apache.catalina.NAMED";
    /**
     * The request attribute used to expose the current connection ID associated
     * with the request, if any. Used with multiplexing protocols such as
     * HTTTP/2.
     */
    public static final String CONNECTION_ID = "org.apache.coyote.connectionID";
    /**
     * The request attribute used to expose the current stream ID associated
     * with the request, if any. Used with multiplexing protocols such as
     * HTTTP/2.
     */
    public static final String STREAM_ID = "org.apache.coyote.streamID";
    /**
     * The request attribute that is set to {@code Boolean.TRUE} if some request
     * parameters have been ignored during request parameters parsing. It can
     * happen, for example, if there is a limit on the total count of parseable
     * parameters, or if parameter cannot be decoded, or any other error
     * happened during parameter parsing.
     */
    public static final String PARAMETER_PARSE_FAILED_ATTR = "org.apache.catalina.parameter_parse_failed";
    /**
     * The reason that the parameter parsing failed.
     */
    public static final String PARAMETER_PARSE_FAILED_REASON_ATTR = "org.apache.catalina.parameter_parse_failed_reason";
    /**
     * The request attribute that is set to the value of {@code Boolean.TRUE}
     * by the RemoteIpFilter, RemoteIpValve (and other similar components) that identifies
     * a request which been forwarded via one or more proxies.
     */
    public static final String REQUEST_FORWARDED_ATTRIBUTE = "org.apache.tomcat.request.forwarded";
    /**
     * The request attribute under which we store the array of X509Certificate
     * objects representing the certificate chain presented by our client,
     * if any.
     */
    public static final String CERTIFICATES_ATTR = "javax.servlet.request.X509Certificate";
    /**
     * The request attribute under which we store the name of the cipher suite
     * being used on an SSL connection (as an object of type
     * java.lang.String).
     */
    public static final String CIPHER_SUITE_ATTR = "javax.servlet.request.cipher_suite";
    /**
     * The request attribute under which we store the key size being used for
     * this SSL connection (as an object of type java.lang.Integer).
     */
    public static final String KEY_SIZE_ATTR = "javax.servlet.request.key_size";
    /**
     * The request attribute under which we store the session id being used
     * for this SSL connection (as an object of type java.lang.String).
     */
    public static final String SSL_SESSION_ID_ATTR = "javax.servlet.request.ssl_session_id";
    /**
     * The request attribute key for the session manager.
     * This one is a Tomcat extension to the Servlet spec.
     */
    public static final String SSL_SESSION_MGR_ATTR = "javax.servlet.request.ssl_session_mgr";
    /**
     * The subject under which the AccessControlContext is running.
     */
    public static final String SUBJECT_ATTR = "javax.security.auth.subject";


    // ------------------------------------------------- Session attribute names
    /**
     * The servlet context attribute under which we store the alternate
     * deployment descriptor for this web application
     */
    public static final String ALT_DD_ATTR = "org.apache.catalina.deploy.alt_dd";


    // ------------------------------------------ ServletContext attribute names
    /**
     * Name of the ServletContext attribute under which we store the context
     * Realm's CredentialHandler (if both the Realm and the CredentialHandler
     * exist).
     */
    public static final String CREDENTIAL_HANDLER = "org.apache.catalina.CredentialHandler";
    /**
     * The WebResourceRoot which is associated with the context. This can be
     * used to manipulate static files.
     */
    public static final String RESOURCES_ATTR = "org.apache.catalina.resources";
    /**
     * Name of the ServletContext attribute under which we store the web
     * application version string (the text that appears after ## when parallel
     * deployment is used).
     */
    public static final String WEBAPP_VERSION = "org.apache.catalina.webappVersion";


    /**
     * The flag which controls strict servlet specification compliance. Setting
     * this flag to {@code true} will change the defaults for other settings.
     */
    public static final boolean STRICT_SERVLET_COMPLIANCE =
            Boolean.parseBoolean (System.getProperty ("org.apache.catalina.STRICT_SERVLET_COMPLIANCE", "false"));



    /**
     * Has security been turned on?
     */
    public static final boolean IS_SECURITY_ENABLED = (System.getSecurityManager () != null);

    static {
        String temp;
        temp = System.getProperty ("catalina.base");
        if (temp == null) {
            temp = System.getProperty ("user.dir");
            log.warn ("catalina.base == null,fallback to user.dir = " + temp);
        }
        JERRY_MOUSE_BASE = temp;
    }
}
