package com.splunk.kafka.connect;

import com.splunk.hecclient.HecClientConfig;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.*;


public class SplunkSinkConnectorConfig extends AbstractConfig {
    public static final String INDEX = "index";
    public static final String SOURCETYPE = "sourcetype";
    public static final String SOURCE = "source";

    public static final String TOKEN_CONF = "splunk.hec.token";
    public static final String URI_CONF = "splunk.hec.uri";
    public static final String RAW_CONF = "splunk.hec.raw";
    public static final String ACK_CONF = "splunk.hec.ack.enabled";
    public static final String INDEX_CONF = "splunk.indexes";
    public static final String SOURCETYPE_CONF = "splunk.sourcetypes";
    public static final String SOURCE_CONF = "splunk.sources";
    public static final String HTTP_KEEPALIVE_CONF = "splunk.hec.http.keepalive";
    public static final String SSL_VALIDATE_CERTIFICATES_CONF = "splunk.hec.ssl.validate.certs";
    public static final String SSL_TRUSTSTORE_PATH_CONF = "splunk.hec.ssl.trust.store.path";
    public static final String SSL_TRUSTSTORE_PASSWORD_CONF = "splunk.hec.ssl.trust.store.password";
    public static final String SOCKET_TIMEOUT_CONF = "splunk.hec.socket.timeout"; // seconds
    public static final String EVENT_TIMEOUT_CONF = "splunk.hec.event.timeout"; // seconds
    public static final String ACK_POLL_INTERVAL_CONF = "splunk.hec.ack.poll.interval"; // seconds
    public static final String ACK_POLL_THREADS_CONF = "splunk.hec.ack.poll.threads";
    public static final String MAX_HTTP_CONNECTION_PER_CHANNEL_CONF = "splunk.hec.max.http.connection.per.channel";
    public static final String TOTAL_HEC_CHANNEL_CONF = "splunk.hec.total.channels";
    public static final String ENRICHEMENT_CONF = "splunk.hec.json.event.enrichment";
    public static final String MAX_BATCH_SIZE_CONF = "splunk.hec.max.batch.size"; // record count
    public static final String HEC_THREDS_CONF = "splunk.hec.threads";
    public static final String TRACK_CHANNEL_CONF = "splunk.hec.track.channel";

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
    static final String MAX_BATCH_SIZE_DOC = "Max number of Kafka record to be sent to Splunk HEC for one POST";
    static final String HEC_THREADS_DOC = "Number of threads used to POST events to Splunk HEC in single task";
    static final String TRACK_CHANNEL_DOC = "Track HEC channel or not. Is only applicable to splunk.hec.raw=false case";

    public final String splunkToken;
    public final String splunkURI;
    public final boolean raw; // /raw or /event HEC
    public final boolean ack; // use HEC ACK ?
    public final String indexes;
    public final String sourcetypes;
    public final String sources;
    public final boolean validateCertificates;
    public final boolean httpKeepAlive;
    public final String trustStorePath;
    public final boolean hasTrustStorePath;
    public final String trustStorePassword;
    public final int eventBatchTimeout;
    public final int ackPollInterval;
    public final int ackPollThreads;
    public final int maxHttpConnPerChannel;
    public final int totalHecChannels;
    public final int socketTimeout;
    public final boolean trackChannel;
    public final int maxBatchSize;
    public final int numberOfThreads;
    public final Map<String, String> enrichements;

    public final Map<String, Map<String, String>> topicMetas;

    public SplunkSinkConnectorConfig(Map<String, String> taskConfig) {
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
        hasTrustStorePath = trustStorePath != null || trustStorePath.isEmpty();
        trustStorePassword = getPassword(SSL_TRUSTSTORE_PASSWORD_CONF).toString();
        eventBatchTimeout = getInt(EVENT_TIMEOUT_CONF);
        ackPollInterval = getInt(ACK_POLL_INTERVAL_CONF);
        ackPollThreads = getInt(ACK_POLL_THREADS_CONF);
        maxHttpConnPerChannel = getInt(MAX_HTTP_CONNECTION_PER_CHANNEL_CONF);
        totalHecChannels = getInt(TOTAL_HEC_CHANNEL_CONF);
        socketTimeout = getInt(SOCKET_TIMEOUT_CONF);
        enrichements = parseEnrichements(getString(ENRICHEMENT_CONF));
        trackChannel = getBoolean(TRACK_CHANNEL_CONF);
        maxBatchSize = getInt(MAX_BATCH_SIZE_CONF);
        numberOfThreads = getInt(HEC_THREDS_CONF);
        topicMetas = initMetaMap(taskConfig);
    }

    public static ConfigDef conf() {
        return new ConfigDef()
            .define(TOKEN_CONF, ConfigDef.Type.PASSWORD, ConfigDef.Importance.HIGH, TOKEN_DOC)
            .define(URI_CONF, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, URI_DOC)
            .define(RAW_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.MEDIUM, RAW_DOC)
            .define(ACK_CONF, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM, ACK_DOC)
            .define(INDEX_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, INDEX_DOC)
            .define(SOURCETYPE_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, SOURCETYPE_DOC)
            .define(SOURCE_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, SOURCE_DOC)
            .define(HTTP_KEEPALIVE_CONF, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM, HTTP_KEEPALIVE_DOC)
            .define(SSL_VALIDATE_CERTIFICATES_CONF, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM, SSL_VALIDATE_CERTIFICATES_DOC)
            .define(SSL_TRUSTSTORE_PATH_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.HIGH, SSL_TRUSTSTORE_PATH_DOC)
            .define(SSL_TRUSTSTORE_PASSWORD_CONF, ConfigDef.Type.PASSWORD, "", ConfigDef.Importance.HIGH, SSL_TRUSTSTORE_PASSWORD_DOC)
            .define(EVENT_TIMEOUT_CONF, ConfigDef.Type.INT, 120, ConfigDef.Importance.MEDIUM, EVENT_TIMEOUT_DOC)
            .define(ACK_POLL_INTERVAL_CONF, ConfigDef.Type.INT, 10, ConfigDef.Importance.MEDIUM, ACK_POLL_INTERVAL_DOC)
            .define(ACK_POLL_THREADS_CONF, ConfigDef.Type.INT, 2, ConfigDef.Importance.MEDIUM, ACK_POLL_THREADS_DOC)
            .define(MAX_HTTP_CONNECTION_PER_CHANNEL_CONF, ConfigDef.Type.INT, 2, ConfigDef.Importance.MEDIUM, MAX_HTTP_CONNECTION_PER_CHANNEL_DOC)
            .define(TOTAL_HEC_CHANNEL_CONF, ConfigDef.Type.INT, 2, ConfigDef.Importance.HIGH, TOTAL_HEC_CHANNEL_DOC)
            .define(SOCKET_TIMEOUT_CONF, ConfigDef.Type.INT, 60, ConfigDef.Importance.LOW, SOCKET_TIMEOUT_DOC)
            .define(ENRICHEMENT_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW, ENRICHMENT_DOC)
            .define(TRACK_CHANNEL_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.LOW, TRACK_CHANNEL_DOC)
            .define(HEC_THREDS_CONF, ConfigDef.Type.INT, 1, ConfigDef.Importance.LOW, HEC_THREADS_DOC)
            .define(MAX_BATCH_SIZE_CONF, ConfigDef.Type.INT, 100, ConfigDef.Importance.MEDIUM, MAX_BATCH_SIZE_DOC);
    }

    /**
    * Configuration Method to setup all settings related to Splunk HEC Client
    */
    public HecClientConfig getHecClientConfig() {
        HecClientConfig config = new HecClientConfig(Arrays.asList(splunkURI.split(",")), splunkToken);
        config.setDisableSSLCertVerification(!validateCertificates)
                .setSocketTimeout(socketTimeout)
                .setMaxHttpConnectionPerChannel(maxHttpConnPerChannel)
                .setTotalChannels(totalHecChannels)
                .setEventBatchTimeout(eventBatchTimeout)
                .setHttpKeepAlive(httpKeepAlive)
                .setAckPollInterval(ackPollInterval)
                .setAckPollThreads(ackPollThreads)
                .setEnableChannelTracking(trackChannel);
        return config;
    }

    public boolean hasMetaDataConfigured() {
        return (indexes != null && !indexes.isEmpty()
                || (sources != null && !sources.isEmpty())
                || (sourcetypes != null && !sourcetypes.isEmpty()));
    }

    public String toString() {
        return "splunkURI:" + splunkURI + ", " +
            "raw:" + raw + ", " +
            "ack:" + ack + ", " +
            "indexes:" + indexes + ", " +
            "sourcetypes:" + sourcetypes + ", " +
            "sources:" + sources + ", " +
            "httpKeepAlive:" + httpKeepAlive + ", " +
            "validateCertificates:" + validateCertificates + ", " +
            "trustStorePath:" + trustStorePath + ", " +
            "hasTrustStorePath:" + hasTrustStorePath + ", " +
            "socketTimeout:" + socketTimeout + ", " +
            "eventBatchTimeout:" + eventBatchTimeout + ", " +
            "ackPollInterval:" + ackPollInterval + ", " +
            "ackPollThreads:" + ackPollThreads + ", " +
            "maxHttpConnectionPerChannel:" + maxHttpConnPerChannel + ", " +
            "totalHecChannels:" + totalHecChannels + ", " +
            "enrichement: " + getString(ENRICHEMENT_CONF) + ", " +
            "maxBatchSize: " + maxBatchSize + ", " +
            "numberOfThreads: " + numberOfThreads + ", " +
            "trackChannel: " + trackChannel;
    }

    private static String[] split(String data, String sep) {
        if (data != null && !data.trim().isEmpty()) {
            return data.trim().split(sep);
        }
        return null;
    }

    private static Map<String, String> parseEnrichements(String enrichement) {
        String[] kvs = split(enrichement, ",");
        if (kvs == null) {
            return null;
        }

        Map<String, String> enrichmentKvs = new HashMap<>();
        for (final String kv: kvs) {
            String[] kvPairs = split(kv, "=");
            if (kvPairs.length != 2) {
                throw new ConfigException("Invalid enrichement: " + enrichement + ". Expect key value pairs and separated by comma");
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