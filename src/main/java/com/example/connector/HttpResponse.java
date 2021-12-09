package com.example.connector;


import com.example.connector.Response;

import javax.servlet.http.Cookie;
import java.util.Collection;
import java.util.Collections;


/**
 * An <b>HttpResponse</b> is the Catalina-internal facade for an
 * <code>HttpServletResponse</code> that is to be produced,
 * based on the processing of a corresponding <code>HttpRequest</code>.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.5 $ $Date: 2001/07/22 20:13:30 $
 */

public interface HttpResponse
        extends Response {


    // --------------------------------------------------------- Public Methods


    /**
     * Return an array of all cookies set for this response, or
     * a zero-length array if no cookies have been set.
     */
    Cookie[] getCookies();


    /**
     * Return the value for the specified header, or <code>null</code> if this
     * header has not been set.  If more than one value was added for this
     * name, only the first is returned; use getHeaderValues() to retrieve all
     * of them.
     *
     * @param name Header name to look up
     */
    String getHeader(String name);


    /**
     * Return an array of all the header names set for this response, or
     * a zero-length array if no headers have been set.
     */
    Collection<String> getHeaderNames();


    /**
     * Return an array of all the header values associated with the
     * specified header name, or an zero-length array if there are no such
     * header values.
     *
     * @param name Header name to look up
     */
    String[] getHeaderValues(String name);


    /**
     * Return the error message that was set with <code>sendError()</code>
     * for this Response.
     */
    String getMessage();


    /**
     * Return the HTTP status code associated with this Response.
     */
    int getStatus();


    /**
     * Reset this response, and specify the values for the HTTP status code
     * and corresponding message.
     *
     * @throws IllegalStateException if this response has already been
     *                               committed
     */
    void reset(int status, String message);

}
