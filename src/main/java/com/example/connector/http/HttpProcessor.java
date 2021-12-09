package com.example.connector.http;

import com.example.connector.HttpRequest;
import com.example.connector.HttpResponse;
import com.example.life.LifeCycleSupport;
import com.example.life.Lifecycle;
import com.example.life.LifecycleException;
import com.example.life.LifecycleListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;

/**
 * HttpProcessor内部是单线程的，Connector会保证HttpProcessor会被单线程访问
 *
 * @date 2021/12/8 19:55
 */
public final class HttpProcessor implements Lifecycle {
    private final LifeCycleSupport lifeCycleSupport = new LifeCycleSupport (this);
    private HttpConnector connector;
    private HttpRequest request;
    private HttpResponse response;
    private FullHttpRequest fullHttpRequest;
    private ChannelHandlerContext handlerContext;
    private int port;

    public HttpProcessor(HttpConnector httpConnector) {
        this.connector = httpConnector;
        this.request = (HttpRequestImpl) connector.createRequest ();
        this.response = (HttpResponseImpl) connector.createResponse ();
        this.port = connector.getPort ();
    }

    /**
     * 处理一个http报文，并给出响应
     */
    public void process(FullHttpRequest request, ChannelHandlerContext handlerContext) {
        this.fullHttpRequest = request;
        this.handlerContext = handlerContext;

        try {

        } catch (Exception e) {

        }
    }

    /**
     * 给req装填header
     */
    private void parseHeaders() {

        System.out.printf ("装填header后为 %s", request);
    }

    /**
     * 给req装填连接信息
     */
    private void parseConnection() {
        request.setProtocol (fullHttpRequest.protocolVersion ().protocolName ());
        request.setServerPort (port);
        request.setMethod (fullHttpRequest.method ().name ());
        request.setRequestURI (fullHttpRequest.uri ().toLowerCase ());
        request.setSecure (connector.getSecure ());
        InetSocketAddress inetSocketAddress = (InetSocketAddress) (handlerContext.channel ()).remoteAddress ();
        request.setRemoteAddress (inetSocketAddress.getHostString ());
        System.out.printf ("装填请求后为 %s", request);
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

    @Override
    public void start() throws LifecycleException {

    }

    @Override
    public void stop() throws LifecycleException {

    }
}
