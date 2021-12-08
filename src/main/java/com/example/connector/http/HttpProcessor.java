package com.example.connector.http;

import com.example.connector.HttpRequest;
import com.example.connector.HttpResponse;

/**
 * HttpProcessor内部是单线程的，Connector会保证HttpProcessor会被单线程访问
 *
 * @date 2021/12/8 19:55
 */
public final class HttpProcessor {
    private Connector connector;
    private HttpRequest request;
    private HttpResponse response;
}
