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
import java.security.KeyStore;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.xml.crypto.KeySelectorException;
import java.security.KeyStoreException;
import java.util.HashMap;
import java.util.Map;


public class SplunkSinkConnectorConfigTest {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkConnectorConfigTest.class);

    @Test
    public void create() {
        UnitUtil uu = new UnitUtil();
        uu.enrichementMap.put("hello", "world");

        Map<String, String> config = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);


        Assert.assertEquals(uu.enrichementMap, connectorConfig.enrichments);
        Assert.assertEquals(1, connectorConfig.topicMetas.size());
        Assert.assertEquals(0, connectorConfig.topicMetas.get("mytopic").size());
        assertMeta(connectorConfig);
        commonAssert(connectorConfig);
    }

    @Test
    public void getHecConfig() {
        for (int i = 0; i < 2; i++) {
            UnitUtil uu = new UnitUtil();
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
            Assert.assertEquals(uu.maxHttpConnPerChannel, config.getMaxHttpConnectionPerChannel());
            Assert.assertEquals(uu.totalHecChannels, config.getTotalChannels());
            Assert.assertEquals(uu.eventBatchTimeout, config.getEventBatchTimeout());
            Assert.assertEquals(uu.httpKeepAlive, config.getHttpKeepAlive());
            Assert.assertEquals(uu.ackPollInterval, config.getAckPollInterval());
            Assert.assertEquals(uu.ackPollThreads, config.getAckPollThreads());
            Assert.assertEquals(uu.trackData, config.getEnableChannelTracking());
        }
    }

    @Test
    public void getHecConfigCustomKeystore() {
        UnitUtil uu = new UnitUtil(1);

        Map<String, String> taskConfig = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        HecConfig config = connectorConfig.getHecConfig();
        Assert.assertEquals(true, config.getHasCustomTrustStore());
        Assert.assertEquals(uu.trustStorePath, config.getTrustStorePath());
        Assert.assertEquals(uu.trustStorePassword, config.getTrustStorePassword());
    }

    @Test
    public void testCustomKeystore() throws KeyStoreException {
        UnitUtil uu = new UnitUtil(1);

        Map<String, String> taskConfig = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        HecConfig config = connectorConfig.getHecConfig();
        Assert.assertEquals(true, config.getHasCustomTrustStore());
        Assert.assertEquals(uu.trustStorePath, config.getTrustStorePath());
        Assert.assertEquals(uu.trustStorePassword, config.getTrustStorePassword());

        SSLContext context = Hec.loadCustomSSLContext(config.getTrustStorePath(),config.getTrustStorePassword());
        Assert.assertNotNull(context);

    }


    @Test
    public void createWithoutEnrichment() {
        UnitUtil uu = new UnitUtil();
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
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.ENRICHMENT_CONF, "i1,i2");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
    }

    @Test
    public void createWithMetaDataUniform() {
        // index, source, sourcetype have same number of elements
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
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
        UnitUtil uu = new UnitUtil();

        // one index, multiple source, sourcetypes
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
        UnitUtil uu = new UnitUtil();

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
        UnitUtil uu = new UnitUtil();

        // one index, multiple source, sourcetypes
        Map<String, String> config = uu.createTaskConfig();
        config.put(SinkConnector.TOPICS_CONFIG, "t1,t2,t3");
        config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1,i2");
        config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "s1,s2,s3");
        config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "e1,e2,e3");
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
    }

    @Test
    public void toStr() {
        UnitUtil uu = new UnitUtil();

        Map<String, String> config = uu.createTaskConfig();
        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);
        String s = connectorConfig.toString();

        // Cred should not be in toString
        Assert.assertNotNull(s);
        Assert.assertFalse(s.contains(uu.trustStorePassword));
        Assert.assertFalse(s.contains(uu.token));
    }

    private void assertMeta(final SplunkSinkConnectorConfig connectorConfig) {
        UnitUtil uu = new UnitUtil();

        Assert.assertEquals(uu.indexes, connectorConfig.indexes);
        Assert.assertEquals(uu.sourcetypes, connectorConfig.sourcetypes);
        Assert.assertEquals(uu.sources, connectorConfig.sources);
    }

    private void commonAssert(final SplunkSinkConnectorConfig connectorConfig) {
        UnitUtil uu = new UnitUtil();

        Assert.assertEquals(uu.token, connectorConfig.splunkToken);
        Assert.assertEquals(uu.uri, connectorConfig.splunkURI);
        Assert.assertEquals(uu.raw, connectorConfig.raw);
        Assert.assertEquals(uu.ack, connectorConfig.ack);

        Assert.assertEquals(uu.httpKeepAlive, connectorConfig.httpKeepAlive);
        Assert.assertEquals(uu.validateCertificates, connectorConfig.validateCertificates);
        Assert.assertEquals(uu.trustStorePath, connectorConfig.trustStorePath);
        Assert.assertEquals(uu.trustStorePassword, connectorConfig.trustStorePassword);
        Assert.assertEquals(uu.eventBatchTimeout, connectorConfig.eventBatchTimeout);
        Assert.assertEquals(uu.ackPollInterval, connectorConfig.ackPollInterval);
        Assert.assertEquals(uu.ackPollThreads, connectorConfig.ackPollThreads);
        Assert.assertEquals(uu.maxHttpConnPerChannel, connectorConfig.maxHttpConnPerChannel);
        Assert.assertEquals(uu.totalHecChannels, connectorConfig.totalHecChannels);
        Assert.assertEquals(uu.socketTimeout, connectorConfig.socketTimeout);
        Assert.assertEquals(uu.trackData, connectorConfig.trackData);
        Assert.assertEquals(uu.maxBatchSize, connectorConfig.maxBatchSize);
        Assert.assertEquals(uu.numOfThreads, connectorConfig.numberOfThreads);
    }
}
