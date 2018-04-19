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

import java.io.UnsupportedEncodingException;

/**
 *  RawEvent is used as the Object to represented Splunk events when the /services/collector/raw HEC endpoint is to be
 *  used for Splunk ingestion.
 * <p>
 * This class contains overridden methods from Event which will allow serialization of the RawEvent object into a
 * String or byte array.
 * @see         Event
 * @version     1.0
 * @since       1.0
 */
public final class RawEvent extends Event {
    public RawEvent(Object data, Object tied) {
        super(data, tied);
        // by default disable carriage return line breaker
        setLineBreaker("");
    }

    /**
     * Checks to see if a byte representation of RawEvent has already been calculated. If so this value is returned.
     * Next a serious of type comparison checks to determinate the format type of data that was used to create the raw
     * event. If its a String, convert to bytes. if its already of a byte array type, return a byte array. Finally if
     * we slip to the final conditional we assume the data is in json format. the json event is then converted to bytes.
     *
     * @return  Serialized byte array representation of RawEvent including all variables in superclass Event. Will return the
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

        if (event instanceof String) {
            String s = (String) event;
            try {
                bytes = s.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                log.error("failed to encode as UTF-8", ex);
                throw new HecException("Not UTF-8 encodable ", ex);
            }
        } else if (event instanceof byte[]) {
            bytes = (byte[]) event;
        } else {
            // JSON object
            try {
                bytes = jsonMapper.writeValueAsBytes(event);
            } catch (Exception ex) {
                log.error("Invalid json data", ex);
                throw new HecException("Failed to json marshal the data", ex);
            }
        }

        return bytes;
    }

    /**
     * Sets the value of the line breaker. The line breaker is used to add a separator value that is streamed along
     * with the event into Splunk. This line breaker value can then be used in conjunction with the Splunk configurable
     * LINE_BREAKER to <a href="http://docs.splunk.com/Documentation/Splunk/7.0.1/Data/Configureeventlinebreaking">
     * break events</a>.
     *
     * @return Current representation of RawEvent.
     *
     * @since   1.0
     */
    public final Event setLineBreaker(final String breaker) {
        if (breaker != null)
            this.lineBreaker = breaker;

        return this;
    }

    /**
     * Raw event is serialized to a String and returned.
     *
     * @return   String representation of RawEvent including all variables in superclass Event.
     *
     * @throws  HecException
     * @since   1.0
     */
    @Override
    public String toString() {
        try {
            return new String(getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            log.error("failed to decode as UTF-8", ex);
            throw new HecException("Not UTF-8 decodable", ex);
        }
    }
}
