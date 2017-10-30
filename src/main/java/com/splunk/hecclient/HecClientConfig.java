package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/20/17.
 */
public final class HecClientConfig {
    private List<String> uris;
    private String token;
    private boolean disableSSLCertVerification = false;
    private boolean httpKeepAlive = true;
    private int maxHttpConnectionPerChannel = 2;
    private int totalChannels = 2;
    private int eventBatchTimeout = 60 * 2; // in seconds
    private int ackPollInterval = 10; // in seconds
    private int ackPollThreads = 2;
    private int socketTimeout = 60; // in seconds
    private int socketSendBufferSize = 8 * 1024 * 1024; // in byte
    private boolean enableChannelTracking = false;

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

    public int getMaxHttpConnectionPerChannel() {
        return maxHttpConnectionPerChannel;
    }

    public int getEventBatchTimeout() {
        return eventBatchTimeout;
    }

    public int getAckPollInterval() {
        return ackPollInterval;
    }

    public int getAckPollThreads() {
        return ackPollThreads;
    }

    public int getTotalChannels() {
        return totalChannels;
    }

    public boolean getEnableChannelTracking() {
        return enableChannelTracking;
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

    public HecClientConfig setMaxHttpConnectionPerChannel(int poolSize) {
        maxHttpConnectionPerChannel = poolSize;
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

    public HecClientConfig  setAckPollThreads(int num) {
        ackPollThreads = num;
        return this;
    }

    public HecClientConfig setTotalChannels(int channels) {
        totalChannels = channels;
        return this;
    }

    public HecClientConfig setEnableChannelTracking(boolean trackChannel) {
        enableChannelTracking = trackChannel;
        return this;
    }
}