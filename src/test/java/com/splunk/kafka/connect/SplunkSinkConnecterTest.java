package com.splunk.kafka.connect;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.junit.Assert;
import org.junit.jupiter.api.Test;


import java.util.*;

class SplunkSinkConnecterTest {

    @Test
    void version() {
        SinkConnector connector = new SplunkSinkConnector();
        Assert.assertEquals("1.0.0", connector.version());
    }

    @Test
    void startStop() {
        SinkConnector connector = new SplunkSinkConnector();
        Map<String, String> taskConfig = new HashMap<>();
        taskConfig.put("a", "b");

        connector.start(taskConfig);
        List<Map<String, String>> tasks = connector.taskConfigs(10);
        Assert.assertEquals(10, tasks.size());
        for (Map<String, String> task: tasks) {
            Assert.assertEquals(taskConfig, task);
        }

        connector.stop();
    }

    @Test
    public void taskClass() {
        SinkConnector connector = new SplunkSinkConnector();
        Class<? extends Task> taskClass = connector.taskClass();
        Assert.assertEquals(SplunkSinkTask.class, taskClass);
    }

    @Test
    public void config() {
        SinkConnector connector = new SplunkSinkConnector();
        ConfigDef config = connector.config();
        Assert.assertNotNull(config);
    }
}