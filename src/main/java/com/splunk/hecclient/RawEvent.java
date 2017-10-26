package com.splunk.hecclient;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by kchen on 10/17/17.
 */
public class RawEvent extends Event {
    public RawEvent(Object data, Object tied) {
        super(data, tied);
    }

    @Override
    public byte[] getBytes() {
        if (bytes != null) {
            return bytes;
        }

        if (data instanceof String) {
            String s = (String) data;
            try {
                bytes = s.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                log.error("failed to encode as UTF-8", ex);
                throw new HecClientException("Not UTF-8 encodable ", ex);
            }
        } else if (data instanceof byte[]) {
            bytes = (byte[]) data;
        } else {
            // JSON object
            try {
                bytes = jsonMapper.writeValueAsBytes(data);
            } catch (Exception ex) {
                log.error("Invalid json data", ex);
                throw new HecClientException("Failed to json marshal the data", ex);
            }
        }
        return bytes;
    }

    @Override
    public RawEvent addExtraFields(final Map<String, String> fields) {
        return this;
    }

    @Override
    public String toString() {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            log.error("failed to decode as UTF-8", ex);
            throw new HecClientException("Not UTF-8 decodable", ex);
        }
    }
}
