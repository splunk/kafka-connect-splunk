package com.splunk.hecclient;


/**
 * Created by kchen on 10/17/17.
 */
public final class JsonEventBatch extends EventBatch {
    public static final String endpoint = "/services/collector/event";
    public static final String contentType = "application/json; profile=urn:splunk:event:1.0; charset=utf-8";

    @Override
    public void add(Event event) {
        if (event instanceof JsonEvent) {
            events.add(event);
            len += event.length();
        } else {
            throw new HecException("only JsonEvent can be add to JsonEventBatch");
        }
    }

    @Override
    public final String getRestEndpoint() {
        return endpoint;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public EventBatch createFromThis() {
        return new JsonEventBatch();
    }
}