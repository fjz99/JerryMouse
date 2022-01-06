package com.example.resource;

//TODO rename

/**
 * 包括文件夹和文件的通用操作，比如获得名字，修改时间等
 */
public interface CommonInterface {
    /**
     * 获得对应的属性
     */
    ResourceAttributes getAttributes();

    /**
     * 查找文件夹内的
     */
    ResourceAttributes getAttributes(String name);
}
