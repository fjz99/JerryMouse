package com.example.connector;

import io.netty.buffer.ByteBuf;

import java.io.InputStream;

/**
 * 将netty的byteBuf包装为一个Stream
 *
 * @date 2021/12/8 21:04
 */
public class ByteBufInputStream extends InputStream {
    protected ByteBuf byteBuf;//相当于缓冲区byte[]了

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public ByteBufInputStream(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public int read() {
        if (byteBuf.readableBytes () > 0) {
            return byteBuf.readByte ();
        } else return -1;
    }
}
