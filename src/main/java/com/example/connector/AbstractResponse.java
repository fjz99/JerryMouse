package com.example.connector;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.util.Locale;

/**
 * 不区分协议的resp,即这个可以认为是针对TCP的
 *
 * @date 2021/12/8 20:24
 */
public abstract class AbstractResponse implements Response, ServletResponse {
    protected Request request;
    protected String encoding;
    protected String contentType;
    protected ServletOutputStream outputStream;
    protected int contentLength;
    protected int bufferSize;
    protected boolean committed;
    protected Locale locale;


}
