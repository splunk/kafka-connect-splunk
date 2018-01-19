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

import com.splunk.hecclient.Event;
import com.splunk.hecclient.EventBatch;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

final class KafkaRecordTracker {
    private Map<TopicPartition, TreeMap<Long, EventBatch>> all; // TopicPartition + Long offset represents the SinkRecord
    private long total;
    private ConcurrentLinkedQueue<EventBatch> failed;

    public KafkaRecordTracker() {
        all = new HashMap<>();
        failed = new ConcurrentLinkedQueue<>();
        total = 0;
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

                if (!tpRecords.containsKey(record.kafkaOffset())) {
                    tpRecords.put(record.kafkaOffset(), batch);
                    total += 1;
                }
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
                    total -= 1;
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

    public long totalEvents() {
        return total;
    }
}
