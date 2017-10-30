package com.splunk.hecclient;


/**
 * Created by kchen on 10/17/17.
 */
public final class JsonEventBatch extends EventBatch {
    @Override
    public void add(Event event) {
        if (event instanceof JsonEvent) {
            events.add(event);
            len += event.length();
        } else {
            throw new HecClientException("only JsonEvent can be add to JsonEventBatch");
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

    @Override
    public EventBatch createFromThis() {
        return new JsonEventBatch();
    }
}