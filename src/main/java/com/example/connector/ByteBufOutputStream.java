package com.example.connector;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 将netty的byteBuf包装为一个Stream
 *
 * @date 2021/12/8 21:05
 */
public class ByteBufOutputStream extends OutputStream {
    protected ByteBuf byteBuf;//相当于缓冲区byte[]了,自动扩容

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public ByteBufOutputStream(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public void write(int b) throws IOException {
        byteBuf.writeByte (b);
    }
}
