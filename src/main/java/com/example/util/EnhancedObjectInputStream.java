package com.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

/**
 * 主要就是加了classLoader
 * 即很多时候，反序列化也需要规定类加载器，即对象的实例必须对应特定类加载器加载的类
 *
 * @date 2021/12/22 11:41
 */
public class EnhancedObjectInputStream extends ObjectInputStream {
    private final ClassLoader classLoader;

    public EnhancedObjectInputStream(ClassLoader classLoader, InputStream stream) throws IOException {
        super (stream);
        this.classLoader = classLoader;
    }

    /**
     * 使用特定的类加载器
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName ();
        try {
            return Class.forName (name, false, classLoader);
        } catch (ClassNotFoundException e) {
            try {
                // Try also the superclass because of primitive types
                return super.resolveClass (desc);
            } catch (ClassNotFoundException e2) {
                // Rethrow original exception, as it can have more information
                // about why the class was not found. BZ 48007
                throw e;
            }
        }
    }

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces)
            throws IOException, ClassNotFoundException {

        Class<?>[] cinterfaces = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            cinterfaces[i] = classLoader.loadClass(interfaces[i]);
        }

        try {
            return Proxy.getProxyClass(classLoader, cinterfaces);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }
}
