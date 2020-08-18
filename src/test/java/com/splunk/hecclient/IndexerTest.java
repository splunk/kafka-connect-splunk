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

import com.splunk.kafka.connect.VersionUtils;
import org.apache.http.Header;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

public class IndexerTest {
    private static final String baseUrl =  "https://localhost:8088";
    private static final String token =  "mytoken";

    @Test
    public void getHeaders() {
        Indexer indexer = new Indexer(baseUrl, token, null, null);

        Header[] headers = indexer.getHeaders();
        Assert.assertEquals(5, headers.length);
        Assert.assertEquals("Authorization", headers[0].getName());
        Assert.assertEquals("Splunk " + token, headers[0].getValue());
        Assert.assertEquals("X-Splunk-Request-Channel", headers[1].getName());
        Assert.assertEquals(indexer.getChannel().getId(), headers[1].getValue());
        Assert.assertEquals("Connection", headers[2].getName());
        Assert.assertEquals("Keep-Alive", headers[2].getValue());
        Assert.assertEquals("__splunk_app_name", headers[3].getName());
        Assert.assertEquals(VersionUtils.getAppName(), headers[3].getValue());
        Assert.assertEquals("__splunk_app_version", headers[4].getName());
        Assert.assertEquals(VersionUtils.getVersionString(), headers[4].getValue());

        indexer.setKeepAlive(false);
        Assert.assertFalse(indexer.getKeepAlive());
        headers = indexer.getHeaders();
        Assert.assertEquals(5, headers.length);
        Assert.assertEquals("Authorization", headers[0].getName());
        Assert.assertEquals("Splunk " + token, headers[0].getValue());
        Assert.assertEquals("X-Splunk-Request-Channel", headers[1].getName());
        Assert.assertEquals(indexer.getChannel().getId(), headers[1].getValue());
        Assert.assertEquals("Connection", headers[2].getName());
        Assert.assertEquals("close", headers[2].getValue());
        Assert.assertEquals("__splunk_app_name", headers[3].getName());
        Assert.assertEquals(VersionUtils.getAppName(), headers[3].getValue());
        Assert.assertEquals("__splunk_app_version", headers[4].getName());
        Assert.assertEquals(VersionUtils.getVersionString(), headers[4].getValue());

        // try again
        indexer.setKeepAlive(false);
        headers = indexer.getHeaders();
        Assert.assertEquals(5, headers.length);
        Assert.assertEquals("Authorization", headers[0].getName());
        Assert.assertEquals("Splunk " + token, headers[0].getValue());
        Assert.assertEquals("X-Splunk-Request-Channel", headers[1].getName());
        Assert.assertEquals(indexer.getChannel().getId(), headers[1].getValue());
        Assert.assertEquals("Connection", headers[2].getName());
        Assert.assertEquals("close", headers[2].getValue());
        Assert.assertEquals("__splunk_app_name", headers[3].getName());
        Assert.assertEquals(VersionUtils.getAppName(), headers[3].getValue());
        Assert.assertEquals("__splunk_app_version", headers[4].getName());
        Assert.assertEquals(VersionUtils.getVersionString(), headers[4].getValue());

    }

    @Test
    public void getterSetter() {
        Indexer indexer = new Indexer(baseUrl, token, null, null);

        Assert.assertEquals(baseUrl, indexer.getBaseUrl());
        Assert.assertEquals(token, indexer.getToken());

        HecChannel ch = indexer.getChannel();
        Assert.assertNotNull(ch);
    }

    @Test
    public void toStr() {
        Indexer indexer = new Indexer(baseUrl, token, null, null);
        Assert.assertEquals(baseUrl, indexer.toString());
    }

    @Test
    public void sendWithSuccess() {
        for (int i = 0; i < 2; i++) {
            CloseableHttpClientMock client = new CloseableHttpClientMock();
            if (i == 0) {
                client.setResponse(CloseableHttpClientMock.success);
            }
            PollerMock poller = new PollerMock();

            Indexer indexer = new Indexer(baseUrl, token, client, poller);
            EventBatch batch = UnitUtil.createBatch();
            boolean result = indexer.send(batch);
            Assert.assertTrue(result);
            Assert.assertNotNull(poller.getBatch());
            Assert.assertNull(poller.getFailedBatch());
            Assert.assertNull(poller.getException());
            Assert.assertEquals(indexer.getChannel(), poller.getChannel());
            Assert.assertEquals(CloseableHttpClientMock.success, poller.getResponse());
        }
    }

    @Test
    public void sendWithInvalidData() {
        CloseableHttpClientMock client = new CloseableHttpClientMock();
        client.setResponse(CloseableHttpClientMock.invalidDataFormat);
        PollerMock poller = new PollerMock();

        Indexer indexer = new Indexer(baseUrl, token, client, poller);
        EventBatch batch = UnitUtil.createBatch();
        boolean result = indexer.send(batch);
        Assert.assertTrue(result);
        Assert.assertNotNull(poller.getBatch());
        Assert.assertNull(poller.getFailedBatch());
        Assert.assertNull(poller.getException());
        Assert.assertEquals(indexer.getChannel(), poller.getChannel());
        Assert.assertEquals(CloseableHttpClientMock.success, poller.getResponse());
    }


    @Test
    public void sendWithServerBusy() {
        CloseableHttpClientMock client = new CloseableHttpClientMock();
        client.setResponse(CloseableHttpClientMock.serverBusy);

        Indexer indexer = assertFailure(client);
        Assert.assertTrue(indexer.hasBackPressure());
        indexer.setBackPressureThreshold(2000);
        UnitUtil.milliSleep(2500);
        Assert.assertFalse(indexer.hasBackPressure());
        // Assert again
        Assert.assertFalse(indexer.hasBackPressure());
    }

    @Test
    public void ConfirmShortBackPressureConfig() {
        CloseableHttpClientMock client = new CloseableHttpClientMock();
        client.setResponse(CloseableHttpClientMock.serverBusy);

        Indexer indexer = assertFailure(client);
        Assert.assertTrue(indexer.hasBackPressure());
        indexer.setBackPressureThreshold(10000);
        UnitUtil.milliSleep(5000);
        Assert.assertTrue(indexer.hasBackPressure());
        UnitUtil.milliSleep(6000);
        Assert.assertFalse(indexer.hasBackPressure());
    }

    @Test
    public void sendWithIOError() {
        CloseableHttpClientMock client = new CloseableHttpClientMock();
        client.setResponse(CloseableHttpClientMock.exception);
        assertFailure(client);
    }

    @Test
    public void sendWithCloseError() {
        CloseableHttpClientMock client = new CloseableHttpClientMock();
        client.setResponse(CloseableHttpClientMock.success);
        client.setThrowOnClose(true);
        assertFailure(client);
    }

    @Test
    public void sendWithReadError() {
        CloseableHttpClientMock client = new CloseableHttpClientMock();
        client.setResponse(CloseableHttpClientMock.success);
        client.setThrowOnGetContent(true);
        assertFailure(client);
    }

    private Indexer assertFailure(CloseableHttpClient client) {
        PollerMock poller = new PollerMock();

        Indexer indexer = new Indexer(baseUrl, token, client, poller);
        EventBatch batch = UnitUtil.createBatch();
        boolean result = indexer.send(batch);
        Assert.assertFalse(result);
        Assert.assertNull(poller.getBatch());
        Assert.assertNotNull(poller.getFailedBatch());
        Assert.assertNotNull(poller.getException());
        Assert.assertEquals(indexer.getChannel(), poller.getChannel());
        return indexer;
    }
}
