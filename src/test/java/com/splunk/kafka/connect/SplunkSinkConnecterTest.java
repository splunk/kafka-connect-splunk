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

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import java.util.*;

class SplunkSinkConnecterTest {

    @Test
    void startStop() {
        SinkConnector connector = new SplunkSinkConnector();
        Map<String, String> taskConfig = new HashMap<>();
        taskConfig.put("a", "b");

        connector.start(taskConfig);
        List<Map<String, String>> tasks = connector.taskConfigs(10);
        Assertions.assertEquals(10, tasks.size());
        for (Map<String, String> task: tasks) {
            Assertions.assertEquals(taskConfig, task);
        }

        connector.stop();
    }

    @Test
    public void taskClass() {
        SinkConnector connector = new SplunkSinkConnector();
        Class<? extends Task> taskClass = connector.taskClass();
        Assertions.assertEquals(SplunkSinkTask.class, taskClass);
    }

    @Test
    public void config() {
        SinkConnector connector = new SplunkSinkConnector();
        ConfigDef config = connector.config();
        Assertions.assertNotNull(config);
    }
}