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

import com.splunk.hecclient.HecConfig;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.*;

public final class SplunkSinkConnectorConfig extends AbstractConfig {
    static final String INDEX = "index";
    static final String SOURCETYPE = "sourcetype";
    static final String SOURCE = "source";

    static final String TOKEN_CONF = "splunk.hec.token";
    static final String URI_CONF = "splunk.hec.uri";
    static final String RAW_CONF = "splunk.hec.raw";
    static final String ACK_CONF = "splunk.hec.ack.enabled";
    static final String INDEX_CONF = "splunk.indexes";
    static final String SOURCETYPE_CONF = "splunk.sourcetypes";
    static final String SOURCE_CONF = "splunk.sources";
    static final String HTTP_KEEPALIVE_CONF = "splunk.hec.http.keepalive";
    static final String SSL_VALIDATE_CERTIFICATES_CONF = "splunk.hec.ssl.validate.certs";
    static final String SSL_TRUSTSTORE_PATH_CONF = "splunk.hec.ssl.trust.store.path";
    static final String SSL_TRUSTSTORE_PASSWORD_CONF = "splunk.hec.ssl.trust.store.password";
    static final String SOCKET_TIMEOUT_CONF = "splunk.hec.socket.timeout"; // seconds
    static final String EVENT_TIMEOUT_CONF = "splunk.hec.event.timeout"; // seconds
    static final String ACK_POLL_INTERVAL_CONF = "splunk.hec.ack.poll.interval"; // seconds
    static final String ACK_POLL_THREADS_CONF = "splunk.hec.ack.poll.threads";
    static final String MAX_HTTP_CONNECTION_PER_CHANNEL_CONF = "splunk.hec.max.http.connection.per.channel";
    static final String TOTAL_HEC_CHANNEL_CONF = "splunk.hec.total.channels";
    static final String ENRICHMENT_CONF = "splunk.hec.json.event.enrichment";
    static final String USE_RECORD_TIMESTAMP_CONF = "splunk.hec.use.record.timestamp";
    static final String MAX_BATCH_SIZE_CONF = "splunk.hec.max.batch.size"; // record count
    static final String HEC_THREDS_CONF = "splunk.hec.threads";
    static final String LINE_BREAKER_CONF = "splunk.hec.raw.line.breaker";
    static final String MAX_OUTSTANDING_EVENTS_CONF = "splunk.hec.max.outstanding.events";
    static final String MAX_RETRIES_CONF = "splunk.hec.max.retries";
    static final String TRACK_DATA_CONF = "splunk.hec.track.data";

     // Kafka configuration description strings
    static final String TOKEN_DOC = "The authorization token to use when writing data to splunk.";
    static final String URI_DOC = "The URI of the remote splunk to write data do.";
    static final String RAW_DOC = "Flag to determine if use /raw HEC endpoint when indexing data to Splunk.";
    static final String ACK_DOC = "Flag to determine if use turn on HEC ACK when indexing data to Splunk.";
    static final String INDEX_DOC = "Splunk index names for Kafka topic data, separated by comma";
    static final String SOURCETYPE_DOC = "Splunk sourcetype names for Kafka topic data, separated by comma";
    static final String SOURCE_DOC = "Splunk source names for Kafka topic data, separated by comma";
    static final String HTTP_KEEPALIVE_DOC = "Keepalive HTTP Connection to HEC server";
    static final String SSL_VALIDATE_CERTIFICATES_DOC = "Flag to determine if ssl connections should validate the "
            + "certificate of the remote host.";
    static final String SSL_TRUSTSTORE_PATH_DOC = "Path on the local disk to the certificate trust store.";
    static final String SSL_TRUSTSTORE_PASSWORD_DOC = "Password for the trust store.";
    static final String EVENT_TIMEOUT_DOC = "Max duration in seconds to wait commit response after sending to Splunk.";
    static final String ACK_POLL_INTERVAL_DOC = "Interval in seconds to poll event ACKs from Splunk.";
    static final String ACK_POLL_THREADS_DOC = "Number of threads used to query ACK for single task.";
    static final String MAX_HTTP_CONNECTION_PER_CHANNEL_DOC = "Max HTTP connections pooled for one HEC Channel "
            + "when posting events to Splunk.";
    static final String TOTAL_HEC_CHANNEL_DOC = "Total HEC Channels used to post events to Splunk. When enabling HEC ACK, "
            + "setting to the same or 2X number of indexers is generally good.";
    static final String SOCKET_TIMEOUT_DOC = "Max duration in seconds to read / write data to network before its timeout.";
    static final String ENRICHMENT_DOC = "Enrich the JSON events by specifying key value pairs separated by comma. "
            + "Is only applicable to splunk.hec.raw=false case";
    static final String USE_RECORD_TIMESTAMP_DOC = "Set event timestamp to Kafka record timestamp";
    static final String MAX_BATCH_SIZE_DOC = "Max number of Kafka record to be sent to Splunk HEC for one POST";
    static final String HEC_THREADS_DOC = "Number of threads used to POST events to Splunk HEC in single task";
    static final String LINE_BREAKER_DOC = "Line breaker for /raw HEC endpoint. The line breaker can help Splunkd to do event breaking";
    static final String MAX_OUTSTANDING_EVENTS_DOC = "Number of outstanding events which are not ACKed kept in memory";
    static final String MAX_RETRIES_DOC = "Number of retries for failed batches before giving up";
    static final String TRACK_DATA_DOC = "Track data loss, latency or not. Is only applicable to splunk.hec.raw=false case";

    final String splunkToken;
    final String splunkURI;
    final boolean raw; // /raw or /event HEC
    final boolean ack; // use HEC ACK ?
    final String indexes;
    final String sourcetypes;
    final String sources;
    final boolean validateCertificates;
    final boolean httpKeepAlive;
    final String trustStorePath;
    final String trustStorePassword;
    final int eventBatchTimeout;
    final int ackPollInterval;
    final int ackPollThreads;
    final int maxHttpConnPerChannel;
    final int totalHecChannels;
    final int socketTimeout;
    final boolean trackData;
    final boolean useRecordTimestamp;
    final int maxBatchSize;
    final int numberOfThreads;
    final int maxOutstandingEvents;
    final int maxRetries;
    final String lineBreaker;
    final Map<String, String> enrichments;

    final Map<String, Map<String, String>> topicMetas;

    SplunkSinkConnectorConfig(Map<String, String> taskConfig) {
        super(conf(), taskConfig);
        splunkToken = getPassword(TOKEN_CONF).value();
        splunkURI = getString(URI_CONF);
        raw = getBoolean(RAW_CONF);
        ack = getBoolean(ACK_CONF);
        indexes = getString(INDEX_CONF);
        sourcetypes = getString(SOURCETYPE_CONF);
        sources = getString(SOURCE_CONF);
        httpKeepAlive = getBoolean(HTTP_KEEPALIVE_CONF);
        validateCertificates = getBoolean(SSL_VALIDATE_CERTIFICATES_CONF);
        trustStorePath = getString(SSL_TRUSTSTORE_PATH_CONF);
        trustStorePassword = getPassword(SSL_TRUSTSTORE_PASSWORD_CONF).value();
        eventBatchTimeout = getInt(EVENT_TIMEOUT_CONF);
        ackPollInterval = getInt(ACK_POLL_INTERVAL_CONF);
        ackPollThreads = getInt(ACK_POLL_THREADS_CONF);
        maxHttpConnPerChannel = getInt(MAX_HTTP_CONNECTION_PER_CHANNEL_CONF);
        totalHecChannels = getInt(TOTAL_HEC_CHANNEL_CONF);
        socketTimeout = getInt(SOCKET_TIMEOUT_CONF);
        enrichments = parseEnrichments(getString(ENRICHMENT_CONF));
        trackData = getBoolean(TRACK_DATA_CONF);
        useRecordTimestamp = getBoolean(USE_RECORD_TIMESTAMP_CONF);
        maxBatchSize = getInt(MAX_BATCH_SIZE_CONF);
        numberOfThreads = getInt(HEC_THREDS_CONF);
        lineBreaker = getString(LINE_BREAKER_CONF);
        maxOutstandingEvents = getInt(MAX_OUTSTANDING_EVENTS_CONF);
        maxRetries = getInt(MAX_RETRIES_CONF);
        topicMetas = initMetaMap(taskConfig);
    }

    public static ConfigDef conf() {
        return new ConfigDef()
            .define(TOKEN_CONF, ConfigDef.Type.PASSWORD, ConfigDef.Importance.HIGH, TOKEN_DOC)
            .define(URI_CONF, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, URI_DOC)
            .define(RAW_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.MEDIUM, RAW_DOC)
            .define(ACK_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.MEDIUM, ACK_DOC)
            .define(INDEX_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, INDEX_DOC)
            .define(SOURCETYPE_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, SOURCETYPE_DOC)
            .define(SOURCE_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, SOURCE_DOC)
            .define(HTTP_KEEPALIVE_CONF, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM, HTTP_KEEPALIVE_DOC)
            .define(SSL_VALIDATE_CERTIFICATES_CONF, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM, SSL_VALIDATE_CERTIFICATES_DOC)
            .define(SSL_TRUSTSTORE_PATH_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.HIGH, SSL_TRUSTSTORE_PATH_DOC)
            .define(SSL_TRUSTSTORE_PASSWORD_CONF, ConfigDef.Type.PASSWORD, "", ConfigDef.Importance.HIGH, SSL_TRUSTSTORE_PASSWORD_DOC)
            .define(EVENT_TIMEOUT_CONF, ConfigDef.Type.INT, 300, ConfigDef.Importance.MEDIUM, EVENT_TIMEOUT_DOC)
            .define(ACK_POLL_INTERVAL_CONF, ConfigDef.Type.INT, 10, ConfigDef.Importance.MEDIUM, ACK_POLL_INTERVAL_DOC)
            .define(ACK_POLL_THREADS_CONF, ConfigDef.Type.INT, 2, ConfigDef.Importance.MEDIUM, ACK_POLL_THREADS_DOC)
            .define(MAX_HTTP_CONNECTION_PER_CHANNEL_CONF, ConfigDef.Type.INT, 2, ConfigDef.Importance.MEDIUM, MAX_HTTP_CONNECTION_PER_CHANNEL_DOC)
            .define(TOTAL_HEC_CHANNEL_CONF, ConfigDef.Type.INT, 2, ConfigDef.Importance.HIGH, TOTAL_HEC_CHANNEL_DOC)
            .define(SOCKET_TIMEOUT_CONF, ConfigDef.Type.INT, 60, ConfigDef.Importance.LOW, SOCKET_TIMEOUT_DOC)
            .define(ENRICHMENT_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW, ENRICHMENT_DOC)
            .define(TRACK_DATA_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.LOW, TRACK_DATA_DOC)
            .define(USE_RECORD_TIMESTAMP_CONF, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM, USE_RECORD_TIMESTAMP_DOC)
            .define(HEC_THREDS_CONF, ConfigDef.Type.INT, 1, ConfigDef.Importance.LOW, HEC_THREADS_DOC)
            .define(LINE_BREAKER_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, LINE_BREAKER_DOC)
            .define(MAX_OUTSTANDING_EVENTS_CONF, ConfigDef.Type.INT, 1000000, ConfigDef.Importance.MEDIUM, MAX_OUTSTANDING_EVENTS_DOC)
            .define(MAX_RETRIES_CONF, ConfigDef.Type.INT, -1, ConfigDef.Importance.MEDIUM, MAX_RETRIES_DOC)
            .define(MAX_BATCH_SIZE_CONF, ConfigDef.Type.INT, 500, ConfigDef.Importance.MEDIUM, MAX_BATCH_SIZE_DOC);
    }

    /**
    * Configuration Method to setup all settings related to Splunk HEC Client
    */
    public HecConfig getHecConfig() {
        HecConfig config = new HecConfig(Arrays.asList(splunkURI.split(",")), splunkToken);
        config.setDisableSSLCertVerification(!validateCertificates)
                .setSocketTimeout(socketTimeout)
                .setMaxHttpConnectionPerChannel(maxHttpConnPerChannel)
                .setTotalChannels(totalHecChannels)
                .setEventBatchTimeout(eventBatchTimeout)
                .setHttpKeepAlive(httpKeepAlive)
                .setAckPollInterval(ackPollInterval)
                .setAckPollThreads(ackPollThreads)
                .setEnableChannelTracking(trackData);
        return config;
    }

    public boolean hasMetaDataConfigured() {
        return (indexes != null && !indexes.isEmpty()
                || (sources != null && !sources.isEmpty())
                || (sourcetypes != null && !sourcetypes.isEmpty()));
    }

    public String toString() {
        return "splunkURI:" + splunkURI + ", "
                + "raw:" + raw + ", "
                + "ack:" + ack + ", "
                + "indexes:" + indexes + ", "
                + "sourcetypes:" + sourcetypes + ", "
                + "sources:" + sources + ", "
                + "httpKeepAlive:" + httpKeepAlive + ", "
                + "validateCertificates:" + validateCertificates + ", "
                + "trustStorePath:" + trustStorePath + ", "
                + "socketTimeout:" + socketTimeout + ", "
                + "eventBatchTimeout:" + eventBatchTimeout + ", "
                + "ackPollInterval:" + ackPollInterval + ", "
                + "ackPollThreads:" + ackPollThreads + ", "
                + "maxHttpConnectionPerChannel:" + maxHttpConnPerChannel + ", "
                + "totalHecChannels:" + totalHecChannels + ", "
                + "enrichment: " + getString(ENRICHMENT_CONF) + ", "
                + "maxBatchSize: " + maxBatchSize + ", "
                + "numberOfThreads: " + numberOfThreads + ", "
                + "lineBreaker: " + lineBreaker + ", "
                + "maxOutstandingEvents: " + maxOutstandingEvents + ", "
                + "maxRetries: " + maxRetries + ", "
                + "useRecordTimestamp: " + useRecordTimestamp + ", "
                + "trackData: " + trackData;
    }

    private static String[] split(String data, String sep) {
        if (data != null && !data.trim().isEmpty()) {
            return data.trim().split(sep);
        }
        return null;
    }

    private static Map<String, String> parseEnrichments(String enrichment) {
        String[] kvs = split(enrichment, ",");
        if (kvs == null) {
            return null;
        }

        Map<String, String> enrichmentKvs = new HashMap<>();
        for (final String kv: kvs) {
            String[] kvPairs = split(kv, "=");
            if (kvPairs.length != 2) {
                throw new ConfigException("Invalid enrichment: " + enrichment+ ". Expect key value pairs and separated by comma");
            }
            enrichmentKvs.put(kvPairs[0], kvPairs[1]);
        }
        return enrichmentKvs;
    }

    private String getMetaForTopic(String[] metas, int expectedLength, int curIdx, String confKey) {
        if (metas == null) {
            return null;
        }

        if (metas.length == 1) {
            return metas[0];
        } else if (metas.length == expectedLength) {
            return metas[curIdx];
        } else {
            throw new ConfigException("Invalid " + confKey + " configuration=" + metas);
        }
    }

    private Map<String, Map<String, String>> initMetaMap(Map<String, String> taskConfig) {
        String[] topics = split(taskConfig.get(SinkConnector.TOPICS_CONFIG), ",");
        String[] topicIndexes = split(indexes, ",");
        String[] topicSourcetypes = split(sourcetypes, ",");
        String[] topicSources = split(sources, ",");

        Map<String, Map<String, String>> metaMap = new HashMap<>();
        int idx = 0;
        for (String topic: topics) {
            HashMap<String, String> topicMeta = new HashMap<>();
            String meta = getMetaForTopic(topicIndexes, topics.length, idx, INDEX_CONF);
            if (meta != null) {
                topicMeta.put(INDEX, meta);
            }

            meta = getMetaForTopic(topicSourcetypes, topics.length, idx, SOURCETYPE_CONF);
            if (meta != null) {
                topicMeta.put(SOURCETYPE, meta);
            }

            meta = getMetaForTopic(topicSources, topics.length, idx, SOURCE_CONF);
            if (meta != null) {
                topicMeta.put(SOURCE, meta);
            }

            metaMap.put(topic, topicMeta);
            idx += 1;
        }
        return metaMap;
    }
}
