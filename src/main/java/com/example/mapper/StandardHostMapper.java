package com.example.mapper;

import com.example.Container;
import com.example.Context;
import com.example.connector.AbstractRequest;
import com.example.connector.HttpRequest;
import com.example.connector.Request;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

/**
 * TODO 模仿tomcat8，把mapper放到request中
 * 因为mapper逻辑太简单了，没必要用接口
 *
 * @date 2021/12/29 17:14
 */
@Slf4j
public final class StandardHostMapper extends AbstractMapper {
    @Override
    public Container map(Request request, boolean update) {
        if (request.getContext () != null) {
            //已经映射过了
            return request.getContext ();
        }

        String uri = ((HttpRequest) request).getDecodedRequestURI ();

        log.debug ("Mapping request URI '" + uri + "'");
        if (uri == null)
            return null;

        // Match on the longest possible context path prefix
        log.trace ("Trying the longest context path prefix");
        Context context;
        String mapuri = uri;
        while (true) {
            context = (Context) container.findChild (mapuri);
            if (context != null)
                break;
            int slash = mapuri.lastIndexOf ('/');
            if (slash < 0)
                break;
            mapuri = mapuri.substring (0, slash);
        }

        // If no Context matches, select the default Context
        if (context == null) {
            log.trace ("Trying the default context");
            context = (Context) container.findChild ("");
        }

        // Complain if no Context has been selected
        if (context == null) {
            log.warn ("standardHost.mappingError {}", uri);
            return null;
        }

        log.debug (" Mapped to context '" + context.getPath () + "'");

        if (request.getContext () == null) {
            request.setContext (context);
            ((HttpRequest) request).setContextPath (context.getPath ());
        }
        return context;
    }
}
