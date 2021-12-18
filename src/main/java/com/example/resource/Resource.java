package com.example.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 资源抽象
 */
public abstract class Resource {

    /**
     * Binary content.
     */
    protected byte[] binaryContent = null;
    /**
     * Input stream.
     */
    protected InputStream inputStream = null;


    public Resource() {
    }

    public Resource(InputStream inputStream) {
        setContent (inputStream);
    }


    public Resource(byte[] binaryContent) {
        setContent (binaryContent);
    }

    /**
     * Content accessor.
     *
     * @return InputStream
     */
    public InputStream streamContent()
            throws IOException {
        if (binaryContent != null) {
            return new ByteArrayInputStream (binaryContent);
        }
        return inputStream;
    }

    /**
     * Content accessor.
     *
     * @return binary content
     */
    public byte[] getContent() {
        return binaryContent;
    }

    /**
     * Content mutator.
     *
     * @param inputStream New input stream
     */
    public void setContent(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Content mutator.
     *
     * @param binaryContent New bin content
     */
    public void setContent(byte[] binaryContent) {
        this.binaryContent = binaryContent;
    }

    /**
     * 获得对应的属性
     */
    public abstract ResourceAttributes getAttribute();

}
