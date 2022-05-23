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
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.connect.sink.SinkTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SplunkSinkConnectorConfig extends AbstractConfig {
    // General
    static final String INDEX = "index";
    static final String SOURCE = "source";
    static final String SOURCETYPE = "sourcetype";
    // Required Parameters
    static final String URI_CONF = "splunk.hec.uri";
    static final String TOKEN_CONF = "splunk.hec.token";
    // General Parameters
    static final String INDEX_CONF = "splunk.indexes";
    static final String SOURCE_CONF = "splunk.sources";
    static final String SOURCETYPE_CONF = "splunk.sourcetypes";
    static final String FLUSH_WINDOW_CONF = "splunk.flush.window";
    static final String TOTAL_HEC_CHANNEL_CONF = "splunk.hec.total.channels";
    static final String MAX_HTTP_CONNECTION_PER_CHANNEL_CONF = "splunk.hec.max.http.connection.per.channel";
    static final String MAX_BATCH_SIZE_CONF = "splunk.hec.max.batch.size"; // record count
    static final String HTTP_KEEPALIVE_CONF = "splunk.hec.http.keepalive";
    static final String HEC_THREDS_CONF = "splunk.hec.threads";
    static final String SOCKET_TIMEOUT_CONF = "splunk.hec.socket.timeout"; // seconds
    static final String SSL_VALIDATE_CERTIFICATES_CONF = "splunk.hec.ssl.validate.certs";
    static final String ENABLE_COMPRESSSION_CONF = "splunk.hec.enable.compression";
    // Acknowledgement Parameters
    // Use Ack
    static final String ACK_CONF = "splunk.hec.ack.enabled";
    static final String ACK_POLL_INTERVAL_CONF = "splunk.hec.ack.poll.interval"; // seconds
    static final String ACK_POLL_THREADS_CONF = "splunk.hec.ack.poll.threads";
    static final String EVENT_TIMEOUT_CONF = "splunk.hec.event.timeout"; // seconds
    static final String MAX_OUTSTANDING_EVENTS_CONF = "splunk.hec.max.outstanding.events";
    static final String MAX_RETRIES_CONF = "splunk.hec.max.retries";
    static final String HEC_BACKOFF_PRESSURE_THRESHOLD = "splunk.hec.backoff.threshhold.seconds";
    // Endpoint Parameters
    static final String RAW_CONF = "splunk.hec.raw";
    // /raw endpoint only
    static final String LINE_BREAKER_CONF = "splunk.hec.raw.line.breaker";
    // /event endpoint only
    static final String USE_RECORD_TIMESTAMP_CONF = "splunk.hec.use.record.timestamp";
    static final String ENRICHMENT_CONF = "splunk.hec.json.event.enrichment";
    static final String TRACK_DATA_CONF = "splunk.hec.track.data";
    static final String HEC_EVENT_FORMATTED_CONF = "splunk.hec.json.event.formatted";
    // Trust store
    static final String SSL_TRUSTSTORE_PATH_CONF = "splunk.hec.ssl.trust.store.path";
    static final String SSL_TRUSTSTORE_PASSWORD_CONF = "splunk.hec.ssl.trust.store.password";
    //Headers
    static final String HEADER_SUPPORT_CONF = "splunk.header.support";
    static final String HEADER_CUSTOM_CONF = "splunk.header.custom";
    static final String HEADER_INDEX_CONF = "splunk.header.index";
    static final String HEADER_SOURCE_CONF = "splunk.header.source";
    static final String HEADER_SOURCETYPE_CONF = "splunk.header.sourcetype";
    static final String HEADER_HOST_CONF = "splunk.header.host";
    // Load Balancer
    static final String LB_POLL_INTERVAL_CONF = "splunk.hec.lb.poll.interval";

     // Kerberos config
     static final String KERBEROS_USER_PRINCIPAL_CONF = "kerberos.user.principal";
     static final String KERBEROS_KEYTAB_PATH_CONF = "kerberos.keytab.path";

    // Kafka configuration description strings
    // Required Parameters
    static final String URI_DOC = "Splunk HEC URIs. Either a list of FQDNs or IPs of all Splunk indexers, separated "
            + "with a \",\", or a load balancer. The connector will load balance to indexers using "
            + "round robin. Splunk Connector will round robin to this list of indexers. "
            + "https://hec1.splunk.com:8088,https://hec2.splunk.com:8088,https://hec3.splunk.com:8088";
    static final String TOKEN_DOC = "Splunk Http Event Collector token.";
    // General Parameters
    static final String INDEX_DOC = "Splunk index names for Kafka topic data separated by comma for multiple topics to "
            + "indexers (\"prod-index1,prod-index2,prod-index3\").";
    static final String SOURCE_DOC = "Splunk event source metadata for Kafka topic data. The same configuration rules "
            + "as indexes can be applied. If left un-configured, the default source binds to"
            + " the HEC token. By default, this setting is empty.";
    static final String SOURCETYPE_DOC = "Splunk event sourcetype metadata for Kafka topic data. The same configuration "
            + "rules as indexes can be applied here. If left unconfigured, the default source"
            + " binds to the HEC token. By default, this setting is empty"
            + "through to splunk. Only use with JSON Event endpoint";
    static final String FLUSH_WINDOW_DOC = "The interval in seconds at which the events from kafka connect will be flushed to Splunk.";
    static final String TOTAL_HEC_CHANNEL_DOC = "Total HEC Channels used to post events to Splunk. When enabling HEC ACK, "
            + "setting to the same or 2X number of indexers is generally good.";
    static final String MAX_HTTP_CONNECTION_PER_CHANNEL_DOC = "Max HTTP connections pooled for one HEC Channel "
            + "when posting events to Splunk.";
    static final String MAX_BATCH_SIZE_DOC = "Maximum batch size when posting events to Splunk. The size is the actual number of "
            + "Kafka events not the byte size. By default, this is set to 100.";
    static final String HTTP_KEEPALIVE_DOC = "Valid settings are true or false. Enables or disables HTTP connection "
            + "keep-alive. By default, this is set to true";
    static final String HEC_THREADS_DOC = "Controls how many threads are spawned to do data injection via HEC in a single "
            + "connector task. By default, this is set to 1.";
    static final String SOCKET_TIMEOUT_DOC = "Max duration in seconds to read / write data to network before internal TCP "
            + "Socket timeout.By default, this is set to 60 seconds.";
    static final String SSL_VALIDATE_CERTIFICATES_DOC = "Valid settings are true or false. Enables or disables HTTPS "
            + "certification validation. By default, this is set to true.";
    static final String ENABLE_COMPRESSSION_DOC = "Valid settings are true or false. Used for enable or disable gzip-compression. By default, this is set to false.";
    // Acknowledgement Parameters
    // Use Ack
    static final String ACK_DOC = "Valid settings are true or false. When set to true Splunk Connect for Kafka will "
            + "poll event ACKs for POST events before check-pointing the Kafka offsets. This is used "
            + "to prevent data loss, as this setting implements guaranteed delivery. By default, this "
            + "setting is set to true.";
    static final String ACK_POLL_INTERVAL_DOC = "This setting is only applicable when splunk.hec.ack.enabled is set to "
            + "true. Internally it controls the event ACKs polling interval. By default, "
            + "this setting is 10 seconds.";
    static final String ACK_POLL_THREADS_DOC = "This setting is used for performance tuning and is only applicable when "
            + "splunk.hec.ack.enabled is set to true. It controls how many threads "
            + "should be spawned to poll event ACKs. By default, this is set to 1.";
    static final String EVENT_TIMEOUT_DOC = "This setting is applicable when splunk.hec.ack.enabled is set to true. "
            + "When events are POSTed to Splunk and before they are ACKed, this setting "
            + "determines how long the connector will wait before timing out and resending. "
            + "By default, this is set to 300 seconds.";
    static final String MAX_OUTSTANDING_EVENTS_DOC = "Maximum amount of un-acknowledged events kept in memory by connector. "
            + "Will trigger back-pressure event to slow collection. By default, this "
            + "is set to 1000000.";
    static final String MAX_RETRIES_DOC = "Number of retries for failed batches before giving up. By default this is set to "
            + "-1 which will retry indefinitely.";

    static final String HEC_BACKOFF_PRESSURE_THRESHOLD_DOC = "The amount of time Splunk Connect for Kafka waits on errors "
            + "sending events to Splunk to attempt resending it";
    // Endpoint Parameters
    static final String RAW_DOC = "Set to true in order for Splunk software to ingest data using the the /raw HEC "
            + "endpoint. Default is false, which will use the /event endpoint.";
    // /raw endpoint only
    static final String LINE_BREAKER_DOC = "Only applicable to /raw HEC endpoint. The setting is used to specify a custom "
            + "line breaker to help Splunk separate the events correctly. Note: For example"
            + "you can specify \"#####\" as a special line breaker.By default, this setting is "
            + "empty.";
    // /event endpoint only
    static final String USE_RECORD_TIMESTAMP_DOC = "Valid settings are true or false. When set to `true`, The timestamp "
            + "is retrieved from the Kafka record and passed to Splunk as a HEC meta-data "
            + "override. This will index events in Splunk with the record timestamp. By "
            + "default, this is set to true.";
    static final String ENRICHMENT_DOC = "Only applicable to /event HEC endpoint. This setting is used to enrich raw data "
            + "with extra metadata fields. It contains a list of key value pairs separated by \",\"."
            + " The configured enrichment metadata will be indexed along with raw event data "
            + "by Splunk software. Note: Data enrichment for /event HEC endpoint is only available "
            + "in Splunk Enterprise 6.5 and above. By default, this setting is empty.";
    static final String TRACK_DATA_DOC = "Valid settings are true or false. When set to true, data loss and data injection "
            + "latency metadata will be indexed along with raw data. This setting only works in "
            + "conjunction with /event HEC endpoint (\"splunk.hec.raw\" : \"false\"). By default"
            + ", this is set to false.";
    static final String HEC_EVENT_FORMATTED_DOC = "Ensures events that are pre-formatted into the properly formatted HEC "
            + "JSON format as per http://dev.splunk.com/view/event-collector/SP-CAAAE6P have meta-data and event data indexed "
            + "correctly by Splunk.";
    // TBD
    static final String SSL_TRUSTSTORE_PATH_DOC = "Path on the local disk to the certificate trust store.";
    static final String SSL_TRUSTSTORE_PASSWORD_DOC = "Password for the trust store.";

    static final String HEADER_SUPPORT_DOC = "Setting will enable Kafka Record headers to be used for meta data override";
    static final String HEADER_CUSTOM_DOC = "Setting will enable look for Record headers with these values and add them"
            + "to each event if present. Custom headers are configured separated by comma for multiple headers. ex,  (\"custom_header_1,custom_header_2,custom_header_3\").";
    static final String HEADER_INDEX_DOC = "Header to use for Splunk Header Index";
    static final String HEADER_SOURCE_DOC = "Header to use for Splunk Header Source";
    static final String HEADER_SOURCETYPE_DOC = "Header to use for Splunk Header Sourcetype";
    static final String HEADER_HOST_DOC = "Header to use for Splunk Header Host";

    // Load Balancer
    static final String LB_POLL_INTERVAL_DOC = "This setting controls the load balancer polling interval. By default, "
            + "this setting is 120 seconds.";
    
    static final String KERBEROS_USER_PRINCIPAL_DOC = "Kerberos user principal";
    static final String KERBEROS_KEYTAB_LOCATION_DOC = "Kerberos keytab path";

    final String splunkToken;
    final String splunkURI;
    final Map<String, Map<String, String>> topicMetas;

    final String indexes;
    final String sourcetypes;
    final String sources;

    final int flushWindow;
    final int totalHecChannels;
    final int maxHttpConnPerChannel;
    final int maxBatchSize;
    final boolean httpKeepAlive;
    final int numberOfThreads;
    final int socketTimeout;
    final boolean validateCertificates;
    final boolean enableCompression;
    final int lbPollInterval;

    final boolean ack;
    final int ackPollInterval;
    final int ackPollThreads;
    final int eventBatchTimeout;
    final int maxOutstandingEvents;
    final int maxRetries;
    final int backoffThresholdSeconds;

    final boolean raw;
    final boolean hecEventFormatted;

    final String lineBreaker;
    final boolean useRecordTimestamp;
    final Map<String, String> enrichments;
    final boolean trackData;

    final boolean hasTrustStorePath;
    final String trustStorePath;
    final String trustStorePassword;

    final boolean headerSupport;
    final String headerCustom;
    final String headerIndex;
    final String headerSource;
    final String headerSourcetype;
    final String headerHost;

    final String kerberosUserPrincipal;
    final String kerberosKeytabPath;


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
        hasTrustStorePath = StringUtils.isNotBlank(trustStorePath);
        trustStorePassword = getPassword(SSL_TRUSTSTORE_PASSWORD_CONF).value();
        validateHttpsConfig(splunkURI);
        eventBatchTimeout = getInt(EVENT_TIMEOUT_CONF);
        ackPollInterval = getInt(ACK_POLL_INTERVAL_CONF);
        ackPollThreads = getInt(ACK_POLL_THREADS_CONF);
        maxHttpConnPerChannel = getInt(MAX_HTTP_CONNECTION_PER_CHANNEL_CONF);
        lbPollInterval = getInt(LB_POLL_INTERVAL_CONF);
        flushWindow = getInt(FLUSH_WINDOW_CONF);
        totalHecChannels = getInt(TOTAL_HEC_CHANNEL_CONF);
        socketTimeout = getInt(SOCKET_TIMEOUT_CONF);
        enrichments = parseEnrichments(getString(ENRICHMENT_CONF));
        trackData = getBoolean(TRACK_DATA_CONF);
        useRecordTimestamp = getBoolean(USE_RECORD_TIMESTAMP_CONF);
        maxBatchSize = getInt(MAX_BATCH_SIZE_CONF);
        numberOfThreads = getInt(HEC_THREDS_CONF);
        if (taskConfig.get(LINE_BREAKER_CONF) != null && taskConfig.get(LINE_BREAKER_CONF).length() == 1) {
            lineBreaker = taskConfig.get(LINE_BREAKER_CONF);
        } else {
            lineBreaker = getString(LINE_BREAKER_CONF);
        }
        maxOutstandingEvents = getInt(MAX_OUTSTANDING_EVENTS_CONF);
        maxRetries = getInt(MAX_RETRIES_CONF);
        backoffThresholdSeconds = getInt(HEC_BACKOFF_PRESSURE_THRESHOLD);
        hecEventFormatted = getBoolean(HEC_EVENT_FORMATTED_CONF);
        validateTopicsAndTopicsRegexCombination(taskConfig);
        topicMetas = initMetaMap(taskConfig);
        headerSupport = getBoolean(HEADER_SUPPORT_CONF);
        headerCustom = getString(HEADER_CUSTOM_CONF);
        headerIndex = getString(HEADER_INDEX_CONF);
        headerSource = getString(HEADER_SOURCE_CONF);
        headerSourcetype = getString(HEADER_SOURCETYPE_CONF);
        headerHost = getString(HEADER_HOST_CONF);
        kerberosUserPrincipal = getString(KERBEROS_USER_PRINCIPAL_CONF);
        kerberosKeytabPath = getString(KERBEROS_KEYTAB_PATH_CONF);
        enableCompression = getBoolean(ENABLE_COMPRESSSION_CONF);
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
                .define(FLUSH_WINDOW_CONF, ConfigDef.Type.INT, 30, ConfigDef.Importance.LOW, FLUSH_WINDOW_DOC)
                .define(TOTAL_HEC_CHANNEL_CONF, ConfigDef.Type.INT, 2, ConfigDef.Importance.HIGH, TOTAL_HEC_CHANNEL_DOC)
                .define(SOCKET_TIMEOUT_CONF, ConfigDef.Type.INT, 60, ConfigDef.Importance.LOW, SOCKET_TIMEOUT_DOC)
                .define(ENRICHMENT_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW, ENRICHMENT_DOC)
                .define(TRACK_DATA_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.LOW, TRACK_DATA_DOC)
                .define(USE_RECORD_TIMESTAMP_CONF, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM, USE_RECORD_TIMESTAMP_DOC)
                .define(HEC_THREDS_CONF, ConfigDef.Type.INT, 1, ConfigDef.Importance.LOW, HEC_THREADS_DOC)
                .define(LINE_BREAKER_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, LINE_BREAKER_DOC)
                .define(MAX_OUTSTANDING_EVENTS_CONF, ConfigDef.Type.INT, 1000000, ConfigDef.Importance.MEDIUM, MAX_OUTSTANDING_EVENTS_DOC)
                .define(MAX_RETRIES_CONF, ConfigDef.Type.INT, -1, ConfigDef.Importance.MEDIUM, MAX_RETRIES_DOC)
                .define(HEC_BACKOFF_PRESSURE_THRESHOLD, ConfigDef.Type.INT, 60, ConfigDef.Importance.MEDIUM, HEC_BACKOFF_PRESSURE_THRESHOLD_DOC)
                .define(HEC_EVENT_FORMATTED_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.LOW, HEC_EVENT_FORMATTED_DOC)
                .define(MAX_BATCH_SIZE_CONF, ConfigDef.Type.INT, 500, ConfigDef.Importance.MEDIUM, MAX_BATCH_SIZE_DOC)
                .define(HEADER_SUPPORT_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.MEDIUM, HEADER_SUPPORT_DOC)
                .define(HEADER_CUSTOM_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, HEADER_CUSTOM_DOC)
                .define(HEADER_INDEX_CONF, ConfigDef.Type.STRING, "splunk.header.index", ConfigDef.Importance.MEDIUM, HEADER_INDEX_DOC)
                .define(HEADER_SOURCE_CONF, ConfigDef.Type.STRING, "splunk.header.source", ConfigDef.Importance.MEDIUM, HEADER_SOURCE_DOC)
                .define(HEADER_SOURCETYPE_CONF, ConfigDef.Type.STRING, "splunk.header.sourcetype", ConfigDef.Importance.MEDIUM, HEADER_SOURCETYPE_DOC)
                .define(HEADER_HOST_CONF, ConfigDef.Type.STRING, "splunk.header.host", ConfigDef.Importance.MEDIUM, HEADER_HOST_DOC)
                .define(LB_POLL_INTERVAL_CONF, ConfigDef.Type.INT, 120, ConfigDef.Importance.LOW, LB_POLL_INTERVAL_DOC)
                .define(ENABLE_COMPRESSSION_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.MEDIUM, ENABLE_COMPRESSSION_DOC)
                .define(KERBEROS_USER_PRINCIPAL_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, KERBEROS_USER_PRINCIPAL_DOC)
                .define(KERBEROS_KEYTAB_PATH_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, KERBEROS_KEYTAB_LOCATION_DOC);
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
              .setlbPollInterval(lbPollInterval)
              .setAckPollThreads(ackPollThreads)
              .setEnableChannelTracking(trackData)
              .setBackoffThresholdSeconds(backoffThresholdSeconds)
              .setTrustStorePath(trustStorePath)
              .setTrustStorePassword(trustStorePassword)
              .setHasCustomTrustStore(hasTrustStorePath)
              .setKerberosPrincipal(kerberosUserPrincipal)
              .setKerberosKeytabPath(kerberosKeytabPath);
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
                + "headerSupport:" + headerSupport + ", "
                + "headerCustom:" + headerCustom + ", "
                + "httpKeepAlive:" + httpKeepAlive + ", "
                + "validateCertificates:" + validateCertificates + ", "
                + "trustStorePath:" + trustStorePath + ", "
                + "socketTimeout:" + socketTimeout + ", "
                + "eventBatchTimeout:" + eventBatchTimeout + ", "
                + "ackPollInterval:" + ackPollInterval + ", "
                + "ackPollThreads:" + ackPollThreads + ", "
                + "maxHttpConnectionPerChannel:" + maxHttpConnPerChannel + ", "
                + "flushWindow:" + flushWindow + ", "
                + "totalHecChannels:" + totalHecChannels + ", "
                + "enrichment:" + getString(ENRICHMENT_CONF) + ", "
                + "maxBatchSize:" + maxBatchSize + ", "
                + "numberOfThreads:" + numberOfThreads + ", "
                + "lineBreaker:" + lineBreaker + ", "
                + "maxOutstandingEvents:" + maxOutstandingEvents + ", "
                + "maxRetries:" + maxRetries + ", "
                + "useRecordTimestamp:" + useRecordTimestamp + ", "
                + "hecEventFormatted:" + hecEventFormatted + ", "
                + "trackData:" + trackData + ", "
                + "headerSupport:" + headerSupport + ", "
                + "headerCustom:" + headerCustom + ", "
                + "headerIndex:" + headerIndex + ", "
                + "headerSource:" + headerSource + ", "
                + "headerSourcetype:" + headerSourcetype + ", "
                + "headerHost:" + headerHost + ", "
                + "enableCompression:" + enableCompression + ", "
                + "lbPollInterval:" + lbPollInterval;
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
        for (final String kv : kvs) {
            String[] kvPairs = split(kv, "=");
            if (kvPairs.length != 2) {
                throw new ConfigException("Invalid enrichment: " + enrichment + ". Expect key value pairs and separated by comma");
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
        /*
         ** to allow the use of 'topics.regex' instead of the static list of topics.
         ** If topics.regex is specified in the config, the Connector will subscribe to all matching topics.
         ** If topics.regex is used, mapping from topic value to Splunk metadata will not work,
         ** so either the Headers must define the Splunk metadata, or simply rely on the HEC token
         ** to set default index, sourcetype, etc.
         */

        // If the config has no "topics" values, skip metamap formation
        if (topics != null && topics.length != 0) {
            for (String topic : topics) {
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
        }
        return metaMap;
    }

    private void validateHttpsConfig(String uriConf) {
        List<String> uris = Arrays.asList(uriConf.split(","));
        for (String uri : uris) {
            if (uri.startsWith("https://") && this.validateCertificates && !this.hasTrustStorePath) {
                throw new ConfigException("Invalid Secure HTTP (HTTPS) configuration: "
                        + SplunkSinkConnectorConfig.URI_CONF + "='" + uriConf + "',"
                        + SplunkSinkConnectorConfig.SSL_VALIDATE_CERTIFICATES_CONF + "='" + this.validateCertificates + "',"
                        + SplunkSinkConnectorConfig.SSL_TRUSTSTORE_PATH_CONF + "='" + this.trustStorePath + "'");
            }
        }
    }

    private void validateTopicsAndTopicsRegexCombination(Map<String, String> taskConfig) {
        String topics = taskConfig.get(SinkConnector.TOPICS_CONFIG);
        String topicsRegex = taskConfig.get(SinkTask.TOPICS_REGEX_CONFIG);
        if(StringUtils.isEmpty(topics) && StringUtils.isEmpty(topicsRegex)) {
            throw new ConfigException("Either topics or topics.regex value must be provided in the config");
        } else if (StringUtils.isNotEmpty(topics) && StringUtils.isNotEmpty(topicsRegex)) {
            throw new ConfigException("Should not provide both topics and topics.regex's value at the same time in the config");
        }
    }

}
