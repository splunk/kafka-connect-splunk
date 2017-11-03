package com.splunk.hecclient;

/**
 * Created by kchen on 10/29/17.
 */
public interface HecInf {
    boolean send(EventBatch batch);
    void close();
}
