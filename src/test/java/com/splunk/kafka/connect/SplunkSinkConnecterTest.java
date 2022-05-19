/*
 * Copyright 2017 Splunk, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.splunk.kafka.connect;

import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.KERBEROS_KEYTAB_PATH_CONF;
import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.KERBEROS_USER_PRINCIPAL_CONF;
import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.TOKEN_CONF;
import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.URI_CONF;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.junit.Assert;
import org.junit.jupiter.api.Test;


import java.util.*;

class SplunkSinkConnecterTest {
    private static final String TEST_URI = "http://localhost:8000";
    private static final String TEST_TOKEN = "ab12351s";
    private static final String TEST_KERB_PRINCIPAL = "testuser";
    private static final String TEST_KERB_KEYTAB_PATH = "krb.keytab";

    @Test
    void startStop() {
        SinkConnector connector = new SplunkSinkConnector();
        Map<String, String> taskConfig = new HashMap<>();
        taskConfig.put("a", "b");

        connector.start(taskConfig);
        List<Map<String, String>> tasks = connector.taskConfigs(10);
        Assert.assertEquals(10, tasks.size());
        for (Map<String, String> task: tasks) {
            Assert.assertEquals(taskConfig, task);
        }

        connector.stop();
    }

    @Test
    public void taskClass() {
        SinkConnector connector = new SplunkSinkConnector();
        Class<? extends Task> taskClass = connector.taskClass();
        Assert.assertEquals(SplunkSinkTask.class, taskClass);
    }

    @Test
    public void config() {
        SinkConnector connector = new SplunkSinkConnector();
        ConfigDef config = connector.config();
        Assert.assertNotNull(config);
    }
    
    @Test
    public void testValidKerberosBothEmpty() {
        final Map<String, String> configs = new HashMap<>();
        addNecessaryConfigs(configs);
        SinkConnector connector = new SplunkSinkConnector();
        Config result = connector.validate(configs);
        assertNoErrors(result);
    }

    @Test
    public void testValidKerberosBothSet() {
        final Map<String, String> configs = new HashMap<>();
        addNecessaryConfigs(configs);
        configs.put(KERBEROS_USER_PRINCIPAL_CONF, TEST_KERB_PRINCIPAL);
        configs.put(KERBEROS_KEYTAB_PATH_CONF, TEST_KERB_KEYTAB_PATH);
        SinkConnector connector = new SplunkSinkConnector();
        Config result = connector.validate(configs);
        assertNoErrors(result);
    }

    @Test
    public void testInvalidKerberosOnlyPrincipalSet() {
        final Map<String, String> configs = new HashMap<>();
        addNecessaryConfigs(configs);
        configs.put(KERBEROS_USER_PRINCIPAL_CONF, TEST_KERB_PRINCIPAL);
        SplunkSinkConnector connector = new SplunkSinkConnector();
        Config result = connector.validate(configs);
        assertHasErrorMessage(result, KERBEROS_USER_PRINCIPAL_CONF, "must be set");
        assertHasErrorMessage(result, KERBEROS_KEYTAB_PATH_CONF, "must be set");
    }

    @Test
    public void testInvalidKerberosOnlyKeytabSet() {
        final Map<String, String> configs = new HashMap<>();
        addNecessaryConfigs(configs);
        configs.put(KERBEROS_KEYTAB_PATH_CONF, TEST_KERB_KEYTAB_PATH);
        SplunkSinkConnector connector = new SplunkSinkConnector();
        Config result = connector.validate(configs);
        assertHasErrorMessage(result, KERBEROS_USER_PRINCIPAL_CONF, "must be set");
        assertHasErrorMessage(result, KERBEROS_KEYTAB_PATH_CONF, "must be set");
    }

    private void addNecessaryConfigs(Map<String, String> configs) {
        configs.put(URI_CONF, TEST_URI);
        configs.put(TOKEN_CONF, "blah");
    }

    private void assertHasErrorMessage(Config config, String property, String msg) {
        for (ConfigValue configValue : config.configValues()) {
            if (configValue.name().equals(property)) {
                assertFalse(configValue.errorMessages().isEmpty());
                assertTrue(configValue.errorMessages().get(0).contains(msg));
            }
        }
    }

    private void assertNoErrors(Config config) {
        config.configValues().forEach(c -> assertTrue(c.errorMessages().isEmpty()));
    }
}