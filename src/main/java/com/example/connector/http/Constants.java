package com.example.connector.http;

import java.time.format.DateTimeFormatter;

/**
 * @date 2021/12/8 19:52
 */
public final class Constants {
    public static final int MAX_BODY_LENGTH = 65536;//超过就会忽略
    //    public static final int WRITE_BUFFER_SIZE = 1024;//即写body的时候，会持续写
    public static final String DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern (DATE_TIME_FORMAT);
}
