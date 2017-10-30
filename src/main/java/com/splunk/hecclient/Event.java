package com.splunk.hecclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by kchen on 10/17/17.
 */
public abstract class Event {
    static final String TIME = "time";
    static final String HOST = "host";
    static final String INDEX = "index";
    static final String SOURCE = "source";
    static final String SOURCETYPE = "sourcetype";

    static final ObjectMapper jsonMapper = new ObjectMapper();
    protected static final Logger log = LoggerFactory.getLogger(Event.class);

    protected long time = -1; // epochMillis
    protected String source;
    protected String sourcetype;
    protected String host;
    protected String index;
    protected final Object data;
    protected byte[] bytes; // populated once, use forever

    private Object tied; // attached comparable object

    public Event(Object eventData, Object tiedObj) {
        if (eventData == null) {
            throw new HecClientException("Null data for event");
        }

        data = eventData;
        tied = tiedObj;
    }

    public final Event setTime(long epochMillis) {
        this.time = epochMillis;
        return this;
    }

    public final Event setSource(String source) {
        this.source = source;
        return this;
    }

    public final Event setSourcetype(String sourcetype) {
        this.sourcetype = sourcetype;
        return this;
    }

    public final Event setHost(String host) {
        this.host = host;
        return this;
    }

    public final Event setIndex(String index) {
        this.index = index;
        return this;
    }

    public final long getTime() {
        return time;
    }

    public final String getSource() {
        return source;
    }

    public final String getSourcetype() {
        return sourcetype;
    }

    public final String getHost() {
        return host;
    }

    public final String getIndex() {
        return index;
    }

    public final Object getData() {
        return data;
    }

    public final Object getTiedObject() {
        return tied;
    }

    public final int length() {
        byte[] data = getBytes();
        if (endswith(data, (byte) '\n')) {
            return data.length;
        }
        return data.length + 1;
    }

    public final InputStream getInputStream() {
        return new ByteArrayInputStream(getBytes());
    }

    public final void writeTo(OutputStream out) throws IOException {
        byte[] data = getBytes();
        out.write(data);
        if (!endswith(data, (byte) '\n')) {
            // insert '\n'
            out.write('\n');
        }
    }

    public abstract byte[] getBytes();

    public abstract String toString();

    public abstract Event addExtraFields(final Map<String, String> fields);

    private static boolean endswith(byte[] data, byte b) {
        return data.length >= 1 && data[data.length - 1] == b;
    }
}
