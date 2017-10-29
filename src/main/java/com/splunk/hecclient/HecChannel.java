package com.splunk.hecclient;

import org.apache.http.client.methods.HttpUriRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kchen on 10/18/17.
 */
public class HecChannel {
    private String id;
    private Map<String, String> chField;
    private Indexer indexer;

    public HecChannel(Indexer idx) {
        id = newChannelId();
        indexer = idx;
    }

    public Indexer getIndexer() {
        return indexer;
    }

    public String getId() {
        return id;
    }

    public HecChannel setTracking(boolean trackChannel) {
        if (trackChannel) {
            enableTracking();
        } else {
            disableTracking();
        }

        return this;
    }

    private HecChannel enableTracking() {
        if (chField == null) {
            chField = new HashMap<>();
            chField.put("hec-channel", id);
        }
        return this;
    }

    private HecChannel disableTracking() {
        if (chField != null) {
            chField = null;
        }
        return this;
    }

    public boolean send(EventBatch batch) {
        if (chField != null) {
            batch.addExtraFields(chField);
        }
        return indexer.send(batch);
    }

    // for convenience
    public String executeHttpRequest(final HttpUriRequest req) {
        return indexer.executeHttpRequest(req);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof HecChannel) {
            HecChannel ch = (HecChannel) obj;
            return id.equals(ch.getId());
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }

    private static String newChannelId() {
        return java.util.UUID.randomUUID().toString();
    }
}