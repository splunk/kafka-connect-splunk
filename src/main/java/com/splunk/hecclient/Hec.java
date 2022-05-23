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

import org.apache.http.impl.client.CloseableHttpClient;

import java.io.FileInputStream;
import java.io.IOException;

import java.security.cert.CertificateException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.KeyManagementException;

import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.kafka.connect.errors.ConnectException;

/**
 * Hec is the central class which will construct the HTTP Event Collector Client to send messages to Splunk.
 *
 * There a 2 classes of HEC client
 *       1.) With Acknowledgement
 *       2.) Without Acknowledgement
 * <p>
 * This class contains factory methods to configure the Hec Client which is reliant on 4 key components
 *       1.) HecConfig - The Hec Configuration(HEC Uri, HEc tokens, total channels, etc..)
 *       2.) LoadBalancerInf - The Load Balancer Interface provides Channel Tracking and Event Batch sending functionality
 *       3.) Poller - For the with Acknowledgement class of Hec clients. Poller will register a HECAckPoller which is
 *           responsible for polling the Splunk HEC endpoint and providing success/failure information per batch.
 *       4.) CloseableHttpClient is the httpclient used to send Event batches to Splunk. close() is used sparingly and will
 *           only close in the event of a failure or finishing of the Poller.
 *
 * @version     1.1.0
 * @since       1.0.0
 * @see         HecConfig
 * @see         LoadBalancerInf
 * @see         Poller
 * @see         CloseableHttpClient
 * @see         HecAckPoller
 */
public class Hec implements HecInf {
    private LoadBalancerInf loadBalancer;
    private Poller poller;
    private CloseableHttpClient httpClient;
    private boolean ownHttpClient = false; //flag for when the HTTPClient is created as part of this Hec object being created

   /**
    * Factory method to creates a new HEC Client with Acknowledgment.
    *
    * @param config    HecConfig containing settings to configure HEC Client.
    * @param callback  PollerCallback providing Acknowledgement functionality.
    * @return          Newly created HEC Client object.
    * @since           1.0.0
    * @see             HecConfig
    * @see             PollerCallback
    */
    public static Hec newHecWithAck(HecConfig config, PollerCallback callback) {
        Hec hec = newHecWithAck(config, Hec.createHttpClient(config), callback);
        hec.setOwnHttpClient(true);
        return hec;
    }

   /**
    * Factory method to create a new HEC Client with Acknowledgment while providing a valid CloseableHttpClient.
    *
    * @param config      HecConfig containing settings to configure HEC Client
    * @param httpClient  CloseableHttpClient provided to factory to be used sending events.
    * @param callback    PollerCallback providing Acknowledgement functionality.
    * @return            Newly created HEC Client object.
    * @since             1.0.0
    * @see               HecConfig
    * @see               PollerCallback
    * @see               CloseableHttpClient
    */
    public static Hec newHecWithAck(HecConfig config, CloseableHttpClient httpClient, PollerCallback callback) {
        return new Hec(config, httpClient, createPoller(config, callback), new LoadBalancer(config, httpClient));
    }

   /**
    * Factory method to create a new HEC Client with Acknowledgment while providing a LoadBalancer.
    *
    * @param config        HecConfig containing settings to configure HEC Client
    * @param callback      PollerCallback providing Acknowledgement functionality.
    * @param loadBalancer  Load Balancer Interface for channel management.
    * @return              Newly created HEC Client object.
    * @since               1.0.0
    * @see                 HecConfig
    * @see                 PollerCallback
    * @see                 LoadBalancer
    */
    public static Hec newHecWithAck(HecConfig config, PollerCallback callback, LoadBalancerInf loadBalancer) {
        Hec hec = new Hec(config, Hec.createHttpClient(config), createPoller(config, callback), loadBalancer);
        hec.setOwnHttpClient(true);
        return hec;
    }

   /**
    * Factory method to create a new HEC Client without Acknowledgment.
    *
    * @param config        HecConfig containing settings to configure HEC Client
    * @param callback      PollerCallback providing Acknowledgement functionality.
    * @return              Newly created HEC Client object.
    * @since               1.0.0
    * @see                 HecConfig
    * @see                 PollerCallback
    */
    public static Hec newHecWithoutAck(HecConfig config, PollerCallback callback) {
        Hec hec = newHecWithoutAck(config, Hec.createHttpClient(config), callback);
        hec.setOwnHttpClient(true);
        return hec;
    }

   /**
    * Factory method to create a new HEC Client without Acknowledgment while providing a valid CloseableHttpClient.
    *
    * @param config      HecConfig containing settings to configure HEC Client
    * @param httpClient  CloseableHttpClient provided to factory to be used sending events.
    * @param callback    PollerCallback providing Acknowledgement \functionality.
    * @return            Newly created HEC Client object.
    * @since             1.0.0
    * @see               HecConfig
    * @see               PollerCallback
    * @see               CloseableHttpClient
    */
    public static Hec newHecWithoutAck(HecConfig config, CloseableHttpClient httpClient, PollerCallback callback) {
        return new Hec(config, httpClient, new ResponsePoller(callback), new LoadBalancer(config, httpClient));
    }

   /**
    * Factory method to create a new HEC Client without Acknowledgment while providing a LoadBalancer.
    *
    * @param config        HecConfig containing settings to configure HEC Client
    * @param callback      PollerCallback providing Acknowledgement functionality.
    * @param loadBalancer  Load Balancer Interface for channel management.
    * @return              Newly created HEC Client object.
    * @since               1.0.0
    * @see                 HecConfig
    * @see                 PollerCallback
    * @see                 LoadBalancer
    */
    public static Hec newHecWithoutAck(HecConfig config, PollerCallback callback, LoadBalancerInf loadBalancer) {
        Hec hec = new Hec(config, Hec.createHttpClient(config), new ResponsePoller(callback), loadBalancer);
        hec.setOwnHttpClient(true);
        return hec;
    }

   /**
    * CreatePoller creates a HecAckPoller from selected HecConfig values and a PollerCallback object.
    * The amount of simultaneous acknowledgment threads, the polling interval and the event batch.(Send Events timeout)
    *
    * @param config        HecConfig containing settings to configure HEC Client
    * @param callback      PollerCallback providing Acknowledgement functionality.
    * @return              Newly created HECAckPoller object.
    * @since               1.0.0
    * @see                 HecConfig
    * @see                 PollerCallback
    * @see                 LoadBalancer
    */
    public static HecAckPoller createPoller(HecConfig config, PollerCallback callback) {
        return new HecAckPoller(callback)
                .setAckPollInterval(config.getAckPollInterval())
                .setAckPollThreads(config.getAckPollThreads())
                .setEventBatchTimeout(config.getEventBatchTimeout());
    }

   /**
    * Hec is created to send events to Splunk's HTTP Event Collector.
    *
    * @param config        HecConfig containing settings to configure HEC Client
    * @param httpClient    CloseableHttpClient provided to factory to be used sending events.
    * @param poller        HecAckPoller for polling acknowledgments from Splunk. ReponsePoller for No acknowledgment.
    * @param loadBalancer  Load Balancer Interface for channel management.
    * @since               1.0.0
    * @see                 HecConfig
    * @see                 CloseableHttpClient
    * @see                 Poller
    * @see                 LoadBalancerInf
    */
    public Hec(HecConfig config, CloseableHttpClient httpClient, Poller poller, LoadBalancerInf loadBalancer) {
        for (int i = 0; i < config.getTotalChannels(); ) {
            for (String uri : config.getUris()) {
                Indexer indexer = new Indexer(uri, httpClient, poller, config);
                indexer.setKeepAlive(config.getHttpKeepAlive());
                indexer.setBackPressureThreshold(config.getBackoffThresholdSeconds());
                loadBalancer.add(uri, indexer.getChannel().setTracking(config.getEnableChannelTracking()));
                i++;
            }
        }

        this.loadBalancer = loadBalancer;
        this.poller = poller;
        this.poller.start();
        this.httpClient = httpClient;
    }

   /**
    * Setter method for when an HttpClient is created as part of this objects creation. Hec has a factory method for
    *
    * @param ownHttpClient  Flag for signally the CloseableHttpClient has been constructed as part of this object.
    * @since                1.0.0
    * @return               Instance of Hec Object
    * @see                  CloseableHttpClient
    */
    public Hec setOwnHttpClient(boolean ownHttpClient) {
        this.ownHttpClient = ownHttpClient;
        return this;
    }

   /**
    * @see HecInf#send(EventBatch)
    */
    @Override
    public final void send (final EventBatch batch) {
        if (batch.isEmpty()) {
            return;
        }
        loadBalancer.send(batch);
    }

   /**
    * @see HecInf#close()
    */
    @Override
    public final void close() {
        poller.stop();
        if (ownHttpClient) {
            try {
                httpClient.close();
            } catch (Exception ex) {
                throw new HecException("failed to close http client", ex);
            }
        }
        loadBalancer.close();
    }

   /**
    * createHttpClient will construct 2 different versions of the a CloseableHttpClient depending on whether a custom
    * trust store is to be used or a default configuration is substantial enough. When a trust store path and password
    * is provided createHttpClient will build an SSL Context to be used with the HTTP Client from the Keystore provided
    * in conjunction with a default TrustManager.
    *
    * @param config Hec Configuration used to construct
    * @since        1.0.0
    * @throws       HecException
    * @return       A configured CloseableHTTPClient customized to the settings proved through config.
    * @see          CloseableHttpClient
    * @see          HecException
    */
    public static CloseableHttpClient createHttpClient(final HecConfig config) {
        int poolSizePerDest = config.getMaxHttpConnectionPerChannel();

        if (config.kerberosAuthEnabled()) {
            try {
              return new HttpClientBuilder().buildKerberosClient();
            } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException ex) {
              throw new ConnectException("Unable to build Kerberos Client", ex);
            }
          }

        // Code block for default client construction
        if(!config.getHasCustomTrustStore() &&
           StringUtils.isBlank(config.getTrustStorePath()) &&
           StringUtils.isBlank(config.getTrustStorePassword())) {  // no trust store path or password provided via config

            return new HttpClientBuilder().setDisableSSLCertVerification(config.getDisableSSLCertVerification())
                    .setMaxConnectionPoolSizePerDestination(poolSizePerDest)
                    .setMaxConnectionPoolSize(poolSizePerDest * config.getUris().size())
                    .build();
        }

        // Code block for custom keystore client construction
        SSLContext context = loadCustomSSLContext(config.getTrustStorePath(), config.getTrustStorePassword());

        if (context != null) {
            return new HttpClientBuilder()
                .setDisableSSLCertVerification(config.getDisableSSLCertVerification())
                .setMaxConnectionPoolSizePerDestination(poolSizePerDest)
                .setMaxConnectionPoolSize(poolSizePerDest * config.getUris().size())
                .setSslContext(context)
                .build();
        }
        else {
             //failure configuring SSL Context created from trust store path and password values
             throw new HecException("trust store path provided but failed to initialize ssl context");
         }
    }

   /**
    * loadCustomSSLContext will take a path to a java key store and a password decode and load the key-store.
    * Passing on the keystore to the loadTrustManagerFactory to retrieve an SSL Context to be used in the creation of
    * a Hec Client with custom key store functionality.
    *
    * @param    path  A file path to the custom key store to be used.
    * @param    pass  The password for the key store file.
    * @since          1.1.0
    * @throws         HecException
    * @return         A configured SSLContect to be used in a CloseableHttpClient
    * @see            KeyStore
    * @see            SSLContext
    */
    public static SSLContext loadCustomSSLContext(String path, String pass) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fileInputStream = new FileInputStream(path);
            ks.load(fileInputStream, pass.toCharArray());

            SSLContext sslContext = loadTrustManagerFactory(ks);
            fileInputStream.close();

            return sslContext;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
            throw new HecException("error loading trust store, check values for trust store and trust store-password", ex);
        }
    }

   /**
    * loadTrustManagerFactory will receive a KeyStore with a loaded key and return an initialized SSLContext to be used
    * for te HEC client later.
    *
    * @param  keyStore A loaded keystore
    * @since           1.1.0
    * @throws          HecException
    * @return          A configured SSLContext to be used in a CloseableHttpClient
    * @see             TrustManagerFactory
    * @see             SSLContext
    * @see             NoSuchAlgorithmException
    * @see             KeyStoreException
    * @see             KeyManagementException
    */
    public static SSLContext loadTrustManagerFactory(KeyStore keyStore) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

            return sslContext;
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
            throw new HecException("error loading KeyStoreManager", ex);
        }
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }
}
