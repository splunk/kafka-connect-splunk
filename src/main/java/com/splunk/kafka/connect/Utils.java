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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {
    private static final String VERSION_PROPERTIES_FILE = "/version.properties";
    private static final String DEFAULT_VERSION = "dev";
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    /**
     * Returns the version string that is set in the version.properties
     * resource file
     *
     * @return version string
    */
    public static String getVersionString() {
        List<String> properties = readVersionProperties();

        return getVersionFromProperties(properties);
    }

    /**
     * Returns the version string gets from the list of properties.
     * If version string does not exist, returns the default version.
     *
     * @param properties list of git properties
     * @return           the version string
    */
    public static String getVersionFromProperties(List<String> properties) {
        String versionStr = DEFAULT_VERSION;

        if (properties != null && properties.size() > 0) {
            for (String item : properties) {
                String[] res = item.split("gitversion=");
                if (res.length > 1) {
                    versionStr = res[1].trim();
                    log.debug("found git version string={} in version.properties file", versionStr);
                    break;
                }
            }
        }

        return versionStr;
    }

    /**
     * Returns a list of properties by reading version properties file
     *
     * @return list of properties
    */
    public static List<String> readVersionProperties() {
        return readResourceFile(VERSION_PROPERTIES_FILE);
    }

    /**
     * Returns a list of properties by reading given resource file
     * Each line in the file is an item of the list
     * 
     * @param resourceFileName name of the resource file
     * @return                 list of properties
    */
    public static List<String> readResourceFile(String resourceFileName) {
        List<String> properties = new ArrayList<>();

        try {
            InputStream in = Utils.class.getResourceAsStream(resourceFileName);

            // if the resource file can't be found, return an empty list
            if (in == null) {
                return properties;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;

            while ((line = reader.readLine()) != null)
            {
                properties.add(line);
            }

            // close the BufferedReader when we're done
            reader.close();
        } catch (IOException ex) {
            log.error("Failed to read properties file {}.", VERSION_PROPERTIES_FILE, ex);
            return properties;
        }

        return properties;
    }

}