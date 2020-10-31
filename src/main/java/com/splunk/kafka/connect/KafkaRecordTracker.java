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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class KafkaRecordTracker {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkTask.class);
    private ConcurrentMap<TopicPartition, ConcurrentNavigableMap<Long, EventBatch>> all; // TopicPartition + Long offset represents the SinkRecord
    private AtomicLong total;
    private ConcurrentLinkedQueue<EventBatch> failed;
    private volatile Map<TopicPartition, OffsetAndMetadata> offsets;

    public KafkaRecordTracker() {
        all = new ConcurrentHashMap<>();
        failed = new ConcurrentLinkedQueue<>();
        total = new AtomicLong();
        offsets = new HashMap<>();
    }

    public void removeAckedEventBatches(final List<EventBatch> batches) {
        for (final EventBatch batch: batches) {
            //log.debug("Processing batch {}", batch.getUUID());
            removeAckedEventBatch(batch);
        }
    }

    public void removeAckedEventBatch(final EventBatch batch) {
        final List<Event> events = batch.getEvents();
        final Event event = events.get(0);
        if (event.getTied() instanceof SinkRecord) {
            final SinkRecord record = (SinkRecord) event.getTied();
            TopicPartition tp = new TopicPartition(record.topic(), record.kafkaPartition());
            //log.debug("Processing topic {} partition {}", record.topic(), record.kafkaPartition());
            ConcurrentNavigableMap<Long, EventBatch> tpRecords = all.get(tp);
            if (tpRecords == null) {
                log.error("KafkaRecordTracker removing a batch in an unknown partition {} {} {}", record.topic(), record.kafkaPartition(), record.kafkaOffset());
                return;
            }
            long offset = -1;
            Iterator<Map.Entry<Long, EventBatch>> iter = tpRecords.entrySet().iterator();
            for (; iter.hasNext();) {
                Map.Entry<Long, EventBatch> e = iter.next();
                if (e.getValue().isCommitted()) {
                    //log.debug("processing offset {}", e.getKey());
                    offset = e.getKey();
                    iter.remove();
                    total.decrementAndGet();
                } else {
                    break;
                }
            }
            if (offset >= 0) {
                offsets.put(tp, new OffsetAndMetadata(offset + 1));
            }
        }
    }

    public void addFailedEventBatch(final EventBatch batch) {
        if (!batch.isFailed()) {
            throw new RuntimeException("event batch was not failed");
        }
        failed.add(batch);
        log.info("total failed batches {}", failed.size());
    }

    public void addEventBatch(final EventBatch batch) {
        for (final Event event: batch.getEvents()) {
            if (event.getTied() instanceof SinkRecord) {
                final SinkRecord record = (SinkRecord) event.getTied();
                TopicPartition tp = new TopicPartition(record.topic(), record.kafkaPartition());
                ConcurrentNavigableMap<Long, EventBatch> tpRecords = all.computeIfAbsent(tp, k -> new ConcurrentSkipListMap<>());
                tpRecords.computeIfAbsent(record.kafkaOffset(), k -> { total.incrementAndGet(); return batch; });
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
    // find all lowest consecutive committed offsets, calculate
    // the topic/partition offsets and then remove them
    public Map<TopicPartition, OffsetAndMetadata> computeOffsets() {
        return offsets;
    }

    public long totalEvents() {
        return total.get();
    }
}
