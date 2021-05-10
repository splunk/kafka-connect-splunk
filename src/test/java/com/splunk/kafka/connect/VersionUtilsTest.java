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

import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

public final class VersionUtilsTest {

    @Test
    public void  getVersionFromProperties() {
        String version = VersionUtils.getVersionFromProperties(null);
        Assert.assertEquals(version, "dev");

        version = VersionUtils.getVersionFromProperties(new ArrayList<>());
        Assert.assertEquals(version, "dev");

        List<String> properties = VersionUtils.readResourceFile("/testversion.properties");
        version = VersionUtils.getVersionFromProperties(properties);
        Assert.assertEquals(version, "0.1.3");
    }

    @Test
    public void readResourceFile() {
        // test when the resource file does not exist 
        List<String> res = VersionUtils.readResourceFile("/randomFile");
        Assert.assertEquals(res.size(), 0);


        res = VersionUtils.readResourceFile("/testversion.properties");
        Assert.assertEquals(res.size(), 3);
    }
}