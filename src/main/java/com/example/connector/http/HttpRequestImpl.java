package com.example.connector.http;

import com.example.connector.AbstractRequest;
import com.example.connector.Connector;
import com.example.connector.HttpRequest;
import com.example.util.RequestUtil;
import lombok.ToString;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 目前只有1.1版本
 *
 * @date 2021/12/8 20:22
 */
@ToString
public class HttpRequestImpl extends AbstractRequest implements HttpRequest, HttpServletRequest {
    protected final Connector connector;
    protected Map<String, List<String>> headers = new ConcurrentHashMap<> ();
    protected Map<String, Cookie> cookies = new ConcurrentHashMap<> ();
    protected String method;
    protected String queryString;
    protected String contextPath;
    protected String requestURI;
    protected String decodedRequestURI;
    protected String servletPath;
    protected InetAddress inet;
    protected String pathInfo;

    public HttpRequestImpl(Connector connector) {
        this.connector = connector;
    }

    public void recycle() {
        super.recycle ();
        inet = null;
        headers.clear ();
        cookies.clear ();
        method = null;
        queryString = null;
        contextPath = null;
        requestURI = null;
        decodedRequestURI = null;
        servletPath = null;
        pathInfo = null;
    }

    /**
     * [Package Private] Return the InetAddress of the remote client of
     * this request.
     */
    InetAddress getInet() {
        return inet;
    }

    /**
     * [Package Private] Set the InetAddress of the remote client of
     * this request.
     *
     * @param inet The new InetAddress
     */
    void setInet(InetAddress inet) {
        this.inet = inet;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.put (cookie.getName (), cookie);
    }

    @Override
    public void setContentLength(int length) {
        this.contentLength = length;
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
    }

    @Override
    public void addHeader(String name, String value) {
        if (!headers.containsKey (name)) {
            headers.put (name, new ArrayList<> ());
        }
        headers.get (name).add (value);
    }

    @Override
    public void addLocale(Locale locale) {
        synchronized (locales) {
            locales.add (locale);
        }
    }

    /**
     * 会覆盖
     *
     * @param name   Name of this request parameter
     * @param values Corresponding values for this request parameter
     */
    @Override
    public void addParameter(String name, String[] values) {
        parameterMap.put (name, values);
    }

    @Override
    public void clearCookies() {
        cookies.clear ();
    }

    @Override
    public void clearHeaders() {
        headers.clear ();
    }

    @Override
    public void clearLocales() {
        locales.clear ();
    }

    @Override
    public void clearParameters() {
        parameterMap.clear ();
    }

    /**
     * 获得解码之后的uri，即url编码的字符串再解码的结果
     * 是全写如http://ssss/sss/sss ??
     */
    @Override
    public String getDecodedRequestURI() {
        if (decodedRequestURI == null) {
            decodedRequestURI = RequestUtil.URLDecode (getRequestURI ());
        }
        return decodedRequestURI;
    }

    @Override
    public void setDecodedRequestURI(String uri) {
        this.decodedRequestURI = uri;
    }

    /**
     * todo
     * 暂时不实现
     */
    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return cookies.values ().toArray (new Cookie[0]);
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
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
    public Enumeration<String> getHeaders(String name) {
        return Collections.enumeration (new ArrayList<> (headers.get (name)));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration (new ArrayList<> (headers.keySet ()));
    }

    @Override
    public int getIntHeader(String name) {
        List<String> list = headers.get (name);
        if (list == null) {
            return -1;
        } else return Integer.parseInt (list.get (0));
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * 不知道是啥
     */
    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    /**
     * Set the path information for this Request.  This will normally be called
     * when the associated Context is mapping the Request to a particular
     * Wrapper.
     *
     * @param path The path information
     */
    @Override
    public void setPathInfo(String path) {
        this.pathInfo = path;
    }

    /**
     * 不知道是啥
     */
    @Override
    public String getPathTranslated() {
//        if (context == null)
//            return (null);
//
//        if (pathInfo == null)
//            return (null);
//        else
//            return (context.getServletContext ().getRealPath (pathInfo));
        return null;
    }

    /**
     * 不知道是啥
     * 获得区分context的uri，比如/root？？
     */
    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public void setContextPath(String path) {
        this.contextPath = path;
    }

    /**
     * 不知道是啥
     */
    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public void setQueryString(String query) {
        this.queryString = query;
    }

    /**
     * 和认证有关，暂时没有实现
     */
    @Override
    public String getRemoteUser() {
        return null;
    }

    /**
     * 和认证有关，暂时没有实现
     */
    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    /**
     * 和认证有关，暂时没有实现
     */
    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    /**
     * 会话，暂时没有实现 todo
     */
    @Override
    public String getRequestedSessionId() {
        return null;
    }


    @Override
    public String getRequestURI() {
        return requestURI;
    }

    /**
     * 这个要求返回的uri是特殊的。。fixme
     *
     * @param uri The request URI
     */
    @Override
    public void setRequestURI(String uri) {
        this.requestURI = uri;
    }

    /**
     * does not include query string parameters.
     * 不包括请求参数的全写uri
     */
    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer ();
        String scheme = getScheme ();
        int port = getServerPort ();
        if (port < 0)
            port = 80; // Work around java.net.URL bug

        url.append (scheme);
        url.append ("://");
        url.append (getServerName ());
        if ((scheme.equals ("http") && (port != 80))
                || (scheme.equals ("https") && (port != 443))) {
            url.append (':');
            url.append (port);
        }
        url.append (getRequestURI ());

        return url;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public void setServletPath(String path) {
        this.servletPath = path;
    }

    /**
     * 会话，暂时没有实现 todo
     */
    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    /**
     * 会话，暂时没有实现 todo
     */
    @Override
    public HttpSession getSession() {
        return null;
    }

    /**
     * 会话，暂时没有实现 todo
     */
    @Override
    public String changeSessionId() {
        return null;
    }

    /**
     * 会话，暂时没有实现 todo
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    /**
     * 会话，暂时没有实现 todo
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    /**
     * 会话，暂时没有实现 todo
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    /**
     * 会话，暂时没有实现 todo
     */
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    /**
     * 和认证有关，暂时没有实现
     */
    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    /**
     * 和认证有关，暂时没有实现
     */
    @Override
    public void login(String username, String password) throws ServletException {

    }

    /**
     * 和认证有关，暂时没有实现
     */
    @Override
    public void logout() throws ServletException {

    }

    /**
     * 不实现
     */
    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    /**
     * 不实现
     */
    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return null;
    }

    /**
     * 不实现
     */
    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }
}
