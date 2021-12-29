package com.example.core;

import com.example.filter.FilterConfigImpl;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;

/**
 * 维护了一个filter chain，具体的filter和servlet从外部添加<p>
 * 当chain到达末尾后，就会执行{@link Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}<p>
 * 使用tomcat8的实现，借助数组，更轻量级
 *
 * @date 2021/12/25 17:13
 */
@Slf4j
public final class FilterChainImpl implements FilterChain {

    /**
     * 数组扩容大小
     */
    private static final int CAPACITY_INCREMENT = 10;
    private FilterConfig[] chain = new FilterConfig[CAPACITY_INCREMENT];
    /**
     * 下一个使用的filter的位置和filter总数
     */
    private int pos = 0;
    private int n = 0;

    private Servlet servlet;

    /**
     * 源码中有try catch是为了打印日志和触发监听器
     * 异常还是要抛出去的
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        Objects.requireNonNull (servlet, "servlet == null");

        if (pos < n) {
            FilterConfig config = chain[pos++];
            Filter filter = ((FilterConfigImpl) config).getFilter ();
            filter.doFilter (request, response, this);
        } else if (pos == n) {
            //到末尾了
            servlet.service (request, response);
            pos++;
        } else {
            throw new IllegalStateException ("filter chain pos err");
        }
    }

    //检验pos==n??
    public void reset() {
        pos = 0;
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }

    public void addFilter(FilterConfig filterConfig) {
        Objects.requireNonNull (filterConfig);

        //防止多次add
        for (FilterConfig config : chain) {
            if (config == filterConfig) {
                return;
            }
        }

        if (n == chain.length) {
            FilterConfig[] arr = new FilterConfig[n + CAPACITY_INCREMENT];
            System.arraycopy (chain, 0, arr, 0, n);
            chain = arr;
        }
        chain[n++] = filterConfig;
    }

    public void release() {
        pos = 0;
        n = 0;
        Arrays.fill (chain, null);//避免内存泄漏
        servlet = null;
    }

}
