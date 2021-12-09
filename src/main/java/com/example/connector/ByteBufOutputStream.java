package com.example.connector;

import io.netty.buffer.ByteBuf;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 将netty的byteBuf包装为一个Stream
 *
 * @date 2021/12/8 21:05
 */
public class ByteBufOutputStream extends OutputStream {
    protected boolean closed = false;
    protected ByteBuf byteBuf;//相当于缓冲区byte[]了,自动扩容

    public ByteBufOutputStream(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException ("stream closed");
        }
        byteBuf.writeByte (b);
    }

    /**
     * 刷新流没有意义，因为都存在byteBuf中了
     */
    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException ("stream closed");
        }
        closed = true;
    }
}
