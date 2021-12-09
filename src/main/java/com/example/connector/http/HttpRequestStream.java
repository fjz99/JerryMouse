package com.example.connector.http;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取完才执行
 * 总体简单处理即输入读取、输出到buffer都是完成才发送tcp
 *
 * @date 2021/12/8 19:57
 */
public class HttpRequestStream extends ServletInputStream {
    protected InputStream inputStream;
    protected List<ReadListener> listeners;
    protected int nextByte;

    public HttpRequestStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.listeners = new ArrayList<> ();
    }

    /**
     * fixme
     * 这个需要消除多次读取byteBuf的问题
     * <p>
     * body 读取完
     * 因为包装了netty，所以此时已经获得了所有的body（限制大小）
     */
    @Override
    public boolean isFinished() {
        return nextByte == -1;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        listeners.add (readListener);
    }

    /**
     * 这个方法多次read的话会多次触发notifyDone。。
     */
    @Override
    public int read() throws IOException {
        if (isFinished ()) {
            notifyDone ();//再读一次才会触发
            return -1;
        } else {
            int b = nextByte;
            nextByte = inputStream.read ();
            return b;
        }
    }

    private void notifyDone() {
        if (isFinished ()) {
            for (ReadListener listener : listeners) {
                try {
                    listener.onAllDataRead ();
                } catch (IOException e) {
                    e.printStackTrace ();
                    listener.onError (e);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        inputStream.close ();
    }
}
