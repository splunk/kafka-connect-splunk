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
    private final Logger log = LoggerFactory.getLogger(SplunkSinkTask.class.getName());

    private SplunkSinkConnectorConfig connectorConfig;
    private Connection splunk;

    @Override
    public void start(Map<String, String> taskConfig) {
        connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        splunk = Connections.create(new BatchRecordsCallback(), connectorConfig.cloudfwdConnectionSettings());
        log.info("kafka-connect-splunk starts with config={}", connectorConfig);
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        if (records.isEmpty()) {
            return;
        }

        EventBatch batch = Events.createBatch();
        for (SinkRecord record: records) {
            Event event = createCloudfwdEventFrom(record);

            // FIXME, batch size support
            batch.add(event);
        }

        splunk.sendBatch(batch);
        log.info("Sent {} events to Splunk", records.size());
    }

    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(Map<TopicPartition, OffsetAndMetadata> meta) {
        return meta;
    }

    @Override
    public void stop() {
        log.info("kafka-connect-splunk task={} ends with config={}", connectorConfig);
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