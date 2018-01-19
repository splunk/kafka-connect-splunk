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

public final class RawEvent extends Event {
    public RawEvent(Object data, Object tied) {
        super(data, tied);
        // by default disable carriage return line breaker
        setLineBreaker("");
    }

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

    public final Event setLineBreaker(final String breaker) {
        if (breaker != null) {
            this.lineBreaker = breaker;
        }
        return this;
    }

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
