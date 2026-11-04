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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VersionUtils {
    private static final String DEFAULT_VERSION = "unknown";
    private static final String DEFAULT_APP_NAME = "Splunk Connect for Kafka";
    private static final Logger log = LoggerFactory.getLogger(VersionUtils.class);
    private static final String VERSION_RESOURCE = "/META-INF/maven/com.github.splunk.kafka.connect/"
            + "splunk-kafka-connect/pom.properties";

    /**
     * Returns the version from the connector's packaged Maven metadata.
     *
     * @return version string, or "unknown" when metadata is unavailable
     */
    public static String getVersion() {
        return readVersion(VersionUtils.class.getResourceAsStream(VERSION_RESOURCE));
    }

    /**
     * Returns the application name as a string
     *
     * @return app name string
     */
    public static String getAppName() {
        return DEFAULT_APP_NAME;
    }

    /**
     * Reads and closes a Maven metadata stream.
     *
     * @param input Maven metadata stream, or null when unavailable
     * @return version string, or "unknown" when metadata cannot be read
     */
    static String readVersion(InputStream input) {
        try (InputStream in = input) {
            if (in == null) {
                return DEFAULT_VERSION;
            }

            Properties properties = new Properties();
            properties.load(in);

            String version = properties.getProperty("version", "").trim();
            return version.isEmpty() ? DEFAULT_VERSION : version;
        } catch (IOException ex) {
            log.error("Failed to read connector version metadata.", ex);
            return DEFAULT_VERSION;
        }
    }
}
