package com.example.connector.http;

import com.example.connector.HttpResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @date 2021/12/8 19:57
 */
public class HttpResponseStream extends ServletOutputStream {
    protected List<WriteListener> listeners = new CopyOnWriteArrayList<> ();
    protected OutputStream outputStream;
    protected HttpResponse response;
    protected volatile boolean closed = false;

    public HttpResponseStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public boolean isReady() {
        return true;//因为buffer可以扩容
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        listeners.add (writeListener);
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write (b);
    }

    /**
     * 刷新流没有意义，因为都存在byteBuf中了
     */
    @Override
    public void flush() {

    }

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException ("stream closed");
        }
        closed = true;
        outputStream.close ();
    }
}
