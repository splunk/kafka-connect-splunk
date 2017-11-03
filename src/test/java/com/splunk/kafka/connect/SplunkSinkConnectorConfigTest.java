package com.splunk.kafka.connect;

import com.splunk.hecclient.HecConfig;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.sink.SinkConnector;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kchen on 11/2/17.
 */
public class SplunkSinkConnectorConfigTest {
    private static final String topics = "mytopic";
    private static final String token = "mytoken";
    private static final String uri = "https://dummy:8088";
    private static final boolean raw = false;
    private static final boolean ack = true;
    private static final String indexes = "";
    private static final String sourcetypes = "";
    private static final String sources = "";
    private static final boolean httpKeepAlive = true;
    private static final boolean validateCertificates = true;
    private static final String trustStorePath = "/tmp/pki.store";
    private static final String trustStorePassword = "mypass";
    private static final int eventBatchTimeout = 1;
    private static final int ackPollInterval = 1;
    private static final int ackPollThreads = 1;
    private static final int maxHttpConnPerChannel = 1;
    private static final int totalHecChannels = 1;
    private static final int socketTimeout = 1;
    private static final String enrichements = "ni=hao";
    private static final Map<String, String> enrichementMap = new HashMap<>();
    private static final boolean trackChannel = true;
    private static final int maxBatchSize = 1;
    private static final int numOfThreads = 1;

    @Test
    public void create() {
        enrichementMap.put("ni", "hao");

        Map<String, String> config = createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);

        Assert.assertEquals(enrichementMap, connectorConfig.enrichements);
        Assert.assertEquals(1, connectorConfig.topicMetas.size());
        Assert.assertEquals(0, connectorConfig.topicMetas.get("mytopic").size());
        assertMeta(connectorConfig);
        commonAssert(connectorConfig);
    }

    @Test
    public void getHecConfig() {
        for (int i = 0; i < 2; i++) {
            Map<String, String> taskConfig = createTaskConfig();
            if (i == 0) {
                taskConfig.put(SplunkSinkConnectorConfig.SSL_VALIDATE_CERTIFICATES_CONF, String.valueOf(true));
            } else {
                taskConfig.put(SplunkSinkConnectorConfig.SSL_VALIDATE_CERTIFICATES_CONF, String.valueOf(false));
            }
            SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
            HecConfig config = connectorConfig.getHecConfig();
            if (i == 0) {
                Assert.assertEquals(false, config.getDisableSSLCertVerification());
            } else {
                Assert.assertEquals(true, config.getDisableSSLCertVerification());
            }
            Assert.assertEquals(maxHttpConnPerChannel, config.getMaxHttpConnectionPerChannel());
            Assert.assertEquals(totalHecChannels, config.getTotalChannels());
            Assert.assertEquals(eventBatchTimeout, config.getEventBatchTimeout());
            Assert.assertEquals(httpKeepAlive, config.getHttpKeepAlive());
            Assert.assertEquals(ackPollInterval, config.getAckPollInterval());
            Assert.assertEquals(ackPollThreads, config.getAckPollThreads());
            Assert.assertEquals(trackChannel, config.getEnableChannelTracking());
        }
    }

    @Test
    public void createWithoutEnrichment() {
        Map<String, String> config = createTaskConfig();
        config.put(SplunkSinkConnectorConfig.ENRICHEMENT_CONF, "");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertNull(connectorConfig.enrichements);
        assertMeta(connectorConfig);
        commonAssert(connectorConfig);

        config.put(SplunkSinkConnectorConfig.ENRICHEMENT_CONF, null);
        connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertNull(connectorConfig.enrichements);
        assertMeta(connectorConfig);
        commonAssert(connectorConfig);
    }

    @Test(expected = ConfigException.class)
    public void createWithInvalidEnrichment() {
        Map<String, String> config = createTaskConfig();
        config = createTaskConfig();
        config.put(SplunkSinkConnectorConfig.ENRICHEMENT_CONF, "i1,i2");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
    }

    @Test
    public void createWithMetaDataUniform() {
        // index, source, sourcetype have same number of elements
        Map<String, String> config = createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1,t2,t3");
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1,i2,i3");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1,s2,s3");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1,e2,e3");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);

        Map<String, Map<String, String>> topicMetas = new HashMap<>();
        for (int i = 1; i < 4; i++) {
            Map<String, String> meta = new HashMap<>();
            meta.put(SplunkSinkConnectorConfig.INDEX, "i" + String.valueOf(i));
            meta.put(SplunkSinkConnectorConfig.SOURCE, "s" + String.valueOf(i));
            meta.put(SplunkSinkConnectorConfig.SOURCETYPE, "e" + String.valueOf(i));
            topicMetas.put("t" + String.valueOf(i), meta);
        }
        Assert.assertEquals(topicMetas, connectorConfig.topicMetas);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());
        commonAssert(connectorConfig);
    }

    @Test
    public void createWithMetaDataNonUniform() {
        // one index, multiple source, sourcetypes
        Map<String, String> config = createTaskConfig();
        config = createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1,t2,t3");
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1,s2,s3");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1,e2,e3");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);

        Map<String, Map<String, String>> topicMetas = new HashMap<>();
        for (int i = 1; i < 4; i++) {
            Map<String, String> meta = new HashMap<>();
            meta.put(SplunkSinkConnectorConfig.INDEX, "i1");
            meta.put(SplunkSinkConnectorConfig.SOURCE, "s" + String.valueOf(i));
            meta.put(SplunkSinkConnectorConfig.SOURCETYPE, "e" + String.valueOf(i));
            topicMetas.put("t" + String.valueOf(i), meta);
        }
        Assert.assertEquals(topicMetas, connectorConfig.topicMetas);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());
        commonAssert(connectorConfig);
    }

    @Test
    public void hasMetaDataConfigured() {
        // index, source, sourcetypes
        Map<String, String> config = createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1");
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());

        // source, sourcetype
        config = createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1");
        connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());

        // sourcetype
        config = createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1");
        connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());
    }

    @Test(expected = ConfigException.class)
    public void createWithMetaDataError() {
        // one index, multiple source, sourcetypes
        Map<String, String> config = createTaskConfig();
        config = createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1,t2,t3");
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1,i2");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1,s2,s3");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1,e2,e3");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
    }

    @Test
    public void toStr() {
        Map<String, String> config = createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
        String s = connectorConfig.toString();

        // Cred should not be in toString
        Assert.assertNotNull(s);
        Assert.assertFalse(s.contains(trustStorePassword));
        Assert.assertFalse(s.contains(token));
    }

    private void assertMeta(final SplunkSinkConnectorConfig connectorConfig) {
        Assert.assertEquals(indexes, connectorConfig.indexes);
        Assert.assertEquals(sourcetypes, connectorConfig.sourcetypes);
        Assert.assertEquals(sources, connectorConfig.sources);
    }

    private void commonAssert(final SplunkSinkConnectorConfig connectorConfig) {
        Assert.assertEquals(token, connectorConfig.splunkToken);
        Assert.assertEquals(uri, connectorConfig.splunkURI);
        Assert.assertEquals(raw, connectorConfig.raw);
        Assert.assertEquals(ack, connectorConfig.ack);

        Assert.assertEquals(httpKeepAlive, connectorConfig.httpKeepAlive);
        Assert.assertEquals(validateCertificates, connectorConfig.validateCertificates);
        Assert.assertEquals(trustStorePath, connectorConfig.trustStorePath);
        Assert.assertEquals(trustStorePassword, connectorConfig.trustStorePassword);
        Assert.assertEquals(eventBatchTimeout, connectorConfig.eventBatchTimeout);
        Assert.assertEquals(ackPollInterval, connectorConfig.ackPollInterval);
        Assert.assertEquals(ackPollThreads, connectorConfig.ackPollThreads);
        Assert.assertEquals(maxHttpConnPerChannel, connectorConfig.maxHttpConnPerChannel);
        Assert.assertEquals(totalHecChannels, connectorConfig.totalHecChannels);
        Assert.assertEquals(socketTimeout, connectorConfig.socketTimeout);
        Assert.assertEquals(trackChannel, connectorConfig.trackChannel);
        Assert.assertEquals(maxBatchSize, connectorConfig.maxBatchSize);
        Assert.assertEquals(numOfThreads, connectorConfig.numberOfThreads);
    }

    private Map<String, String> createTaskConfig() {
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
        config.put(SplunkSinkConnectorConfig.ENRICHEMENT_CONF, String.valueOf(enrichements));
        config.put(SplunkSinkConnectorConfig.TRACK_CHANNEL_CONF, String.valueOf(trackChannel));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(maxBatchSize));
        config.put(SplunkSinkConnectorConfig.HEC_THREDS_CONF, String.valueOf(numOfThreads));
        return config;
    }
}
