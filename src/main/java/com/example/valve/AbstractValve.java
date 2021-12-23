package com.example.valve;

import com.example.Contained;
import com.example.Container;
import com.example.Valve;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.life.LifecycleBase;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * 就是一个链表节点
 *
 * @date 2021/12/23 13:48
 */
public abstract class AbstractValve
        extends LifecycleBase
        implements Valve, Contained {

    protected static final String name = "AbstractValve";

    protected Valve next;

    protected Container container;

    @Override
    public Container getContainer() {
        return container;
    }

    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    @Override
    public Valve getNext() {
        return next;
    }

    @Override
    public void setNext(Valve valve) {
        this.next = valve;
    }

    /**
     * 这样子类就可以不实现这个方法也不用是abstract的了
     */
    @Override
    public void backgroundProcess() {

    }

    /**
     * 最后一个会空指针，所以basic valve不能继续调用
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (getNext () == null) {
            throw new IllegalStateException ("basic valve不能继续调用getNext ().invoke (request, response);");
        }

        getNext ().invoke (request, response);
    }

    @Override
    public String toString() {
        return name + "{" +
                "container=" + container +
                '}';
    }
}
