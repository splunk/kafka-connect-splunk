package com.splunk.hecclient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.splunk.hecclient.errors.InvalidEventException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kchen on 10/17/17.
 */

public class JsonEvent extends Event {
    private static final String EVENT = "event";


    public JsonEvent(Object data, Object tied) {
        super(data, tied);

        if (data instanceof String) {
            if (((String) data).isEmpty()) {
                throw new InvalidEventException("Empty data json event");
            }
        }
    }

    @Override
    public String toString() {
        try {
            return jsonMapper.writeValueAsString(getJsonNode());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public byte[] getBytes() {
        if (bytes != null) {
            return bytes;
        }

        try {
            return jsonMapper.writeValueAsBytes(getJsonNode());
        } catch (Exception ex) {
            log.error("Invalid json event:" + ex);
            throw new InvalidEventException("Failed to json marshal the event: " + ex.getMessage());
        }
    }

    private ObjectNode getJsonNode() {
        Map eventJSON = new LinkedHashMap();

        if (time > 0) {
            eventJSON.put(TIME, String.valueOf(time));
        }
        putIfPresent(eventJSON, INDEX, index);
        putIfPresent(eventJSON, HOST, host);
        putIfPresent(eventJSON, SOURCETYPE, sourcetype);
        putIfPresent(eventJSON, SOURCE, source);
        eventJSON.put(EVENT, data);

        ObjectNode eventNode = (ObjectNode) jsonMapper.valueToTree(eventJSON);
        return eventNode;
    }

    private static void putIfPresent(Map collection, String tag, String value) {
        if (value != null && !value.isEmpty()) {
            collection.put(tag, value);
        }
    }
}