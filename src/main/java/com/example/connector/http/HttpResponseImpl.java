package com.example.connector.http;

import com.example.connector.AbstractResponse;
import com.example.connector.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;

import static com.example.connector.http.Constants.DATE_TIME_FORMAT;

/**
 * 目前只有1.1版本
 *
 * @date 2021/12/8 20:26
 */
public class HttpResponseImpl extends AbstractResponse implements HttpResponse, HttpServletResponse {
    protected Map<String, String> headers;
    protected Map<String, Cookie> cookies;
    protected boolean suspend;
    protected String message;
    protected HttpResponseStatus status;
    protected static final TimeZone zone = TimeZone.getDefault ();
    protected final SimpleDateFormat format =
            new SimpleDateFormat (DATE_TIME_FORMAT, locale);

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptyList ();
    }

    @Override
    public String[] getHeaderValues(String name) {
        return new String[0];
    }

    @Override
    public String getMessage() {
        return null;
    }

    @Override
    public void addCookie(Cookie cookie) {

    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Override
    public String encodeUrl(String url) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {

    }

    @Override
    public void sendError(int sc) throws IOException {

    }

    @Override
    public void sendRedirect(String location) throws IOException {

    }

    @Override
    public void setDateHeader(String name, long date) {

    }

    @Override
    public void addDateHeader(String name, long date) {

    }

    @Override
    public void setHeader(String name, String value) {

    }

    @Override
    public void addHeader(String name, String value) {

    }

    @Override
    public void setIntHeader(String name, int value) {

    }

    @Override
    public void addIntHeader(String name, int value) {

    }

    @Override
    public void setStatus(int sc) {

    }

    @Override
    public void setStatus(int sc, String sm) {

    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public void reset(int status, String message) {

    }

    @Override
    public void setBufferSize(int size) {

    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    /**
     * 实现发送HTTP报文的功能
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
    }
}
