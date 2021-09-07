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

import com.splunk.hecclient.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.connect.header.Header;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SplunkSinkTask extends SinkTask implements PollerCallback {
    private static final Logger log = LoggerFactory.getLogger(SplunkSinkTask.class);
    private static long flushWindow = 30 * 1000; // 30 seconds
    private static final String HEADERTOKEN = "$$$";

    private HecInf hec;
    private KafkaRecordTracker tracker;
    private SplunkSinkConnectorConfig connectorConfig;
    private List<SinkRecord> bufferedRecords;
    private long lastFlushed = System.currentTimeMillis();
    private long threadId = Thread.currentThread().getId();

    private static final String HOSTNAME;
    static {
        String h = null;
        try {
            h = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }
        HOSTNAME = h;
    }

    @Override
    public void start(Map<String, String> taskConfig) {
        connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        if (hec == null) {
            hec = createHec();
        }
        tracker = new KafkaRecordTracker();
        bufferedRecords = new ArrayList<>();
        if(connectorConfig.flushWindow > 0) {
            flushWindow = connectorConfig.flushWindow * 1000; // Flush window set to user configured value (Multiply by 1000 as all the calculations are done in milliseconds)
        }

        log.info("kafka-connect-splunk task starts with config={}", connectorConfig);
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        long startTime = System.currentTimeMillis();
        log.debug("tid={} received {} records with total {} outstanding events tracked", threadId, records.size(), tracker.totalEvents());

        handleFailedBatches();

        preventTooManyOutstandingEvents();

        bufferedRecords.addAll(records);
        if (bufferedRecords.size() < connectorConfig.maxBatchSize) {
            if (System.currentTimeMillis() - lastFlushed < flushWindow) {
                logDuration(startTime);
                // still in flush window, buffer the records and return
                return;
            }

            if (bufferedRecords.isEmpty()) {
                lastFlushed = System.currentTimeMillis();
                logDuration(startTime);
                return;
            }
        }

        // either flush window reached or max batch size reached
        records = bufferedRecords;
        bufferedRecords = new ArrayList<>();
        lastFlushed = System.currentTimeMillis();
        if (connectorConfig.raw) {
            /* /raw endpoint */
            handleRaw(records);
        } else {
            /* /event endpoint */
            handleEvent(records);
        }
        logDuration(startTime);
    }

    private void logDuration(long startTime) {
        long endTime = System.currentTimeMillis();
        log.debug("tid={} cost={} ms", threadId, endTime - startTime);
    }

    // for testing hook
    SplunkSinkTask setHec(final HecInf hec) {
        this.hec = hec;
        return this;
    }

    KafkaRecordTracker getTracker() {
        return tracker;
    }

    private void handleFailedBatches() {
        Collection<EventBatch> failed = tracker.getAndRemoveFailedRecords();
        if (failed.isEmpty()) {
            return;
        }

        log.debug("handling {} failed batches", failed.size());
        long failedEvents = 0;
        // if there are failed ones, first deal with them
        for (final EventBatch batch: failed) {
            failedEvents += batch.size();
            if (connectorConfig.maxRetries > 0 && batch.getFailureCount() > connectorConfig.maxRetries) {
                log.error("dropping EventBatch {} with {} events after reaching maximum retries {}",
                           batch.getUUID(), batch.size(), connectorConfig.maxRetries);
                continue;
            }
            log.warn("attempting to resend batch {} with {} events, this is attempt {} out of {} for this batch ",
                      batch.getUUID(), batch.size(), batch.getFailureCount(), connectorConfig.maxRetries);
            send(batch);
        }

        log.info("handled {} failed batches with {} events", failed.size(), failedEvents);
        if (failedEvents * 10 > connectorConfig.maxOutstandingEvents) {
            String msg = String.format("failed events have reached 10 %% of max outstanding events %d, pausing the pull of events for a while", connectorConfig.maxOutstandingEvents);
            throw new RetriableException(new HecException(msg));
        }
    }

    private void preventTooManyOutstandingEvents() {
        if (tracker.totalEvents() >= connectorConfig.maxOutstandingEvents) {
            String msg = String.format("max outstanding events %d have reached, pause the pull for a while", connectorConfig.maxOutstandingEvents);
            throw new RetriableException(new HecException(msg));
        }
    }

    private void handleRaw(final Collection<SinkRecord> records) {
        if(connectorConfig.headerSupport) {
            if(records != null) { handleRecordsWithHeader(records); }
        } else if (connectorConfig.hasMetaDataConfigured()) {
            // when setup metadata - index, source, sourcetype, we need partition records for /raw
            Map<TopicPartition, Collection<SinkRecord>> partitionedRecords = partitionRecords(records);
            for (Map.Entry<TopicPartition, Collection<SinkRecord>> entry: partitionedRecords.entrySet()) {
                EventBatch batch = createRawEventBatch(entry.getKey());
                sendEvents(entry.getValue(), batch);
            }
        } else {
            EventBatch batch = createRawEventBatch(null);
            sendEvents(records, batch);
        }
    }

    private void handleRecordsWithHeader(final Collection<SinkRecord> records) {
        HashMap<String, ArrayList<SinkRecord>> recordsWithSameHeaders = new HashMap<>();

        for (SinkRecord record : records) {
            String key = headerId(record);
            if (!recordsWithSameHeaders.containsKey(key)) {
                ArrayList<SinkRecord> recordList = new ArrayList<SinkRecord>();
                recordsWithSameHeaders.put(key, recordList);
            }
            ArrayList<SinkRecord> recordList = recordsWithSameHeaders.get(key);
            recordList.add(record);
        }

        Iterator<Map.Entry<String, ArrayList<SinkRecord>>> itr = recordsWithSameHeaders.entrySet().iterator();
        while(itr.hasNext()) {
            Map.Entry<String, ArrayList<SinkRecord>> set = itr.next();
            String splunkSinkRecordKey = set.getKey();
            ArrayList<SinkRecord> recordArrayList = set.getValue();

            EventBatch batch = createRawHeaderEventBatch(splunkSinkRecordKey);
            sendEvents(recordArrayList, batch);
        }
        log.debug("{} records have been bucketed in to {} batches", records.size(), recordsWithSameHeaders.size());
    }

    public String headerId(SinkRecord sinkRecord) {
        Headers headers = sinkRecord.headers();

        Header indexHeader = headers.lastWithName(connectorConfig.headerIndex);
        Header hostHeader = headers.lastWithName(connectorConfig.headerHost);
        Header sourceHeader = headers.lastWithName(connectorConfig.headerSource);
        Header sourcetypeHeader = headers.lastWithName(connectorConfig.headerSourcetype);

        Map<String, String> metas = connectorConfig.topicMetas.get(sinkRecord.topic());


        StringBuilder headerString = new StringBuilder();

        if(indexHeader != null) {
            headerString.append(indexHeader.value().toString());
        } else {
            if(metas!=null)
            headerString.append(metas.get("index"));
        }

        headerString.append(insertHeaderToken());

        if(hostHeader != null) {
            headerString.append(hostHeader.value().toString());
        } else {
            if(metas!=null)
            headerString.append("default-host");
        }

        headerString.append(insertHeaderToken());

        if(sourceHeader != null) {
            headerString.append(sourceHeader.value().toString());
        } else {
            if(metas!=null)
            headerString.append(metas.get("source"));
        }

        headerString.append(insertHeaderToken());

        if(sourcetypeHeader != null) {
            headerString.append(sourcetypeHeader.value().toString());
        } else {
            if(metas!=null)
            headerString.append(metas.get("sourcetype"));
        }

        headerString.append(insertHeaderToken());

        return headerString.toString();
    }

    public String insertHeaderToken() {
        return HEADERTOKEN;
    }

    private void handleEvent(final Collection<SinkRecord> records) {
        EventBatch batch = new JsonEventBatch();
        sendEvents(records, batch);
    }

    private void sendEvents(final Collection<SinkRecord> records, EventBatch batch) {
        for (final SinkRecord record: records) {
            Event event;
            try {
                event = createHecEventFrom(record);
            } catch (HecEmptyEventException | HecNullEventException ex) {
                log.warn("Ignoring Null/Empty event for topicPartitionOffset=({}, {}, {})",
                        record.topic(), record.kafkaPartition(), record.kafkaOffset(), ex);
                continue;
            } catch (HecException ex) {
                log.error("ignore malformed event for topicPartitionOffset=({}, {}, {})",
                        record.topic(), record.kafkaPartition(), record.kafkaOffset(), ex);
                event = createHecEventFromMalformed(record);
            }

            batch.add(event);
            if (batch.size() >= connectorConfig.maxBatchSize) {
                send(batch);
                // start a new batch after send
                batch = batch.createFromThis();
            }
        }

        // Last batch
        if (!batch.isEmpty()) {
            send(batch);
        }
    }

    private void send(final EventBatch batch) {
        batch.resetSendTimestamp();
        tracker.addEventBatch(batch);
        try {
            hec.send(batch);
        } catch (Exception ex) {
            batch.fail();
            onEventFailure(Arrays.asList(batch), ex);
            log.error("failed to send batch {}" ,batch.getUUID(), ex);
        }
    }

    private EventBatch createRawHeaderEventBatch(String splunkSinkRecord) {
        String[] split = splunkSinkRecord.split("[$]{3}", -1);

        return RawEventBatch.factory()
                .setIndex(split[0])
                .setHost(split[1])
                .setSource(split[2])
                .setSourcetype(split[3])
                .build();
    }

    // setup metadata on RawEventBatch
    private EventBatch createRawEventBatch(final TopicPartition tp) {
        if (tp == null) {
            return RawEventBatch.factory().build();
        }

        Map<String, String> metas = connectorConfig.topicMetas.get(tp.topic());
        if (metas == null || metas.isEmpty()) {
            return RawEventBatch.factory().build();
        }

        return RawEventBatch.factory()
                .setIndex(metas.get(SplunkSinkConnectorConfig.INDEX))
                .setSourcetype(metas.get(SplunkSinkConnectorConfig.SOURCETYPE))
                .setSource(metas.get(SplunkSinkConnectorConfig.SOURCE))
                .build();
    }

    @Override
    public void open(Collection<TopicPartition> partitions) {
        tracker.open(partitions);
    }

    @Override
    public void close(Collection<TopicPartition> partitions) {
        /* Purge buffered events tied to closed partitions because this task
         * won't be able to commit their offsets. */
        bufferedRecords.removeIf(r -> partitions.contains(
            new TopicPartition(r.topic(), r.kafkaPartition())));
        /* Tell tracker about now closed partitions so to clean up. */
        tracker.close(partitions);
    }

    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(Map<TopicPartition, OffsetAndMetadata> meta) {
        // tell Kafka Connect framework what are offsets we can safely commit to Kafka now
        Map<TopicPartition, OffsetAndMetadata> offsets = tracker.computeOffsets();
        log.debug("commits offsets offered={}, pushed={}", offsets, meta);
        return offsets;
    }

    @Override
    public void stop() {
        if (hec != null) {
            hec.close();
        }
        log.info("kafka-connect-splunk task ends with config={}", connectorConfig);
    }

    @Override
    public String version() {
        return VersionUtils.getVersionString();
    }

    public void onEventCommitted(final List<EventBatch> batches) {
        tracker.removeAckedEventBatches(batches);
    }

    public void onEventFailure(final List<EventBatch> batches, Exception ex) {
        log.info("add {} failed batches", batches.size());
        for (EventBatch batch: batches) {
            tracker.addFailedEventBatch(batch);
        }
    }

    private Event createHecEventFrom(final SinkRecord record) {
        if (connectorConfig.raw) {
            RawEvent event = new RawEvent(record.value(), record);
            event.setLineBreaker(connectorConfig.lineBreaker);
            if(connectorConfig.headerSupport) {
                event = (RawEvent)addHeaders(event, record);
            }
            return event;
        }

        JsonEvent event;
        ObjectMapper objectMapper = new ObjectMapper();

        if(connectorConfig.hecEventFormatted) {
            try {
                event = objectMapper.readValue(record.value().toString(), JsonEvent.class);
                event.setTied(record);
                event.addFields(connectorConfig.enrichments);
            } catch(Exception e) {
                log.error("event does not follow correct HEC pre-formatted format: {}", record.value().toString());
                event = createHECEventNonFormatted(record);
            }
        } else {
            event = createHECEventNonFormatted(record);
        }

        if(connectorConfig.headerSupport) {
            addHeaders(event, record);
        }

        if (connectorConfig.trackData) {
            Map<String, String> trackMetas = new HashMap<>();
            trackMetas.put("kafka_offset", String.valueOf(record.kafkaOffset()));
            trackMetas.put("kafka_timestamp", String.valueOf(record.timestamp()));
            trackMetas.put("kafka_topic", record.topic());
            trackMetas.put("kafka_partition", String.valueOf(record.kafkaPartition()));
            if (HOSTNAME != null)
                trackMetas.put("kafka_connect_host", HOSTNAME);
            event.addFields(trackMetas);
        }
        event.validate();

        return event;
    }

    private Event addHeaders(Event event, SinkRecord record) {
        Headers headers = record.headers();
        if(headers.isEmpty() &&  connectorConfig.headerCustom.isEmpty()) {
            return event;
        }

        Header headerIndex = headers.lastWithName(connectorConfig.headerIndex);
        Header headerHost = headers.lastWithName(connectorConfig.headerHost);
        Header headerSource = headers.lastWithName(connectorConfig.headerSource);
        Header headerSourcetype = headers.lastWithName(connectorConfig.headerSourcetype);

        if (headerIndex != null) {
            event.setIndex(headerIndex.value().toString());
        }
        if (headerHost != null) {
            event.setHost(headerHost.value().toString());
        }
        if (headerSource != null) {
            event.setSource(headerSource.value().toString());
        }
        if (headerSourcetype != null) {
            event.setSourcetype(headerSourcetype.value().toString());
        }

        // Custom headers are configured with a comma separated list passed in configuration
        // "custom_header_1,custom_header_2,custom_header_3"
        if (!connectorConfig.headerCustom.isEmpty()) {
            String[] customHeaders = connectorConfig.headerCustom.split(",\\s?");
            Map<String, String> headerMap = new HashMap<>();
            for (String header : customHeaders) {
                    Header customHeader = headers.lastWithName(header);
                if (customHeader != null) {
                    headerMap.put(header, customHeader.value().toString());
                }
            }
            event.addFields(headerMap);
        }
        return event;
    }

    private JsonEvent createHECEventNonFormatted(final SinkRecord record) {
        JsonEvent event = new JsonEvent(record.value(), record);
        if (connectorConfig.useRecordTimestamp && record.timestamp() != null) {
            event.setTime(record.timestamp() / 1000.0); // record timestamp is in milliseconds
        }

        Map<String, String> metas = connectorConfig.topicMetas.get(record.topic());
        if (metas != null) {
            event.setIndex(metas.get(SplunkSinkConnectorConfig.INDEX));
            event.setSourcetype(metas.get(SplunkSinkConnectorConfig.SOURCETYPE));
            event.setSource(metas.get(SplunkSinkConnectorConfig.SOURCE));
            event.addFields(connectorConfig.enrichments);
        }
        return event;
    }

    private Event createHecEventFromMalformed(final SinkRecord record) {
        Object data;
        if (connectorConfig.raw) {
            data = "timestamp=" + record.timestamp() + ", topic='" + record.topic() + '\'' +
                    ", partition=" + record.kafkaPartition() +
                    ", offset=" + record.kafkaOffset() + ", type=malformed";
        } else {
            Map<String, Object> v = new HashMap<>();
            v.put("timestamp", record.timestamp());
            v.put("topic", record.topic());
            v.put("partition", record.kafkaPartition());
            v.put("offset", record.kafkaOffset());
            v.put("type", "malformed");
            data = v;
        }

        final SinkRecord r = record.newRecord("malformed", 0, null, null, null, data, record.timestamp());
        return createHecEventFrom(r);
    }

    // partition records according to topic-partition key
    private Map<TopicPartition, Collection<SinkRecord>> partitionRecords(Collection<SinkRecord> records) {
        Map<TopicPartition, Collection<SinkRecord>> partitionedRecords = new HashMap<>();

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

    private HecInf createHec() {
        if (connectorConfig.numberOfThreads > 1) {
            return new ConcurrentHec(connectorConfig.numberOfThreads, connectorConfig.ack,
                    connectorConfig.getHecConfig(), this);
        } else {
            if (connectorConfig.ack) {
                return Hec.newHecWithAck(connectorConfig.getHecConfig(), this);
            } else {
                return Hec.newHecWithoutAck(connectorConfig.getHecConfig(), this);
            }
        }
    }
}
