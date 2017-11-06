package com.splunk.hecclient;

/**
 * Created by kchen on 10/31/17.
 */
public interface LoadBalancerInf {
    void add(HecChannel channel);
    void remove(HecChannel channel);
    void send(final EventBatch batch);
    int size();
}
