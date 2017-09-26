package com.splunk.kafka.connect;

import com.splunk.cloudfwd.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kchen on 9/21/17.
 */
public class SplunkSinkTask extends SinkTask {
    private final Logger LOG = LoggerFactory.getLogger(SplunkSinkTask.class.getName());

    private SplunkSinkConnectorConfig connectorConfig;
    private Connection splunk;
    private String taskId;

    @Override
    public void start(Map<String, String> taskConfig) {
        this.connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        this.splunk = Connections.create(new BatchRecordsCallback(), this.connectorConfig.cloudfwdConnectionSettings());
        this.taskId = taskConfig.get("assigned_task_id");
        this.LOG.info("kafka-connect-splunk task={} starts with config={}", this.taskId, this.connectorConfig.String());
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        if (records.size() == 0) {
            return;
        }

        EventBatch batch = Events.createBatch();
        for (SinkRecord record: records) {
            Event event = this.createCloudfwdEventFrom(record);

            // FIXME, batch size support
            batch.add(event);
        }

        splunk.sendBatch(batch);
    }

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> meta) {
    }

    @Override
    public void stop() {
        this.LOG.info("kafka-connect-splunk task={} ends with config={}", this.taskId, this.connectorConfig.String());
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    static Event createCloudfwdEventFrom(SinkRecord record) {
        // FIXME, raw event and json event mode is configurable
        EventWithMetadata event = new EventWithMetadata(record.value(), record.kafkaOffset());

        // FIXME, other metadata overrides
        event.setHost("localhost");

        return event;
    }
}