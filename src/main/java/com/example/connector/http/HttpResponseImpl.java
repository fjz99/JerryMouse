package com.example.connector.http;

import com.example.connector.AbstractResponse;
import com.example.connector.HttpResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 目前只有1.1版本
 *
 * @date 2021/12/8 20:26
 */
public class HttpResponseImpl extends AbstractResponse implements HttpResponse, HttpServletResponse {
    private Map<String, String> headers;
    private Map<String, Cookie> cookies;


}
