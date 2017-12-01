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

import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public abstract class EventBatch {
    private static Logger log = LoggerFactory.getLogger(EventBatch.class);

    private static final int INIT = 0;
    private static final int COMMITTED = 1;
    private static final int FAILED = 2;

    private volatile int status = INIT;
    private int failureCount = 0;
    private long sendTimestamp = System.currentTimeMillis() / 1000; // in seconds
    protected int len;
    protected List<Event> events = new ArrayList<>();

    public abstract String getRestEndpoint();
    public abstract String getContentType();
    public abstract void add(Event event);
    public abstract EventBatch createFromThis();

    public final void addExtraFields(final Map<String, String> fields) {
        for (final Event event: events) {
            event.addFields(fields);
        }
    }

    public final boolean isTimedout(long ttl) {
        long flightTime = System.currentTimeMillis() / 1000 - sendTimestamp;
        if (flightTime < ttl) {
            return false;
        }

        log.warn("timed out event batch after {} seconds not acked", ttl);
        return true;
    }

    public final void resetSendTimestamp() {
        sendTimestamp = System.currentTimeMillis() / 1000;
    }

    public final boolean isFailed() {
        return status == FAILED;
    }

    public final boolean isCommitted() {
        return status == COMMITTED;
    }

    public final EventBatch init() {
        status = INIT;
        return this;
    }

    public final EventBatch fail() {
        status = FAILED;
        failureCount += 1;
        return this;
    }

    public final EventBatch commit() {
        status = COMMITTED;
        return this;
    }

    public final int getFailureCount() {
        return failureCount;
    }

    public final List<Event> getEvents() {
        return events;
    }

    // Total length of data for all events
    public final int length() {
        return len;
    }

    // Total number of events
    public final int size() {
        return events.size();
    }

    public final boolean isEmpty() {
        return events.isEmpty();
    }

    public final HttpEntity getHttpEntity() {
        AbstractHttpEntity e = new HttpEventBatchEntity();
        e.setContentType(getContentType());
        return e;
    }

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (Event e: events) {
            builder.append(e.toString());
            builder.append(",");
        }
        builder.append("]");
        return builder.toString();
    }

    private class HttpEventBatchEntity extends AbstractHttpEntity {
        @Override
        public boolean isRepeatable() {
            return true;
        }

        @Override
        public long getContentLength() {
            return length();
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            return new SequenceInputStream(new Enumeration<InputStream>() {
                int idx = -1;

                @Override
                public boolean hasMoreElements() {
                    return !events.isEmpty() && (idx + 1) < events.size();
                }

                @Override
                public InputStream nextElement() {
                    return events.get(++idx).getInputStream();
                }
            });
        }

        @Override
        public void writeTo(OutputStream outstream) throws IOException {
            for (final Event e : events) {
                e.writeTo(outstream);
            }
        }
    }
}
