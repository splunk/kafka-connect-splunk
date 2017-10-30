package com.splunk.hecclient.examples;

import com.splunk.hecclient.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.*;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;


public final class IndexerExample{
    public static void main(String[] args) {
        String uri = "https://localhost:8088";
        String token = "1CB57F19-DC23-419A-8EDA-BA545DD3674D";
        String ackToken = "1B901D2B-576D-40CD-AF1E-98141B499534";
        String ackToken2 = "1B901D2B-576D-40CD-AF1E-98141B499534";

        Poller poller = new ResponsePoller(new PrintIt());

        CloseableHttpClient ackClient = createHttpClient();
        Poller ackPoller = new HecAckPoller(new PrintIt());
        ackPoller.start();

        CloseableHttpClient client = createHttpClient();
        Indexer indexer = new Indexer(uri, ackToken, client, ackPoller);

        // Json
        int n = 100000;
        int m = 100;

        Thread jsonThr = new Thread(new Runnable() {
            public void run() {
                for (int j = 0; j < n; j++) {
                    EventBatch batch = new JsonEventBatch();
                    for (int i = 0; i < m; i++) {
                        Event evt = new JsonEvent("my message: " +  (m * j + i), null);
                        evt.setSourcetype("test-json-event");
                        batch.add(evt);
                    }
                    indexer.send(batch);
                }
            }
        });
        jsonThr.start();

        // raw
        Thread rawThr = new Thread(new Runnable() {
            public void run() {
                 for (int j = 0; j < n; j++) {
                    EventBatch batch = new RawEventBatch("main", null,"test-raw-event", null, -1);
                    for (int i = 0; i < m; i++) {
                        Event evt = new RawEvent("my raw message: " +  (m * j + i) + "\n", null);
                        evt.setSourcetype("test-raw-event");
                        batch.add(evt);
                    }
                    indexer.send(batch);
                }
            }
        });
        rawThr.start();

        try {
            TimeUnit.SECONDS.sleep(6000);
        } catch (InterruptedException ex) {
        }

        Logger log = LoggerFactory.getLogger(IndexerExample.class);
        log.info("Done");
        poller.stop();
        ackPoller.stop();
        try {
            ackClient.close();
            client.close();
        } catch (Exception ex) {
            log.error("failed to close client", ex);
        }
    }

    private static CloseableHttpClient createHttpClient() {
        TrustStrategy trustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) {
                return true;
            }
        };

        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        SSLContext context = null;
        try {
            context = new SSLContextBuilder().loadTrustMaterial(trustStrategy).build();
        } catch (Exception ex) {
            throw new RuntimeException("failed to create SSL connection factory", ex);
        }

        SSLConnectionSocketFactory f = new SSLConnectionSocketFactory(context,  hostnameVerifier);
        return HttpClients.custom()
                .setSSLSocketFactory(f)
                .build();
    }
}