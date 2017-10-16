package com.splunk.kafka.connect;

import com.splunk.cloudfwd.*;
import com.splunk.cloudfwd.error.HecConnectionStateException;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kchen on 9/21/17.
 */
public class SplunkSinkTask extends SinkTask {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkTask.class);

    private SplunkSinkConnectorConfig connectorConfig;
    private Map<TopicPartition, Connection> splunkConns;
    private ConcurrentHashMap<TopicPartition, OffsetAndMetadata> committedOffsets;

    @Override
    public void start(Map<String, String> taskConfig) {
        connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        splunkConns = new HashMap();
        committedOffsets = new ConcurrentHashMap();
        log.info("kafka-connect-splunk task starts with config={}", connectorConfig);
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        if (records.isEmpty()) {
            return;
        }

        Map<TopicPartition, Collection<SinkRecord>> partitionedRecords = partitionRecords(records);
        for (Map.Entry<TopicPartition, Collection<SinkRecord>> entry: partitionedRecords.entrySet()) {
            Connection splunk = getCloudFwdConnection(entry.getKey());
            EventBatch batch = Events.createBatch();
            for (SinkRecord record: entry.getValue()) {
                if (record.value() == null || record.value().toString().isEmpty()) {
                    continue;
                }

                Event event = createCloudfwdEventFrom(record);
                // FIXME, batch size support
                batch.add(event);
            }

            if (batch.getNumEvents() == 0) {
                continue;
            }

            splunk.sendBatch(batch);
            log.info("Sent {} events for {} to Splunk", batch.getNumEvents(), entry.getKey());
        }
    }

    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(Map<TopicPartition, OffsetAndMetadata> meta) {
        // tell Kafka Connect framework what kind of offsets we can safely commit to Kafka now
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap(committedOffsets);
        log.info("commits offsets {}", offsets);
        return offsets;
    }

    @Override
    public void stop() {
        log.info("kafka-connect-splunk task ends with config={}", connectorConfig);
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    public void commitOffset(TopicPartition key, long offset) {
        committedOffsets.put(key, new OffsetAndMetadata(offset + 1));
    }

    private Event createCloudfwdEventFrom(SinkRecord record) {
        // FIXME, raw event and json event mode is configurable
        EventWithMetadata event = new EventWithMetadata(record.value(), record.kafkaOffset());
        Map<String, String> metas = connectorConfig.topicMetas.get(record.topic());

        if (metas.get(connectorConfig.INDEX) != null) {
            event.setIndex(metas.get(connectorConfig.INDEX));
        }

        if (metas.get(connectorConfig.SOURCETYPE) != null) {
            event.setSourceType(metas.get(connectorConfig.SOURCETYPE));
        }

        if (metas.get(connectorConfig.SOURCE) != null) {
            event.setSource(metas.get(connectorConfig.SOURCE));
        }

        return event;
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

    // Get cloudfwd connection for specific topic-partition, create a new connection object if necessary
    private Connection getCloudFwdConnection(TopicPartition key) {
        Connection conn = splunkConns.get(key);
        if (conn != null) {
            return conn;
        }

        log.info("create new cloudfwd connection for {}", key);
        conn = Connections.create(new BatchRecordsCallback(key, this), connectorConfig.cloudfwdConnectionSettings());
        splunkConns.put(key, conn);
        return conn;
    }

    // sendBatch will send the EventBatch to Splunk. If it enounters any exception, convert the exception to
    // RetriableException, then the framework will do the retry etc
    // Note: do not retry it here ourselves since it will probably break the conumer group membership if we
    // the retry takes too much time
    private static void sendBatch(Connection splunk, EventBatch batch) {
        boolean retry = false;
        Exception cause = null;

        try {
            splunk.sendBatch(batch);
        } catch (HecConnectionStateException ex) {
            if (ex.getType() == HecConnectionStateException.Type.ALREADY_ACKNOWLEDGED ||
                    ex.getType() ==  HecConnectionStateException.Type.ALREADY_SENT) {
                // already done, we are good
                retry = false;
            } else {
                retry = true;
                cause = ex;
            }
        } catch (Exception ex) {
            retry = true;
            cause = ex;
        }

        if (retry) {
            log.error("sending batch to splunk encountered error", cause);
            throw new RetriableException(cause);
        }
    }
}