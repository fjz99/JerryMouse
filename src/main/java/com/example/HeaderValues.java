package com.example;

import io.netty.handler.codec.http.HttpHeaderValues;

/**
 * @date 2022/1/5 13:51
 */
public final class HeaderValues {
    public static final String KEEP_ALIVE = HttpHeaderValues.KEEP_ALIVE.toString ();
    public static final String CLOSE = HttpHeaderValues.CLOSE.toString ();
    public static final String APPLICATION_JSON = HttpHeaderValues.APPLICATION_JSON.toString ();
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString ();

}
