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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 *  JSONEvent is used as the Object to represented Splunk events when the /services/collector/event HEC endpoint is to
 *  be used for Splunk ingestion.
 * <p>
 * This class contains overridden methods from Event which will allow adding extra fields to events,
 * retrieving extra fields, converting the JsonEvent object to a String and converting the JsonEvent object into a byte
 * representation.
 * @see         Event
 * @version     1.0
 * @since       1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JsonEvent extends Event {
    private Map<String, String> fields;

    /**
     * Creates a new json event.
     *
     * @param data    Object representation of the event itself without all the extras. Event Data Only
     * @param tied    Object representation of the entire Record being constructed into an Event.
     *
     * @since         1.0
     * @see           Event
     */
    public JsonEvent(Object data, Object tied) {
        super(data, tied);
    }

    /**
     * Creates a new json event with default values.
     *
     * @since           1.0
     */
    JsonEvent() {
    }

    /**
     * ExtraFields consist of custom fields used for enriching events to be bundled in with the base Event. This can
     * used to categorize certain events, allowing flexibility of searching for this field after ingested in Splunk.
     * This differs from the setFields method as it will append any extra fields to the the
     *
     * @param extraFields  Object representation of the event with associated meta-data.
     * @return             Current representation of JsonEvent.
     * @see                JsonEvent
     * @since              1.0
     */
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

    /**
     * ExtraFields consist of custom fields used for enriching events to be bundled in with the base Event. This can
     * used to categorize certain events, allowing flexibility of searching for this field after ingested in Splunk.
     * This differs from the addFields method as it will replace any fields that are currently associated to this object.
     *
     * @param extraFields  Object representation of the event with associated meta-data.
     * @return             Current representation of JsonEvent.
     * @see                JsonEvent
     * @since              1.0
     */
    @Override
    public JsonEvent setFields(final Map<String, String> extraFields) {
        fields = extraFields;
        invalidate();
        return this;
    }

    /**
     * ExtraFields consist of custom fields used for enriching events to be bundled in with the base Event. This can
     * used to categorize certain events, allowing flexibility of searching for this field after ingested in Splunk.
     *
     * @return             Map representation of fields
     * @see                Map
     * @since              1.0
     */
    @Override
    public Map<String, String> getFields() {
        return fields;
    }

    /**
     * Using ObjectMapper the JsonEvent is serialized to a String and returned.
     *
     * @return  Serialized String representation of JsonEvent including all variables in superclass Event.
     *
     * @throws  HecException
     * @see     com.fasterxml.jackson.databind.ObjectMapper
     * @since   1.0
     */
    @Override
    public String toString() {
        try {
            return jsonMapper.writeValueAsString(this);
        } catch (Exception ex) {
            log.error("failed to json serlized JsonEvent", ex);
            throw new HecException("failed to json serialized JsonEvent", ex);
        }
    }

    /**
     * Checks to ensure the byte representation of the Event has not already been calculated. If so, it will return
     * what is already in variable bytes. Otherwise the ObjectMapper through annotations will serialize the
     * JsonEvent object.
     *
     * @return  Serialized byte array representation of JsonEvent including all variables in superclass Event. Will return the
     * value already contained in bytes if it is not null for the Event.
     *
     * @throws  HecException
     * @see     com.fasterxml.jackson.databind.ObjectMapper
     * @since   1.0
     */
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
