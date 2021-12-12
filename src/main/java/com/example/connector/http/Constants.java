package com.example.connector.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;

import java.time.format.DateTimeFormatter;

/**
 * @date 2021/12/8 19:52
 */
public final class Constants {
    public static final int MAX_BODY_LENGTH = 65536;//超过就会忽略
    //    public static final int WRITE_BUFFER_SIZE = 1024;//即写body的时候，会持续写
    public static final String DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern (DATE_TIME_FORMAT);
    public static final String SERVER_INFO = "JerryMouse (HTTP/1.1 Connector)";

    //headers
    public static final String CONTENT_LENGTH = HttpHeaderNames.CONTENT_LENGTH.toString ();
    public static final String CONTENT_TYPE = HttpHeaderNames.CONTENT_TYPE.toString ();
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString ();
    public static final String COOKIE = HttpHeaderNames.COOKIE.toString ();
    public static final String ACCEPT_LANGUAGE = HttpHeaderNames.ACCEPT_LANGUAGE.toString ();
    public static final String CONNECTION = HttpHeaderNames.CONNECTION.toString ();
    public static final String KEEP_ALIVE = HttpHeaderValues.KEEP_ALIVE.toString ();
    public static final String CLOSE = HttpHeaderValues.CLOSE.toString ();
    public static final String APPLICATION_JSON = HttpHeaderValues.APPLICATION_JSON.toString ();
    public static final String LOCATION = HttpHeaderNames.LOCATION.toString ();
}
