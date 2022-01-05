package com.example.connector.http;

import com.example.Globals;
import com.example.connector.ByteBufInputStream;
import com.example.connector.ByteBufOutputStream;
import com.example.life.LifecycleBase;
import com.example.util.RequestUtil;
import com.example.util.StringParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.Globals.SERVER_INFO;
import static com.example.HeaderValues.APPLICATION_JSON;
import static com.example.HeaderValues.APPLICATION_X_WWW_FORM_URLENCODED;
import static com.example.connector.http.Constants.DATE_TIME_FORMATTER;
import static com.google.common.net.HttpHeaders.*;

/**
 * HttpProcessor内部是单线程的，Connector会保证HttpProcessor会被单线程访问
 *
 * @date 2021/12/8 19:55
 */
@Slf4j
public final class HttpProcessor extends LifecycleBase {
    /**
     * 用于匹配在query中的session id
     */
    private static final String match = ";" + Globals.SESSION_PARAMETER_NAME + "=";
    private static final Map<String, String> defaultHeaders = new HashMap<> ();

    static {
        defaultHeaders.put (CONTENT_TYPE, APPLICATION_JSON);
        defaultHeaders.put (SERVER, SERVER_INFO);
        defaultHeaders.put (CONNECTION, KEEP_ALIVE);
    }

    private final StringParser parser = new StringParser ();
    private final HttpConnector connector;
    private final int port;
    private HttpRequestImpl request;
    private HttpResponseImpl response;
    private FullHttpRequest fullHttpRequest;
    private FullHttpResponse fullHttpResponse;
    private ChannelHandlerContext handlerContext;
    private ByteBuf reqBuf;
    private ByteBuf respBuf;

    public HttpProcessor(HttpConnector httpConnector) {
        this.connector = httpConnector;
        this.request = (HttpRequestImpl) connector.createRequest ();
        this.response = (HttpResponseImpl) connector.createResponse ();
        this.port = connector.getPort ();
    }

    /**
     * 处理一个http报文，并给出响应
     * connection的close netty会自动处理
     */
    public void process(FullHttpRequest r, ChannelHandlerContext handlerContext) {
        log.info ("处理请求 {}", r);
        boolean ok = true;

        prepareProcess (r, handlerContext);

        try {
            parseConnection ();
            parseRequest ();

            prepareResponse ();
        } catch (Exception e) {
            //事实上netty已经解析过了，所以不会出现这种情况
            e.printStackTrace ();
            response.sendError (HttpServletResponse.SC_BAD_REQUEST);
            ok = false;
        }

        if (ok) {

            try {
                //container.invoke
                connector.getContainerInternal ().invoke (request, response);
            } catch (Exception e) {
                e.printStackTrace ();
                response.sendError (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        //先关闭流，这样缓冲区的byte都会完全写入byteBuf fixme
        //即用户不一定会flush，这样close的时候会自动flush
        try {
            response.finishResponse ();
        } catch (Throwable e) {
//            log.error ("response process.invoke ", e);
        }

        try {
            request.finishRequest ();
        } catch (Throwable e) {
//            log.error ("request process.invoke ", e);
        }

        doSend ();

        recycle ();
    }

    private void prepareProcess(FullHttpRequest r, ChannelHandlerContext handlerContext) {
        this.fullHttpRequest = r;
        this.handlerContext = handlerContext;
        this.reqBuf = fullHttpRequest.content ();
        if (this.respBuf == null) {
            respBuf = PooledByteBufAllocator.DEFAULT.buffer ();
        }
//        if (request == null) {
//            request = new HttpRequestImpl (connector);
//        }
    }

    /**
     * 给req装填header
     * 解析cookie、header、paramMap
     * Content-Type为 x-www-form-urlencoded的body
     * locale（根据accept）
     */
    private void parseRequest() throws UnsupportedEncodingException {
        for (Map.Entry<String, String> header : fullHttpRequest.headers ()) {
            String key = header.getKey ();
            String value = header.getValue ();
            request.addHeader (key, value);


            if (key.equalsIgnoreCase (CONTENT_LENGTH)) {
                request.setContentLength (Integer.parseInt (value));
            } else if (key.equalsIgnoreCase (CONTENT_TYPE)) {
                request.setContentType (value);

                if (value.equalsIgnoreCase (APPLICATION_X_WWW_FORM_URLENCODED)) {
                    //解析body
                    Map<String, String> requestParams = getRequestParams (fullHttpRequest);
                    requestParams.forEach ((k, v) -> request.addParameter (k, new String[]{v}));
                    //清除body
                    reqBuf.clear ();
                }
            } else if (key.equalsIgnoreCase (ACCEPT_LANGUAGE)) {
                parseAcceptLanguage (value);
            } else if (key.equalsIgnoreCase (COOKIE)) {
                //cookie
                List<Cookie> cookies = RequestUtil.parseCookieHeader (value);
                for (Cookie cookie : cookies) {
                    //解析session
                    parseSessionFromCookie (cookie);
                    request.addCookie (cookie);
                }
            }
        }

        String uri = fullHttpRequest.uri ();
        int index;
        if ((index = uri.indexOf ('?')) >= 0) {
            request.setQueryString (uri.substring (index + 1));
            request.setRequestURI (uri.substring (0, index));//必须是没有query、schema、host、port的
        } else {
            request.setRequestURI (uri);
        }

        parseSessionFromURL (uri);

        //提取query param
        Map<String, String[]> map = new HashMap<> ();
        RequestUtil.parseParameters (map, request.getQueryString (), "utf-8");
        request.setParameterMap (map);

        log.trace ("装填headers后为 {}", request.headers);
    }

    private void parseSessionFromURL(String uri) {
        int semicolon = uri.indexOf (match);
        if (semicolon >= 0) {
            String rest = uri.substring (semicolon + match.length ());
            int semicolon2 = rest.indexOf (';');
            if (semicolon2 >= 0) {
                request.setRequestedSessionId (rest.substring (0, semicolon2));
            } else {
                request.setRequestedSessionId (rest);
            }
            request.setRequestedSessionURL (true);
            log.debug ("Requested URL session id is " + request.getRequestedSessionId ());
        } else {
            //因为id可能被cookie设置了，所以不要管id
            //request.setRequestedSessionId (null);
            request.setRequestedSessionURL (false);
        }
    }

    /**
     * 由cookie获得session
     */
    private void parseSessionFromCookie(Cookie cookie) {
        if (cookie.getName ().equals
                (Globals.SESSION_COOKIE_NAME)) {

            if (!request.isRequestedSessionIdFromCookie ()) {
                //如果有多个session id的话（因为cookie一个key可以多个value），就保留第一个
                request.setRequestedSessionId (cookie.getValue ());
                request.setRequestedSessionCookie (true);
                request.setRequestedSessionURL (false);
                log.debug ("Requested cookie session id is " + request.getRequestedSessionId ());
            }
        }
    }

    /**
     * 复杂的地方在于要处理权重
     * 但是似乎还没有用到这个字段
     * TODO
     */
    private void parseAcceptLanguage(String value) {

        // Store the accumulated languages that have been requested in
        // a local collection, sorted by the quality value (so we can
        // add Locales in descending order).  The values will be ArrayLists
        // containing the corresponding Locales to be added
        TreeMap<Double, ArrayList<Locale>> locales = new TreeMap<> ();

        // Preprocess the value to remove all whitespace
        int white = value.indexOf (' ');
        if (white < 0)
            white = value.indexOf ('\t');
        if (white >= 0) {
            StringBuilder sb = new StringBuilder ();
            int len = value.length ();
            for (int i = 0; i < len; i++) {
                char ch = value.charAt (i);
                if ((ch != ' ') && (ch != '\t'))
                    sb.append (ch);
            }
            value = sb.toString ();
        }

        // Process each comma-delimited language specification
        parser.setString (value);        // ASSERT: parser is available to us
        int length = parser.getLength ();
        while (true) {

            // Extract the next comma-delimited entry
            int start = parser.getIndex ();
            if (start >= length)
                break;
            int end = parser.findChar (',');
            String entry = parser.extract (start, end).trim ();
            parser.advance ();   // For the following entry

            // Extract the quality factor for this entry
            double quality = 1.0;
            int semi = entry.indexOf (";q=");
            if (semi >= 0) {
                try {
                    quality = Double.parseDouble (entry.substring (semi + 3));
                } catch (NumberFormatException e) {
                    quality = 0.0;
                }
                entry = entry.substring (0, semi);
            }

            // Skip entries we are not going to keep track of
            if (quality < 0.00005)
                continue;       // Zero (or effectively zero) quality factors
            if ("*".equals (entry))
                continue;

            // Extract the language and country for this entry
            String language = null;
            String country = null;
            String variant = null;
            int dash = entry.indexOf ('-');
            if (dash < 0) {
                language = entry;
                country = "";
                variant = "";
            } else {
                language = entry.substring (0, dash);
                country = entry.substring (dash + 1);
                int vDash = country.indexOf ('-');
                if (vDash > 0) {
                    String cTemp = country.substring (0, vDash);
                    variant = country.substring (vDash + 1);
                    country = cTemp;
                } else {
                    variant = "";
                }
            }

            // Add a new Locale to the list of Locales for this quality level
            Locale locale = new Locale (language, country, variant);
            Double key = -quality;  // Reverse the order
            ArrayList<Locale> values = locales.computeIfAbsent (key, k -> new ArrayList<> ());
            values.add (locale);
        }

        // Process the quality values in highest->lowest order (due to
        // negating the Double value when creating the key)
        for (double key : locales.keySet ()) {
            ArrayList<Locale> list = locales.get (key);
            for (Locale locale : list) {
                request.addLocale (locale);
            }
        }

    }

    /**
     * 如果是Content-Type为 x-www-form-urlencoded的body
     * 应该这么解析
     */
    private Map<String, String> getRequestParams(FullHttpRequest request) {
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder (new DefaultHttpDataFactory (false), request);
        List<InterfaceHttpData> httpPostData = decoder.getBodyHttpDatas ();
        Map<String, String> params = new HashMap<> ();

        for (InterfaceHttpData data : httpPostData) {
            if (data.getHttpDataType () == InterfaceHttpData.HttpDataType.Attribute) {
                MemoryAttribute attribute = (MemoryAttribute) data;
                params.put (attribute.getName (), attribute.getValue ());
            }
        }
        return params;
    }

    /**
     * 给req装填连接信息
     */
    private void parseConnection() {
        request.setProtocol (fullHttpRequest.protocolVersion ().protocolName ());
        request.setServerPort (port);
        request.setMethod (fullHttpRequest.method ().name ());
        request.setSecure (connector.getSecure ());
        request.setScheme (connector.getScheme ());

        InetSocketAddress inetSocketAddress = Optional
                .ofNullable (handlerContext)
                .map (ChannelHandlerContext::channel)
                .map (x -> (InetSocketAddress) x.remoteAddress ())
                .orElse (null);
        if (inetSocketAddress != null) {

            //会dns域名解析，所以可能是域名，也可能是ip
            //因为http基于tcp基于ip，无法确定域名，只有ip
            //host组件基于这个
            String host = checkLocalhost (inetSocketAddress.getAddress ());
            request.setServerName (host);
            request.setRemoteHost (host);

            request.setRemoteAddress (inetSocketAddress.getHostString ());//不会dns
            request.setInet (inetSocketAddress.getAddress ());
        }


        request.setResponse (response);//其实并没有用到。。
        request.setConnector (connector);
        request.setStream (new ByteBufInputStream (reqBuf));

        log.trace ("装填请求后为 {}", request);
    }

    /**
     * 给响应添加默认头
     */
    private void prepareResponse() {
        response.setStream (new ByteBufOutputStream (respBuf));
        response.setRequest (request);
        response.setByteBuf (respBuf);


        log.trace ("prepareResponse后为 {}", request);
    }

    /**
     * 默认的header
     */
    private void setDefaultHeaders() {
        for (Map.Entry<String, String> e : defaultHeaders.entrySet ()) {
            if (response.getHeaders (e.getKey ()) == null) {
                response.setHeader (e.getKey (), e.getValue ());
            }
        }

        if (response.getHeaders (DATE) == null) {
            response.setHeader (DATE, LocalDateTime.now ().format (DATE_TIME_FORMATTER));
        }

        if (response.getHeaders (CONTENT_LENGTH) == null) {
            response.setHeader (CONTENT_LENGTH, String.valueOf (respBuf.readableBytes ()));
        }

    }

    private void doSend() {
        setDefaultHeaders ();

        fullHttpResponse = new DefaultFullHttpResponse (HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf (response.getStatus ()),
                respBuf);
        HttpHeaders respHeaders = fullHttpResponse.headers ();
        for (String headerName : response.getHeaderNames ()) {
            String[] headerValues = response.getHeaderValues (headerName);

            for (String headerValue : headerValues) {

                if (headerName.equalsIgnoreCase (CONTENT_LENGTH) ||
                        headerName.equalsIgnoreCase (CONTENT_TYPE)) {
                    //覆盖
                    respHeaders.set (headerName, headerValue);
                } else {
                    respHeaders.add (headerName, headerValue);
                }

            }
        }

        parseCookieToHeader (respHeaders);

        handlerContext.writeAndFlush (fullHttpResponse);

        log.info ("请求 {} {} 发送响应, status={}", request.getMethod (), request.getDecodedRequestURI (),
                response.getStatus ());
        log.debug ("响应headers:");
        respHeaders.forEach (x -> log.debug ("header: {} -> {}", x.getKey (), x.getValue ()));
    }

    /**
     * 将cookie重新编码成uri，session id已经放到cookie中了
     * fixme 基于uri的session id咋样？？
     */
    private void parseCookieToHeader(HttpHeaders respHeaders) {
        //添加session id
        HttpSession session = request.getSession (true);

        //如果从host到context映射失败的话，那就根本没有manger，自然就没有session
        if (session != null) {
            Cookie cookie = new Cookie (Globals.SESSION_COOKIE_NAME, session.getId ());
            cookie.setSecure (connector.getSecure ());
            cookie.setMaxAge (session.getMaxInactiveInterval ());
            String s = request.getContextPath ();
            if (!s.startsWith ("/")) {
                s = "/" + s;
            }
            cookie.setPath (s);//session存在说明context存在
            response.addCookie (cookie);
        }

        StringBuilder builder = new StringBuilder ();
        Cookie[] cookies = response.getCookies ();
        if (cookies.length == 0)
            return;

        for (int i = 0; i < cookies.length; i++) {
            String s = RequestUtil.encodeCookie (cookies[i]);
            if (i > 0)
                builder.append ("; ");
            builder.append (s);
        }

        respHeaders.set (SET_COOKIE, builder.toString ());
        log.trace ("添加cookie header {}", builder);
    }

    private void recycle() {
        request.recycle ();
        response.recycle ();
    }

    private String checkLocalhost(InetAddress address) {
        String localhost = "localhost";
        String hostName = address.getHostName ();

        if (hostName.equals ("127.0.0.1") || hostName.equals ("0:0:0:0:0:0:0:1")) {
            return localhost;
        } else return hostName;
    }
}
