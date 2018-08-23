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

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * SplunkSinkRecord provides helper functionality to enable Header support for the Splunk Connect for Kafka, Namely
 * Header functionality introspection and comparison.
 * <p>
 *
 * @version     1.1.0
 * @since       1.1.0
 */
public class SplunkSinkRecord {
    Headers headers;
    SplunkSinkConnectorConfig connectorConfig;
    String splunkHeaderIndex = "";
    String splunkHeaderHost = "";
    String splunkHeaderSource = "";
    String splunkHeaderSourcetype = "";

    public SplunkSinkRecord() {}

    /**
     * Creates a new Kafka Header utility object. Will take a Kafka SinkRecord and Splunk Sink Connector configuration
     * and create the object based on Headers included with te Kafka Record.
     *
     * @param       record           Kafka SinkRecord to be introspected and headers retrieved from.
     * @param       connectorConfig  Splunk Connector configuration used to determine headers of importance
     * @version     1.1.0
     * @since       1.1.0
     */
    public SplunkSinkRecord(SinkRecord record, SplunkSinkConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
        this.headers = record.headers();
        setMetadataValues();
    }

    /**
     * CompareRecordHeaders will compare a SinkRecords Header values against values that have already populate the
     * Kakfa Header Utility object. This is used in batching events with the same meta-data values while using the /raw
     * event point in Splunk
     *
     * @param       record   Kafka SinkRecord to be introspected and headers retrieved from.
     * @version     1.1.0
     * @since       1.1.0
     */
    protected boolean compareRecordHeaders(SinkRecord record) {
        headers = record.headers();
        String index = headers.lastWithName(connectorConfig.headerIndex).value().toString();
        String host = headers.lastWithName(connectorConfig.headerHost).value().toString();
        String source = headers.lastWithName(connectorConfig.headerSource).value().toString();
        String sourcetype = headers.lastWithName(connectorConfig.headerSourcetype).value().toString();

        if(splunkHeaderIndex.equals(index) && splunkHeaderHost.equals(host) &&
           splunkHeaderSource.equals(source) && splunkHeaderSourcetype.equals(sourcetype))  {
            return true;
        }
        return false;
    }

    private void setMetadataValues() {
        splunkHeaderIndex = this.headers.lastWithName(connectorConfig.headerIndex).value().toString();
        splunkHeaderHost = this.headers.lastWithName(connectorConfig.headerHost).value().toString();
        splunkHeaderSource = this.headers.lastWithName(connectorConfig.headerSource).value().toString();
        splunkHeaderSourcetype = this.headers.lastWithName(connectorConfig.headerSourcetype).value().toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(splunkHeaderIndex)
                .append(splunkHeaderHost)
                .append(splunkHeaderSource)
                .append(splunkHeaderSourcetype)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SplunkSinkRecord) {
            final SplunkSinkRecord other = (SplunkSinkRecord) obj;
            return new EqualsBuilder()
                    .append(splunkHeaderIndex, other.splunkHeaderIndex)
                    .append(splunkHeaderHost, other.splunkHeaderHost)
                    .append(splunkHeaderSource, other.splunkHeaderSource)
                    .append(splunkHeaderSourcetype, other.splunkHeaderSourcetype)
                    .isEquals();
        }
        return false;
    }

    public Headers getHeaders() {
        return headers;
    }
    public String getSplunkHeaderIndex() {
        return splunkHeaderIndex;
    }
    public String getSplunkHeaderHost() {
        return splunkHeaderHost;
    }
    public String getSplunkHeaderSource() {
        return splunkHeaderSource;
    }
    public String getSplunkHeaderSourcetype() {
        return splunkHeaderSourcetype;
    }
}
