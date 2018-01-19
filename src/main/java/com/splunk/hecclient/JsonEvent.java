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
