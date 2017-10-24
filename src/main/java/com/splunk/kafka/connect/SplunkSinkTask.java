package com.splunk.kafka.connect;

import com.splunk.hecclient.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
// import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kchen on 9/21/17.
 */
public class SplunkSinkTask extends SinkTask {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkTask.class);

    private Hec hec;
    private SplunkSinkConnectorConfig connectorConfig;
    private ConcurrentHashMap<TopicPartition, OffsetAndMetadata> committedOffsets;
    private KafkaRecordTracker tracker;

    @Override
    public void start(Map<String, String> taskConfig) {
        connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        hec = buildHec(connectorConfig.getHecClientConfig());
        committedOffsets = new ConcurrentHashMap();
        tracker = new KafkaRecordTracker();

        log.info("kafka-connect-splunk task starts with config={}", connectorConfig);
    }

    private Hec buildHec(final HecClientConfig config) {
        if (connectorConfig.ack) {
            return new HecWithAck(config, new HecPollerCallback(this));
        } else {
            return new HecWithoutAck(config, new HecPollerCallback(this));
        }
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        // FIXEME back pressure detection

        Map<TopicPartition, Collection<SinkRecord>> failed = tracker.getAndRemoveFailedRecords();
        if (failed != null && !failed.isEmpty()) {
            // if there are failed ones, first deal with them
            handlePartitionedRecords(failed);
            // throw new RetriableException(new HecClientException("need handle failed records first"));
        }

        if (records.isEmpty()) {
            return;
        }

        if (connectorConfig.raw) {
            /* /raw endpoint */
            handleRaw(records);
        } else {
            /* /event endpoint */
            handleEvent(records);
        }
    }

    private void handleRaw(final Collection<SinkRecord> records) {
        handlePartitionedRecords(partitionRecords(records));
    }

    private void handlePartitionedRecords(Map<TopicPartition, Collection<SinkRecord>> partitionedRecords) {
        for (Map.Entry<TopicPartition, Collection<SinkRecord>> entry: partitionedRecords.entrySet()) {
            EventBatch batch;
            if (connectorConfig.raw) {
                batch = createRawEventBatch(entry.getKey());
            } else {
                batch = new JsonEventBatch();
            }
            sendEvents(entry.getValue(), batch);
        }
    }

    private void handleEvent(final Collection<SinkRecord> records) {
        EventBatch batch = new JsonEventBatch();
        sendEvents(records, batch);
    }

    private void sendEvents(final Collection<SinkRecord> records, EventBatch batch) {
        for (SinkRecord record: records) {
            Event event;
            try {
                event = createHecEventFrom(record);
            } catch (HecClientException ex) {
                log.info("ignore null or empty event for topicPartition={}-{}", record.topic(), record.kafkaPartition());
                continue;
            }

            // FIXME, batch size support
            batch.add(event);
        }

        send(batch);
    }

    private void send(final EventBatch batch) {
        try {
            hec.send(batch);
        } catch (Exception ex) {
            log.error("sending batch to splunk encountered error", ex);

        }
        log.info("Sent {} events to Splunk", batch.size());
    }

    // setup metadata on RawEventBatch
    private EventBatch createRawEventBatch(final TopicPartition tp) {
        Map<String, String> metas = connectorConfig.topicMetas.get(tp.topic());
        return RawEventBatch.factory()
                .setIndex(metas.get(connectorConfig.INDEX))
                .setSourcetype(metas.get(connectorConfig.SOURCETYPE))
                .setSource(metas.get(connectorConfig.SOURCE))
                .build();
    }

    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(Map<TopicPartition, OffsetAndMetadata> meta) {
        // tell Kafka Connect framework what kind of offsets we can safely commit to Kafka now
        // Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap(committedOffsets);
        // log.info("commits offsets {}", offsets);
        // return offsets;
        // FIXME for computing the offsets
        return meta;
    }

    @Override
    public void stop() {
        hec.close();
        log.info("kafka-connect-splunk task ends with config={}", connectorConfig);
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    public void commitOffset(TopicPartition key, long offset) {
        committedOffsets.put(key, new OffsetAndMetadata(offset + 1));
    }

    public void onEventCommitted(final List<EventBatch> batches) {
        trackRecords(batches, false);
    }

    public void onEventFailure(final List<EventBatch> batches, Exception ex) {
        trackRecords(batches, true);
    }

    private void trackRecords(final List<EventBatch> batches, boolean failed) {
        for (final EventBatch batch: batches) {
            for (final Event event: batch.getEvents()) {
                if (event.getTiedObject() instanceof SinkRecord) {
                    final SinkRecord rec = (SinkRecord) event.getTiedObject();
                    if (failed) {
                        tracker.addFailedRecord(rec);
                    } else {
                        tracker.addCommittedRecord(rec);
                    }
                }
            }
        }
    }

    private Event createHecEventFrom(SinkRecord record) {
        if (connectorConfig.raw) {
            return new RawEvent(record.value(), record);
       } else {
            // meta data for /event endpoint is per event basis
            Event event = new JsonEvent(record.value(), record);
            Map<String, String> metas = connectorConfig.topicMetas.get(record.topic());
            event.setIndex(metas.get(connectorConfig.INDEX));
            event.setSourcetype(metas.get(connectorConfig.SOURCETYPE));
            event.setSource(metas.get(connectorConfig.SOURCE));
            return event;
        }
    }

    // partition records according to topic-partition key
    private Map<TopicPartition, Collection<SinkRecord>> partitionRecords(Collection<SinkRecord> records) {
        Map<TopicPartition, Collection<SinkRecord>> partitionedRecords = new HashMap();

        for (SinkRecord record: records) {
            TopicPartition key = new TopicPartition(record.topic(), record.kafkaPartition());
            Collection<SinkRecord> partitioned = partitionedRecords.get(key);
            if (partitioned == null) {
                partitioned = new ArrayList<>();
                partitionedRecords.put(key, partitioned);
            }
            partitioned.add(record);
        }
        return partitionedRecords;
    }
}