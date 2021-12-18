package com.example.logger;

import static com.example.util.PropertyManager.define;

/**
 * 常量
 */
public final class Constants {
    public static final String LEVEL_NAME = define ("log.level", "TRACE");
    public static final Level LEVEL = Level.valueOf (LEVEL_NAME.toUpperCase ());

    public static final String PREFIX = define ("log.prefix", "mouse.");
    public static final String POSTFIX = define ("log.postfix", ".log");

    public static final int MAX_DAYS = (int) define ("log.maxDays", "5", Integer.class);

    public static final boolean ROTATABLE = (boolean) define ("log.rotatable", "true", Boolean.class);
}
