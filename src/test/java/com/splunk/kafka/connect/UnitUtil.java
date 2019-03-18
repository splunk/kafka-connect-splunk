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

    UnitUtil(int profile) {
        this.configProfile = new ConfigProfile(profile);
    }

    public Map<String, String> createTaskConfig() {
        Map<String, String> config = new HashMap<>();
        config.put(SinkConnector.TOPICS_CONFIG, configProfile.getTopics());
        config.put(SplunkSinkConnectorConfig.TOKEN_CONF, configProfile.getToken());
        config.put(SplunkSinkConnectorConfig.URI_CONF, configProfile.getUri());
        config.put(SplunkSinkConnectorConfig.RAW_CONF, String.valueOf(configProfile.isRaw()));
        config.put(SplunkSinkConnectorConfig.ACK_CONF , String.valueOf(configProfile.isAck()));
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, configProfile.getIndexes());
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, configProfile.getSourcetypes());
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, configProfile.getSources());
        config.put(SplunkSinkConnectorConfig.HTTP_KEEPALIVE_CONF, String.valueOf(configProfile.isHttpKeepAlive()));
        config.put(SplunkSinkConnectorConfig.SSL_VALIDATE_CERTIFICATES_CONF, String.valueOf(configProfile.isValidateCertificates()));

        if(configProfile.getTrustStorePath() != null ) {
            config.put(SplunkSinkConnectorConfig.SSL_TRUSTSTORE_PATH_CONF, configProfile.getTrustStorePath());
            config.put(SplunkSinkConnectorConfig.SSL_TRUSTSTORE_PASSWORD_CONF, configProfile.getTrustStorePassword());
        }

        config.put(SplunkSinkConnectorConfig.EVENT_TIMEOUT_CONF, String.valueOf(configProfile.getEventBatchTimeout()));
        config.put(SplunkSinkConnectorConfig.ACK_POLL_INTERVAL_CONF, String.valueOf(configProfile.getAckPollInterval()));
        config.put(SplunkSinkConnectorConfig.MAX_HTTP_CONNECTION_PER_CHANNEL_CONF, String.valueOf(configProfile.getMaxHttpConnPerChannel()));
        config.put(SplunkSinkConnectorConfig.ACK_POLL_THREADS_CONF, String.valueOf(configProfile.getAckPollThreads()));
        config.put(SplunkSinkConnectorConfig.TOTAL_HEC_CHANNEL_CONF, String.valueOf(configProfile.getTotalHecChannels()));
        config.put(SplunkSinkConnectorConfig.SOCKET_TIMEOUT_CONF, String.valueOf(configProfile.getSocketTimeout()));
        config.put(SplunkSinkConnectorConfig.ENRICHMENT_CONF, String.valueOf(configProfile.getEnrichements()));
        config.put(SplunkSinkConnectorConfig.TRACK_DATA_CONF, String.valueOf(configProfile.isTrackData()));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(configProfile.getMaxBatchSize()));
        config.put(SplunkSinkConnectorConfig.HEC_THREDS_CONF, String.valueOf(configProfile.getNumOfThreads()));
        return config;
    }

    public static void milliSleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException ex) {
        }
    }
}
