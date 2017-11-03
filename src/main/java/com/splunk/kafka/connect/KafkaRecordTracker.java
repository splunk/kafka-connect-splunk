package com.splunk.kafka.connect;

import com.splunk.hecclient.Event;
import com.splunk.hecclient.EventBatch;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by kchen on 10/24/17.
 */

final class KafkaRecordTracker {
    private Map<TopicPartition, TreeMap<Long, EventBatch>> all; // TopicPartition + Long offset represents the SinkRecord
    private ConcurrentLinkedQueue<EventBatch> failed;

    public KafkaRecordTracker() {
        all = new HashMap<>();
        failed = new ConcurrentLinkedQueue<>();
    }

    public void addFailedEventBatch(final EventBatch batch) {
        if (!batch.isFailed()) {
            throw new RuntimeException("event batch was not failed");
        }
        failed.add(batch);
    }

    public void addEventBatch(final EventBatch batch) {
        for (final Event event: batch.getEvents()) {
            if (event.getTied() instanceof SinkRecord) {
                final SinkRecord record = (SinkRecord) event.getTied();
                TopicPartition tp = new TopicPartition(record.topic(), record.kafkaPartition());
                TreeMap<Long, EventBatch> tpRecords = all.get(tp);
                if (tpRecords == null) {
                    tpRecords = new TreeMap<>();
                    all.put(tp, tpRecords);
                }
                tpRecords.put(record.kafkaOffset(), batch);
            }
        }
    }

    public Collection<EventBatch> getAndRemoveFailedRecords() {
        Collection<EventBatch> records = new ArrayList<>();
        while (!failed.isEmpty()) {
            final EventBatch batch = failed.poll();
            if (batch != null) {
                records.add(batch);
            }
        }
        return records;
    }

    // Loop through all SinkRecords for all topic partitions to
    // find all lowest consecutive committed offsets, caculate
    // the topic/partition offsets and then remove them
    public Map<TopicPartition, OffsetAndMetadata> computeOffsets() {
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        for (Map.Entry<TopicPartition, TreeMap<Long, EventBatch>> entry: all.entrySet()) {
            long offset = -1;
            Iterator<Map.Entry<Long, EventBatch>> iter = entry.getValue().entrySet().iterator();
            for (; iter.hasNext();) {
                Map.Entry<Long, EventBatch> e = iter.next();
                if (e.getValue().isCommitted()) {
                    offset = e.getKey();
                    iter.remove();
                } else {
                    break;
                }
            }

            if (offset >= 0) {
                offsets.put(entry.getKey(), new OffsetAndMetadata(offset + 1));
            }
        }
        return offsets;
    }
}
