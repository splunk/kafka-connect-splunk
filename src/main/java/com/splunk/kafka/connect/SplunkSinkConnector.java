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
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.splunk.hecclient.Event;
import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.JsonEvent;
import com.splunk.hecclient.JsonEventBatch;

public final class SplunkSinkConnector extends SinkConnector {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkConnector.class);
    private Map<String, String> taskConfig;
    private Map<String, ConfigValue> values;
    private List<ConfigValue> validations;
    private AbstractClientWrapper abstractClientWrapper = new HecClientWrapper();


    public void setHecInstance(AbstractClientWrapper abstractClientWrapper) {
        this.abstractClientWrapper = abstractClientWrapper;
    }

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
        validateSplunkConfigurations(connectorConfigs);
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

    private static String[] split(String data, String sep) {
        if (data != null && !data.trim().isEmpty()) {
            return data.trim().split(sep);
        }
        return null;
    }


    private void validateSplunkConfigurations(final Map<String, String> configs) throws ConfigException {
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(configs);
        String[] indexes = split(connectorConfig.indexes, ",");
        if(indexes == null || indexes.length == 0) {
            preparePayloadAndExecuteRequest(connectorConfig, "");
        } else {
            for (String index : indexes) {
                preparePayloadAndExecuteRequest(connectorConfig, index);
            }
        }
    }

    private void preparePayloadAndExecuteRequest(SplunkSinkConnectorConfig connectorConfig, String index) throws ConfigException {
        Header[] headers;
        if (connectorConfig.ack) {
            headers = new Header[]{
                new BasicHeader("Authorization", String.format("Splunk %s", connectorConfig.splunkToken)),
                new BasicHeader("X-Splunk-Request-Channel", java.util.UUID.randomUUID().toString())
            };
        } else {
            headers = new Header[]{
                new BasicHeader("Authorization", String.format("Splunk %s", connectorConfig.splunkToken)),
            };
        }
        String endpoint = "/services/collector";
        List<String> hecURIs = Arrays.asList(connectorConfig.splunkURI.split(","));
        String url = hecURIs.get(0) + endpoint;
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(headers);
        EventBatch batch = new JsonEventBatch();
        Event event = new JsonEvent("Splunk HEC Configuration Check", null);
        event.setIndex(index);
        event.setSource("kafka-connect");
        event.setSourcetype("kafka-connect");
        batch.add(event);
        httpPost.setEntity(batch.getHttpEntity());
        CloseableHttpClient httpClient = abstractClientWrapper.getClient(connectorConfig.getHecConfig());
        executeHttpRequest(httpPost, httpClient);
    }



    private void executeHttpRequest(final HttpUriRequest req, CloseableHttpClient httpClient) throws ConfigException {
        CloseableHttpResponse resp = null;
        HttpContext context;
        context = HttpClientContext.create();
        try {
            resp = httpClient.execute(req, context);
            int status = resp.getStatusLine().getStatusCode();

            String respPayload = EntityUtils.toString(resp.getEntity(), "utf-8");
            if (status > 299){
                throw new ConfigException(String.format("Bad splunk configurations with status code:%s response:%s",status,respPayload));
            }
        } catch (ClientProtocolException ex) {
            throw new ConfigException("Invalid splunk SSL configuration detected while validating configuration",ex);
        } catch (IOException ex) {
            throw new ConfigException("Invalid Splunk Configurations",ex);
        } catch (ConfigException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConfigException("failed to process http payload",ex);
        } finally {
            try {
                if (resp!= null) {
                    resp.close();
                }
            } catch (Exception ex) {
                throw new ConfigException("failed to close http response",ex);
            }
        }
    }
}
