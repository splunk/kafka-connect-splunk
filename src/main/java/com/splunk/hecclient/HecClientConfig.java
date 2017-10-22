package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/20/17.
 */
final public class HecClientConfig {
    private List<String> uris;
    private String token;
    private boolean disableSSLCertVerification = false;
    private boolean httpKeepAlive = true;
    private int ackPollerThreads = 2;
    private int maxHttpConnectionPerIndexer = 4;
    private int eventBatchTimeout = 60 * 2; // in seconds
    private int ackPollInterval = 10; // in seconds
    private int socketTimeout = 60; // in seconds
    private int socketSendBufferSize = 8 * 1024 * 1024; // in byte

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

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public int getSocketSendBufferSize() {
        return socketSendBufferSize;
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

    public HecClientConfig setSocketTimeout(int timeout /*seconds*/) {
        socketTimeout = timeout;
        return this;
    }

    public HecClientConfig setSocketSendBufferSize(int bufSize /*bytes*/) {
        socketSendBufferSize = bufSize;
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

    public HecClientConfig setEventBatchTimeout(int timeout /*seconds*/) {
        eventBatchTimeout = timeout;
        return this;
    }

    public HecClientConfig setAckPollInterval(int interval /*seconds*/) {
        ackPollInterval = interval;
        return this;
    }
}
