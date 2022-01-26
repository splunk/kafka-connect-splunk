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

import com.splunk.hecclient.Hec;
import com.splunk.hecclient.HecConfig;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.sink.SinkConnector;

import org.apache.kafka.connect.sink.SinkTask;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.security.KeyStoreException;
import java.util.HashMap;
import java.util.Map;

public class SplunkSinkConnectorConfigTest {

    @Test
    public void create() {
        UnitUtil uu = new UnitUtil(0);
        uu.configProfile.getEnrichementMap().put("ni", "hao");
        uu.configProfile.getEnrichementMap().put("hello", "world");

        Map<String, String> config = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);

        Assert.assertEquals(uu.configProfile.getEnrichementMap(), connectorConfig.enrichments);
        Assert.assertEquals(1, connectorConfig.topicMetas.size());
        Assert.assertEquals(0, connectorConfig.topicMetas.get("mytopic").size());
        assertMeta(connectorConfig);
        commonAssert(connectorConfig);
    }

    @Test
    public void getHecConfig() {
        for (int i = 0; i < 2; i++) {
            UnitUtil uu = new UnitUtil(0);
            Map<String, String> taskConfig = uu.createTaskConfig();
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
            Assert.assertEquals(uu.configProfile.getMaxHttpConnPerChannel(), config.getMaxHttpConnectionPerChannel());
            Assert.assertEquals(uu.configProfile.getTotalHecChannels(), config.getTotalChannels());
            Assert.assertEquals(uu.configProfile.getEventBatchTimeout(), config.getEventBatchTimeout());
            Assert.assertEquals(uu.configProfile.isHttpKeepAlive(), config.getHttpKeepAlive());
            Assert.assertEquals(uu.configProfile.getAckPollInterval(), config.getAckPollInterval());
            Assert.assertEquals(uu.configProfile.getAckPollThreads(), config.getAckPollThreads());
            Assert.assertEquals(uu.configProfile.isTrackData(), config.getEnableChannelTracking());
        }
    }

    @Test
    public void getHecConfigCustomKeystore() {
        UnitUtil uu = new UnitUtil(1);

        Map<String, String> taskConfig = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        HecConfig config = connectorConfig.getHecConfig();
        Assert.assertEquals(true, config.getHasCustomTrustStore());
        Assert.assertEquals(uu.configProfile.getTrustStorePath(), config.getTrustStorePath());
        Assert.assertEquals(uu.configProfile.getTrustStorePassword(), config.getTrustStorePassword());
    }

    @Test
    public void testCustomKeystore() throws KeyStoreException {
        UnitUtil uu = new UnitUtil(1);

        Map<String, String> taskConfig = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        HecConfig config = connectorConfig.getHecConfig();
        Assert.assertEquals(true, config.getHasCustomTrustStore());
        Assert.assertEquals(uu.configProfile.getTrustStorePath(), config.getTrustStorePath());
        Assert.assertEquals(uu.configProfile.getTrustStorePassword(), config.getTrustStorePassword());

        SSLContext context = Hec.loadCustomSSLContext(config.getTrustStorePath(),config.getTrustStorePassword());
        Assert.assertNotNull(context);

    }

    @Test
    public void testNoCustomKeystore() throws KeyStoreException {
        UnitUtil uu = new UnitUtil(2);

        Map<String, String> taskConfig = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        HecConfig config = connectorConfig.getHecConfig();

        Assert.assertEquals(false, config.getHasCustomTrustStore());
    }


    @Test
    public void createWithoutEnrichment() {
        UnitUtil uu = new UnitUtil(0);
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.ENRICHMENT_CONF, "");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertNull(connectorConfig.enrichments);
        assertMeta(connectorConfig);
        commonAssert(connectorConfig);

        config.put(SplunkSinkConnectorConfig.ENRICHMENT_CONF, null);
        connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertNull(connectorConfig.enrichments);
        assertMeta(connectorConfig);
        commonAssert(connectorConfig);
    }

    @Test(expected = ConfigException.class)
    public void createWithInvalidEnrichment() {
        UnitUtil uu = new UnitUtil(0);
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.ENRICHMENT_CONF, "i1,i2");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
    }

    @Test(expected = ConfigException.class)
    public void createWithInvalidHttpsConfig() {
        UnitUtil uu = new UnitUtil(0);
        uu.configProfile.setValidateCertificates(true);
        uu.configProfile.setTrustStorePath("");
        Map<String, String> config = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
    }

    @Test
    public void createWithMetaDataUniform() {
        // index, source, sourcetype have same number of elements
        UnitUtil uu = new UnitUtil(0);
        Map<String, String> config = uu.createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1,t2,t3");
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1,i2,i3");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1,s2,s3");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1,e2,e3");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);

        Map<String, Map<String, String>> topicMetas = new HashMap<>();
        for (int i = 1; i < 4; i++) {
            Map<String, String> meta = new HashMap<>();
            meta.put(SplunkSinkConnectorConfig.INDEX, "i" + i);
            meta.put(SplunkSinkConnectorConfig.SOURCE, "s" + i);
            meta.put(SplunkSinkConnectorConfig.SOURCETYPE, "e" + i);
            topicMetas.put("t" + i, meta);
        }
        Assert.assertEquals(topicMetas, connectorConfig.topicMetas);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());
        commonAssert(connectorConfig);
    }

    @Test
    public void createWithMetaDataNonUniform() {
        UnitUtil uu = new UnitUtil(0);

        // one index, multiple source, source types
        Map<String, String> config = uu.createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1,t2,t3");
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1,s2,s3");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1,e2,e3");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);

        Map<String, Map<String, String>> topicMetas = new HashMap<>();
        for (int i = 1; i < 4; i++) {
            Map<String, String> meta = new HashMap<>();
            meta.put(SplunkSinkConnectorConfig.INDEX, "i1");
            meta.put(SplunkSinkConnectorConfig.SOURCE, "s" + i);
            meta.put(SplunkSinkConnectorConfig.SOURCETYPE, "e" + i);
            topicMetas.put("t" + i, meta);
        }
        Assert.assertEquals(topicMetas, connectorConfig.topicMetas);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());
        commonAssert(connectorConfig);
    }

    @Test
    public void hasMetaDataConfigured() {
        UnitUtil uu = new UnitUtil(0);

        // index, source, sourcetypes
        Map<String, String> config = uu.createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1");
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());

        // source, sourcetype
        config = uu.createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1");
        connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());

        // sourcetype
        config = uu.createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1");
        connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertTrue(connectorConfig.hasMetaDataConfigured());
    }

    @Test(expected = ConfigException.class)
    public void createWithMetaDataError() {
        UnitUtil uu = new UnitUtil(0);

        // one index, multiple source, sourcetypes
        Map<String, String> config = uu.createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1,t2,t3");
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1,i2");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1,s2,s3");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1,e2,e3");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
    }

    @Test
    public void createWithEmptyTopicMetaData() {
        UnitUtil uu = new UnitUtil(4);

        // when topics.regex value use in config then skip formation of topicMeta
        Map<String, String> config = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertEquals(0, connectorConfig.topicMetas.size());
    }

    @Test(expected = ConfigException.class)
    public void testTopicsAndTopicsRegexCombination() {
        UnitUtil uu = new UnitUtil(4);
        Map<String, String> config = uu.createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "topic1");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
    }

    @Test(expected = ConfigException.class)
    public void testEmptyTopicsAndTopicsRegexCombination() {
        UnitUtil uu = new UnitUtil(4);
        Map<String, String> config = uu.createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "");
        config.put(SinkTask.TOPICS_REGEX_CONFIG, "");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
    }

    @Test
    public void testSpecialCharLineBreaker() {
        UnitUtil uu = new UnitUtil(0);
        Map<String, String> config = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertEquals("\n", connectorConfig.lineBreaker);

        config.put(SplunkSinkConnectorConfig.LINE_BREAKER_CONF, "\r");
        connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertEquals("\r", connectorConfig.lineBreaker);

        config.put(SplunkSinkConnectorConfig.LINE_BREAKER_CONF, "\t");
        connectorConfig = new SplunkSinkConnectorConfig(config);
        Assert.assertEquals("\t", connectorConfig.lineBreaker);
    }

    @Test
    public void toStr() {
        UnitUtil uu = new UnitUtil(0);

        Map<String, String> config = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
        String s = connectorConfig.toString();

        // Cred should not be in toString
        Assert.assertNotNull(s);
        Assert.assertFalse(s.contains(uu.configProfile.getTrustStorePassword()));
        Assert.assertFalse(s.contains(uu.configProfile.getToken()));
    }

    private void assertMeta(final SplunkSinkConnectorConfig connectorConfig) {
        UnitUtil uu = new UnitUtil(0);

        Assert.assertEquals(uu.configProfile.getIndexes(), connectorConfig.indexes);
        Assert.assertEquals(uu.configProfile.getSourcetypes(), connectorConfig.sourcetypes);
        Assert.assertEquals(uu.configProfile.getSources(), connectorConfig.sources);
    }

    private void commonAssert(final SplunkSinkConnectorConfig connectorConfig) {
        UnitUtil uu = new UnitUtil(0);

        Assert.assertEquals(uu.configProfile.getToken(), connectorConfig.splunkToken);
        Assert.assertEquals(uu.configProfile.getUri(), connectorConfig.splunkURI);
        Assert.assertEquals(uu.configProfile.isRaw(), connectorConfig.raw);
        Assert.assertEquals(uu.configProfile.isAck(), connectorConfig.ack);

        Assert.assertEquals(uu.configProfile.isHttpKeepAlive(), connectorConfig.httpKeepAlive);
        Assert.assertEquals(uu.configProfile.isValidateCertificates(), connectorConfig.validateCertificates);
        Assert.assertEquals(uu.configProfile.getTrustStorePath(), connectorConfig.trustStorePath);
        Assert.assertEquals(uu.configProfile.getTrustStorePassword(), connectorConfig.trustStorePassword);
        Assert.assertEquals(uu.configProfile.getEventBatchTimeout(), connectorConfig.eventBatchTimeout);
        Assert.assertEquals(uu.configProfile.getAckPollInterval(), connectorConfig.ackPollInterval);
        Assert.assertEquals(uu.configProfile.getAckPollThreads(), connectorConfig.ackPollThreads);
        Assert.assertEquals(uu.configProfile.getMaxHttpConnPerChannel(), connectorConfig.maxHttpConnPerChannel);
        Assert.assertEquals(uu.configProfile.getTotalHecChannels(), connectorConfig.totalHecChannels);
        Assert.assertEquals(uu.configProfile.getSocketTimeout(), connectorConfig.socketTimeout);
        Assert.assertEquals(uu.configProfile.isTrackData(), connectorConfig.trackData);
        Assert.assertEquals(uu.configProfile.getMaxBatchSize(), connectorConfig.maxBatchSize);
        Assert.assertEquals(uu.configProfile.getNumOfThreads(), connectorConfig.numberOfThreads);
    }
}
