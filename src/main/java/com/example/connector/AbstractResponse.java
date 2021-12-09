package com.example.connector;

import com.example.connector.http.Connector;
import com.example.util.StringManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * 不区分协议的resp,即这个可以认为是针对TCP的
 *
 * @date 2021/12/8 20:24
 */
public abstract class AbstractResponse implements Response, ServletResponse {
    protected Request request;
    protected String encoding;
    protected String contentType;
    protected ServletOutputStream servletOutputStream;
    protected OutputStream outputStream;//bytebuf 的
    protected long contentLength;
    protected boolean committed;//能关闭、刷新，也可以写，允许多次调用flush；？？？
    protected Locale locale = Locale.getDefault ();
    protected Connector connector;
    protected ByteBuf byteBuf;//负责维护buffer
    protected ChannelHandlerContext handlerContext;
    protected StringManager sm;
    protected boolean suspended;//把输出流挂起，此时无法写，能关闭，无法刷新；？？？
    protected boolean err;
    protected PrintWriter writer;


    @Override
    public Connector getConnector() {
        return connector;
    }

    @Override
    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    @Override
    public int getContentCount() {
        return 0;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        resetBuffer ();
    }

    @Override
    public void setLocale(Locale loc) {
        this.locale = loc;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void setCommitted(boolean appCommitted) {

    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public OutputStream getStream() {
        return outputStream;
    }

    @Override
    public void setStream(OutputStream stream) {
        outputStream = stream;
    }

    @Override
    public boolean isSuspended() {
        return this.suspended;
    }

    @Override
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    @Override
    public void setError() {
        err = true;
    }

    @Override
    public boolean isError() {
        return err;
    }

    @Override
    public ServletOutputStream createOutputStream() throws IOException {
        return null;
    }

    @Override
    public void finishResponse() throws IOException {

    }

    @Override
    public int getContentLength() {
        return (int) contentLength;
    }

    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return null;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        encoding = charset;
    }

    @Override
    public void setContentLength(int len) {
        contentLength = len;
    }

    @Override
    public void setContentLengthLong(long len) {
        contentLength = len;
    }

    @Override
    public void setContentType(String type) {
        contentType = type;
    }

    /**
     * fixme 使用netty但是仅仅用byteBuf
     * 刷新body buffer
     * 因为我用的netty，所以这个方法没有意义
     * 这个方法本来应该刷新buffer到socket stream中。。
     *
     * @throws IOException
     */
    @Override
    public void flushBuffer() throws IOException {
//        if (!isCommitted ()) {
//            setCommitted (true);
//        } else {
//            throw new IOException ("已commit，无法再flushBuffer了");
//        }
    }

    @Override
    public void resetBuffer() {
        byteBuf.clear ();
    }

    @Override
    public PrintWriter getReporter() {
        return null;
    }

    @Override
    public void recycle() {
        resetBuffer ();
        outputStream = null;

        committed = false;
        suspended = false;
        // connector is NOT reset when recycling
        contentLength = -1;
        contentType = null;
        encoding = null;
        locale = Locale.getDefault ();
        request = null;
        servletOutputStream = null;
        writer = null;
        err = false;
    }
}
