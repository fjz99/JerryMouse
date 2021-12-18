package com.example.logger;

import java.io.PrintWriter;

/**
 * @date 2021/12/17 18:53
 */
public abstract class FileLogger extends AbstractLogger {
    private final String prefix = "mouse.";
    private final String suffix = ".log";


    private PrintWriter writer = null;

}
