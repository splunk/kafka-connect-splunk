package com.splunk.kafka.connect;

import org.apache.kafka.connect.sink.SinkConnector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by kchen on 11/2/17.
 */
public class UnitUtil {
    final String topics = "mytopic";
    final String token = "mytoken";
    final String uri = "https://dummy:8088";
    final boolean raw = false;
    final boolean ack = true;
    final String indexes = "";
    final String sourcetypes = "";
    final String sources = "";
    final boolean httpKeepAlive = true;
    final boolean validateCertificates = true;
    final String trustStorePath = "/tmp/pki.store";
    final String trustStorePassword = "mypass";
    final int eventBatchTimeout = 1;
    final int ackPollInterval = 1;
    final int ackPollThreads = 1;
    final int maxHttpConnPerChannel = 1;
    final int totalHecChannels = 1;
    final int socketTimeout = 1;
    final String enrichements = "ni=hao";
    final Map<String, String> enrichementMap = new HashMap<>();
    final boolean trackData = true;
    final int maxBatchSize = 1;
    final int numOfThreads = 1;

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
