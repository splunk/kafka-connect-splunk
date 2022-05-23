/*
 * Copyright 2017 Splunk, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.splunk.hecclient;

import java.util.List;

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
    private int backoffThresholdSeconds = 60 * 1000;
    private boolean enableChannelTracking = false;
    private boolean hasCustomTrustStore = false;
    private String trustStorePath;
    private String trustStorePassword;
    private int lbPollInterval = 120; // in seconds
    private String kerberosPrincipal;
    private String kerberosKeytabPath;

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

    public int getlbPollInterval() {
        return lbPollInterval;
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

    public int getBackoffThresholdSeconds() {
        return backoffThresholdSeconds;
    }

    public boolean getHasCustomTrustStore() { return hasCustomTrustStore; }

    public String getTrustStorePath() { return trustStorePath; }

    public String getTrustStorePassword() { return trustStorePassword; }

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

    public HecConfig setlbPollInterval(int interval /*seconds*/) {
        lbPollInterval = interval * 1000;
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

    public HecConfig setTrustStorePath(String path) {
        trustStorePath = path;
        return this;
    }

    public HecConfig setTrustStorePassword(String pass) {
        trustStorePassword = pass;
        return this;
    }

    public HecConfig setHasCustomTrustStore(boolean hasStore) {
        hasCustomTrustStore = hasStore;
        return this;
    }

    public HecConfig setEnableChannelTracking(boolean trackChannel) {
        enableChannelTracking = trackChannel;
        return this;
    }

    public HecConfig setBackoffThresholdSeconds(int backoffSeconds) {
        backoffThresholdSeconds = backoffSeconds * 1000;
        return this;
    }

    public String kerberosPrincipal() {
        return kerberosPrincipal;
    }

    public HecConfig setKerberosPrincipal(String kerberosPrincipal) {
        this.kerberosPrincipal = kerberosPrincipal;
        return this;
    }

    public String kerberosKeytabLocation() {
        return kerberosKeytabPath;
    }

    public HecConfig setKerberosKeytabPath(String kerberosKeytabPath) {
        this.kerberosKeytabPath = kerberosKeytabPath;
        return this;
    }

    public boolean kerberosAuthEnabled() {
        return !kerberosPrincipal().isEmpty();
    }
}
