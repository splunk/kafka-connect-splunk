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

import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SplunkSinkConnector extends SinkConnector {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkConnector.class);
    private Map<String, String> taskConfig;
    private Map<String, ConfigValue> values;
    private List<ConfigValue> validations;

    @Override
    public void start(Map<String, String> taskConfig) {
        this.taskConfig = taskConfig;
        log.info("kafka-connect-splunk starts");
    }

    @Override
    public void stop() {
        log.info("kafka-connect-splunk stops");
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> tasks = new ArrayList<>();
        for (int i = 0; i < maxTasks; i++) {
            tasks.add(taskConfig);
        }
        log.info("kafka-connect-splunk discovered {} tasks", tasks.size());
        return tasks;
    }

    @Override
    public Class<? extends Task> taskClass() {
        return SplunkSinkTask.class;
    }

    @Override
    public String version() {
        return VersionUtils.getVersionString();
    }

    @Override
    public ConfigDef config() {
        return SplunkSinkConnectorConfig.conf();
    }

    
    @Override
    public Config validate(final Map<String, String> connectorConfigs) {
        Config config = super.validate(connectorConfigs);
        validations = config.configValues();
        values = validations.stream().collect(Collectors.toMap(ConfigValue::name, Function.identity()));

        validateKerberosConfigs(connectorConfigs);
        return new Config(validations);
    }

    void validateKerberosConfigs(final Map<String, String> configs) {
        final String keytab = configs.getOrDefault(KERBEROS_KEYTAB_PATH_CONF, "");
        final String principal = configs.getOrDefault(KERBEROS_USER_PRINCIPAL_CONF, "");

        if (StringUtils.isNotEmpty(keytab) && StringUtils.isNotEmpty(principal)) {
            return;
        }

        if (keytab.isEmpty() && principal.isEmpty()) {
            return;
        }

        String errorMessage = String.format(
            "Either both or neither '%s' and '%s' must be set for Kerberos authentication. ",
            KERBEROS_KEYTAB_PATH_CONF,
            KERBEROS_USER_PRINCIPAL_CONF
        );
        addErrorMessage(KERBEROS_KEYTAB_PATH_CONF, errorMessage);
        addErrorMessage(KERBEROS_USER_PRINCIPAL_CONF, errorMessage);
    }

    private void addErrorMessage(String property, String error) {
        values.get(property).addErrorMessage(error);
    }
}
