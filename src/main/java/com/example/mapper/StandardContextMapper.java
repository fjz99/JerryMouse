package com.example.mapper;


import com.example.Container;
import com.example.Mapper;
import com.example.Wrapper;
import com.example.connector.HttpRequest;
import com.example.connector.Request;
import com.example.core.StandardContext;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;


/**
 * Implementation of <code>Mapper</code> for a <code>Context</code>,
 * designed to process HTTP requests.  This mapper selects an appropriate
 * <code>Wrapper</code> based on the request URI included in the request.
 * <p>
 * <b>IMPLEMENTATION NOTE</b>:  This Mapper only works with a
 * <code>StandardContext</code>, because it relies on internal APIs.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.8 $ $Date: 2002/03/14 20:58:24 $
 */
@Slf4j
public final class StandardContextMapper implements Mapper {

    /**
     * The Container with which this Mapper is associated.
     */
    private StandardContext context = null;
    /**
     * The protocol with which this Mapper is associated.
     */
    private String protocol = null;


    /**
     * Return the Container with which this Mapper is associated.
     */
    public Container getContainer() {
        return (context);
    }


    /**
     * Set the Container with which this Mapper is associated.
     *
     * @param container The newly associated Container
     * @throws IllegalArgumentException if this Container is not
     *                                  acceptable to this Mapper
     */
    public void setContainer(Container container) {
        if (!(container instanceof StandardContext))
            throw new IllegalArgumentException ();
        context = (StandardContext) container;
    }


    /**
     * Return the protocol for which this Mapper is responsible.
     */
    public String getProtocol() {
        return (this.protocol);
    }


    /**
     * Set the protocol for which this Mapper is responsible.
     *
     * @param protocol The newly associated protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    /**
     * Return the child Container that should be used to process this Request,
     * based upon its characteristics.  If no such child Container can be
     * identified, return <code>null</code> instead.
     *
     * @param request Request being processed
     * @param update  Update the Request to reflect the mapping selection?
     * @throws IllegalArgumentException if the relative portion of the
     *                                  path cannot be URL decoded
     */
    public Container map(Request request, boolean update) {
        // Has this request already been mapped?
        if (update && (request.getWrapper () != null))
            return (request.getWrapper ());

        // Identify the context-relative URI to be mapped
        String contextPath =
                ((HttpServletRequest) request.getRequest ()).getContextPath ();
        String requestURI = ((HttpRequest) request).getDecodedRequestURI ();
        String relativeURI = requestURI.substring (contextPath.length ());

        log.debug ("Mapping contextPath='" + contextPath +
                "' with requestURI='" + requestURI +
                "' and relativeURI='" + relativeURI + "'");

        // Apply the standard request URI mapping rules from the specification
        Wrapper wrapper = null;
        String servletPath = relativeURI;
        String pathInfo = null;
        String name = null;

        // Rule 1 -- Exact Match
        log.debug ("Trying exact match");
        if (!(relativeURI.equals ("/")))
            name = context.findServletMapping (relativeURI);
        if (name != null)
            wrapper = (Wrapper) context.findChild (name);
        if (wrapper != null) {
            servletPath = relativeURI;
        }

        // Rule 2 -- Prefix Match
        if (wrapper == null) {
            log.debug ("  Trying prefix match");
            servletPath = relativeURI;
            while (true) {
                name = context.findServletMapping (servletPath + "/*");
                if (name != null)
                    wrapper = (Wrapper) context.findChild (name);
                if (wrapper != null) {
                    pathInfo = relativeURI.substring (servletPath.length ());
                    if (pathInfo.length () == 0)
                        pathInfo = null;
                    break;
                }
                int slash = servletPath.lastIndexOf ('/');
                if (slash < 0)
                    break;
                servletPath = servletPath.substring (0, slash);
            }
        }

        // Rule 3 -- Extension Match
        if (wrapper == null) {
            log.debug ("  Trying extension match");
            int slash = relativeURI.lastIndexOf ('/');
            if (slash >= 0) {
                String last = relativeURI.substring (slash);
                int period = last.lastIndexOf ('.');
                if (period >= 0) {
                    String pattern = "*" + last.substring (period);
                    name = context.findServletMapping (pattern);
                    if (name != null)
                        wrapper = (Wrapper) context.findChild (name);
                    if (wrapper != null) {
                        servletPath = relativeURI;
                    }
                }
            }
        }

        // Rule 4 -- Default Match
        if (wrapper == null) {
            log.debug ("  Trying default match");
            name = context.findServletMapping ("/");
            if (name != null)
                wrapper = (Wrapper) context.findChild (name);
            if (wrapper != null) {
                servletPath = relativeURI;
            }
        }

        // Update the Request (if requested) and return this Wrapper
        if (wrapper != null)
            log.debug (" Mapped to servlet '" + wrapper.getName () +
                    "' with servlet path '" + servletPath +
                    "' and path info '" + pathInfo +
                    "' and update=" + update);
        if (update) {
            request.setWrapper (wrapper);
            ((HttpRequest) request).setServletPath (servletPath);
            ((HttpRequest) request).setPathInfo (pathInfo);
        }
        return wrapper;
    }

}
