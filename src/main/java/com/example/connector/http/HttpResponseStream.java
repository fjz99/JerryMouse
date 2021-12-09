package com.example.connector.http;

import com.example.connector.ByteBufOutputStream;
import com.example.connector.HttpResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @date 2021/12/8 19:57
 */
public class HttpResponseStream extends ServletOutputStream {
    protected List<WriteListener> listeners = new ArrayList<> ();
    protected ByteBufOutputStream outputStream;
    protected HttpResponse response;

    public HttpResponseStream(ByteBufOutputStream outputStream) {
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

    @Override
    public void flush() throws IOException {
        super.flush ();
    }
}
