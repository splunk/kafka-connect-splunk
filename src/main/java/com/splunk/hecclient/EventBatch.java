package com.splunk.hecclient;

import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */
public abstract class EventBatch {
    protected int len;
    protected List<Event> events = new ArrayList();

    public abstract String getRestEndpoint();
    public abstract String getContentType();
    public abstract void add(Event event);

    // Total length of data for all events
    public int length() {
        return len;
    }

    // Total number of events
    public int size() {
        return events.size();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public HttpEntity getHttpEntity() {
        AbstractHttpEntity e = new HttpEventBatchEntity();
        e.setContentType(getContentType());
        return e;
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
            for (Event e : events) {
                e.writeTo(outstream);
            }
        }
    }
}