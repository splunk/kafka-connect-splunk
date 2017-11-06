package com.splunk.hecclient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.*;

import java.io.*;
import java.util.Map;

/**
 * Created by kchen on 10/17/17.
 */

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class Event {
    static final String TIME = "time";
    static final String HOST = "host";
    static final String INDEX = "index";
    static final String SOURCE = "source";
    static final String SOURCETYPE = "sourcetype";

    static final ObjectMapper jsonMapper = new ObjectMapper();
    protected static final Logger log = LoggerFactory.getLogger(Event.class);

    protected Long time = null; // epochMillis

    protected String source;
    protected String sourcetype;
    protected String host;
    protected String index;
    protected Object event;

    @JsonIgnore
    protected String lineBreaker = "\n";

    @JsonIgnore
    protected byte[] bytes; // populated once, use forever

    @JsonIgnore
    private Object tied; // attached object

    public Event(Object eventData, Object tiedObj) {
        checkEventData(eventData);

        event = eventData;
        tied = tiedObj;
    }

    // for JSON deserialization
    Event() {
    }

    public final Event setEvent(final Object data) {
        checkEventData(data);

        event = data;
        return this;
    }

    public final Event setTied(final Object tied) {
        this.tied = tied;
        return this;
    }

    public final Event setTime(final long epochMillis) {
        this.time = epochMillis;
        return this;
    }

    public final Event setSource(final String source) {
        this.source = source;
        return this;
    }

    public final Event setSourcetype(final String sourcetype) {
        this.sourcetype = sourcetype;
        return this;
    }

    public final Event setHost(final String host) {
        this.host = host;
        return this;
    }

    public final Event setIndex(final String index) {
        this.index = index;
        return this;
    }


    public final Long getTime() {
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

    public final Object getEvent() {
        return event;
    }

    public final String getLineBreaker() {
        return lineBreaker;
    }

    public final Object getTied() {
        return tied;
    }

    public Event addFields(final Map<String, String> fields) {
        return this;
    }

    public Event setFields(final Map<String, String> fields) {
        return this;
    }

    public Map<String, String> getFields() {
        return null;
    }

    public final int length() {
        byte[] data = getBytes();
        return data.length + lineBreaker.getBytes().length;
    }

    @JsonIgnore
    public final InputStream getInputStream() {
        byte[] data = getBytes();
        InputStream eventStream = new ByteArrayInputStream(data);

        // avoid copying the event
        InputStream carriageReturnStream = new ByteArrayInputStream(lineBreaker.getBytes());
        return new SequenceInputStream(eventStream, carriageReturnStream);
    }

    public final void writeTo(OutputStream out) throws IOException {
        byte[] data = getBytes();
        out.write(data);

        // append line breaker
        byte[] breaker = lineBreaker.getBytes();
        out.write(breaker);
    }

    abstract byte[] getBytes();

    private static void checkEventData(Object eventData) {
        if (eventData == null) {
            throw new HecException("Null data for event");
        }

        if (eventData instanceof String) {
            if (((String) eventData).isEmpty()) {
                throw new HecException("Empty event");
            }
        }
    }
}
