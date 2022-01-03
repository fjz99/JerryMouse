package com.example.startup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HostConfigTest {

    @Test
    void deploy() {
        Bootstrap.main (new String[]{"start"});
    }
}
