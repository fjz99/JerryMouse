package com.example.connector;


import com.example.connector.Request;
import com.example.session.Session;

import javax.servlet.http.Cookie;
import java.util.Locale;
import java.util.Map;


/**
 * An <b>HttpRequest</b> is the Catalina internal facade for an
 * <code>HttpServletRequest</code> that is to be processed, in order to
 * produce the corresponding <code>HttpResponse</code>.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.5 $ $Date: 2002/03/14 20:57:20 $
 */
public interface HttpRequest extends Request {

    Session getSessionInternal(boolean create);



    /**
     * Add a Cookie to the set of Cookies associated with this Request.
     *
     * @param cookie The new cookie
     */
    void addCookie(Cookie cookie);

    /**
     * Set the content length associated with this Request.
     *
     * @param length The new content length
     */
    void setContentLength(int length);


    /**
     * Set the content type (and optionally the character encoding)
     * associated with this Request.  For example,
     * <code>text/html; charset=ISO-8859-4</code>.
     *
     * @param type The new content type
     */
    void setContentType(String type);

    /**
     * Add a Header to the set of Headers associated with this Request.
     *
     * @param name  The new header name
     * @param value The new header value
     */
    void addHeader(String name, String value);


    /**
     * Add a Locale to the set of preferred Locales for this Request.  The
     * first added Locale will be the first one returned by getLocales().
     *
     * @param locale The new preferred Locale
     */
    void addLocale(Locale locale);


    /**
     * Add a parameter name and corresponding set of values to this Request.
     * (This is used when restoring the original request on a form based
     * login).
     *
     * @param name   Name of this request parameter
     * @param values Corresponding values for this request parameter
     */
    void addParameter(String name, String[] values);


    /**
     * Clear the collection of Cookies associated with this Request.
     */
    void clearCookies();


    /**
     * Clear the collection of Headers associated with this Request.
     */
    void clearHeaders();


    /**
     * Clear the collection of Locales associated with this Request.
     */
    void clearLocales();


    /**
     * Clear the collection of parameters associated with this Request.
     */
    void clearParameters();


    /**
     * Set the authentication type used for this request, if any; otherwise
     * set the type to <code>null</code>.  Typical values are "BASIC",
     * "DIGEST", or "SSL".
     *
     * @param type The authentication type used
     */
//    void setAuthType(String type);


    /**
     * Set the context path for this Request.  This will normally be called
     * when the associated Context is mapping the Request to a particular
     * Wrapper.
     *
     * @param path The context path
     */
    void setContextPath(String path);


    /**
     * Set the HTTP request method used for this Request.
     *
     * @param method The request method
     */
    void setMethod(String method);


    /**
     * Set the query string for this Request.  This will normally be called
     * by the HTTP Connector, when it parses the request headers.
     *
     * @param query The query string
     */
    void setQueryString(String query);


    /**
     * Set the path information for this Request.  This will normally be called
     * when the associated Context is mapping the Request to a particular
     * Wrapper.
     *
     * @param path The path information
     */
    void setPathInfo(String path);


    /**
     * Set a flag indicating whether or not the requested session ID for this
     * request came in through a cookie.  This is normally called by the
     * HTTP Connector, when it parses the request headers.
     *
     * @param flag The new flag
     */
//    void setRequestedSessionCookie(boolean flag);


    /**
     * Set the requested session ID for this request.  This is normally called
     * by the HTTP Connector, when it parses the request headers.
     *
     * @param id The new session id
     */
//    void setRequestedSessionId(String id);


    /**
     * Set a flag indicating whether or not the requested session ID for this
     * request came in through a URL.  This is normally called by the
     * HTTP Connector, when it parses the request headers.
     *
     * @param flag The new flag
     */
//    void setRequestedSessionURL(boolean flag);


    /**
     * Set the unparsed request URI for this Request.  This will normally be
     * called by the HTTP Connector, when it parses the request headers.
     *
     * @param uri The request URI
     */
    void setRequestURI(String uri);

    /**
     * Get the decoded request URI.
     *
     * @return the URL decoded request URI
     */
    String getDecodedRequestURI();

    /**
     * Set the decoded request URI.
     *
     * @param uri The decoded request URI
     */
    void setDecodedRequestURI(String uri);

    /**
     * Set the servlet path for this Request.  This will normally be called
     * when the associated Context is mapping the Request to a particular
     * Wrapper.
     *
     * @param path The servlet path
     */
    void setServletPath(String path);


    /**
     * Set the Principal who has been authenticated for this Request.  This
     * value is also used to calculate the value to be returned by the
     * <code>getRemoteUser()</code> method.
     *
     * @param principal The user Principal
     */
//    void setUserPrincipal(Principal principal);


}
