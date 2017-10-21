package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/20/17.
 */
final public class HecClientConfig {
    private List<String> uris;
    private String token;
    private boolean disableSSLCertVerification;
    private boolean httpKeepAlive;
    private int ackPollerThreads;
    private int maxHttpConnectionPerIndexer;
    private int eventBatchTimeout; // in seconds
    private int ackPollInterval; // in seconds

    public HecClientConfig(List<String> uris, String token) {
        this.uris = uris;
        this.token = token;
    }

    public List<String> getUris() {
        return uris;
    }

    public String getToken() {
        return token;
    }

    public boolean getDisableSSLCertVerification() {
        return disableSSLCertVerification;
    }

    public boolean getHttpKeepAlive() {
        return httpKeepAlive;
    }

    public int getAckPollerThreads() {
        return ackPollerThreads;
    }

    public int getMaxHttpConnectionPerIndexer() {
        return maxHttpConnectionPerIndexer;
    }

    public int getEventBatchTimeout() {
        return eventBatchTimeout;
    }

    public int getAckPollInterval() {
        return ackPollInterval;
    }

    public HecClientConfig setDisableSSLCertVerification(boolean disableVerfication) {
        disableSSLCertVerification = disableVerfication;
        return this;
    }

    public HecClientConfig setHttpKeepAlive(boolean keepAlive) {
        httpKeepAlive = keepAlive;
        return this;
    }

    public HecClientConfig setAckPollerThreads(int threads) {
        ackPollerThreads = threads;
        return this;
    }

    public HecClientConfig setMaxHttpConnectionPerIndexer(int poolSize) {
        maxHttpConnectionPerIndexer = poolSize;
        return this;
    }

    public HecClientConfig setEventBatchTimeout(int timeout) {
        eventBatchTimeout = timeout;
        return this;
    }

    public HecClientConfig setAckPollInterval(int interval) {
        ackPollInterval = interval;
        return this;
    }
}
