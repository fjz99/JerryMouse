package com.example.valve.basic;

import com.example.Host;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.valve.AbstractValve;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @date 2021/12/30 20:46
 */
public class StandardEngineValve extends AbstractValve {
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        if (!(request.getRequest () instanceof HttpServletRequest) ||
                !(response.getResponse () instanceof HttpServletResponse)) {
            return;
        }

        Host host = ((Host) getContainer ().map (request, true));
        if (host == null) {
            if (!response.isError ()) {
                ((HttpServletResponse) response).setStatus (HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }

        host.invoke (request, response);
    }
}
