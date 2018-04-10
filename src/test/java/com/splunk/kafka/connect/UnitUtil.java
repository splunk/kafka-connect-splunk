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

import org.apache.kafka.connect.sink.SinkConnector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UnitUtil {
    ConfigProfile configProfile;

    final String topics;
    final String token;
    final String uri;
    final boolean raw;
    final boolean ack;
    final String indexes;
    final String sourcetypes;
    final String sources;
    final boolean httpKeepAlive;
    final boolean validateCertificates;
    final boolean hasTrustStorePath;
    final String trustStorePath;
    final String trustStorePassword;
    final int eventBatchTimeout;
    final int ackPollInterval;
    final int ackPollThreads;
    final int maxHttpConnPerChannel;
    final int totalHecChannels;
    final int socketTimeout;
    final String enrichements;
    final Map<String, String> enrichementMap;
    final boolean trackData;
    final int maxBatchSize;
    final int numOfThreads;

    UnitUtil(int profile) {
        this.configProfile = new ConfigProfile(profile);
        topics = configProfile.getTopics();
        token = configProfile.getToken();
        uri = configProfile.getUri();
        raw = configProfile.isRaw();
        ack = configProfile.isAck();
        indexes = configProfile.getIndexes();
        sourcetypes = configProfile.getSourcetypes();
        sources = configProfile.getSources();
        httpKeepAlive = configProfile.isHttpKeepAlive();
        validateCertificates = configProfile.isValidateCertificates();
        hasTrustStorePath = configProfile.isHasTrustStorePath();
        trustStorePath = configProfile.getTrustStorePath();
        trustStorePassword = configProfile.getTrustStorePassword();
        eventBatchTimeout = configProfile.getEventBatchTimeout();
        ackPollInterval = configProfile.getAckPollInterval();
        ackPollThreads = configProfile.getAckPollThreads();
        maxHttpConnPerChannel = configProfile.getMaxHttpConnPerChannel();
        totalHecChannels = configProfile.getTotalHecChannels();
        socketTimeout = configProfile.getSocketTimeout();
        enrichements = configProfile.getEnrichements();
        enrichementMap = configProfile.getEnrichementMap();
        trackData = configProfile.isTrackData();
        maxBatchSize = configProfile.getMaxBatchSize();
        numOfThreads = configProfile.getNumOfThreads();
    }

    public Map<String, String> createTaskConfig() {
        Map<String, String> config = new HashMap<>();
        config.put(SinkConnector.TOPICS_CONFIG, topics);
        config.put(SplunkSinkConnectorConfig.TOKEN_CONF, token);
        config.put(SplunkSinkConnectorConfig.URI_CONF, uri);
        config.put(SplunkSinkConnectorConfig.RAW_CONF, String.valueOf(raw));
        config.put(SplunkSinkConnectorConfig.ACK_CONF , String.valueOf(ack));
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, indexes);
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, sourcetypes);
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, sources);
        config.put(SplunkSinkConnectorConfig.HTTP_KEEPALIVE_CONF, String.valueOf(httpKeepAlive));
        config.put(SplunkSinkConnectorConfig.SSL_VALIDATE_CERTIFICATES_CONF, String.valueOf(validateCertificates));
        config.put(SplunkSinkConnectorConfig.SSL_TRUSTSTORE_PATH_CONF, trustStorePath);
        config.put(SplunkSinkConnectorConfig.SSL_TRUSTSTORE_PASSWORD_CONF, trustStorePassword);
        config.put(SplunkSinkConnectorConfig.EVENT_TIMEOUT_CONF, String.valueOf(eventBatchTimeout));
        config.put(SplunkSinkConnectorConfig.ACK_POLL_INTERVAL_CONF, String.valueOf(ackPollInterval));
        config.put(SplunkSinkConnectorConfig.MAX_HTTP_CONNECTION_PER_CHANNEL_CONF, String.valueOf(maxHttpConnPerChannel));
        config.put(SplunkSinkConnectorConfig.ACK_POLL_THREADS_CONF, String.valueOf(ackPollThreads));
        config.put(SplunkSinkConnectorConfig.TOTAL_HEC_CHANNEL_CONF, String.valueOf(totalHecChannels));
        config.put(SplunkSinkConnectorConfig.SOCKET_TIMEOUT_CONF, String.valueOf(socketTimeout));
        config.put(SplunkSinkConnectorConfig.ENRICHMENT_CONF, String.valueOf(enrichements));
        config.put(SplunkSinkConnectorConfig.TRACK_DATA_CONF, String.valueOf(trackData));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(maxBatchSize));
        config.put(SplunkSinkConnectorConfig.HEC_THREDS_CONF, String.valueOf(numOfThreads));
        return config;
    }

    public static void milliSleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException ex) {
        }
    }
}
