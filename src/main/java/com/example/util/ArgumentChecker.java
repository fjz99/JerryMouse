package com.example.util;

import java.util.Objects;

/**
 * @date 2021/12/25 21:10
 */
public class ArgumentChecker {
    private ArgumentChecker() {

    }

    public static void requireNonNull(Object... o) {
        Objects.requireNonNull (o);
        for (Object o1 : o) {
            Objects.requireNonNull (o1);
        }
    }
}
