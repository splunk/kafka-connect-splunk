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
import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.URI_CONF;
import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.TOKEN_CONF;
import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.INDEX_CONF;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.SSL_VALIDATE_CERTIFICATES_CONF;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.SOCKET_TIMEOUT_CONF;;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.MAX_HTTP_CONNECTION_PER_CHANNEL_CONF;;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.TOTAL_HEC_CHANNEL_CONF;;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.EVENT_TIMEOUT_CONF;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.INDEX_CONF;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.INDEX_CONF;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.INDEX_CONF;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.INDEX_CONF;
// import static com.splunk.kafka.connect.SplunkSinkConnectorConfig.INDEX_CONF;

import com.splunk.hecclient.Event;
import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.Hec;
import com.splunk.hecclient.HecConfig;
import com.splunk.hecclient.JsonEvent;
import com.splunk.hecclient.JsonEventBatch;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SplunkSinkConnector extends SinkConnector {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkConnector.class);
    private Map<String, String> taskConfig;
    private Map<String, ConfigValue> values;
    private List<ConfigValue> validations;
    private HecClosableClient hecAb = new HecCreateClient();


    public void setHecInstance(HecClosableClient hecAb){
        this.hecAb=hecAb;
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
        validateHealthCheckForSplunkIndexes(connectorConfigs);
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


    private void validateHealthCheckForSplunkIndexes(final Map<String, String> configs)  {
        
        throw new ConfigException("check");
        // String splunkURI = configs.getOrDefault(URI_CONF,"");
        // String indexes = configs.getOrDefault(INDEX_CONF,"");
        // String splunkToken = configs.getOrDefault(TOKEN_CONF,"");
        // if (indexes!=""){
        //     log.info("started", splunkToken);
        //     System.out.println("started");
        //     String[] topicIndexes = split(indexes, ",");
        //     for (String index: topicIndexes){
        //         healthCheckForSplunkHEC(splunkURI,index,splunkToken,hecAb,configs);
        //             // throw new ConfigException("encountered exception when post data");
        //     }
        // }        
    }

    private void healthCheckForSplunkHEC(String splunkURI,String index,String splunkToken,HecClosableClient clientInstance,final Map<String, String> configs)   {
        log.info("healthCheckForSplunkHEC", splunkToken, clientInstance);
        Header[] headers;
        headers = new Header[1];
        headers[0] = new BasicHeader("Authorization", String.format("Splunk %s", splunkToken));
        String endpoint = "/services/collector";
        String url = splunkURI + endpoint;
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(headers);
        EventBatch batch = new JsonEventBatch();
        Event event = new JsonEvent("a:a", null);
        event.setIndex(index);
        batch.add(event);
        httpPost.setEntity(batch.getHttpEntity());
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(configs);
        CloseableHttpClient httpClient = clientInstance.getClient(connectorConfig.getHecConfig());
        // if (taskConfig!=null){
        //     SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(taskConfig, hecAb);
        //     httpClient =  clientInstance.getClient(connectorConfig.getHecConfig());
        // }
        // else {
        // httpClient =  clientInstance.getClient(null);
        // }
        
        try {
            executeHttpRequest(httpPost,httpClient);
        } catch (ConfigException e){

        }
        
        // if (s!=null){
        //     addErrorMessage(splunkURI, s);
        // }
        return;
    }

	

    private String  executeHttpRequest(final HttpUriRequest req,CloseableHttpClient httpClient){
        CloseableHttpResponse resp = null;
        HttpContext context;
        context = HttpClientContext.create();
            try {                
                resp = httpClient.execute(req, context);
            } 
            catch (IOException ex) {
                throw new ConfigException("Invalid", ex);
            //    ex.printStackTrace();
            //     throw new ConfigException("encountered exception when post data", ex);
            }
        String respPayload;
        if (resp !=null){
            HttpEntity entity = resp.getEntity();
            try {
                respPayload = EntityUtils.toString(entity, "utf-8");
            } catch (Exception ex) {
                // throw new ConfigException("failed to process http response", ex);
            } finally {
                try {
                    resp.close();
                } catch (Exception ex) {
                    // throw new ConfigException("failed to close http response", ex);
                }
            }
            int status = resp.getStatusLine().getStatusCode();
            log.info(status+"");
            if (status==201) {
                return null;
                // throw new ConfigException(String.format("Bad splunk configurations with status code:%s response:%s",status,respPayload));
            }
        }else{
            return "erorrrrr";
        }

       return "erorrrrr";
    }


 
}
