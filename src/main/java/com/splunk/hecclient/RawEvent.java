package com.splunk.hecclient;

import java.io.UnsupportedEncodingException;

/**
 * Created by kchen on 10/17/17.
 */
public final class RawEvent extends Event {
    public RawEvent(Object data, Object tied) {
        super(data, tied);
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
                throw new HecClientException("Not UTF-8 encodable ", ex);
            }
        } else if (event instanceof byte[]) {
            bytes = (byte[]) event;
        } else {
            // JSON object
            try {
                bytes = jsonMapper.writeValueAsBytes(event);
            } catch (Exception ex) {
                log.error("Invalid json data", ex);
                throw new HecClientException("Failed to json marshal the data", ex);
            }
        }

        return bytes;
    }

    @Override
    public String toString() {
        try {
            return new String(getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            log.error("failed to decode as UTF-8", ex);
            throw new HecClientException("Not UTF-8 decodable", ex);
        }
    }
}