package com.example.util;

import org.junit.jupiter.api.Test;

import static com.example.util.PropertyManager.define;
import static org.junit.jupiter.api.Assertions.*;

class PropertyManagerTest {

    @Test
    public void test() {
        System.setProperty ("abc", "111");
        String a = define ("abc", "fff");
        System.out.println (PropertyManager.getManager ().getProperty ("abc"));
        System.out.println (a);
        assertEquals (a, "111");
        assertEquals (PropertyManager.getManager ().getProperty ("abc"), "111");
    }

    @Test
    public void test2() {
        String a = define ("abc", "fff");
        System.out.println (PropertyManager.getManager ().getProperty ("abc"));
        System.out.println (a);
        assertEquals (a, "fff");
        assertEquals (PropertyManager.getManager ().getProperty ("abc"), "fff");
    }

    @Test
    public void testType() {
        Integer a = (Integer) define ("abc", "123", Integer.class);
        System.out.println (a);
        assertEquals (a, 123);
    }
}
