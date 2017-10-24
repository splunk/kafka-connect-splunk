package com.splunk.kafka.connect;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created by kchen on 10/24/17.
 */
public class KafkaRecordTracker {
    private static Logger log = LoggerFactory.getLogger(KafkaRecordTracker.class);

    private ConcurrentHashMap<TopicPartition, ConcurrentSkipListMap<Long, SinkRecord>> committed;
    private ConcurrentHashMap<TopicPartition, ConcurrentLinkedQueue<SinkRecord>> failed;

    public KafkaRecordTracker() {
        committed = new ConcurrentHashMap<>();
        failed = new ConcurrentHashMap<>();
    }

    public void addFailedRecord(final SinkRecord record) {
        TopicPartition tp = new TopicPartition(record.topic(), record.kafkaPartition());
        ConcurrentLinkedQueue<SinkRecord> tpFailed = failed.get(tp);
        if (tpFailed == null) {
            failed.putIfAbsent(tp, new ConcurrentLinkedQueue<>());
            tpFailed = failed.get(tp);
        }
        tpFailed.add(record);
    }

    public void addCommittedRecord(final SinkRecord record) {
        TopicPartition tp = new TopicPartition(record.topic(), record.kafkaPartition());
        ConcurrentSkipListMap<Long, SinkRecord> tpCommitted = committed.get(tp);
        if (tpCommitted == null) {
            committed.putIfAbsent(tp, new ConcurrentSkipListMap<Long, SinkRecord>());
            tpCommitted = committed.get(tp);
        }
        SinkRecord rec = tpCommitted.putIfAbsent(record.kafkaOffset(), record);
        if (rec != null) {
            log.warn("record with offset={} from topicPartition={} has already committed", record.kafkaOffset(), tp);
        }
    }

    public Map<TopicPartition, Collection<SinkRecord>> getAndRemoveFailedRecords() {
        if (failed.isEmpty()) {
            return null;
        }

        Map<TopicPartition, Collection<SinkRecord>> failedRecords = new HashMap<>();
        for (Map.Entry<TopicPartition, ConcurrentLinkedQueue<SinkRecord>> entry: failed.entrySet()) {
            ConcurrentLinkedQueue<SinkRecord> recordQueue = entry.getValue();
            if (recordQueue.isEmpty()) {
                continue;
            }

            Collection<SinkRecord> records = new ArrayList<>();
            while (!recordQueue.isEmpty()) {
                final SinkRecord record = recordQueue.poll();
                if (record == null) {
                    continue;
                }
                records.add(record);
            }
            failedRecords.put(entry.getKey(), records);
        }

        return failedRecords;
    }
}
