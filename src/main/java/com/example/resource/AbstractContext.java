package com.example.resource;

import java.util.Collection;

/**
 * @date 2021/12/18 12:25
 */
public abstract class AbstractContext {
    /**
     * 即webroot的位置
     */
    private String docBase;

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        if (docBase == null) {
            throw new IllegalArgumentException ("docBase null");
        }

        this.docBase = docBase;
    }

    /**
     * Retrieves the named object.
     *
     * @param name the name of the object to look up
     * @return the object bound to name
     */
    public abstract Object lookup(String name);

    public abstract ResourceAttributes getAttributes(String name);

    /**
     * 获得所有的file和dir
     *
     * @param path 某个目录下,例如/WEB-INF/lib,必须是doc base为work dir的路径
     */
    public abstract Collection<Object> list(String path);
}
