package com.splunk.hecclient;

/**
 * Created by kchen on 10/29/17.
 */
public interface HecInf {
    public boolean send (EventBatch batch);
    public void close();
}
