package com.example.connector;


import com.example.connector.http.HttpConnector;

import javax.servlet.Servlet;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;


/**
 * 不区分协议，通用的接口
 * ServletResponse等是外部的接口，而这个是内部的接口！
 * <p>
 * A <b>Response</b> is the Catalina-internal facade for a
 * <code>ServletResponse</code> that is to be produced,
 * based on the processing of a corresponding <code>Request</code>.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.7 $ $Date: 2002/03/15 19:12:48 $
 */

public interface Response {


    // ------------------------------------------------------------- Properties


    /**
     * Return the Connector through which this Response is returned.
     */
    HttpConnector getConnector();


    /**
     * Set the Connector through which this Response is returned.
     *
     * @param httpConnector The new connector
     */
    void setConnector(HttpConnector httpConnector);


    /**
     * Return the number of bytes actually written to the output stream.
     */
    int getContentCount();


    /**
     * Return the Context with which this Response is associated.
     */
//    Context getContext();


    /**
     * Set the Context with which this Response is associated.  This should
     * be called as soon as the appropriate Context is identified.
     *
     * @param context The associated Context
     */
//    void setContext(Context context);

    /**
     * Application commit flag accessor.
     */
    boolean isCommitted();

    /**
     * 标志位，指明是否提交响应（即flush）
     * 直接针对底层Stream加以约束即可
     * Set the application commit flag.
     *
     * @param appCommitted The new application committed flag value
     */
    void setCommitted(boolean appCommitted);

    /**
     * Return the "processing inside an include" flag.
     */
//    boolean getIncluded();


    /**
     * Set the "processing inside an include" flag.
     *
     * @param included <code>true</code> if we are currently inside a
     *                 RequestDispatcher.include(), else <code>false</code>
     */
//    void setIncluded(boolean included);


    /**
     * Return descriptive information about this Response implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
//    String getInfo();


    /**
     * Return the Request with which this Response is associated.
     */
    Request getRequest();


    /**
     * Set the Request with which this Response is associated.
     *
     * @param request The new associated request
     */
    void setRequest(Request request);


    /**
     * Return the <code>ServletResponse</code> for which this object
     * is the facade.
     */
//    ServletResponse getResponse();


    /**
     * Return the output stream associated with this Response.
     */
    OutputStream getStream();


    /**
     * Set the output stream associated with this Response.
     *
     * @param stream The new output stream
     */
    void setStream(OutputStream stream);

    /**
     * Suspended flag accessor.
     */
    boolean isSuspended();

    /**
     * 标志位，指明是否挂起，避免sendErr、sendRedirect之后servlet还再调用write等
     * 直接针对底层Stream加以约束即可
     * <p>
     * Set the suspended flag.
     *
     * @param suspended The new suspended flag value
     */
    void setSuspended(boolean suspended);

    /**
     * Set the error flag.
     */
    void setError();


    /**
     * Error flag accessor.
     */
    boolean isError();


    // --------------------------------------------------------- Public Methods


    /**
     * Create and return a ServletOutputStream to write the content
     * associated with this Response.
     *
     * @throws IOException if an input/output error occurs
     */
    ServletOutputStream createOutputStream() throws IOException;


    /**
     * Perform whatever actions are required to flush and close the output
     * stream or writer, in a single operation.
     *
     * @throws IOException if an input/output error occurs
     */
    void finishResponse() throws IOException;


    /**
     * Return the content length that was set or calculated for this Response.
     */
    int getContentLength();


    /**
     * Return the content type that was set or calculated for this response,
     * or <code>null</code> if no content type was set.
     */
    String getContentType();


    /**
     * Return a PrintWriter that can be used to render error messages,
     * regardless of whether a stream or writer has already been acquired.
     *
     * @return Writer which can be used for error reports. If the response is
     * not an error report returned using sendError or triggered by an
     * unexpected exception thrown during the servlet processing
     * (and only in that case), null will be returned if the response stream
     * has already been used.
     */
    PrintWriter getReporter();


    /**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    void recycle();


    /**
     * Reset the data buffer but not any status or header information.
     */
//    void resetBuffer();


    /**
     * Send an acknowledgment of a request.
     *
     * @throws IOException if an input/output error occurs
     */
//    void sendAcknowledgement()
//            throws IOException;

    /**
     * @return 返回facade
     */
    ServletResponse getResponse();
}
