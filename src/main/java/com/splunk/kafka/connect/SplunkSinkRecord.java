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
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SplunkSinkRecord provides helper functionality to enable Header support for the Splunk Connect for Kafka, Namely
 * Header functionality introspection and comparison.
 * <p>
 *
 * @version     1.1.0
 * @since       1.1.0
 */
public class SplunkSinkRecord {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkRecord.class);
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
        if(this.headers != null) {
            setMetadataValues();
        }
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

        Header indexHeader = headers.lastWithName(connectorConfig.headerIndex);
        Header hostHeader = headers.lastWithName(connectorConfig.headerHost);
        Header sourceHeader = headers.lastWithName(connectorConfig.headerSource);
        Header sourcetypeHeader = headers.lastWithName(connectorConfig.headerSourcetype);

        String index = "";
        String host = "";
        String source = "";
        String sourcetype = "";

        if(indexHeader != null) {
            index = indexHeader.value().toString();
        }
        if(hostHeader != null) {
            host = hostHeader.value().toString();
        }
        if(sourceHeader != null) {
            source = sourceHeader.value().toString();
        }
        if(sourcetypeHeader != null) {
            sourcetype = sourcetypeHeader.value().toString();
        }

        return splunkHeaderIndex.equals(index) && splunkHeaderHost.equals(host) &&
               splunkHeaderSource.equals(source) && splunkHeaderSourcetype.equals(sourcetype);
    }

    private void setMetadataValues() {
        Header indexHeader = this.headers.lastWithName(connectorConfig.headerIndex);
        Header hostHeader = this.headers.lastWithName(connectorConfig.headerHost);
        Header sourceHeader = this.headers.lastWithName(connectorConfig.headerSource);
        Header sourcetypeHeader = this.headers.lastWithName(connectorConfig.headerSourcetype);

        if(indexHeader != null) {
            splunkHeaderIndex = indexHeader.value().toString();
        }
        if(hostHeader != null) {
            splunkHeaderHost = hostHeader.value().toString();
        }
        if(sourceHeader != null) {
            splunkHeaderSource = sourceHeader.value().toString();
        }
        if(sourcetypeHeader != null) {
            splunkHeaderSourcetype = sourcetypeHeader.value().toString();
        }
    }

    public String id() {
    String separator = "$$$";
    return new StringBuilder()
            .append(splunkHeaderIndex)
            .append(separator)
            .append(splunkHeaderHost)
            .append(separator)
            .append(splunkHeaderSource)
            .append(separator)
            .append(splunkHeaderSourcetype)
            .toString();
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
            return id().equals(other.id());
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