package com.example.connector;

import com.example.connector.http.HttpConnector;
import com.example.connector.http.HttpResponseStream;
import com.example.util.StringManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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

    /**
     * 似乎用不到，因为目前是byteBuf读满才发送的
     */
    @Deprecated
    protected boolean committed;//能关闭、刷新，也可以写，允许多次调用flush；？？？

    protected Locale locale = Locale.getDefault ();
    protected HttpConnector httpConnector;
    protected ByteBuf byteBuf;//负责维护buffer
    protected ChannelHandlerContext handlerContext;
    protected StringManager sm;

    /**
     * 把输出流挂起，此时无法写，能关闭，无法刷新；？？？
     * 防止sendErr，sendRedirect后还在写buf
     * 使用静默，不抛出异常
     */
    protected boolean suspended;
    protected boolean err;
    protected PrintWriter writer;

    public void setByteBuf(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public int getBufferSize() {
        return byteBuf.readableBytes ();
    }

    @Override
    public void setBufferSize(int size) {
        //do nothing
    }

    @Override
    public HttpConnector getConnector() {
        return httpConnector;
    }

    @Override
    public void setConnector(HttpConnector httpConnector) {
        this.httpConnector = httpConnector;
    }

    @Override
    public int getContentCount() {
        return byteBuf.readableBytes ();
    }

    /**
     * 和源码中不同
     */
    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void setCommitted(boolean appCommitted) {
        this.committed = appCommitted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        resetBuffer ();
        outputStream = null;
        writer = null;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void setLocale(Locale loc) {
        this.locale = loc;
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
        if (servletOutputStream == null) {
            servletOutputStream = new HttpResponseStream (outputStream);
        }
        return servletOutputStream;
    }

    /**
     * 在复用resp之前调用
     * 就关闭流释放资源即可，源码中还flush了，但我不用
     */
    @Override
    public void finishResponse() throws IOException {
        if (servletOutputStream != null) {
            try {
                servletOutputStream.close ();
            } catch (IOException e) {
                e.printStackTrace ();
            }
        }

        if (writer != null) {
            writer.close ();
        }
    }

    @Override
    public int getContentLength() {
        return (int) contentLength;
    }

    @Override
    public void setContentLength(int len) {
        contentLength = len;
    }

    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        encoding = charset;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String type) {
        contentType = type;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter (new OutputStreamWriter (outputStream));
        }
        return writer;
    }

    @Override
    public void setContentLengthLong(long len) {
        contentLength = len;
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
        if (isError ()) {
            try {
                if (servletOutputStream == null) {
                    createOutputStream ();
                }
            } catch (IOException e) {
                e.printStackTrace ();
                return null;
            }
            return new PrintWriter (servletOutputStream);//其实那个outputStream都无所谓，毕竟都是委托
        } else {
            throw new IllegalStateException ("不是err状态");
        }
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

    @Override
    public ServletResponse getResponse() {
        return this;
    }
}
