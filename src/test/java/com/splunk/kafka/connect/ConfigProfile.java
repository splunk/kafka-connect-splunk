package com.splunk.kafka.connect;

import java.util.HashMap;
import java.util.Map;

public class ConfigProfile {
    private String topics;
    private String token;
    private String uri;
    private boolean raw;
    private boolean ack;
    private String indexes;
    private String sourcetypes;
    private String sources;
    private boolean httpKeepAlive;
    private boolean validateCertificates;
    private boolean hasTrustStorePath;
    private String trustStorePath;
    private String trustStorePassword;
    private int eventBatchTimeout;
    private int ackPollInterval;
    private int ackPollThreads;
    private int maxHttpConnPerChannel;
    private int totalHecChannels;
    private int socketTimeout;
    private String enrichements;
    private Map<String, String> enrichementMap;
    private boolean trackData;
    private int maxBatchSize;
    private int numOfThreads;

    public ConfigProfile() {
        this(0);
    }

    public ConfigProfile(int profile) {
        switch (profile) {
            case 0:  buildProfileDefault();
                     break;
            case 1:  buildProfileOne();
                     break;
            case 2:  buildProfileTwo();
                     break;
            default: buildProfileDefault();
                     break;
        }
    }

    /* Default Profile:
        - JSON Endpoint
        - With Ack
        - With Truststore
    */
    public ConfigProfile buildProfileDefault() {
        this.topics = "mytopic";
        this.token = "mytoken";
        this.uri = "https://dummy:8088";
        this.raw = false;
        this.ack = true;
        this.indexes = "";
        this.sourcetypes = "";
        this.sources = "";
        this.httpKeepAlive = true;
        this.validateCertificates = true;
        this.hasTrustStorePath = true;
        this.trustStorePath = "./src/test/resources/keystoretest.jks";
        this.trustStorePassword = "Notchangeme";
        this.eventBatchTimeout = 1;
        this.ackPollInterval = 1;
        this.ackPollThreads = 1;
        this.maxHttpConnPerChannel = 1;
        this.totalHecChannels = 1;
        this.socketTimeout = 1;
        this.enrichements = "ni=hao,hello=world";
        this.enrichementMap = new HashMap<>();
        this.trackData = true;
        this.maxBatchSize = 1;
        this.numOfThreads = 1;
        return this;
    }

    /*  Profile One:
        - Raw Endpoint
        - No Ack
        - With Trust Store
    */
    public ConfigProfile buildProfileOne() {
        this.topics = "kafka-data";
        this.token = "mytoken";
        this.uri = "https://dummy:8088";
        this.raw = true;
        this.ack = false;
        this.indexes = "index-1";
        this.sourcetypes = "kafka-data";
        this.sources = "kafka-connect";
        this.httpKeepAlive = true;
        this.validateCertificates = true;
        this.hasTrustStorePath = true;
        this.trustStorePath = "./src/test/resources/keystoretest.jks";
        this.trustStorePassword = "Notchangeme";
        this.eventBatchTimeout = 1;
        this.ackPollInterval = 1;
        this.ackPollThreads = 1;
        this.maxHttpConnPerChannel = 1;
        this.totalHecChannels = 1;
        this.socketTimeout = 1;
        this.enrichements = "hello=world";
        this.enrichementMap = new HashMap<>();
        this.trackData = false;
        this.maxBatchSize = 1;
        this.numOfThreads = 1;
        return this;
    }

    /*
        Profile Two:
        - Raw Endpoint
        - No Ack
        - No Trust Store
    */
    public ConfigProfile buildProfileTwo() {
        this.topics = "kafka-data";
        this.token = "mytoken";
        this.uri = "https://dummy:8088";
        this.raw = true;
        this.ack = false;
        this.indexes = "index-1";
        this.sourcetypes = "kafka-data";
        this.sources = "kafka-connect";
        this.httpKeepAlive = true;
        this.validateCertificates = false;
        this.eventBatchTimeout = 1;
        this.ackPollInterval = 1;
        this.ackPollThreads = 1;
        this.maxHttpConnPerChannel = 1;
        this.totalHecChannels = 1;
        this.socketTimeout = 1;
        this.enrichements = "hello=world";
        this.enrichementMap = new HashMap<>();
        this.trackData = false;
        this.maxBatchSize = 1;
        this.numOfThreads = 1;
        return this;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isRaw() {
        return raw;
    }

    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    public boolean isAck() {
        return ack;
    }

    public void setAck(boolean ack) {
        this.ack = ack;
    }

    public String getIndexes() {
        return indexes;
    }

    public void setIndexes(String indexes) {
        this.indexes = indexes;
    }

    public String getSourcetypes() {
        return sourcetypes;
    }

    public void setSourcetypes(String sourcetypes) {
        this.sourcetypes = sourcetypes;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public boolean isHttpKeepAlive() {
        return httpKeepAlive;
    }

    public void setHttpKeepAlive(boolean httpKeepAlive) {
        this.httpKeepAlive = httpKeepAlive;
    }

    public boolean isValidateCertificates() {
        return validateCertificates;
    }

    public void setValidateCertificates(boolean validateCertificates) {
        this.validateCertificates = validateCertificates;
    }

    public boolean isHasTrustStorePath() {
        return hasTrustStorePath;
    }

    public void setHasTrustStorePath(boolean hasTrustStorePath) {
        this.hasTrustStorePath = hasTrustStorePath;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public int getEventBatchTimeout() {
        return eventBatchTimeout;
    }

    public void setEventBatchTimeout(int eventBatchTimeout) {
        this.eventBatchTimeout = eventBatchTimeout;
    }

    public int getAckPollInterval() {
        return ackPollInterval;
    }

    public void setAckPollInterval(int ackPollInterval) {
        this.ackPollInterval = ackPollInterval;
    }

    public int getAckPollThreads() {
        return ackPollThreads;
    }

    public void setAckPollThreads(int ackPollThreads) {
        this.ackPollThreads = ackPollThreads;
    }

    public int getMaxHttpConnPerChannel() {
        return maxHttpConnPerChannel;
    }

    public void setMaxHttpConnPerChannel(int maxHttpConnPerChannel) {
        this.maxHttpConnPerChannel = maxHttpConnPerChannel;
    }

    public int getTotalHecChannels() {
        return totalHecChannels;
    }

    public void setTotalHecChannels(int totalHecChannels) {
        this.totalHecChannels = totalHecChannels;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getEnrichements() {
        return enrichements;
    }

    public void setEnrichements(String enrichements) {
        this.enrichements = enrichements;
    }

    public Map<String, String> getEnrichementMap() {
        return enrichementMap;
    }

    public void setEnrichementMap(Map<String, String> enrichementMap) {
        this.enrichementMap = enrichementMap;
    }

    public boolean isTrackData() {
        return trackData;
    }

    public void setTrackData(boolean trackData) {
        this.trackData = trackData;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public int getNumOfThreads() {
        return numOfThreads;
    }

    public void setNumOfThreads(int numOfThreads) {
        this.numOfThreads = numOfThreads;
    }

    @Override public String toString() {
        return "ConfigProfile{" + "topics='" + topics + '\'' + ", token='" + token + '\'' + ", uri='" + uri + '\'' + ", raw=" + raw + ", ack=" + ack + ", indexes='" + indexes + '\'' + ", sourcetypes='" + sourcetypes + '\'' + ", sources='" + sources + '\'' + ", httpKeepAlive=" + httpKeepAlive + ", validateCertificates=" + validateCertificates + ", hasTrustStorePath=" + hasTrustStorePath + ", trustStorePath='" + trustStorePath + '\'' + ", trustStorePassword='" + trustStorePassword + '\'' + ", eventBatchTimeout=" + eventBatchTimeout + ", ackPollInterval=" + ackPollInterval + ", ackPollThreads=" + ackPollThreads + ", maxHttpConnPerChannel=" + maxHttpConnPerChannel + ", totalHecChannels=" + totalHecChannels + ", socketTimeout=" + socketTimeout + ", enrichements='" + enrichements + '\'' + ", enrichementMap=" + enrichementMap + ", trackData=" + trackData + ", maxBatchSize=" + maxBatchSize + ", numOfThreads=" + numOfThreads + '}';
    }
}
