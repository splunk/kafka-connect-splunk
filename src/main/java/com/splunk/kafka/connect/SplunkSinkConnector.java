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
import org.apache.http.client.methods.HttpGet;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SplunkSinkConnector extends SinkConnector {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkConnector.class);
    private Map<String, String> taskConfig;
    final private String COLLECTOR_ENDPOINT = "/services/collector";
    final private String HEALTH_CHECK_ENDPOINT = COLLECTOR_ENDPOINT + "/health";

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

        HecConfig hecConfig = connectorConfig.getHecConfig();
        try (CloseableHttpClient httpClient = createHttpClient(hecConfig)) {

            if (!validateCollector(httpClient, hecConfig, configValues)) {
                return config;
            }

            validateAccess(httpClient, hecConfig, configValues);

        } catch (IOException e) {
            log.error("Configuration validation error", e);
            recordErrors(
                configValues,
                "Configuration validation error: " + e.getMessage(),
                SplunkSinkConnectorConfig.TOKEN_CONF
            );
        }

        return config;
    }

    /**
     * We validate the collector by querying the HEC collector health.
     *
     * For a valid collector endpoint, this returns a HTTP 200 OK with the payload:
     * {"text":"HEC is healthy","code":17}
     * For an invalid hostname and other errors, the Java UnknownHostException and other similar
     * Exceptions are thrown.
     *
     * @param httpClient HEC HTTP Client
     * @param hecConfig The HEC configuration
     * @param configValues The configuration ConfigValues
     * @return Whether the validation was successful
     */
    private boolean validateCollector(
        CloseableHttpClient httpClient,
        HecConfig hecConfig,
        Map<String, ConfigValue> configValues
    ) {
        List<String> uris = hecConfig.getUris();

        Map<String, String> connectionFailedCollectors = new LinkedHashMap<>();

        for (String uri : uris) {
            log.trace("Connecting to " + uri);
            HttpGet request = new HttpGet(uri + HEALTH_CHECK_ENDPOINT);

            int status = -1;
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    log.trace("Connection succeeded for collector {}", uri);
                } else {
                    log.trace("Connection failed for collector {}", uri);
                    connectionFailedCollectors.put(uri, response.getStatusLine().toString());
                }
            } catch (Exception e) {
                log.error("Caught exception while connecting", e);
                connectionFailedCollectors.put(uri, e.getMessage());
            }
        }

        if (!connectionFailedCollectors.isEmpty()) {
            log.trace("Connection failed: " + connectionFailedCollectors);
            recordErrors(
                configValues,
                "Connection Failed: " + connectionFailedCollectors,
                SplunkSinkConnectorConfig.URI_CONF
            );
            return false;
        }

        return true;
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
     * @param httpClient HEC HTTP Client
     * @param hecConfig The HEC configuration
     * @param configValues The configuration ConfigValues
     */
    private void validateAccess(
        CloseableHttpClient httpClient,
        HecConfig hecConfig,
        Map<String, ConfigValue> configValues
    ) throws UnsupportedEncodingException {
        List<String> uris = hecConfig.getUris();

        Map<String, String> accessFailedCollectors = new LinkedHashMap<>();

        for (String uri : uris) {
            log.trace("Validating " + uri);
            HttpPost request = new HttpPost(uri + COLLECTOR_ENDPOINT);
            request.setEntity(new StringEntity(""));

            request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Splunk %s", hecConfig.getToken()));

            int status = -1;
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                status = response.getStatusLine().getStatusCode();
                if (status == 400 || status == 200) {
                    log.trace("Validation succeeded for collector {}", uri);
                } else if (status == 403) {
                    log.trace("Invalid HEC token for collector {}", uri);
                    accessFailedCollectors.put(uri, response.getStatusLine().toString());
                } else {
                    log.trace("Validation failed for {}", uri);
                    accessFailedCollectors.put(uri, response.getStatusLine().toString());
                }
            } catch (Exception e) {
                log.error("Caught exception while validating", e);
                accessFailedCollectors.put(uri, e.getMessage());
            }
        }

        if (!accessFailedCollectors.isEmpty()) {
            log.trace("Validation failed: " + accessFailedCollectors);
            recordErrors(
                configValues,
                "Validation Failed: " + accessFailedCollectors,
                SplunkSinkConnectorConfig.TOKEN_CONF
            );
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
