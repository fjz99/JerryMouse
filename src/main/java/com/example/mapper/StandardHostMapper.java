package com.example.mapper;

import com.example.Container;
import com.example.Context;
import com.example.connector.AbstractRequest;
import com.example.connector.HttpRequest;
import com.example.connector.Request;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.TreeMap;

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
        Context context = null;


        for (Container child : getContainer ().findChildren ()) {
            String path = ((Context) child).getPath ();
            if (!path.startsWith ("/")) {
                path = "/" + path;
            }

            if (uri.contains (path)) {
                if (context == null || context.getPath ().length () < path.length ()) {
                    context = ((Context) child);
                }
            }
        }

        // Complain if no Context has been selected
        if (context == null) {
            log.warn ("standardHost.mappingError {}", uri);
            return null;
        }

        log.debug (" Mapped to context '" + context.getPath () + "'");

        if (update) {
            request.setContext (context);
            ((HttpRequest) request).setContextPath (context.getPath ());
        }
        return context;
    }
}
