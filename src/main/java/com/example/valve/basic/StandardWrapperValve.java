package com.example.valve.basic;

import com.example.Context;
import com.example.Wrapper;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.connector.http.HttpRequestImpl;
import com.example.core.FilterChainImpl;
import com.example.filter.FilterChainFactory;
import com.example.valve.AbstractValve;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 负责分发任务给具体的filter chain、servlet
 *
 * @date 2021/12/25 21:19
 */
@Slf4j
public class StandardWrapperValve extends AbstractValve {
    public static final String RETRY_AFTER = HttpHeaderNames.RETRY_AFTER.toString ();

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Wrapper wrapper = (Wrapper) this.container;
        Context context = (Context) container.getParent ();
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;


        //1.检查服务不可用的问题（比如启动失败等）
        if (!context.isAvailable ()) {
            //说明context启动失败
            httpServletResponse.sendError (HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        //wrapper不可用可能是短暂的
        if (!wrapper.isUnavailable ()) {
            processUnavailable (httpServletResponse);
            return;
        }

        //2.分配一个servlet
        Servlet servlet;
        try {
            servlet = wrapper.allocate ();
        } catch (UnavailableException e) {
            wrapper.unavailable (e);
            processUnavailable (httpServletResponse);
            log.error ("wrapper.allocate时，服务不可用", e);
            return;
        } catch (Throwable e) {
            log.error ("wrapper.allocate失败", e);
            sendException (httpServletResponse, e);
            return;
        }

        //3.创建一个filter chain
        //fixme 有问题，因为加上了http://等，需要去掉
        String uri = ((HttpRequestImpl) httpServletRequest).getDecodedRequestURI ();
        FilterChain filterChain = FilterChainFactory.createFilterChain (uri, wrapper, servlet);

        //4.执行
        try {
            filterChain.doFilter (request.getRequest (), response.getResponse ());
        } catch (UnavailableException e) {
            wrapper.unavailable (e);
            processUnavailable (httpServletResponse);
            log.error ("filterChain.doFilter时，服务不可用", e);
        } catch (Throwable e) {
            sendException (httpServletResponse, e);
            log.error ("filterChain.doFilter失败", e);
        } finally {
            //保证一定释放资源
            //5.释放filterChain
            try {
                //其实没啥用
                ((FilterChainImpl) filterChain).release ();
            } catch (Throwable e) {
                //打日志，但是不能sendErr，因为此时servlet程序已经close stream了
                log.error ("filterChain.release失败", e);
            }

            //6.deallocate servlet
            try {
                //其实没啥用
                wrapper.deallocate (servlet);
            } catch (Throwable e) {
                //打日志，但是不能sendErr，因为此时servlet程序已经close stream了
                log.error ("wrapper.deallocate失败", e);
            }

            //7.如果永久不可用，那就unload servlet
            if (wrapper.getAvailable () == Long.MAX_VALUE) {
                try {
                    wrapper.unload ();
                } catch (Throwable e) {
                    //打日志，但是不能sendErr，因为此时servlet程序已经close stream了
                    log.error ("wrapper.unload失败", e);
                }
            }
        }

    }

    private void processUnavailable(HttpServletResponse httpServletResponse) throws IOException {
        long available = ((Wrapper) container).getAvailable ();
        if (available > 0 && available < Long.MAX_VALUE) {
            //等待一段时间后就可用了
            httpServletResponse.addDateHeader (RETRY_AFTER, available);
            httpServletResponse.sendError (HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else if (available == Long.MAX_VALUE) {
            //永久不可用，就返回404
            httpServletResponse.sendError (HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void sendException(HttpServletResponse httpServletResponse, Throwable e) throws IOException {
        httpServletResponse.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

}
