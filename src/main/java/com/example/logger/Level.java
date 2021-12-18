package com.example.logger;

/**
 * @date 2021/12/17 18:44
 */
public enum Level {
    TRACE (0), INFO (2), WARN (3), DEBUG (1), ERROR (4), FATAL (5);

    private final int order;

    Level(int order) {
        this.order = order;
    }

    public boolean contains(Level level) {
        return this.order <= level.order;
    }
}
