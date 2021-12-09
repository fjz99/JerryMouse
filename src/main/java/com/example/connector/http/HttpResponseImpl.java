package com.example.connector.http;

import com.example.connector.AbstractResponse;
import com.example.connector.HttpResponse;
import io.netty.handler.codec.http.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.connector.http.Constants.DATE_TIME_FORMAT;

/**
 * 目前只有1.1版本
 *
 * @date 2021/12/8 20:26
 */
public class HttpResponseImpl extends AbstractResponse implements HttpResponse, HttpServletResponse {
    protected static final TimeZone zone = TimeZone.getDefault ();
    private static final FullHttpResponse DEFAULT =
            new DefaultFullHttpResponse (HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    protected final SimpleDateFormat format =
            new SimpleDateFormat (DATE_TIME_FORMAT, locale);
    protected Map<String, List<String>> headers = new ConcurrentHashMap<> ();
    protected Map<String, Cookie> cookies = new ConcurrentHashMap<> ();
    protected boolean suspend;
    protected FullHttpResponse fullHttpResponse = DEFAULT.copy ();
    /**
     * err msg
     */
    protected String message;

    protected HttpResponseStatus status;

    @Override
    public Cookie[] getCookies() {
        return cookies.values ().toArray (new Cookie[0]);
    }

    @Override
    public String getHeader(String name) {
        return Optional
                .ofNullable (headers.get (name))
                .map (x -> x.get (0))
                .orElse (null);
    }

    /**
     * Some headers, such as Accept-Language can be sent by
     * clients as several headers each with a different value rather than
     * sending the header as a comma separated list.
     * 即可能header也会重复，也可以用分号分割。。
     */
    @Override
    public Collection<String> getHeaders(String name) {
        return headers.get (name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet ();
    }

    @Override
    public String[] getHeaderValues(String name) {
        return headers.get (name).toArray (new String[]{});
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.put (cookie.getName (), cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey (name);
    }

    /**
     * 没实现 todo
     */
    @Override
    public String encodeURL(String url) {
        return null;
    }

    /**
     * 将sessionId编码到url中
     * Return the specified URL with the specified session identifier
     * suitably encoded.
     *
     * @param url       URL to be encoded with the session id
     * @param sessionId Session id to be included in the encoded URL
     */
    private String toEncoded(String url, String sessionId) {

        if ((url == null) || (sessionId == null))
            return (url);

        String path = url;
        String query = "";
        String anchor = "";
        int question = url.indexOf ('?');
        if (question >= 0) {
            path = url.substring (0, question);
            query = url.substring (question);
        }
        int pound = path.indexOf ('#');
        if (pound >= 0) {
            anchor = path.substring (pound);
            path = path.substring (0, pound);
        }
        StringBuffer sb = new StringBuffer (path);
        if (sb.length () > 0) { // jsessionid can't be first.
            sb.append (";jsessionid=");
            sb.append (sessionId);
        }
        sb.append (anchor);
        sb.append (query);
        return (sb.toString ());

    }

    /**
     * 检查是否需要把sessionId编码到url中
     * 因为sessionId可以从url或者cookie中获得。。
     * <p>
     * Return <code>true</code> if the specified URL should be encoded with
     * a session identifier.  This will be true if all of the following
     * conditions are met:
     * <ul>
     * <li>The request we are responding to asked for a valid session
     * <li>The requested session ID was not received via a cookie
     * <li>The specified URL points back to somewhere within the web
     *     application that is responding to this request
     * </ul>
     *
     * @param location Absolute URL to be validated
     **/
//    private boolean isEncodeable(String location) {
//
//        if (location == null)
//            return (false);
//
//        // Is this an intra-document reference?
//        if (location.startsWith ("#"))
//            return (false);
//
//        // Are we in a valid session that is not using cookies?
//        HttpServletRequest hreq = (HttpServletRequest) request.getRequest ();
//        HttpSession session = hreq.getSession (false);
//        if (session == null)
//            return (false);
//        if (hreq.isRequestedSessionIdFromCookie ())
//            return (false);
//
//        // Is this a valid absolute URL?
//        URL url = null;
//        try {
//            url = new URL (location);
//        } catch (MalformedURLException e) {
//            return (false);
//        }
//
//        // Does this URL match down to (and including) the context path?
//        if (!hreq.getScheme ().equalsIgnoreCase (url.getProtocol ()))
//            return (false);
//        if (!hreq.getServerName ().equalsIgnoreCase (url.getHost ()))
//            return (false);
//        int serverPort = hreq.getServerPort ();
//        if (serverPort == -1) {
//            if ("https".equals (hreq.getScheme ()))
//                serverPort = 443;
//            else
//                serverPort = 80;
//        }
//        int urlPort = url.getPort ();
//        if (urlPort == -1) {
//            if ("https".equals (url.getProtocol ()))
//                urlPort = 443;
//            else
//                urlPort = 80;
//        }
//        if (serverPort != urlPort)
//            return (false);
//
//        String contextPath = getContext ().getPath ();
//        if ((contextPath != null) && (contextPath.length () > 0)) {
//            String file = url.getFile ();
//            if ((file == null) || !file.startsWith (contextPath))
//                return (false);
//            if (file.indexOf (";jsessionid=" + session.getId ()) >= 0)
//                return (false);
//        }
//
//        // This URL belongs to our web application, so it is encodeable
//        return (true);
//
//    }


    /**
     * 产生一个编码后的url
     * todo session相关，未实现
     */
    @Override
    public String encodeRedirectURL(String url) {
//        if (isEncodeable (toAbsolute (url))) {
//            HttpServletRequest hreq =
//                    (HttpServletRequest) request.getRequest ();
//            return (toEncoded (url, hreq.getSession ().getId ()));
//        } else
//            return (url);
        return null;
    }

    /**
     * 没实现 todo
     */
    @Override
    public String encodeUrl(String url) {
        return encodeURL (url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL (url);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
//        HttpResponseStatus.parseLine ()
    }

    //具体由外部包装发送
    @Override
    public void sendError(int sc) throws IOException {
        if (isSuspended ()) {
            throw new IllegalStateException ("suspend");
        }

        status = HttpResponseStatus.valueOf (sc);

        setSuspended (true);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (isSuspended ()) {
            throw new IllegalStateException ("suspend");
        }

        status = HttpResponseStatus.FOUND;//302 本来叫Moved Temporarily
//        fullHttpResponse.setStatus (status);
//        fullHttpResponse.headers ().set ("location", location);


        setSuspended (true);
    }

    @Override
    public void setDateHeader(String name, long date) {
        setHeader (name, format.format (new Date (date)));
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader (name, format.format (new Date (date)));
    }

    @Override
    public void setHeader(String name, String value) {
        ArrayList<String> list = new ArrayList<> ();
        list.add (value);
        headers.put (name, list);
    }

    @Override
    public void addHeader(String name, String value) {
        if (!headers.containsKey (name)) {
            headers.put (name, new ArrayList<> ());
        }
        headers.get (name).add (value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader (name, String.valueOf (value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        addHeader (name, String.valueOf (value));
    }

    @Override
    public void setStatus(int sc, String sm) {
        setStatus (sc);
    }

    @Override
    public int getStatus() {
        return status.code ();
    }

    @Override
    public void setStatus(int sc) {
        status = HttpResponseStatus.valueOf (sc);
    }

    /**
     * Reset this response, and specify the values for the HTTP status code
     * and corresponding message.
     *
     * @throws IllegalStateException if this response has already been
     *                               committed
     */
    @Override
    public void reset(int status, String message) {
        reset ();
        setStatus (status);
        this.message = message;
    }

    /**
     * 啥也不做
     * 等到invoke结束后才发送
     */
    @Override
    public void flushBuffer() throws IOException {
        if (suspend) {
            throw new IOException ("已挂起");
        }

        super.flushBuffer ();

        //todo
    }

    /**
     * 清除，但是不设置为null
     */
    @Override
    public void reset() {
        super.reset ();

        cookies.clear ();
        headers.clear ();
        status = HttpResponseStatus.OK;
        fullHttpResponse = DEFAULT.copy ();
    }


    /**
     * 设置为null
     */
    @Override
    public void recycle() {
        super.recycle ();

        cookies.clear ();
        headers.clear ();
        status = HttpResponseStatus.OK;
        fullHttpResponse = DEFAULT.copy ();
    }
}
