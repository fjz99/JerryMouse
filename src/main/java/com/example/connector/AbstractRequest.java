package com.example.connector;

import com.example.connector.http.Connector;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.BufferedReader;
import java.util.Locale;

/**
 * 不区分协议的req,即这个可以认为是针对TCP的
 *
 * @date 2021/12/8 20:23
 */
public abstract class AbstractRequest implements Request, ServletRequest {
    //tcp
    protected String protocol;
    protected String remoteAddress;
    protected int remotePort;
    protected String localAddress;
    protected int localPort;//我的port
    protected String serverName;
    protected boolean secure;
    protected String scheme;
    protected Locale locale;

    //servlet规范
    protected ServletContext servletContext;
    protected DispatcherType dispatcherType;
    protected ServletInputStream inputStream;
    protected BufferedReader reader;

    //其他组件
    protected Connector connector;
    protected Response response;

}
