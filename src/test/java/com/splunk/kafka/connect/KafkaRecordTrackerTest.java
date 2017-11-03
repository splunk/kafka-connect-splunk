package com.splunk.kafka.connect;

import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.UnitUtil;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by kchen on 11/2/17.
 */
public class KafkaRecordTrackerTest {
    @Test
    public void addFailedEventBatch() {
        EventBatch batch = UnitUtil.createBatch();
        batch.fail();

        KafkaRecordTracker tracker = new KafkaRecordTracker();
        tracker.addFailedEventBatch(batch);
        Collection<EventBatch> failed = tracker.getAndRemoveFailedRecords();
        Assert.assertEquals(1, failed.size());
    }

    @Test(expected = RuntimeException.class)
    public void addNonFailedEventBatch() {
        EventBatch batch = UnitUtil.createBatch();
        KafkaRecordTracker tracker = new KafkaRecordTracker();
        tracker.addFailedEventBatch(batch);
    }

    @Test
    public void addEventBatch() {
        List<EventBatch> batches = new ArrayList<>();
        KafkaRecordTracker tracker = new KafkaRecordTracker();
        for (int i = 0; i < 3; i++) {
            EventBatch batch = UnitUtil.createBatch();
            batch.getEvents().get(0).setTied(createSinkRecord(i));
            batches.add(batch);
            tracker.addEventBatch(batch);
        }
        Map<TopicPartition, OffsetAndMetadata> offsets = tracker.computeOffsets();
        Assert.assertTrue(offsets.isEmpty());

        batches.get(0).commit();
        offsets = tracker.computeOffsets();
        Assert.assertEquals(1, offsets.size());

        batches.get(2).commit();
        offsets = tracker.computeOffsets();
        Assert.assertEquals(0, offsets.size());

        batches.get(1).commit();
        offsets = tracker.computeOffsets();
        Assert.assertEquals(1, offsets.size());

        offsets = tracker.computeOffsets();
        Assert.assertEquals(0, offsets.size());

    }

    @Test
    public void addEventBatchWithNonSinkRecord() {
        KafkaRecordTracker tracker = new KafkaRecordTracker();
        for (int i = 0; i < 3; i++) {
            EventBatch batch = UnitUtil.createBatch();
            batch.getEvents().get(0).setTied("");
            batch.commit();
            tracker.addEventBatch(batch);
        }
        Map<TopicPartition, OffsetAndMetadata> offsets = tracker.computeOffsets();
        Assert.assertEquals(0, offsets.size());
    }

    private SinkRecord createSinkRecord(long offset) {
        return new SinkRecord("t", 1, null, null, null, "ni, hao", offset);
    }
}
