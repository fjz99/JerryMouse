package com.example.valve.basic;

import com.example.Container;
import com.example.Context;
import com.example.connector.HttpRequest;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.session.Session;
import com.example.session.StandardSession;
import com.example.valve.AbstractValve;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * @date 2021/12/29 17:25
 */
@Slf4j
public class StandardHostValve extends AbstractValve {
    private static final long MAX_PAUSED_WAITING_TIME = 2000;

    /**
     * 找不到context返回500
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (!(request.getRequest () instanceof HttpServletRequest) ||
                !(response.getResponse () instanceof HttpServletResponse)) {
            return;
        }

        Context context = ((Context) getContainer ().map (request, true));

        //查看是否paused
        long startTime = System.currentTimeMillis ();
        while (context != null &&
                (System.currentTimeMillis () - startTime) <= MAX_PAUSED_WAITING_TIME &&
                context.getPaused ()) {
            context = ((Context) getContainer ().map (request, true));
            try {
                Thread.sleep (500);
            } catch (InterruptedException ignored) {

            }
        }
        if (context != null && context.getPaused ()) {
            context = null;
        }
        if (context == null) {
            ((HttpServletResponse) response).sendError (HttpServletResponse.SC_NOT_FOUND,
                    "无法找到对应的Context");
            log.error ("请求uri {} 映射不到任何context", ((HttpRequest) request).getDecodedRequestURI ());
            return;
        }


        // Bind the context CL to the current thread
        Thread.currentThread ().setContextClassLoader
                (context.getLoader ().getClassLoader ());

        //激活session，servlet的执行时间忽略不计
        //不创建新的，否则access会把isNew=false
        //因为用了session facade，所以需要手动查找session
        Session session = ((HttpRequest) request).getSessionInternal (false);
        if (session != null) {
            session.access ();
        }

        context.invoke (request, response);
    }

}
