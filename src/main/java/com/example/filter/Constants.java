package com.example.filter;

/**
 * @date 2021/12/26 18:25
 */
public final class Constants {
    /**
     * 系统的预制的filter不应该用webapp class loader加载
     */
    public static final String SYSTEM_FILTER_PREFIX;

    static {
        String name = FilterConfigImpl.class.getCanonicalName ();
        SYSTEM_FILTER_PREFIX = name.substring (0, name.lastIndexOf ('.'));
        System.out.println ("SYSTEM_FILTER_PREFIX=" + SYSTEM_FILTER_PREFIX);
    }
}
