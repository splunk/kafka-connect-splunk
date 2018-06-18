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
package com.splunk.hecclient;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.http.client.utils.URIBuilder;

public final class RawEventBatch extends EventBatch {
    public static final String endpoint = "/services/collector/raw";
    public static final String contentType = "text/plain; profile=urn:splunk:event:1.0; charset=utf-8";

    private String index;
    private String source;
    private String sourcetype;
    private String host;
    private long time = -1;

    // index, source etc metadata is for the whole raw batch
    public RawEventBatch(String index, String source, String sourcetype, String host, long time) {
        this.index = index;
        this.source = source;
        this.sourcetype = sourcetype;
        this.host = host;
        this.time = time;
    }

    public String getIndex() {
        return index;
    }

    public String getSource() {
        return source;
    }

    public String getSourcetype() {
        return sourcetype;
    }

    public String getHost() {
        return host;
    }

    public long getTime() {
        return time;
    }

    public static Builder factory() {
        return new Builder();
    }

    public static final class Builder {
        private String index;
        private String source;
        private String sourcetype;
        private String host;
        private long time = -1;

        public Builder setIndex(final String index) {
            this.index = index;
            return this;
        }

        public Builder setSource(final String source) {
            this.source = source;
            return this;
        }

        public Builder setSourcetype(final String sourcetype) {
            this.sourcetype = sourcetype;
            return this;
        }

        public Builder setHost(final String host) {
            this.host = host;
            return this;
        }

        public Builder setTime(final long time) {
            this.time = time;
            return this;
        }

        public RawEventBatch build() {
            return new RawEventBatch(index, source, sourcetype, host, time);
        }
    }

    @Override
    public void add(Event event) throws HecException {
        if (event instanceof RawEvent) {
            events.add(event);
            len += event.length();
        } else {
            throw new HecException("only RawEvent can be add to RawEventBatch");
        }
    }

    @Override
    public final String getRestEndpoint() {
        return  endpoint + getMetadataParams();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public EventBatch createFromThis() {
        return new Builder()
                .setIndex(index)
                .setSource(source)
                .setSourcetype(sourcetype)
                .setHost(host)
                .build();
    }

    private String getMetadataParams() {
        URIBuilder params = new URIBuilder();
        putIfPresent(index, "index", params);
        putIfPresent(sourcetype, "sourcetype", params);
        putIfPresent(source, "source", params);
        putIfPresent(host, "host", params);

        if (time != -1) {
            params.addParameter("time",  String.valueOf(time));
        }

        return params.toString();
    }

    private static void putIfPresent(String val, String tag, URIBuilder params) {
        if (val != null && !val.isEmpty()) {
            params.addParameter(tag,  val);
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
        .append(index)
        .append(sourcetype)
        .append(source)
        .append(host)
        .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RawEventBatch) {
            final RawEventBatch other = (RawEventBatch) obj;
            return new EqualsBuilder()
            .append(index, other.index)
            .append(sourcetype, other.sourcetype)
            .append(source, other.source)
            .append(host, other.host)
            .isEquals();
        } else {
            return false;
        }
    }
}