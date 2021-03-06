package com.example.connector.http;

import com.example.Container;
import com.example.Service;
import com.example.connector.Connector;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.life.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.ThreadPerTaskExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @date 2021/12/8 19:55
 */
@Slf4j
public class HttpConnector extends LifecycleBase implements Connector {
    private static final String info = "com.example.connector.http.HttpConnector：一个 http connector";
    private final List<HttpProcessor> runningProcessors = new ArrayList<> ();
    private int port = 8080;
    private Container container;
    private String scheme = "http";
    private boolean secure = false;
    private NioEventLoopGroup group;
    private Service service;//为了使server.xml解析正确执行

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    //为了使server.xml解析正确执行
    public Container getContainerInternal() {
        if (container != null) {
            return container;
        }
        if (service != null) {
            return service.getContainer ();
        }
        return null;
    }

    public Container getContainer() {
        return container;
    }

    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * 同步的，启动后会阻塞住
     */
    public synchronized void start() throws LifecycleException {
        super.start ();

        ServerBootstrap b = new ServerBootstrap ();
        group = new NioEventLoopGroup ();
        b.group (group)
                .channel (NioServerSocketChannel.class)
                .childHandler (new ChannelInitializer<SocketChannel> () {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline () //内部是分类型有序的！
                                .addLast ("decoder", new HttpRequestDecoder ())
                                .addLast ("encoder", new HttpResponseEncoder ())
                                .addLast ("aggregator", new HttpObjectAggregator (Constants.MAX_BODY_LENGTH))
//                                代表聚合的消息内容长度不超过512kb。
//                                这样可以接收到完整的报文，包括body
//                                HttpObjectAggregato也是一个ChannelHandler
                                .addLast ("handler", new HttpHandler ());
                    }
                })
                //会影响（但是不是决定）全连接队列accept大小
                .option (ChannelOption.SO_BACKLOG, 128) // determining the number of connections queued
                .childOption (ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        try {
            //等待端口绑定
            b.bind (port).sync ();
            //异步启动
        } catch (InterruptedException e) {
            log.error (this + " err shutdown", e);
        }
        log.info (this + " 启动完成");
    }

    @Override
    public String toString() {
        return String.format ("Connector[%s-%s]", scheme, port);
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        super.stop ();

        //关闭线程池
        group.shutdownGracefully ();

        //关闭所有子组件;因为是组合关系
        //copy一份，防止被processor组件修改
        final List<HttpProcessor> processors = new ArrayList<> (runningProcessors);
        for (HttpProcessor runningProcessor : processors) {
            runningProcessor.stop ();
        }

        log.info ("{} shutdown", this);
    }

    public void removeProcessor(HttpProcessor processor) {
        runningProcessors.remove (processor);
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public boolean getSecure() {
        return secure;
    }

    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public Request createRequest() {
        return new HttpRequestImpl (this);
    }

    @Override
    public Response createResponse() {
        return new HttpResponseImpl (this);
    }

    private class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private final AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
            //交给processor处理,直接同步执行即可
            //仅仅处理一次http请求
            //fixme 先不池化
            HttpProcessor processor = new HttpProcessor (HttpConnector.this);
            //先启动
            try {
                processor.start ();
            } catch (LifecycleException e) {
                e.printStackTrace ();
            }
            runningProcessors.add (processor);
            processor.process (msg, ctx);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            super.channelReadComplete (ctx);
            ctx.flush ();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error ("netty连接异常", cause);
            if (null != ctx) ctx.close ();
        }
    }

}
