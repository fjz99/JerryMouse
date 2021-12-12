package com.example.connector.http;

import com.example.Container;
import com.example.connector.Connector;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.life.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @date 2021/12/8 19:55
 */
public class HttpConnector implements Connector, Lifecycle {
    private final LifeCycleSupport lifeCycleSupport = new LifeCycleSupport (this);
    private final int port = 8080;
    private final String info = "com.example.connector.http.HttpConnector：一个 http connector";
    private final List<HttpProcessor> runningProcessors = new ArrayList<> ();
    private Container container;
    private String scheme = "http";
    private boolean secure = false;
    private ChannelFuture future;
    private NioEventLoopGroup group;
    private boolean started;//fixme start是同步调用还是异步？

    public Container getContainer() {
        return container;
    }

    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifeCycleSupport.addLifecycleListener (listener);
    }

    @Override
    public List<LifecycleListener> findLifecycleListeners() {
        return lifeCycleSupport.getListeners ();
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifeCycleSupport.removeLifecycleListener (listener);
    }

    /**
     * 目前端口等是写死的
     */
    public void start() throws LifecycleException {
        if (started) {
            throw new LifecycleException ("already started");
        }
        started = true;
        lifeCycleSupport.fireLifecycleEvent (EventType.START_EVENT, null);

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
            future = b.bind (port).sync ();
            future.channel ().closeFuture ().sync (); // 例子里没有这行代码 会导致服务器直接退出
        } catch (InterruptedException e) {
            e.printStackTrace ();
            System.out.println ("err shutdown");
        }
    }

    @Override
    public void stop() throws LifecycleException {
        if (!started) {
            throw new LifecycleException ("先start再stop");
        }
        started = false;
        lifeCycleSupport.fireLifecycleEvent (EventType.STOP_EVENT, null);

        //关闭线程池
        group.shutdownGracefully ();

        //关闭所有子组件;因为是组合关系
        //copy一份，防止被processor组件修改
        final List<HttpProcessor> processors = new ArrayList<> (runningProcessors);
        for (HttpProcessor runningProcessor : processors) {
            runningProcessor.stop ();
        }
        System.out.println ("connector shutdown");
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
            System.out.println ("content " + msg.content ().toString (Charset.defaultCharset ()));
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
            System.out.println ("exceptionCaught");
            if (null != cause) cause.printStackTrace ();
            if (null != ctx) ctx.close ();
        }
    }

}