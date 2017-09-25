package com.splunk.kafka.connect;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kchen on 9/24/17.
 */
public class SplunkSinkConnector extends SinkConnector {
    private final Logger LOG = LoggerFactory.getLogger(SplunkSinkConnector.class.getName());

    private Map<String, String> taskConfig;

    @Override
    public void start(Map<String, String> taskConfig) {
        this.taskConfig = taskConfig;
        this.LOG.info("kafka-connect-splunk starts");
    }

    @Override
    public void stop() {
        this.LOG.info("kafka-connect-splunk stops");
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> tasks = new ArrayList<>();
        for (int i = 0; i < maxTasks; i++) {
            // FIXME,
            // 1. discover how many partitions in the topic, task number should not execeed partition number
            // 2. assign task ID
            tasks.add(this.taskConfig);
        }
        this.LOG.info("kafka-connect-splunk discovered {} tasks", tasks.size());
        return tasks;
    }

    @Override
    public Class<? extends Task> taskClass() {
        return SplunkSinkTask.class;
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public ConfigDef config() {
        return SplunkSinkConnectorConfig.conf();
    }
}
