package com.example.connector.http;

import com.example.connector.AbstractRequest;
import com.example.connector.HttpRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 目前只有1.1版本
 *
 * @date 2021/12/8 20:22
 */
public class httpRequestImpl extends AbstractRequest implements HttpRequest, HttpServletRequest {
    protected Map<String, String> headers;
    protected Map<String, Cookie> cookies;
    protected Map<String, String> parameterMap;
    protected String method;
    protected String queryString;
    protected String contextPath;
    protected String requestURI;
    protected String decodedRequestUR;
    protected String servletPath;
}
