package com.example.connector.http;

import com.example.Container;
import com.example.life.LifecycleException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

class HttpConnectorTest {
    @Test
    public void test() throws LifecycleException, IOException, ServletException {
        HttpConnector connector = new HttpConnector ();

        //echo server
        Container container = mock (Container.class);
        doAnswer (invocation -> {
            HttpRequestImpl argument = invocation.getArgument (0);
            HttpResponseImpl resp = invocation.getArgument (1);
            System.out.println ("req " + argument);
            System.out.println ("resp " + resp);
//            resp.setHeader (HttpHeaderNames.CONTENT_LENGTH.toString (), "4");
            resp.setHeader (HttpHeaderNames.CONTENT_TYPE.toString (), HttpHeaderValues.APPLICATION_JSON.toString ());
            PrintWriter writer = resp.getWriter ();
            BufferedReader reader = argument.getReader ();
            String s;
            while ((s = reader.readLine ()) != null) {
                writer.write (s);
            }
            writer.flush ();
            argument.headers.forEach ((k, v) ->
                    writer.write (String.format ("%s : %s\n", k, v)));
            return null;
        }).when (container).invoke (any (), any ());
        connector.setContainer (container);

        System.out.println ("start");
        connector.start ();
        System.in.read ();

        System.out.println ("stop");
        connector.stop ();
    }
}
