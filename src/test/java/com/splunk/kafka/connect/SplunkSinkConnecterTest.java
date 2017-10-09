package com.splunk.kafka.connect;

import org.apache.kafka.common.config.ConfigDef;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.*;

@DisplayName("SplunkSinkConnector Tests")
class SplunkSinkConnecterTest {
    SplunkSinkConnector ssc;

    @BeforeEach void init() {
        ssc = new SplunkSinkConnector();
    }

    @Test @DisplayName("Test version of SplunkSinkConnector") void createConnectorAndCheckVersion(TestInfo testInfo) {
        assertEquals("1.0.0", ssc.version());
    }

    @Test @DisplayName("Test ConfigDef of SplunkSinkConnector") void createConnectorAndCheckConfig(TestInfo testInfo) {
        ConfigDef def = ssc.config();
        Set<String> names = def.names();

        /*
        Map<String,ConfigDef.ConfigKey> configKeyMap = def.configKeys();
        System.out.println(configKeyMap.keySet());

        ConfigDef.ConfigKey hec_uri = configKeyMap.get("splunk.hec.uri");
        def.getConfigValue(hec_uri,"splunk.hec.uri");
        */
        //Attempted to validate values however protected access restricts checking values after created.

        //Ensure Hec Token and URI are configured and present
        assertTrue(names.contains("splunk.hec.token"));
        assertTrue(names.contains("splunk.hec.uri"));
    }
}