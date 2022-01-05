package com.example.connector.http;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * @date 2021/12/8 19:52
 */
public final class Constants {
    public static final int MAX_BODY_LENGTH = 65536;//超过就会忽略
    //    public static final int WRITE_BUFFER_SIZE = 1024;//即写body的时候，会持续写
    public static final SimpleDateFormat DATE_TIME_FORMATTER =
            new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
}
