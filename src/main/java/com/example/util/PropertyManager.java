package com.example.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实现配置功能
 * 配置包括一个命名空间（即第一个.之前的字符串），key，defaultValue，value
 * 可以由外部properties读取
 * 例如外部的key为log.level,log.prefix等
 * <p>
 * 还实现了配置覆盖功能，即system.getProperty会覆盖外部properties文件
 * <p>
 * 一旦启动，就不会修改配置了
 *
 * @date 2021/12/17 19:08
 */
public final class PropertyManager {

    private static final PropertyManager manager = new PropertyManager ();
    private static final String DEFAULT_CONFIG_LOCATION = "./server.properties";


    private final Map<String, String> prop;

    private PropertyManager() {
        //因为要提供define方法，所以这个是可变的，就需要是线程安全的
        final Map<String, String> map = new ConcurrentHashMap<> ();

        //配置文件位置
        String property = System.getProperty ("property.location");
        String location = DEFAULT_CONFIG_LOCATION;
        if (property != null) {
            location = property;
        }

        try (FileInputStream fileInputStream = new FileInputStream (location)) {
            Properties properties = new Properties ();
            properties.load (fileInputStream);

            for (Object o : properties.keySet ()) {
                String key = (String) o;
                String sp = System.getProperty (key);

                //优先使用system的property
                if (sp != null) {
                    map.put (key, sp);
                } else {
                    map.put (key, properties.getProperty (key));
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace ();
            System.err.printf ("%s 配置文件加载失败，放弃加载%n\n", location);
        } catch (IOException e) {
            e.printStackTrace ();
            System.err.printf ("%s properties 配置文件解析失败，放弃加载%n\n", location);
        }

        prop = map;
    }

    public static PropertyManager getManager() {
        return manager;
    }

    public static String define(String key, String defaultValue) {
        PropertyManager manager = getManager ();
        String sp = System.getProperty (key);
        if (sp != null) {
            manager.prop.put (key, sp);
            return sp;
        } else if (manager.contains (key)) {
            return manager.getProperty (key);
        } else {
            manager.prop.put (key, defaultValue);
            return defaultValue;
        }
    }

    public static Object define(String key, String defaultValue, Class<?> type) {
        String v = define (key, defaultValue);

        //目前只提供简单的converter
        return convert (type, v);
    }

    private static Object convert(Class<?> type, String v) {
        if (Integer.class == type) {
            return Integer.valueOf (v);
        } else if (Boolean.class == type) {
            return Boolean.valueOf (v);
        } else
            throw new ClassCastException (String.format ("无法转换string到 %s", type.getCanonicalName ()));
    }


    public String getProperty(String key) {
        return getProperty (key, null);
    }

    public String getProperty(String key, String defaultValue) {
        return getManager ().prop.getOrDefault (key, defaultValue);
    }

    public boolean contains(String key) {
        return getManager ().prop.containsKey (key);
    }
}
