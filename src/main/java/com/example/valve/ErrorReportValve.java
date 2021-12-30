package com.example.valve;

import com.example.Globals;
import com.example.connector.HttpResponse;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.connector.http.HttpResponseImpl;
import com.example.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

/**
 * 实现默认的错误页面
 *
 * @date 2021/12/29 19:38
 */
@Slf4j
public class ErrorReportValve extends AbstractValve {

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        //先处理完请求
        super.invoke (request, response);

        if (!shouldSendErrorPage (request, response)) {
            return;
        }

        Object err = ((ServletRequest) request).getAttribute (Globals.EXCEPTION_ATTR);

        if (err != null) {
            //说明有异常

            //重置，这样清除所有的装填body、header等
            ((ServletResponse) response).reset ();
            response.setError ();
            ((HttpServletResponse) response).sendError (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        //假如SendErr方法调用了，那就会设置挂起为true
        //设置挂起为false，从而使stream可以工作，因为stream在挂起时无法write、close等
        //但对我的代码来说无所谓
        response.setSuspended (false);

        sendHTML ((HttpServletRequest) request, (HttpServletResponse) response);
    }

    /**
     * 判断是否需要发送错误html页面
     */
    private boolean shouldSendErrorPage(Request request, Response response) {
        if (!(request instanceof HttpServletRequest) ||
                !(response instanceof HttpServletResponse)) {
            return false;
        }

        int status = ((HttpServletResponse) response).getStatus ();
        if (status < 300 ||
                status == HttpServletResponse.SC_NOT_MODIFIED) {
            return false;
        }
        return true;
    }

    /**
     * 发送JSON类型的错误
     * TODO
     */
    private void sendJSON(HttpServletRequest request, HttpServletResponse response) {

    }

    /**
     * 发送HTML类型的错误
     */
    private void sendHTML(HttpServletRequest request, HttpServletResponse response) {
        int statusCode = response.getStatus ();
        //filter进行html转义
        String message = RequestUtil.filter (((HttpResponse) response).getMessage ());
        if (message == null)
            message = "";
        Throwable rootCause = null;
        Throwable throwable = ((Throwable) request.getAttribute (Globals.EXCEPTION_ATTR));

        if (throwable != null) {
            if (throwable instanceof ServletException)
                rootCause = ((ServletException) throwable).getRootCause ();
        }


        String report = String.format ("http: %s, %s", statusCode, message);
        StringBuilder sb = new StringBuilder ();

        sb.append ("<html><head><title>");
        sb.append (Globals.SERVER_INFO_WITH_NO_VERSION).append (" - ");
        sb.append ("错误报告页面");
        sb.append ("</title>");
        sb.append ("<meta charset=\"utf-8\">");
        sb.append ("<STYLE><!--");
        sb.append ("H1{font-family : sans-serif,Arial,Tahoma;color : white;background-color : #0086b2;} ");
        sb.append ("H3{font-family : sans-serif,Arial,Tahoma;color : white;background-color : #0086b2;} ");
        sb.append ("BODY{font-family : sans-serif,Arial,Tahoma;color : black;background-color : white;} ");
        sb.append ("B{color : white;background-color : #0086b2;} ");
        sb.append ("HR{color : #0086b2;} ");
        sb.append ("--></STYLE> ");
        sb.append ("</head><body>");
        sb.append ("<h1>");
        sb.append ("status=").append (statusCode);
        sb.append (",message=").append (message).append ("</h1>");
        sb.append ("<HR size=\"1\" noshade>");
        sb.append ("<p><b>type</b> ");
        if (throwable != null) {
            sb.append ("异常报告");
        } else {
            sb.append ("错误码报告");
        }
        sb.append ("</p>");
        sb.append ("<p><b>");
        sb.append ("message");
        sb.append ("</b> <u>");
        sb.append (message).append ("</u></p>");
        sb.append ("<p><b>");
        sb.append ("description");
        sb.append ("</b> <u>");
        sb.append (report);
        sb.append ("</u></p>");

        if (throwable != null) {
            StringWriter stackTrace = new StringWriter ();
            throwable.printStackTrace (new PrintWriter (stackTrace));
            sb.append ("<p><b>");
            sb.append ("exception");
            sb.append ("</b> <pre>");
            sb.append (stackTrace);
            sb.append ("</pre></p>");
            if (rootCause != null) {
                stackTrace = new StringWriter ();
                rootCause.printStackTrace (new PrintWriter (stackTrace));
                sb.append ("<p><b>");
                sb.append ("rootCause");
                sb.append ("</b> <pre>");
                sb.append (stackTrace);
                sb.append ("</pre></p>");
            }
        }

        sb.append ("<HR size=\"1\" noshade>");
        sb.append ("<h3>").append (Globals.SERVER_INFO_WITH_AUTHOR).append ("</h3>");
        sb.append ("</body></html>");

        try {
            Writer writer = ((HttpResponse) response).getReporter ();
            if (writer != null) {
                Locale locale = Locale.getDefault ();
                try {
                    response.setContentType ("text/html");
                    response.setLocale (locale);
                } catch (Throwable e) {
                    log.error ("status.setContentType", e);
                }
                // If writer is null, it's an indication that the response has
                // been hard committed already, which should never happen
                writer.write (sb.toString ());
                writer.flush ();
            }
        } catch (IOException | IllegalStateException ignored) {

        }

        log.error ("全局异常处理器捕获异常 {},status={},并返回默认HTML错误页面", throwable, response.getStatus ());
    }
}
