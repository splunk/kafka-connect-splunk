package com.splunk.kafka.connect;

import com.splunk.cloudfwd.PropertyKeys;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;
import java.util.Properties;

/**
* Configuration Class which houses all configuration strings and variables for implementing Kakfa Connect Splunk
* using Kafka Connect and Splunk Cloud Forwarder.
*/
public class SplunkSinkConnectorConfig extends AbstractConfig {
    // Kafka configuration strings
    public static final String TOKEN_CONF = "splunk.hec.token";
    public static final String URI_CONF = "splunk.hec.uri";
    public static final String RAW_CONF = "splunk.hec.raw";
    public static final String ACK_CONF = "splunk.hec.ack.enabled";
    public static final String SSL_VALIDATE_CERTIFICATES_CONF = "splunk.hec.ssl.validate.certs";
    public static final String SSL_TRUSTSTORE_PATH_CONF = "splunk.hec.ssl.trust.store.path";
    public static final String SSL_TRUSTSTORE_PASSWORD_CONF = "splunk.hec.ssl.trust.store.password";
    public static final String CONNECT_TIMEOUT_CONF = "splunk.hec.connect.timeout.ms";
    public static final String READ_TIMEOUT_CONF = "splunk.hec.read.timeout.ms";

     // Kafka configuration description strings
    static final String TOKEN_DOC = "The authorization token to use when writing data to splunk.";
    static final String URI_DOC = "The URI of the remote splunk to write data do.";
    static final String RAW_DOC = "Flag to determine if use /raw HEC endpoint when indexing data to Splunk.";
    static final String ACK_DOC = "Flag to determine if use turn on HEC ACK when indexing data to Splunk.";
    static final String SSL_DOC = "Flag to determine if the connection to splunk should be over ssl.";
    static final String SSL_VALIDATE_CERTIFICATES_DOC = "Flag to determine if ssl connections should validate the certificate" +
                "of the remote host.";
    static final String SSL_TRUSTSTORE_PATH_DOC = "Path on the local disk to the certificate trust store.";
    static final String SSL_TRUSTSTORE_PASSWORD_DOC = "Password for the trust store.";
    static final String CONNECT_TIMEOUT_DOC = "The maximum amount of time for a connection to be established.";
    static final String READ_TIMEOUT_DOC = "Sets the timeout in milliseconds to read data from an established connection " +
                "or 0 for an infinite timeout.";

    // Kafka Configuration Variables
    public final String splunkToken;
    public final String splunkURI;
    public final boolean raw; // /raw or /event HEC
    public final boolean ack; // use HEC ACK ?
    public final boolean validateCertificates;
    public final String trustStorePath;
    public final boolean hasTrustStorePath;
    public final String trustStorePassword;
    public final int connectTimeout;
    public final int readTimeout;

    public SplunkSinkConnectorConfig(Map<String, String> taskConfig) {
        super(conf(), taskConfig);
        splunkToken = this.getPassword(TOKEN_CONF).value();
        splunkURI = this.getString(URI_CONF);
        raw = this.getBoolean(RAW_CONF);
        ack = this.getBoolean(ACK_CONF);
        validateCertificates = this.getBoolean(SSL_VALIDATE_CERTIFICATES_CONF);
        trustStorePath = this.getString(SSL_TRUSTSTORE_PATH_CONF);
        hasTrustStorePath = trustStorePath != null || trustStorePath.isEmpty();
        trustStorePassword = this.getPassword(SSL_TRUSTSTORE_PASSWORD_CONF).toString();
        connectTimeout = this.getInt(CONNECT_TIMEOUT_CONF);
        readTimeout = this.getInt(READ_TIMEOUT_CONF);
    }

    public static ConfigDef conf() {
        return new ConfigDef()
            .define(TOKEN_CONF, ConfigDef.Type.PASSWORD, ConfigDef.Importance.HIGH, TOKEN_DOC)
            .define(URI_CONF, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, URI_DOC)
            .define(RAW_CONF, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.MEDIUM, RAW_DOC)
            .define(ACK_CONF, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM, ACK_DOC)
            .define(SSL_VALIDATE_CERTIFICATES_CONF, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM, SSL_VALIDATE_CERTIFICATES_DOC)
            .define(SSL_TRUSTSTORE_PATH_CONF, ConfigDef.Type.STRING, "", ConfigDef.Importance.HIGH, SSL_TRUSTSTORE_PATH_DOC)
            .define(SSL_TRUSTSTORE_PASSWORD_CONF, ConfigDef.Type.PASSWORD, "", ConfigDef.Importance.HIGH, SSL_TRUSTSTORE_PASSWORD_DOC)
            .define(CONNECT_TIMEOUT_CONF, ConfigDef.Type.INT, 20000, ConfigDef.Importance.LOW, CONNECT_TIMEOUT_DOC)
            .define(READ_TIMEOUT_CONF, ConfigDef.Type.INT, 30000, ConfigDef.Importance.LOW, READ_TIMEOUT_DOC);
    }

    /**
    * Configuration Method to setup all settings related to Splunks Cloud Forwarder
    */
    public Properties cloudfwdConnectionSettings() {
        //FIXME - List of variables available at https://splunk.github.io/cloudfwd/apidocs/constant-values.html
        Properties props = new Properties();
        props.setProperty(PropertyKeys.TOKEN, splunkToken);
        props.setProperty(PropertyKeys.COLLECTOR_URI, splunkURI);

        if (raw) {
            props.setProperty(PropertyKeys.HEC_ENDPOINT_TYPE, "raw");
        } else {
            props.setProperty(PropertyKeys.HEC_ENDPOINT_TYPE, "event");
        }

        props.setProperty(PropertyKeys.ENABLE_CHECKPOINTS, "true");

        if (validateCertificates) {
            props.setProperty(PropertyKeys.DISABLE_CERT_VALIDATION, "false");
        } else {
            props.setProperty(PropertyKeys.DISABLE_CERT_VALIDATION, "true");
        }

        props.setProperty(PropertyKeys.CHANNELS_PER_DESTINATION, "");

        // FIXME
        props.setProperty(PropertyKeys.SSL_CERT_CONTENT, "");

        // FIXME, Added relevant Cloudforwarder settings, importing default values for start
        props.setProperty(PropertyKeys.EVENT_BATCH_SIZE, PropertyKeys.DEFAULT_EVENT_BATCH_SIZE);
        props.setProperty(PropertyKeys.CHANNELS_PER_DESTINATION, PropertyKeys.DEFAULT_CHANNELS_PER_DESTINATION);
        props.setProperty(PropertyKeys.ENABLE_HTTP_DEBUG, "false");
        props.setProperty(PropertyKeys.HEALTH_POLL_MS, PropertyKeys.DEFAULT_HEALTH_POLL_MS);
        props.setProperty(PropertyKeys.RETRIES, PropertyKeys.DEFAULT_RETRIES);
        props.setProperty(PropertyKeys.UNRESPONSIVE_MS, PropertyKeys.DEFAULT_UNRESPONSIVE_MS);

        /*FIXME, may not need these
        props.setProperty(PropertyKeys.ACK_POLL_MS, PropertyKeys.DEFAULT_ACK_POLL_MS);
        props.setProperty(PropertyKeys.ACK_TIMEOUT_MS, PropertyKeys.DEFAULT_ACK_TIMEOUT_MS);
        props.setProperty(PropertyKeys.BLOCKING_TIMEOUT_MS, PropertyKeys.DEFAULT_BLOCKING_TIMEOUT_MS)
        props.setProperty(PropertyKeys.MAX_TOTAL_CHANNELS, PropertyKeys.DEFAULT_MAX_TOTAL_CHANNELS)
        props.setProperty(PropertyKeys.MAX_UNACKED_EVENT_BATCHES_PER_CHANNEL, PropertyKeys.DEFAULT_MAX_UNACKED_EVENT_BATCHES_PER_CHANNEL	)
        */

        return props;
    }

    public String toString() {
        return "splunkURI:" + splunkURI + ", " +
            "raw:" + raw + ", " +
            "ack:" + ack + ", " +
            "validateCertificates:" + validateCertificates + ", " +
            "trustStorePath:" + trustStorePath + ", " +
            "hasTrustStorePath:" + hasTrustStorePath + ", " +
            "connectTimeout:" + connectTimeout + ", " +
            "readTimeout:" + readTimeout;
    }
}
