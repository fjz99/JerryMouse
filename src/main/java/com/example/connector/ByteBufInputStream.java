package com.example.connector;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;

/**
 * 将netty的byteBuf包装为一个Stream
 *
 * @date 2021/12/8 21:04
 */
public class ByteBufInputStream extends InputStream {
    protected ByteBuf byteBuf;//相当于缓冲区byte[]了
    private boolean closed = false;

    public ByteBufInputStream(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    @Override
    public int read() throws IOException {
        if (closed) {
            throw new IOException ("stream closed");
        }
        if (byteBuf.readableBytes () > 0) {
            return byteBuf.readByte ();
        } else return -1;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException ("stream closed");
        }
        closed = true;
    }
}
