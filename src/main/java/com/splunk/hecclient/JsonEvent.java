package com.splunk.hecclient;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kchen on 10/17/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JsonEvent extends Event {
    private Map<String, String> fields;

    public JsonEvent(Object data, Object tied) {
        super(data, tied);
    }

    // for JSON deserilzed
    JsonEvent() {
    }

    @Override
    public JsonEvent addFields(final Map<String, String> extraFields) {
        if (extraFields == null || extraFields.isEmpty()) {
            return this;
        }

        if (fields == null) {
            fields = new HashMap<>();
        }
        fields.putAll(extraFields);
        invalidate();

        return this;
    }

    @Override
    public JsonEvent setFields(final Map<String, String> extraFields) {
        fields = extraFields;
        invalidate();
        return this;
    }

    @Override
    public Map<String, String> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        try {
            return jsonMapper.writeValueAsString(this);
        } catch (Exception ex) {
            log.error("failed to json serlized JsonEvent", ex);
            throw new HecException("failed to json serlized JsonEvent", ex);
        }
    }

    @Override
    public byte[] getBytes() {
        if (bytes != null) {
            return bytes;
        }

        try {
            bytes = jsonMapper.writeValueAsBytes(this);
        } catch (Exception ex) {
            log.error("Invalid json event", ex);
            throw new HecException("Failed to json marshal the event", ex);
        }

        return bytes;
    }
}
