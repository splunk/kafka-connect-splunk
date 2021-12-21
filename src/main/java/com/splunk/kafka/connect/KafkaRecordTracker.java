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
    private static final Logger log = LoggerFactory.getLogger(KafkaRecordTracker.class);
    private ConcurrentMap<TopicPartition, ConcurrentNavigableMap<Long, EventBatch>> all; // TopicPartition + Long offset represents the SinkRecord
    private AtomicLong total;
    private ConcurrentLinkedQueue<EventBatch> failed;
    private volatile Map<TopicPartition, OffsetAndMetadata> offsets;
    private Collection<TopicPartition> partitions;

    public KafkaRecordTracker() {
        all = new ConcurrentHashMap<>();
        failed = new ConcurrentLinkedQueue<>();
        total = new AtomicLong();
        offsets = new HashMap<>();
        partitions = new ArrayList<TopicPartition>();
    }

    /**
     * Remove acked events and update the corresponding offsets finding the
     * lowest consecutive HEC-commited offsets.
     *
     * @param batches the acked event batches
     */
    public void removeAckedEventBatches(final List<EventBatch> batches) {
        log.debug("received acked event batches={}", batches);
        /* Loop all *assigned* partitions to find the lowest consecutive
         * HEC-commited offsets. A batch could contain events coming from a
         * variety of topic/partitions, and scanning those events coulb be
         * expensive.
         * Note that if some events are tied to an unassigned partition those
         * offsets won't be able to be commited.
         */
        for (TopicPartition tp : partitions) {
            ConcurrentNavigableMap<Long, EventBatch> tpRecords = all.get(tp);
            if (tpRecords == null) {
                continue;  // nothing to remove in this case
            }
            long offset = -1;
            Iterator<Map.Entry<Long, EventBatch>> iter = tpRecords.entrySet().iterator();
            for (; iter.hasNext();) {
                Map.Entry<Long, EventBatch> e = iter.next();
                if (e.getValue().isCommitted()) {
                    log.debug("processing offset {}", e.getKey());
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
            /* Don't return null batches. */
            if (batch != null) {
                /* Purge events from closed partitions because it won't be
                 * possible to commit their offsets. */
                batch.getEvents().removeIf(e -> !partitions.contains(getPartitionFromEvent(e)));
                records.add(batch);
            }
        }
        return records;
    }

    /**
     * Return offsets computed when event batches are acked.
     *
     * @return      map of topic/partition to offset/metadata
     */
    public Map<TopicPartition, OffsetAndMetadata> computeOffsets() {
        return offsets;
    }

    public long totalEvents() {
        return total.get();
    }

    public void open(Collection<TopicPartition> partitions) {
        this.partitions.addAll(partitions);
        log.debug("open partitions={} so currently assigned partitions={}",
                  partitions, this.partitions);
    }

    public void close(Collection<TopicPartition> partitions) {
        this.partitions.removeAll(partitions);
        log.debug("close partitions={} so currently assigned partitions={}",
                  partitions, this.partitions);
        cleanupAfterClosedPartitions(partitions);
    }

    private TopicPartition getPartitionFromEvent(Event event) {
        if (event.getTied() instanceof SinkRecord) {
            final SinkRecord r = (SinkRecord) event.getTied();
            return new TopicPartition(r.topic(), r.kafkaPartition());
        } else return null;
    }

    /**
     * Clean up and purge all things related to a partition that's closed (i.e.
     * became unassigned) to this task and reported via SinkTask.close(). This
     * avoids race conditions related to late received acks after a partition
     * rebalance.
     *
     * @param partitions    partition closed and now unassigned for this task
     */
    public void cleanupAfterClosedPartitions(Collection<TopicPartition> partitions)
    {
        /* Purge offsets. */
        offsets.keySet().removeAll(partitions);
        log.warn("purge offsets for closed partitions={} leaving offsets={}",
                 partitions, offsets);

        /* Count and purge outstanding event topic/partition records. */
        long countOfEventsToRemove = partitions.stream()
            .map(tp -> all.get(tp))  // get unassigned topic/partition records
            .filter(Objects::nonNull)  // filter out null values
            .map(tpr -> tpr.size())  // get number of tp records
            .mapToInt(Integer::intValue)  // map to int
            .sum();
        if (countOfEventsToRemove > 0) {
            log.warn("purge events={} from closed partitions={}",
                     countOfEventsToRemove, partitions);
            all.keySet().removeAll(partitions);
            total.addAndGet(-1L * countOfEventsToRemove);
        }
    }
}
