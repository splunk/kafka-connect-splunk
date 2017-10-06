package com.splunk.kafka.connect;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;


@DisplayName("SplunkSinkTask Tests")
class SplunkSinkTaskTest {
    SplunkSinkTask sst;

    @BeforeEach void init() {
        sst = new SplunkSinkTask();
    }

    @Test @DisplayName("Test version of SplunkSinkTask") void createTaskAndCheckVersion(TestInfo testInfo) {
        assertEquals("1.0.0", sst.version());
    }
}