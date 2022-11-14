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

import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class SplunkSinkConnecterTest {

    final private String SPLUNK_URI_HOST1 = "https://www.host1.com:1111/";

    @Mock
    CloseableHttpResponse okHttpResponse;

    @Mock
    CloseableHttpResponse badRequestHttpResponse;

    @Mock
    CloseableHttpResponse forbiddenHttpResponse;

    @Mock
    CloseableHttpResponse notFoundHttpResponse;

    @Spy
    SplunkSinkConnector connector;

    @Spy
    CloseableHttpClient httpClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(httpClient)
            .when(connector)
            .createHttpClient(any());

        when(okHttpResponse.getStatusLine()).
            thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));
        when(okHttpResponse.getEntity())
            .thenReturn(new StringEntity("{\"text\":\"HEC is healthy\",\"code\":17}", ContentType.APPLICATION_JSON));

        when(badRequestHttpResponse.getStatusLine()).
            thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "Bad Request"));
        when(badRequestHttpResponse.getEntity())
            .thenReturn(new StringEntity("{\"text\":\"No data\",\"code\":5}", ContentType.APPLICATION_JSON));

        when(forbiddenHttpResponse.getStatusLine())
            .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_FORBIDDEN, "Forbidden"));
        when(forbiddenHttpResponse.getEntity())
            .thenReturn(new StringEntity("{\"text\":\"Invalid token\",\"code\":4}", ContentType.APPLICATION_JSON));

        when(notFoundHttpResponse.getStatusLine()).
            thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "Not Found"));
        when(notFoundHttpResponse.getEntity())
            .thenReturn(new StringEntity("<html>Not Found</html>", ContentType.APPLICATION_XHTML_XML));
    }

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
    public void testValidationEmptyConfig() {
        Config config = new SplunkSinkConnector().validate(new HashMap<>());

        Map<String, List<String>> errorMessages = config.configValues().stream()
            .collect(Collectors.toMap(ConfigValue::name, ConfigValue::errorMessages));

        assertFalse(errorMessages.get(SplunkSinkConnectorConfig.URI_CONF).isEmpty());
        assertFalse(errorMessages.get(SplunkSinkConnectorConfig.TOKEN_CONF).isEmpty());
    }

    @Test
    public void testValidationSuccess() throws IOException {
        Map<String, String> connectorConfig = getConnectorConfig();

        doReturn(okHttpResponse)
            .doReturn(badRequestHttpResponse)
            .when(httpClient)
            .execute(any());

        Config config = connector.validate(connectorConfig);
        for (ConfigValue value : config.configValues()) {
            assertTrue(value.errorMessages().isEmpty());
        }
    }

    @Test
    public void testValidationSuccessWithSuccessResponse() throws IOException {
        Map<String, String> connectorConfig = getConnectorConfig();

        doReturn(okHttpResponse)
                .doReturn(okHttpResponse)
                .when(httpClient)
                .execute(any());

        Config config = connector.validate(connectorConfig);
        for (ConfigValue value : config.configValues()) {
            assertTrue(value.errorMessages().isEmpty());
        }
    }

    @Test
    public void testConnectionFailure() throws IOException {
        Map<String, String> connectorConfig = getConnectorConfig();

        doThrow(new UnknownHostException("Host not found"))
            .when(httpClient)
            .execute(any());

        Config config = connector.validate(connectorConfig);

        Map<String, List<String>> errorMessages = config.configValues().stream()
            .collect(Collectors.toMap(ConfigValue::name, ConfigValue::errorMessages));

        assertFalse(errorMessages.get(SplunkSinkConnectorConfig.URI_CONF).isEmpty());
        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.TOKEN_CONF).isEmpty());

        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.URI_CONF).get(0).contains(SPLUNK_URI_HOST1));
    }

    @Test
    public void testAccessFailure() throws IOException {
        Map<String, String> connectorConfig = getConnectorConfig();

        doReturn(okHttpResponse)
            .doReturn(forbiddenHttpResponse)
            .when(httpClient)
            .execute(any());

        Config config = connector.validate(connectorConfig);

        Map<String, List<String>> errorMessages = config.configValues().stream()
            .collect(Collectors.toMap(ConfigValue::name, ConfigValue::errorMessages));

        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.URI_CONF).isEmpty());
        assertFalse(errorMessages.get(SplunkSinkConnectorConfig.TOKEN_CONF).isEmpty());

        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.TOKEN_CONF).get(0).contains(SPLUNK_URI_HOST1));
    }

    @Test
    public void testMultipleConnectionFailure() throws IOException {
        Map<String, String> connectorConfig = getConnectorConfig();
        String host2 = "https://www.host2.com:2222/";
        String host3 = "https://www.host3.com:3333/";
        connectorConfig.put(SplunkSinkConnectorConfig.URI_CONF,
            SPLUNK_URI_HOST1 + "," + host2 + "," + host3);

        doReturn(notFoundHttpResponse)
            .doThrow(new UnknownHostException("Host not found"))
            .doReturn(okHttpResponse)
            .when(httpClient)
            .execute(any());

        Config config = connector.validate(connectorConfig);

        Map<String, List<String>> errorMessages = config.configValues().stream()
            .collect(Collectors.toMap(ConfigValue::name, ConfigValue::errorMessages));


        assertFalse(errorMessages.get(SplunkSinkConnectorConfig.URI_CONF).isEmpty());
        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.TOKEN_CONF).isEmpty());

        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.URI_CONF).get(0).contains(SPLUNK_URI_HOST1));
        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.URI_CONF).get(0).contains(host2));
        assertFalse(errorMessages.get(SplunkSinkConnectorConfig.URI_CONF).get(0).contains(host3));
    }

    @Test
    public void testMultipleAccessFailure() throws IOException {
        Map<String, String> connectorConfig = getConnectorConfig();
        String host2 = "https://www.host2.com:2222/";
        String host3 = "https://www.host3.com:3333/";
        connectorConfig.put(SplunkSinkConnectorConfig.URI_CONF,
            SPLUNK_URI_HOST1 + "," + host2 + "," + host3);

        doReturn(okHttpResponse)
            .doReturn(okHttpResponse)
            .doReturn(okHttpResponse)
            .doReturn(badRequestHttpResponse)
            .doThrow(new UnknownHostException("Host not found"))
            .doReturn(notFoundHttpResponse)
            .when(httpClient)
            .execute(any());

        Config config = connector.validate(connectorConfig);

        Map<String, List<String>> errorMessages = config.configValues().stream()
            .collect(Collectors.toMap(ConfigValue::name, ConfigValue::errorMessages));


        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.URI_CONF).isEmpty());
        assertFalse(errorMessages.get(SplunkSinkConnectorConfig.TOKEN_CONF).isEmpty());

        assertFalse(errorMessages.get(SplunkSinkConnectorConfig.TOKEN_CONF).get(0).contains(SPLUNK_URI_HOST1));
        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.TOKEN_CONF).get(0).contains(host2));
        assertTrue(errorMessages.get(SplunkSinkConnectorConfig.TOKEN_CONF).get(0).contains(host3));
    }

    private Map<String, String> getConnectorConfig() {
        Map<String, String> connectorConfig = new HashMap<>();

        connectorConfig.put(SinkConnector.TOPICS_CONFIG, "topic1");
        connectorConfig.put(SplunkSinkConnectorConfig.URI_CONF, SPLUNK_URI_HOST1);
        connectorConfig.put(SplunkSinkConnectorConfig.TOKEN_CONF, "token1");
        connectorConfig.put(SplunkSinkConnectorConfig.SSL_VALIDATE_CERTIFICATES_CONF, "false");

        return connectorConfig;
    }

}
