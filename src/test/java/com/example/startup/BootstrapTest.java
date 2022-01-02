package com.example.startup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BootstrapTest {

    /**
     * 会降级到user.dir work dir所以能运行
     */
    @Test
    void test() {
        Bootstrap.main (new String[]{"start"});
    }

}
