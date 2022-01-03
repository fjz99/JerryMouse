package com.example.startup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HostConfigTest {

    @Test
    void deployDirectory() {
        Bootstrap.main (new String[]{"start"});
    }

    @Test
    void deployWAR() {

    }
}
