package com.example.logger;

import com.example.Container;

import static com.example.logger.Constants.LEVEL;
import static com.example.logger.Level.*;

/**
 * 固定的log format
 * @date 2021/12/17 18:42
 */
public abstract class AbstractLogger implements Logger {
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
    public boolean isDebugEnabled() {
        return LEVEL.contains (DEBUG);
    }

    @Override
    public boolean isErrorEnabled() {
        return LEVEL.contains (ERROR);
    }

    @Override
    public boolean isFatalEnabled() {
        return LEVEL.contains (FATAL);
    }

    @Override
    public boolean isInfoEnabled() {
        return LEVEL.contains (INFO);
    }

    @Override
    public boolean isTraceEnabled() {
        return LEVEL.contains (TRACE);
    }

    @Override
    public boolean isWarnEnabled() {
        return LEVEL.contains (WARN);
    }

    @Override
    public void trace(Object message) {

    }

    @Override
    public void trace(Object message, Throwable t) {

    }

    @Override
    public void debug(Object message) {

    }

    @Override
    public void debug(Object message, Throwable t) {

    }

    @Override
    public void info(Object message) {

    }

    @Override
    public void info(Object message, Throwable t) {

    }

    @Override
    public void warn(Object message) {

    }

    @Override
    public void warn(Object message, Throwable t) {

    }

    @Override
    public void error(Object message) {

    }

    @Override
    public void error(Object message, Throwable t) {

    }

    @Override
    public void fatal(Object message) {

    }

    @Override
    public void fatal(Object message, Throwable t) {

    }

    /**
     * 如何写入
     */
    public abstract void log(String msg);
}
