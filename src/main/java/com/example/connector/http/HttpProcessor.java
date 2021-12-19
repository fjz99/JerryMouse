package com.example.connector.http;

import com.example.connector.ByteBufInputStream;
import com.example.connector.ByteBufOutputStream;
import com.example.life.LifeCycleBase;
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
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.connector.http.Constants.*;

/**
 * HttpProcessor内部是单线程的，Connector会保证HttpProcessor会被单线程访问
 *
 * @date 2021/12/8 19:55
 */
@Slf4j
public final class HttpProcessor extends LifeCycleBase {

    private final StringParser parser = new StringParser ();
    private HttpConnector connector;
    private HttpRequestImpl request;
    private HttpResponseImpl response;
    private FullHttpRequest fullHttpRequest;
    private FullHttpResponse fullHttpResponse;
    private ChannelHandlerContext handlerContext;
    private ByteBuf reqBuf;
    private ByteBuf respBuf;
    private int port;
    private boolean started = false;

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
                connector.getContainer ().invoke (request, response);
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
            e.printStackTrace ();
            log.error ("response process.invoke " + e);
        }

        try {
            request.finishRequest ();
        } catch (Throwable e) {
            e.printStackTrace ();
            log.error ("request process.invoke " + e);
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
//                    todo session
//                    if (cookie.getName ().equals
//                            (Globals.SESSION_COOKIE_NAME)) {
//                        // Override anything requested in the URL
//                        if (!request.isRequestedSessionIdFromCookie ()) {
//                            // Accept only the first session id cookie
//                            request.setRequestedSessionId
//                                    (cookie.getValue ());
//                            request.setRequestedSessionCookie (true);
//                            request.setRequestedSessionURL (false);
//                        }
//                    }
                    request.addCookie (cookie);
                }
            }
        }

        String uri = request.getDecodedRequestURI ();
        int index;
        if ((index = uri.indexOf ('?')) >= 0) {
            request.setQueryString (uri.substring (index + 1));
        }

        //提取query param
        Map<String, String[]> map = new HashMap<> ();
        RequestUtil.parseParameters (map, request.getQueryString (), "utf-8");
        request.setParameterMap (map);

        log.debug ("装填headers后为 {}", request.headers);
    }

    /**
     * 复杂的地方在于要处理权重
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
                continue;       // FIXME - "*" entries are not handled

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
        request.setRequestURI (fullHttpRequest.uri ().toLowerCase ());//fixme 要求是特殊的地址
        request.setSecure (connector.getSecure ());
        request.setScheme (connector.getScheme ());

        InetSocketAddress inetSocketAddress = Optional
                .ofNullable (handlerContext)
                .map (ChannelHandlerContext::channel)
                .map (x -> (InetSocketAddress) x.remoteAddress ())
                .orElse (null);
        if (inetSocketAddress != null) {
            request.setRemoteHost (inetSocketAddress.getHostString ());
            request.setRemoteAddress (inetSocketAddress.getAddress ().getHostAddress ());
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

        setDefaultHeaders ();

        log.trace ("prepareResponse后为 {}", request);
    }

    /**
     * 默认的header
     */
    private void setDefaultHeaders() {
        response.setHeader ("server", SERVER_INFO);
        response.setHeader ("date", LocalDateTime.now ().format (DATE_TIME_FORMATTER));
        response.setHeader (CONNECTION, KEEP_ALIVE);
        response.setHeader (CONTENT_TYPE, APPLICATION_JSON);
    }

    private void doSend() {
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
        if (!respHeaders.contains (CONTENT_LENGTH)) {
            respHeaders.set (CONTENT_LENGTH, respBuf.readableBytes ());
        }

        parseCookieToHeader (respHeaders);

        handlerContext.writeAndFlush (fullHttpResponse);

        log.info ("发送响应结束,status={}", response.getStatus ());
        log.debug ("响应headers = {}", response.headers);
    }

    private void parseCookieToHeader(HttpHeaders respHeaders) {
        //cookie
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

        respHeaders.set (HttpHeaderNames.SET_COOKIE.toString (), builder.toString ());
    }

    private void recycle() {
        request.recycle ();
        response.recycle ();
    }
}
