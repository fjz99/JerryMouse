package com.example.valve.basic;

import com.example.Context;
import com.example.Wrapper;
import com.example.connector.HttpRequest;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.valve.AbstractValve;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 通过mapper进行转发给wrapper
 *
 * @date 2021/12/28 14:27
 */
public class StandardContextValve extends AbstractValve {


    /**
     * 访问静态资源使用default servlet
     * 但是也要保护web-inf和meta-inf文件夹不被静态访问
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (!(request.getRequest () instanceof HttpServletRequest) ||
                !(response.getResponse () instanceof HttpServletResponse)) {
            return;
        }

        HttpServletRequest hreq = (HttpServletRequest) request.getRequest ();
        HttpServletResponse hresp = ((HttpServletResponse) response.getResponse ());

        /*
         * 访问静态资源使用default servlet
         * 但是也要保护web-inf和meta-inf文件夹不被静态访问
         * Context path就是httpxxx的前缀，而requri是http请求uri全写，relative url就是/xx/xx/xx的相对路径
         */
        String contextPath = hreq.getContextPath ();
        String requestURI = ((HttpRequest) request).getDecodedRequestURI ();
        String relativeURI = requestURI.substring (contextPath.length ()).toUpperCase ();
        if (relativeURI.equals ("/META-INF") ||
                relativeURI.equals ("/WEB-INF") ||
                relativeURI.startsWith ("/META-INF/") ||
                relativeURI.startsWith ("/WEB-INF/")) {
            hresp.sendError (HttpServletResponse.SC_NOT_FOUND);
        }
        Wrapper wrapper;
        try {
            wrapper = (Wrapper) getContainer ().map (request, true);
        } catch (IllegalArgumentException e) {
            hresp.sendError (HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (wrapper == null) {
            hresp.sendError (HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContext (((Context) getContainer ()));
        wrapper.invoke (request, response);
    }
}
