package com.splunk.hecclient;

import com.splunk.hecclient.errors.InvalidEventTypeException;

/**
 * Created by kchen on 10/17/17.
 */
public class JsonEventBatch extends EventBatch {
    public void add(Event event) {
        if (event instanceof JsonEvent) {
            events.add(event);
            len += event.length();
        } else {
            throw new InvalidEventTypeException("only JsonEvent can be add to JsonEventBatch");
        }
    }

    @Override
    public final String getRestEndpoint() {
        return "/services/collector/event";
    }

    @Override
    public String getContentType() {
        return "application/json; profile=urn:splunk:event:1.0; charset=utf-8";
    }
}