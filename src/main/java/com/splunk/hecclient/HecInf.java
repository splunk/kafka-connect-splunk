package com.splunk.hecclient;

/**
 * Created by kchen on 10/29/17.
 */
public interface HecInf {
    boolean send(final EventBatch batch);
    void close();
}
