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
package com.splunk.hecclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.splunk.kafka.connect.VersionUtils;
import com.sun.security.auth.module.Krb5LoginModule;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

final class Indexer implements IndexerInf {
    private static final Logger log = LoggerFactory.getLogger(Indexer.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private HecConfig hecConfig;
    private Configuration config;
    private CloseableHttpClient httpClient;
    private HttpContext context;
    private String baseUrl;
    private String hecToken;
    private boolean keepAlive;
    private HecChannel channel;
    private Header[] headers;
    private Poller poller;
    private long backPressure;
    private long lastBackPressure;
    private long backPressureThreshold = 60 * 1000; // 1 min

    // Indexer doesn't own client, ack poller
    public Indexer(String baseUrl,CloseableHttpClient client,Poller poller,HecConfig config) {
        this.httpClient = client;
        this.baseUrl = baseUrl;
        this.hecToken = hecToken;
        this.hecConfig = config;
        this.hecToken = config.getToken();
        this.poller = poller;
        this.context = HttpClientContext.create();
        backPressure = 0;

        channel = new HecChannel(this);

        // Init headers
        headers = new Header[5];
        headers[0] = new BasicHeader("Authorization", String.format("Splunk %s", hecToken));
        headers[1] = new BasicHeader("X-Splunk-Request-Channel", channel.getId());
        headers[3] = new BasicHeader("__splunk_app_name", VersionUtils.getAppName());
        headers[4] = new BasicHeader("__splunk_app_version", VersionUtils.getVersionString());

        keepAlive = false;
        setKeepAlive(true);
    }

    public Indexer setBackPressureThreshold(long threshold)  { //milliseconds
        backPressureThreshold = threshold;
        return this;
    }

    public Indexer setKeepAlive(boolean keepAlive) {
        if (this.keepAlive == keepAlive) {
            return this;
        }

        if (keepAlive) {
            headers[2] = new BasicHeader("Connection", "Keep-Alive");
        } else {
            headers[2] = new BasicHeader("Connection", "close");
        }
        this.keepAlive = keepAlive;
        return this;
    }

    public boolean getKeepAlive() {
        return keepAlive;
    }

    @Override
    public Header[] getHeaders() {
        return headers;
    }

    public String getToken() {
        return hecToken;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    public HecChannel getChannel() {
        return channel;
    }

    // this method is multi-thread safe
    @Override
    public boolean send(final EventBatch batch) {
        String endpoint = batch.getRestEndpoint();
        String url = baseUrl + endpoint;
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(headers);
        if (batch.isEnableCompression()) {
            httpPost.setHeader("Content-Encoding", "gzip");
            httpPost.setEntity(batch.getHttpEntityTemplate());
        } else {
            httpPost.setEntity(batch.getHttpEntity());
        }
        String resp;
        try {
            resp = executeHttpRequest(httpPost);
        } catch (HecException ex) {
            poller.fail(channel, batch, ex);
            return false;
        }

        poller.stickySessionHandler(channel);

        // we are all good
        poller.add(this.channel, batch, resp);
        log.debug("sent {} events to splunk through channel={} indexer={}", batch.size(), channel.getId(), getBaseUrl());

        return true;
    }

    // executeHttpRequest is synchronized since there are multi-threads to access the context
    @Override
    public synchronized String executeHttpRequest(final HttpUriRequest req) {
        CloseableHttpResponse resp;
        if (hecConfig.kerberosAuthEnabled()) {
            if (config == null) {
                defineKerberosConfigs();
            }
            Set<Principal> principals = new HashSet<>(1);
            principals.add(new KerberosPrincipal(hecConfig.kerberosPrincipal()));
            Subject subject = new Subject(false, principals, new HashSet<>(), new HashSet<>());
            try {
                LoginContext lc = new LoginContext("SplunkSinkConnector", subject, null, config);
                lc.login();
                Subject serviceSubject = lc.getSubject();
                resp = Subject.doAs(serviceSubject, (PrivilegedAction<CloseableHttpResponse>) () -> {
                    try {
                        return httpClient.execute(req, context);
                    } catch (IOException ex) {
                        logBackPressure();
                        throw new HecException("Encountered exception while posting data.", ex);
                    }
                });
            } catch (Exception le) {
                throw new HecException(
                    "Encountered exception while authenticating via Kerberos.", le);
            }
        } else {
            try {
                resp = httpClient.execute(req, context);
            } catch (Exception ex) {
                logBackPressure();
                throw new HecException("encountered exception when post data", ex);
            }
        }

        return readAndCloseResponse(resp);
    }


     /**
     * Creates the Kerberos configurations.
     *
     * @return map of kerberos configs
     */
    private Map<String, Object> kerberosConfigMap() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("useTicketCache", "true");
        configs.put("renewTGT", "true");
        configs.put("useKeyTab", "true");
        configs.put("keyTab", hecConfig.kerberosKeytabLocation());
        configs.put("refreshKrb5Config", "true");
        configs.put("principal", hecConfig.kerberosPrincipal());
        configs.put("storeKey", "false");
        configs.put("doNotPrompt", "true");
        return configs;
    }

    private void defineKerberosConfigs() {
        config = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return new AppConfigurationEntry[]{
                    new AppConfigurationEntry(Krb5LoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, kerberosConfigMap())
                };
            }
        };
    }


    private String readAndCloseResponse(CloseableHttpResponse resp) {
        String respPayload;
        HttpEntity entity = resp.getEntity();
        try {
            respPayload = EntityUtils.toString(entity, "utf-8");
        } catch (Exception ex) {
            log.error("failed to process http response", ex);
            throw new HecException("failed to process http response", ex);
        } finally {
            try {
                resp.close();
            } catch (IOException ex) {
                throw new HecException("failed to close http response", ex);
            }
        }
        
        //log.info("event posting, channel={}, cookies={}, cookies.length={}", channel, resp.getHeaders("Set-Cookie"), resp.getHeaders("Set-Cookie").length);

        if((resp.getHeaders("Set-Cookie") != null) && (resp.getHeaders("Set-Cookie").length > 0)) {
            log.info("Sticky session expiry detected, will cleanup old channel and its associated batches");
            poller.setStickySessionToTrue();
        }

        

        int status = resp.getStatusLine().getStatusCode();
        // FIXME 503 server is busy backpressure
        if (status != 200 && status != 201) {
            if (status == 503) {
                logBackPressure();
            }

            log.error("failed to post events resp={}, status={}", respPayload, status);
            JsonNode jsonNode;
            try {
                jsonNode = jsonMapper.readTree(respPayload);
            } catch (Exception ex) {
                log.error("failed to parse response payload", ex);
                throw new HecException("failed to parse response payload", ex);
            }
            
            String respText = (jsonNode.has("text")) ? jsonNode.get("text").asText() : null;

            if (respText == "Invalid data format") {
                ObjectNode objNode = jsonMapper.createObjectNode();
                objNode.put("text", "Invalid data format");
                objNode.put("code", 0); // Mark it as success
                objNode.put("ackId", -1);
                respPayload = objNode.toString();
            } else {
                throw new HecException(String.format("failed to post events resp=%s, status=%d", respPayload, status));
            }
        }

        clearBackPressure();

        return respPayload;
    }

    private void logBackPressure() {
        backPressure += 1;
        lastBackPressure = System.currentTimeMillis();
    }

    private void clearBackPressure() {
        backPressure = 0;
        lastBackPressure = 0;
    }

    @Override
    public String toString() {
        return baseUrl;
    }

    @Override
    public boolean hasBackPressure() {
        if (backPressure > 0) {
            if ((System.currentTimeMillis() - lastBackPressure) < backPressureThreshold) {
                log.warn("Still in Backpressure window {}:{}", System.currentTimeMillis() - lastBackPressure, backPressureThreshold);
                // still in the back-pressure window
                return true;
            } else {
                log.info("Clearing Backpressure");
                clearBackPressure();
                return false;
            }
        }
        return false;
    }
}
