package com.example.connector.http;

import com.example.Globals;
import com.example.connector.AbstractRequest;
import com.example.connector.Connector;
import com.example.connector.HttpRequest;
import com.example.session.Manager;
import com.example.session.Session;
import com.example.session.StandardSession;
import com.example.session.StandardSessionFacade;
import com.example.util.RequestUtil;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.connector.http.Constants.SET_COOKIE;


/**
 * 目前只有1.1版本
 *
 * @date 2021/12/8 20:22
 */
@Slf4j
public class HttpRequestImpl extends AbstractRequest implements HttpRequest, HttpServletRequest {
    protected final Connector connector;
    protected Map<String, List<String>> headers = new ConcurrentHashMap<> ();
    protected Map<String, Cookie> cookies = new ConcurrentHashMap<> ();
    protected String method;
    /**
     * query
     * xx=xx&xx=xx
     */
    protected String queryString;
    /**
     * context的path，比如某个context是/root
     */
    protected String contextPath;
    /**
     * 请求uri，去除host、port、schema、query
     */
    protected String requestURI;
    /**
     * url解码后的requestURI
     */
    protected String decodedRequestURI;
    /**
     * servlet路径，即去除context path的requestURI？？？
     */
    protected String servletPath;
    protected InetAddress inet;
    protected String pathInfo;
    /**
     * 会话id，会被connector解析器设置，id一般存在于query或者cookie中
     */
    protected String requestedSessionId;
    protected Session session;
    /**
     * id存在于query还是cookie中？
     */
    protected boolean requestedSessionCookie = false;
    protected boolean requestedSessionURL = false;

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
        requestedSessionCookie = false;
        requestedSessionURL = false;
        requestedSessionId = null;
        session = null;
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

    /**
     * 会覆盖
     */
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
     * 如/ssss/sss/sss
     * 不包括schema、host、不包含query
     * 指的是path
     * schema://host[:port#]/path/.../[?query-string][#anchor]
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
     * 会话
     */
    @Override
    public String getRequestedSessionId() {
        return requestedSessionId;
    }

    public void setRequestedSessionId(String requestedSessionId) {
        this.requestedSessionId = requestedSessionId;
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
     * 创建一个会话，注意检查会话超时
     */
    @Override
    public HttpSession getSession(boolean create) {
        if (getContext () == null) {
            return null;
        }

        if (session != null && session.isValid ()) {
            return session.getSession ();
        } else {
            session = null;
        }

        Manager manager = getContext ().getManager ();
        if (manager == null) {
            return null;
        }

        if (requestedSessionId != null) {
            try {
                session = manager.findSession (requestedSessionId);
                if (session != null && session.isValid ()) {
                    return session.getSession ();
                } else {
                    session = null;
                }
            } catch (IOException e) {
                log.error ("", e);
                return null;
            }
        }

        if (!create) {
            return null;
        }

        session = manager.createSession (requestedSessionId);
        if (session != null) {
            return session.getSession ();
        } else {
            return null;
        }
    }

    /**
     * 会话
     */
    @Override
    public HttpSession getSession() {
        return getSession (true);
    }


    @Override
    public String changeSessionId() {
        getSession (false);
        if (session == null) {
            throw new IllegalStateException ();
        }

        if (getContext () == null) {
            throw new IllegalStateException ("没有context与这个request关联");
        }
        if (getContext ().getManager () == null) {
            throw new IllegalStateException ("没有manager与这个request关联");
        }

        getContext ().getManager ().changeSessionId (session);
        return session.getIdInternal ();
    }


    @Override
    public boolean isRequestedSessionIdValid() {
        getSession (false);
        if (session == null) {
            return false;
        }
        return session.isValid ();
    }

    public void setRequestedSessionCookie(boolean requestedSessionCookie) {
        this.requestedSessionCookie = requestedSessionCookie;
    }

    public void setRequestedSessionURL(boolean requestedSessionURL) {
        this.requestedSessionURL = requestedSessionURL;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        if (requestedSessionId == null) {
            return false;
        }

        return requestedSessionCookie;
    }


    @Override
    public boolean isRequestedSessionIdFromURL() {
        if (requestedSessionId == null) {
            return false;
        }

        return requestedSessionURL;
    }


    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL ();
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
     * TODO 文件上传功能
     */
    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    /**
     * TODO 文件上传功能
     */
    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return null;
    }

    /**
     * 协议升级，不实现
     */
    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }

    //如果用@ToString的话，就会调用getSession方法，从而加载session
    @Override
    public String toString() {
        return "HttpRequestImpl{" +
                "headers=" + headers +
                ", cookies=" + cookies +
                ", method='" + method + '\'' +
                ", decodedRequestURI='" + decodedRequestURI + '\'' +
                '}';
    }
}
