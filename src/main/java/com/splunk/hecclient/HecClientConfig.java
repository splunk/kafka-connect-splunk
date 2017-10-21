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
    private int maxHttpConnectionPoolSizePerIndexer;
    private int batchEventTimeout; // in seconds

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

    public int getMaxHttpConnectionPoolSizePerIndexer() {
        return maxHttpConnectionPoolSizePerIndexer;
    }

    public int getBatchEventTimeout() {
        return batchEventTimeout;
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

    public HecClientConfig setMaxHttpConnectionPoolSizePerIndexer(int poolSize) {
        maxHttpConnectionPoolSizePerIndexer = poolSize;
        return this;
    }

    public HecClientConfig setBatchEventTimeout(int timeout) {
        batchEventTimeout = timeout;
        return this;
    }
}
