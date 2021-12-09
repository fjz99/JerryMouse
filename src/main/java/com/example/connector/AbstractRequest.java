package com.example.connector;

import com.example.connector.http.HttpConnector;
import com.example.connector.http.HttpRequestStream;

import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;

/**
 * 不区分协议的req,即这个可以认为是针对TCP的
 *
 * @date 2021/12/8 20:23
 */
public abstract class AbstractRequest implements Request, ServletRequest {

    //tcp

    protected final List<Locale> locales = new ArrayList<> ();
    /**
     * protocol/majorVersion;必须提供版本号
     */
    protected String protocol = "HTTP/1.1";
    protected String remoteAddress;
    protected int remotePort;
    protected String localAddress;
    /**
     * 我的port
     */
    protected int localPort;
    /**
     * ip或header中host的值<br/>
     * Returns the host name of the server to which the request was sent.
     * It is the value of the part before ":" in the Host header value,
     * if any, or the resolved server name, or the server IP address.
     */
    protected String serverName;
    protected boolean secure = false;
    /**
     * https等
     */
    protected String scheme = "http";
    protected Locale locale = Locale.getDefault ();
    protected String hostName;
    /**
     * 根据情况，可能返回ip或者hostname
     */
    protected String remoteHost;
    //servlet规范
    protected ServletContext servletContext;
    protected DispatcherType dispatcherType;
    protected ServletInputStream servletInputStream;
    protected Map<String, Object> attributes = new ConcurrentHashMap<> ();
//    protected Context context;

    /**
     * 包装的ByteBuf
     */
    protected InputStream inputStream;

    protected BufferedReader reader;
    protected long contentLength;
    protected String encoding;

    /**
     * query 参数
     */
    protected Map<String, String[]> parameterMap = new ConcurrentHashMap<> ();

    protected String contentType;

    //其他组件
    protected HttpConnector httpConnector;
    protected Response response;

    protected Map<String, Object> notes = new ConcurrentHashMap<> ();

//    protected ByteBuf byteBuf;

    @Override
    public HttpConnector getConnector() {
        return httpConnector;
    }

    @Override
    public void setConnector(HttpConnector httpConnector) {
        this.httpConnector = httpConnector;
    }

    /**
     * 可以增加外观模式
     * 见书
     */
    @Override
    public ServletRequest getRequest() {
        return this;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public InputStream getStream() {
        return inputStream;
    }

    @Override
    public void setStream(InputStream stream) {
        this.inputStream = stream;
    }

    @Override
    public ServletInputStream createInputStream() {
        Objects.requireNonNull (inputStream);
        if (servletInputStream == null) {
            servletInputStream = new HttpRequestStream (inputStream);
            return servletInputStream;
        } else {
            throw new IllegalStateException ("servletInputStream exists");
        }
    }

//    @Override
//    public void setByteBuf(ByteBuf byteBuf) {
//        this.byteBuf = byteBuf;
//    }

    /**
     * 关闭所有的reader等,释放资源
     * 会先调用这个再调用recycle
     * 然后就会关闭这个tcp连接了
     */
    @Override
    public void finishRequest() throws IOException {
        try {
            servletInputStream.close ();//自动关闭里面的
        } catch (IOException e) {
            e.printStackTrace ();
        }

        try {
            reader.close ();
        } catch (IOException e) {
            e.printStackTrace ();
        }
    }

    /**
     * 暂时用不到，毕竟没有池化processor
     * <p>
     * 注意是clear，否则重新分配对象还是有损失
     */
    @Override
    public void recycle() {
        attributes.clear ();
//        byteBuf.clear ();
        parameterMap.clear ();
        locales.clear ();
        notes.clear ();

        encoding = null;
        // connector is NOT reset when recycling
        contentLength = -1;
        contentType = null;
        inputStream = null;
        protocol = null;
        reader = null;
        remoteAddress = null;
        remoteHost = null;
        response = null;
//        scheme = null;
        scheme = "http";

        secure = false;
        serverName = null;
        localPort = -1;
        servletInputStream = null;
        locale = Locale.getDefault ();
    }

    /**
     * 暂时也不知道有啥用
     */
    @Override
    public void setNote(String name, Object value) {
        notes.put (name, value);
    }

    @Override
    public Object getNote(String name) {
        return notes.get (name);
    }

    @Override
    public void removeNote(String name) {
        notes.remove (name);
    }

    @Override
    public void setRemoteAddress(String remote) {
        remoteAddress = remote;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get (name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration (new TreeSet<> (attributes.keySet ()));
    }

    /**
     * ServletReq带的，不针对于http，通用的
     */
    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    /**
     * ServletReq带的，不针对于http，通用的
     */
    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        this.encoding = env;
    }

    /**
     * ServletReq带的，不针对于http，通用的
     */
    @Override
    public int getContentLength() {
        return (int) contentLength;
    }

    /**
     * ServletReq带的，不针对于http，通用的
     */
    @Override
    public long getContentLengthLong() {
        return contentLength;
    }

    /**
     * ServletReq带的，不针对于http，通用的
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return servletInputStream;
    }

    /**
     * 参见实现规范<br/>
     * {@inheritDoc}
     * <br/>
     * ServletReq带的，不针对于http，通用的
     */
    @Override
    public String getParameter(String name) {
        return Optional
                .ofNullable (parameterMap.get (name)).map (x -> x[0])
                .orElse (null);
    }

    /**
     * ServletReq带的，不针对于http，通用的
     *
     * @return 不可变，所以直接返回即可
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration (new TreeSet<> (parameterMap.keySet ()));
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get (name);
    }

    /**
     * 请求参数自然是不可变的
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public void setServerName(String name) {
        this.serverName = name;
    }

    @Override
    public int getServerPort() {
        return localPort;
    }

    @Override
    public void setServerPort(int port) {
        this.localPort = port;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (inputStream != null)
            throw new IllegalStateException ("getInputStream has been called.");
        if (reader == null) {
            String encoding = getCharacterEncoding ();
            if (encoding == null)
                encoding = "ISO-8859-1";
            InputStreamReader isr =
                    new InputStreamReader (createInputStream (), encoding);
            reader = new BufferedReader (isr);
        }
        return reader;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddress;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public void setAttribute(String name, Object o) {
        if (name == null) {
            throw new IllegalArgumentException ("attr name can not be null");
        }
        if (o == null) {
            removeAttribute (name);
            return;
        }
        attributes.put (name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove (name);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * 根据http请求的accept获得需要locale，如果没有就返回默认的
     */
    @Override
    public Enumeration<Locale> getLocales() {
        synchronized (locales) {
            return Collections.enumeration (unmodifiableCollection (locales));
        }
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    /**
     * todo
     * 需要context组件
     * 现在没法写
     */
    @Override
    public String getRealPath(String path) {
//        if (context == null)
//            return (null);
//        ServletContext servletContext = context.getServletContext ();
//        if (servletContext == null)
//            return (null);
//        else {
//            try {
//                return (servletContext.getRealPath (path));
//            } catch (IllegalArgumentException e) {
//                return (null);
//            }
//        }
        return null;
    }

    @Override
    public int getRemotePort() {
        return remotePort;
    }

    @Override
    public String getLocalName() {
        return hostName;
    }

    @Override
    public String getLocalAddr() {
        return localAddress;
    }

    @Override
    public int getLocalPort() {
        return localPort;
    }

    /**
     * todo 没有实现
     */
    @Override
    public ServletContext getServletContext() {
        return null;
    }

    /**
     * todo 没有实现
     */
    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    //下面的async不打算实现
    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }
}
