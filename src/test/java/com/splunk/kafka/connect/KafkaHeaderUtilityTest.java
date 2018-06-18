/*
 * Copyright 2017-2018 Splunk, Inc..
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

import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class KafkaHeaderUtilityTest {

    @Test
    public void checkKafkaHeaderUtilityGetters() {
        UnitUtil uu = new UnitUtil(3);
        Map<String, String> config = uu.createTaskConfig();

        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);

        SinkRecord record = setupRecord();
        Headers headers = record.headers();

        headers.addString(uu.configProfile.getHeaderIndex(), "splunk.header.index");
        headers.addString(uu.configProfile.getHeaderHost(), "splunk.header.host");
        headers.addString(uu.configProfile.getHeaderSource(), "splunk.header.source");
        headers.addString(uu.configProfile.getHeaderSourcetype(), "splunk.header.sourcetype");

        System.out.println(headers.toString());

        KafkaHeaderUtility kafkaHeaderUtility = new KafkaHeaderUtility(record, connectorConfig);

        Assert.assertEquals("splunk.header.index", (kafkaHeaderUtility.getSplunkHeaderIndex()));
        Assert.assertEquals("splunk.header.host", (kafkaHeaderUtility.getSplunkHeaderHost()));
        Assert.assertEquals("splunk.header.source", (kafkaHeaderUtility.getSplunkHeaderSource()));
        Assert.assertEquals("splunk.header.sourcetype", (kafkaHeaderUtility.getSplunkHeaderSourcetype()));
    }

    @Test
    public void CompareRecordHeaders() {
        UnitUtil uu = new UnitUtil(3);
        Map<String, String> config = uu.createTaskConfig();

        SinkRecord record_1 = setupRecord();

        Headers headers_1 = record_1.headers();
        headers_1.addString("splunk.header.index", "header-index");
        headers_1.addString("splunk.header.host", "header.splunk.com");
        headers_1.addString("splunk.header.source", "headersource");
        headers_1.addString("splunk.header.sourcetype", "test message");

        SplunkSinkConnectorConfig connectorConfig = new SplunkSinkConnectorConfig(config);

        KafkaHeaderUtility kafkaHeaderUtility = new KafkaHeaderUtility(record_1, connectorConfig);
        kafkaHeaderUtility.setHeaders(headers_1);

        SinkRecord record_2 = setupRecord();

        Headers headers_2 = record_2.headers();
        headers_2.addString("splunk.header.index", "header-index");
        headers_2.addString("splunk.header.host", "header.splunk.com");
        headers_2.addString("splunk.header.source", "headersource");
        headers_2.addString("splunk.header.sourcetype", "test message");

        Assert.assertTrue(kafkaHeaderUtility.compareRecordHeaders(record_2));

        SinkRecord record_3 = setupRecord();

        Headers headers_3 = record_3.headers();
        headers_3.addString("splunk.header.index", "header-index=diff");
        headers_3.addString("splunk.header.host", "header.splunk.com");
        headers_3.addString("splunk.header.source", "headersource");
        headers_3.addString("splunk.header.sourcetype", "test message");

        Assert.assertFalse(kafkaHeaderUtility.compareRecordHeaders(record_3));
    }

    public SinkRecord setupRecord() {
        String topic = "test-topic";
        int partition = 1;
        Schema keySchema = null;
        Object key = "key";
        Schema valueSchema = null;
        Object value = "value";
        long timestamp = System.currentTimeMillis();

        SinkRecord record = createMockSinkRecord(topic, partition, keySchema, key, valueSchema, value, timestamp);
        return record;
    }

    public SinkRecord createMockSinkRecord(String topic, int partition, Schema keySchema, Object key, Schema valueSchema, Object value, long timestamp) {
        return new SinkRecord(topic, partition, keySchema, key, valueSchema, value, timestamp);
    }
}