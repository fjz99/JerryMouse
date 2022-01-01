package com.example.startup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalinaTest {

    @Test
    void start() {
        String[] args = new String[]{"--config", "/conf/server.xml", "start"};
        new Catalina ().process (args);
    }
}
