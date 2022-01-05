package com.example.servlet;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;

/**
 * @date 2022/1/4 20:36
 */
public final class Constants {
    public static final String IF_RANGE = HttpHeaderNames.IF_RANGE.toString ();
    public static final String ACCEPT_RANGES = HttpHeaderNames.ACCEPT_RANGES.toString ();
    public static final String RANGE = HttpHeaderNames.RANGE.toString ();
    public static final String CONTENT_TYPE = HttpHeaderNames.CONTENT_TYPE.toString ();
    public static final String CONTENT_LENGTH = HttpHeaderNames.CONTENT_LENGTH.toString ();
    public static final String ETAG = HttpHeaderNames.ETAG.toString ();
    public static final String LAST_MODIFIED = HttpHeaderNames.LAST_MODIFIED.toString ();
    public static final String CONTENT_RANGE = HttpHeaderNames.CONTENT_RANGE.toString ();
}
