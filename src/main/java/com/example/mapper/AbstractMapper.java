package com.example.mapper;

import com.example.Container;
import com.example.Mapper;

/**
 * @date 2021/12/29 16:22
 */
public abstract class AbstractMapper implements Mapper {

    protected Container container = null;
    protected String protocol = null;


    public Container getContainer() {
        return (container);
    }


    public void setContainer(Container container) {
        this.container = container;
    }

    public String getProtocol() {
        return (this.protocol);
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

}
