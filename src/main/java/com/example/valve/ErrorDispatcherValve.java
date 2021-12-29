package com.example.valve;

import com.example.connector.Request;
import com.example.connector.Response;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * 实现用户自定义err page
 * 基于{@link javax.servlet.RequestDispatcher}
 *
 * @date 2021/12/29 19:38
 * TODO
 */
public class ErrorDispatcherValve extends AbstractValve {
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        super.invoke (request, response);
    }
}
