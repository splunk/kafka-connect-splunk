package com.splunk.hecclient;

/**
 * Created by kchen on 10/18/17.
 */
public class HecChannel {
    private String id;
    private long batchId;
    private Indexer indexer;

    public HecChannel(Indexer idx) {
        id = newChannelId();
        batchId = 0;
        indexer = idx;
    }

    public String getId() {
        return id;
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

    private static String newChannelId() {
        return java.util.UUID.randomUUID().toString();
    }
}