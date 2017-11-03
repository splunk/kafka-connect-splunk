package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/20/17.
 */
public final class HecConfig {
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

    public HecConfig(List<String> uris, String token) {
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

    public HecConfig setDisableSSLCertVerification(boolean disableVerfication) {
        disableSSLCertVerification = disableVerfication;
        return this;
    }

    public HecConfig setHttpKeepAlive(boolean keepAlive) {
        httpKeepAlive = keepAlive;
        return this;
    }

    public HecConfig setSocketTimeout(int timeout /*seconds*/) {
        socketTimeout = timeout;
        return this;
    }

    public HecConfig setSocketSendBufferSize(int bufSize /*bytes*/) {
        socketSendBufferSize = bufSize;
        return this;
    }

    public HecConfig setMaxHttpConnectionPerChannel(int poolSize) {
        maxHttpConnectionPerChannel = poolSize;
        return this;
    }

    public HecConfig setEventBatchTimeout(int timeout /*seconds*/) {
        eventBatchTimeout = timeout;
        return this;
    }

    public HecConfig setAckPollInterval(int interval /*seconds*/) {
        ackPollInterval = interval;
        return this;
    }

    public HecConfig setAckPollThreads(int num) {
        ackPollThreads = num;
        return this;
    }

    public HecConfig setTotalChannels(int channels) {
        totalChannels = channels;
        return this;
    }

    public HecConfig setEnableChannelTracking(boolean trackChannel) {
        enableChannelTracking = trackChannel;
        return this;
    }
}
