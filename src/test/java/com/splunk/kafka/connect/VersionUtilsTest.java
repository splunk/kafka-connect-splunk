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
package com.splunk.kafka.connect;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

public final class VersionUtilsTest {

    @Test
    public void getVersionMatchesMavenVersion() {
        // given
        String expectedVersion = System.getProperty("splunk.kafka.version");

        // when
        String version = VersionUtils.getVersion();
        String connectorVersion = new SplunkSinkConnector().version();
        String taskVersion = new SplunkSinkTask().version();

        // then
        Assert.assertNotNull("Maven must provide splunk.kafka.version", expectedVersion);
        Assert.assertEquals(expectedVersion, version);
        Assert.assertEquals(expectedVersion, connectorVersion);
        Assert.assertEquals(expectedVersion, taskVersion);
    }

    @Test
    public void readsVersionMockProperties() {
        // given
        String metadata = "version=v2.2.8\n";
        InputStream input = new ByteArrayInputStream(
                metadata.getBytes(StandardCharsets.ISO_8859_1));

        // when
        String version = VersionUtils.readVersion(input);

        // then
        Assert.assertEquals("v2.2.8", version);
    }

    @Test
    public void missingVersionPropertiesReturnsUnknown() {
        // given
        InputStream input = null;

        // when
        String version = VersionUtils.readVersion(input);

        // then
        Assert.assertEquals("unknown", version);
    }
}
