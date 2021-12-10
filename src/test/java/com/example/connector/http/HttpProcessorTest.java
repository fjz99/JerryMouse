package com.example.connector.http;

import com.example.Container;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpProcessorTest {
    HttpConnector connector = mock (HttpConnector.class);
    Container container = mock (Container.class);
    HttpProcessor httpProcessor;
    FullHttpRequest request;
    ChannelHandlerContext handlerContext;
    String testUri1 = "/test?a=1&a=2&b=3&c=abc";
    String testUri2 = "/test";
    String testUri3 = "http://www.test.cn";
    String testUri4 = "http://test;JESSESSIONID=0dsadaxdsax12";
    String testUri5 = "https://event.csdn.net/logstores/csdn-pc-tracking-pageview/track_ua.gif?APIVersion=0.6.0&cid=10_" +
            "6069934530-1630585757536-284397&sid=10_1639135330659.764487&pid=blog&uid=&did=10_6069934530-1630585757536-" +
            "284397&dc_sid=effc4382b7257c75381ab84a84033a92&ref=&curl=https%3A%2F%2Fblog.csdn.net%2Fqq_40491569%2Farticle" +
            "%2Fdetails%2F83472556&utm=&spm=1001.2101&tos=43&adb=0&cCookie=c_dl_fref%3Dhttps%3A%2F%2Fwww.baidu.com%2Fs%3B" +
            "c_dl_um%3D-%3Bc_dl_prid%3D1638767170343_957740%3Bc_dl_rid%3D1638768438101_792660%3Bc_dl_fpage%3D%2Fdownload%" +
            "2Fsuxu284%2F9678987%3Bc_first_ref%3Dwww.baidu.com%3Bc_segment%3D13%3Bc_sid%3Deffc4382b7257c75381ab84a84033a9" +
            "2%3Bc_ref%3Dhttps%253A%2F%2Fwww.baidu.com%2Fs%3Bc_pref%3Dhttps%253A%2F%2Fwww.baidu.com%2Fs%3Bc_first_page%3D" +
            "https%253A%2F%2Fblog.csdn.net%2Fdajinglingpake%2Farticle%2Fdetails%2F109314951%3Bc_session_id%3D10_163913533" +
            "0659.764487%3Bc_page_id%3Ddefault%3B&t=1639135373&screen=1536*864&un=&urn=1639135372757-85cfa823-edce-4fe3-" +
            "97dc-42774034c517&vType=&log_id=463";

    @BeforeEach
    public void before() throws ServletException, IOException {
        connector = mock (HttpConnector.class);
        container = mock (Container.class);
        handlerContext = mock (ChannelHandlerContext.class);

        when (connector.getContainer ()).thenReturn (container);
        when (connector.createRequest ()).thenReturn (new HttpRequestImpl (connector));
        when (connector.createResponse ()).thenReturn (new HttpResponseImpl (connector));

        containerDo (null);
    }

    private void containerDo(Answer<?> answer) throws IOException, ServletException {
        if (answer == null) {
            doAnswer (invocation -> {
                System.out.println (invocation.getArgument (0).toString ());
                System.out.println (invocation.getArgument (1).toString ());
                return null;
            }).when (container).invoke (any (), any ());
        } else {
            doAnswer (answer).when (container).invoke (any (), any ());
        }
        httpProcessor = new HttpProcessor (connector);
    }

    @Test
    public void testSimple() throws ServletException, IOException {
        containerDo (invocation -> {
            HttpRequestImpl argument = invocation.getArgument (0);
            assertEquals (4, argument.getCookies ().length);
            assertTrue (argument.getMethod ().equalsIgnoreCase ("GET"));
            assertEquals (argument.getLocale (), Locale.forLanguageTag ("fr-CH"));
            return null;
        });
        request = new DefaultFullHttpRequest (HttpVersion.HTTP_1_1, HttpMethod.GET, testUri1);
        HttpHeaders headers = request.headers ();
        headers.set ("Accept-Language", "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5");
        headers.set ("Cookie", "user_locale=zh-CN; oschina_new_user=false; remove_member_bulletin=gitee_member_bulletin; close_wechat_tour=true");
        httpProcessor.process (request, handlerContext);
    }

    @Test
    public void testReader() throws ServletException, IOException {
        containerDo (invocation -> {
            HttpRequestImpl argument = invocation.getArgument (0);
            BufferedReader reader = argument.getReader ();
            assertEquals (reader.readLine (), "fuckyou!");
            return null;
        });
        ByteBuf byteBuf = Unpooled.wrappedBuffer ("fuckyou!".getBytes (CharsetUtil.UTF_8));
        request = new DefaultFullHttpRequest (HttpVersion.HTTP_1_1, HttpMethod.GET, testUri1, byteBuf);
        httpProcessor.process (request, handlerContext);
    }

    @Test
    public void testBodyRead() throws ServletException, IOException {
        containerDo (invocation -> {
            HttpRequestImpl argument = invocation.getArgument (0);
            ServletInputStream inputStream = argument.getInputStream ();
            int s;
            while ((s = inputStream.read ()) != -1) {
                System.out.println ((char) s);
            }
            return null;
        });
        ByteBuf byteBuf = Unpooled.wrappedBuffer ("fuckyou!".getBytes ());
        request = new DefaultFullHttpRequest (HttpVersion.HTTP_1_1, HttpMethod.GET, testUri1, byteBuf);
        httpProcessor.process (request, handlerContext);
    }


    @Test
    public void testBodyWrite() throws IOException, ServletException {
        String sf = "fuckyou!";
        String s1 = "\ncao\n";
        containerDo (invocation -> {
            HttpRequestImpl argument = invocation.getArgument (0);
            HttpResponseImpl resp = invocation.getArgument (1);
            BufferedReader reader = argument.getReader ();
            PrintWriter writer = resp.getWriter ();
            String s;
            while ((s = reader.readLine ()) != null) {
                writer.write (s + s1);
                writer.flush ();
            }
            return null;
        });
        doAnswer (invocation -> {
            FullHttpResponse response = invocation.getArgument (0);
            System.out.println (response.content ().toString (Charset.defaultCharset ()));
            assertEquals (response.content ().toString (Charset.defaultCharset ()), sf + s1);
            return null;
        }).when (handlerContext).writeAndFlush (any ());

        ByteBuf byteBuf = Unpooled.wrappedBuffer (sf.getBytes (Charset.defaultCharset ()));
        request = new DefaultFullHttpRequest (HttpVersion.HTTP_1_1, HttpMethod.GET, testUri2, byteBuf);
        HttpHeaders headers = request.headers ();
        headers.set ("Accept-Language", "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5");
        headers.set ("Cookie", "user_locale=zh-CN; oschina_new_user=false; remove_member_bulletin=gitee_member_bulletin; close_wechat_tour=true");
        httpProcessor.process (request, handlerContext);
    }

    @Test
    public void testHeadersWrite() throws IOException, ServletException {
        containerDo (invocation -> {
            HttpRequestImpl argument = invocation.getArgument (0);
            HttpResponseImpl resp = invocation.getArgument (1);

            resp.addHeader ("aa", "bb");
            resp.addHeader ("Accept-Language", "ff");
            resp.addHeader ("Accept-Language", "dd");

            return null;
        });
        doAnswer (invocation -> {
            FullHttpResponse response = invocation.getArgument (0);
            System.out.println (response.headers ());
            return null;
        }).when (handlerContext).writeAndFlush (any ());

        request = new DefaultFullHttpRequest (HttpVersion.HTTP_1_1, HttpMethod.GET, testUri1);
        HttpHeaders headers = request.headers ();
        headers.set ("Accept-Language", "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5");
        headers.set ("Cookie", "user_locale=zh-CN; oschina_new_user=false; remove_member_bulletin=gitee_member_bulletin; close_wechat_tour=true");
        httpProcessor.process (request, handlerContext);
    }

    @Test
    public void testURI() {
        request = new DefaultFullHttpRequest (HttpVersion.HTTP_1_1, HttpMethod.GET, testUri5);
        httpProcessor.process (request, handlerContext);
    }

    @Test
    public void testCookie() throws ServletException, IOException {
        containerDo (invocation -> {
            HttpRequestImpl argument = invocation.getArgument (0);
            HttpResponseImpl resp = invocation.getArgument (1);
            resp.addCookie (new Cookie ("ff","vv"));
            resp.addCookie (new Cookie ("gggg","qqqqqqq"));
            return null;
        });
        doAnswer (invocation -> {
            FullHttpResponse response = invocation.getArgument (0);
            System.out.println (response.headers ());
            return null;
        }).when (handlerContext).writeAndFlush (any ());

        request = new DefaultFullHttpRequest (HttpVersion.HTTP_1_1, HttpMethod.GET, testUri5);
        httpProcessor.process (request, handlerContext);
    }
}
