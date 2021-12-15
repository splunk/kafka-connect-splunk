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

import com.splunk.hecclient.Hec;
import com.splunk.hecclient.HecConfig;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SplunkSinkConnector extends SinkConnector {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkConnector.class);
    private Map<String, String> taskConfig;

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
    public Config validate(Map<String, String> connectorConfigs) {
        Config config = super.validate(connectorConfigs);
        SplunkSinkConnectorConfig connectorConfig;
        try {
            connectorConfig = new SplunkSinkConnectorConfig(connectorConfigs);
        } catch (Exception e) {
            log.warn("Validating configuration caught an exception", e);
            return config;
        }

        Map<String, ConfigValue> configValues =
            config.configValues()
                .stream()
                .collect(Collectors.toMap(
                    ConfigValue::name,
                    Function.identity()));

        validateAccess(connectorConfig, configValues);

        return config;
    }

    /**
     * We validate access by posting an empty payload to the Splunk endpoint.
     *
     * For a valid endpoint and a valid token, this returns a HTTP 400 Bad Request with the
     * payload: {"text":"No data","code":5}
     * For a valid endpoint and an invalid token, this returns a HTTP 403 Forbidden with the
     * payload: {"text":"Invalid token","code":4}
     * For an invalid hostname and other errors, the Java UnknownHostException and other similar
     * Exceptions are thrown.
     *
     * @param connectorConfig The connector configuration
     * @param configValues The configuration ConfigValues
     */
    private void validateAccess(SplunkSinkConnectorConfig connectorConfig, Map<String, ConfigValue> configValues) {
        HecConfig hecConfig = connectorConfig.getHecConfig();

        try (CloseableHttpClient httpClient = createHttpClient(hecConfig)) {
            List<String> uris = hecConfig.getUris();

            Map<String, String> validationFailedIndexers = new LinkedHashMap<>();

            for (String uri : uris) {
                log.trace("Validating " + uri);
                HttpPost request = new HttpPost(uri + "/services/collector");
                request.setEntity(new StringEntity(""));

                request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Splunk %s", hecConfig.getToken()));

                int status = -1;
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    status = response.getStatusLine().getStatusCode();
                    if (status == 400) {
                        log.trace("Validation succeeded for indexer {}", uri);
                    } else if (status == 403) {
                        log.trace("Invalid HEC token for indexer {}", uri);
                        validationFailedIndexers.put(uri, response.getStatusLine().toString());
                    } else {
                        log.trace("Validation failed for {}", uri);
                        validationFailedIndexers.put(uri, response.getStatusLine().toString());
                    }
                } catch (Exception e) {
                    log.error("Caught exception while validating", e);
                    validationFailedIndexers.put(uri, e.getMessage());
                }
            }

            if (!validationFailedIndexers.isEmpty()) {
                log.trace("Validation failed: " + validationFailedIndexers);
                recordErrors(configValues,
                    "Validation Failed: " + validationFailedIndexers,
                    SplunkSinkConnectorConfig.URI_CONF,
                    SplunkSinkConnectorConfig.TOKEN_CONF);
            }
        } catch (IOException e) {
            log.error("Configuration validation error", e);
            recordErrors(configValues,
                "Configuration validation error: " + e.getMessage(),
                SplunkSinkConnectorConfig.URI_CONF,
                SplunkSinkConnectorConfig.TOKEN_CONF);
        }
    }

    // Enables mocking during testing
    CloseableHttpClient createHttpClient(final HecConfig config) {
        return Hec.createHttpClient(config);
    }

    void recordErrors(Map<String, ConfigValue> configValues, String message, String...keys) {
        for (String key : keys) {
            configValues.get(key).addErrorMessage(message);
        }
    }
}
