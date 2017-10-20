package com.splunk.hecclient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by kchen on 10/17/17.
 */
public abstract class Event {
    public static final String TIME = "time";
    public static final String HOST = "host";
    public static final String INDEX = "index";
    public static final String SOURCE = "source";
    public static final String SOURCETYPE = "sourcetype";

    protected static final ObjectMapper jsonMapper = new ObjectMapper();
    protected static final Logger log = LoggerFactory.getLogger(Event.class);

    protected long time = -1; // epochMillis
    protected String source;
    protected String sourcetype;
    protected String host;
    protected String index;
    protected final Object data;
    protected byte[] bytes; // populated once, use forever

    @JsonIgnore
    private Object tied; // attached comparable object

    public Event(Object data, Object tied) {
        if (data == null) {
            throw new HecClientException("Null data for event");
        }

        this.data = data;
        this.tied = tied;
    }

    public void setTime(long epochMillis) {
        this.time = epochMillis;
    }

    public Event setSource(String source) {
        this.source = source;
        return this;
    }

    public Event setSourcetype(String sourcetype) {
        this.sourcetype = sourcetype;
        return this;
    }

    public Event setHost(String host) {
        this.host = host;
        return this;
    }

    public Event setIndex(String index) {
        this.index = index;
        return this;
    }

    public long getTime() {
        return time;
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

    public String getIndex() {
        return index;
    }

    public Object getData() {
        return data;
    }

    public Object getTiedObject() {
        return tied;
    }

    public int length() {
        return getBytes().length;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(getBytes());
    }

    public void writeTo(OutputStream out) throws IOException {
        out.write(getBytes());
    }

    public abstract byte[] getBytes();

    public abstract String toString();
}
