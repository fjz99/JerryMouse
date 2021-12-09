package com.example.connector;


import com.example.connector.http.HttpConnector;
import io.netty.buffer.ByteBuf;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;


/**
 * 不区分协议，通用的接口
 * A <b>Request</b> is the Catalina-internal facade for a
 * <code>ServletRequest</code> that is to be processed, in order to
 * produce the corresponding <code>Response</code>.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.5 $ $Date: 2001/08/01 03:04:04 $
 */

public interface Request {


    // ------------------------------------------------------------- Properties


    /**
     * Return the authorization credentials sent with this request.
     */
//    String getAuthorization();


    /**
     * Set the authorization credentials sent with this request.
     *
     * @param authorization The new authorization credentials
     */
//    void setAuthorization(String authorization);


    /**
     * Return the Connector through which this Request was received.
     */
    HttpConnector getConnector();


    /**
     * Set the Connector through which this Request was received.
     *
     * @param httpConnector The new connector
     */
    void setConnector(HttpConnector httpConnector);


    /**
     * Return the Context within which this Request is being processed.
     */
//    Context getContext();


    /**
     * Set the Context within which this Request is being processed.  This
     * must be called as soon as the appropriate Context is identified, because
     * it identifies the value to be returned by <code>getContextPath()</code>,
     * and thus enables parsing of the request URI.
     *
     * @param context The newly associated Context
     */
//    void setContext(Context context);


    /**
     * Return descriptive information about this Request implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
//    String getInfo();


    /**
     * Return the <code>ServletRequest</code> for which this object
     * is the facade.
     */
    ServletRequest getRequest();


    /**
     * Return the Response with which this Request is associated.
     */
    Response getResponse();


    /**
     * Set the Response with which this Request is associated.
     *
     * @param response The new associated response
     */
    void setResponse(Response response);


    /**
     * Return the Socket (if any) through which this Request was received.
     * This should <strong>only</strong> be used to access underlying state
     * information about this Socket, such as the SSLSession associated with
     * an SSLSocket.
     */
//    Socket getSocket();


    /**
     * Set the Socket (if any) through which this Request was received.
     *
     * @param socket The socket through which this request was received
     */
//    void setSocket(Socket socket);


    /**
     * Return the input stream associated with this Request.
     */
    InputStream getStream();


    /**
     * Set the input stream associated with this Request.
     *
     * @param stream The new input stream
     */
    void setStream(InputStream stream);


    /**
     * Return the Wrapper within which this Request is being processed.
     */
//    Wrapper getWrapper();


    /**
     * Set the Wrapper within which this Request is being processed.  This
     * must be called as soon as the appropriate Wrapper is identified, and
     * before the Request is ultimately passed to an application servlet.
     *
     * @param wrapper The newly associated Wrapper
     */
//    void setWrapper(Wrapper wrapper);


    // --------------------------------------------------------- Public Methods


    /**
     * Create and return a ServletInputStream to read the content
     * associated with this Request.
     *
     * @throws IOException if an input/output error occurs
     */
    ServletInputStream createInputStream() throws IOException;

//    void setByteBuf(ByteBuf byteBuf);


    /**
     * Perform whatever actions are required to flush and close the input
     * stream or reader, in a single operation.
     *
     * @throws IOException if an input/output error occurs
     */
    void finishRequest() throws IOException;


    /**
     * Return the object bound with the specified name to the internal notes
     * for this request, or <code>null</code> if no such binding exists.
     *
     * @param name Name of the note to be returned
     */
    Object getNote(String name);


    /**
     * Return an Iterator containing the String names of all notes bindings
     * that exist for this request.
     */
//    Iterator<?> getNoteNames();


    /**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    void recycle();


    /**
     * Remove any object bound to the specified name in the internal notes
     * for this request.
     *
     * @param name Name of the note to be removed
     */
    void removeNote(String name);


//    /**
//     * Set the content length associated with this Request.
//     *
//     * @param length The new content length
//     */
//    void setContentLength(int length);
//
//
//    /**
//     * Set the content type (and optionally the character encoding)
//     * associated with this Request.  For example,
//     * <code>text/html; charset=ISO-8859-4</code>.
//     *
//     * @param type The new content type
//     */
//    void setContentType(String type);


    /**
     * Bind an object to a specified name in the internal notes associated
     * with this request, replacing any existing binding for this name.
     *
     * @param name  Name to which the object should be bound
     * @param value Object to be bound to the specified name
     */
    void setNote(String name, Object value);


    /**
     * Set the protocol name and version associated with this Request.
     *
     * @param protocol Protocol name and version
     */
    void setProtocol(String protocol);


    /**
     * Set the remote IP address associated with this Request.  NOTE:  This
     * value will be used to resolve the value for <code>getRemoteHost()</code>
     * if that method is called.
     *
     * @param remote The remote IP address
     */
    void setRemoteAddress(String remote);


    /**
     * Set the name of the scheme associated with this request.  Typical values
     * are <code>http</code>, <code>https</code>, and <code>ftp</code>.
     *
     * @param scheme The scheme
     */
    void setScheme(String scheme);


    /**
     * Set the value to be returned by <code>isSecure()</code>
     * for this Request.
     *
     * @param secure The new isSecure value
     */
    void setSecure(boolean secure);


    /**
     * Set the name of the server (virtual host) to process this request.
     *
     * @param name The server name
     */
    void setServerName(String name);


    /**
     * Set the port number of the server to process this request.
     *
     * @param port The server port
     */
    void setServerPort(int port);

}
