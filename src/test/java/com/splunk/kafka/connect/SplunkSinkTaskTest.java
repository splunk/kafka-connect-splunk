package com.splunk.kafka.connect;

import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.RawEventBatch;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class SplunkSinkTaskTest {
    @Test
    public void startStopDefault() {
        SplunkSinkTask task = new SplunkSinkTask();
        UnitUtil uu = new UnitUtil();
        // shall not throw
        task.stop();

        task.start(uu.createTaskConfig());
        task.stop();
    }

    @Test
    public void startStopWithoutAck() {
        SplunkSinkTask task = new SplunkSinkTask();
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.ACK_CONF, String.valueOf(false));

        task.start(config);
        task.stop();
    }

    @Test
    public void version() {
        SplunkSinkTask task = new SplunkSinkTask();
        Assert.assertEquals("1.0.0", task.version());
    }

    @Test
    public void startStopConcurrent() {
        SplunkSinkTask task = new SplunkSinkTask();
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.HEC_THREDS_CONF, "2");

        task.start(config);
        task.stop();
    }

    @Test
    public void putWithEventAndAck() {
        putWithSuccess(false, true);
        putWithSuccess(false, false);
    }

    @Test
    public void putWithoutMaxBatchAligned() {
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.RAW_CONF, String.valueOf(false));
        config.put(SplunkSinkConnectorConfig.ACK_CONF, String.valueOf(true));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(100));

        SplunkSinkTask task = new SplunkSinkTask();
        HecMock hec = new HecMock(task);
        // success
        hec.setSendReturnResult(HecMock.success);
        task.setHec(hec);
        task.start(config);
        task.put(createSinkRecords(120));
        Assert.assertEquals(2, hec.getBatches().size());
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        offsets.put(new TopicPartition(uu.topics, 1), new OffsetAndMetadata(120));
        Assert.assertEquals(offsets, task.preCommit(null));
        Assert.assertTrue(task.getTracker().getAndRemoveFailedRecords().isEmpty());
        task.stop();
    }

    @Test
    public void putWithFailure() {
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.RAW_CONF, String.valueOf(false));
        config.put(SplunkSinkConnectorConfig.ACK_CONF, String.valueOf(true));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(100));

        SplunkSinkTask task = new SplunkSinkTask();
        HecMock hec = new HecMock(task);
        // failure
        hec.setSendReturnResult(HecMock.failure);
        task.setHec(hec);
        task.start(config);
        task.put(createSinkRecords(1000));
        Assert.assertEquals(10, hec.getBatches().size());
        Assert.assertTrue(task.getTracker().computeOffsets().isEmpty());
        Assert.assertEquals(10, task.getTracker().getAndRemoveFailedRecords().size());

        task.stop();
    }

    @Test(expected = RetriableException.class)
    public void putWithFailureAndBackpressure() {
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.RAW_CONF, String.valueOf(false));
        config.put(SplunkSinkConnectorConfig.ACK_CONF, String.valueOf(true));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(100));

        SplunkSinkTask task = new SplunkSinkTask();
        HecMock hec = new HecMock(task);
        // failure
        hec.setSendReturnResult(HecMock.failure);
        task.setHec(hec);
        task.start(config);
        task.put(createSinkRecords(1000));
        task.put(createSinkRecords(1000));

        task.stop();
    }

    @Test(expected = RetriableException.class)
    public void putWithFailureHandleFailedBatches() {
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.RAW_CONF, String.valueOf(false));
        config.put(SplunkSinkConnectorConfig.ACK_CONF, String.valueOf(true));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(100));

        SplunkSinkTask task = new SplunkSinkTask();
        HecMock hec = new HecMock(task);
        // failure
        hec.setSendReturnResult(HecMock.successAndThenFailure);
        task.setHec(hec);
        task.start(config);
        task.put(createSinkRecords(1000));
        task.put(createSinkRecords(1000));

        task.stop();
    }

    @Test
    public void putWithEmptyRecords() {
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.RAW_CONF, String.valueOf(false));
        config.put(SplunkSinkConnectorConfig.ACK_CONF, String.valueOf(true));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(100));

        SplunkSinkTask task = new SplunkSinkTask();
        HecMock hec = new HecMock(task);
        hec.setSendReturnResult(HecMock.success);
        task.setHec(hec);
        task.start(config);
        task.put(createSinkRecords(0));
        Assert.assertTrue(hec.getBatches().isEmpty());
        Assert.assertTrue(task.getTracker().computeOffsets().isEmpty());
        Assert.assertTrue(task.getTracker().getAndRemoveFailedRecords().isEmpty());

        task.stop();
    }

    @Test
    public void putWithInvalidEvent() {
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.RAW_CONF, String.valueOf(false));
        config.put(SplunkSinkConnectorConfig.ACK_CONF, String.valueOf(true));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(6));

        SplunkSinkTask task = new SplunkSinkTask();
        HecMock hec = new HecMock(task);
        hec.setSendReturnResult(HecMock.success);
        task.setHec(hec);
        task.start(config);
        task.put(createSinkRecords(10, ""));
        Assert.assertTrue(hec.getBatches().isEmpty());
        Assert.assertTrue(task.getTracker().computeOffsets().isEmpty());
        Assert.assertTrue(task.getTracker().getAndRemoveFailedRecords().isEmpty());

        task.stop();
    }

    @Test
    public void putWithRawAndAck() {
        putWithSuccess(true, true);
    }

    @Test
    public void putWithRawAndAckWithoutMeta() {
        putWithSuccess(true, false);
    }

    private void putWithSuccess(boolean raw, boolean withMeta) {
        UnitUtil uu = new UnitUtil();
        Map<String, String> config = uu.createTaskConfig();
        config.put(SplunkSinkConnectorConfig.RAW_CONF, String.valueOf(raw));
        config.put(SplunkSinkConnectorConfig.ACK_CONF, String.valueOf(true));
        config.put(SplunkSinkConnectorConfig.MAX_BATCH_SIZE_CONF, String.valueOf(100));
        if (withMeta) {
            config.put(SplunkSinkConnectorConfig.INDEX_CONF, "i1");
            config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "s1");
            config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "e1");
        } else {
            config.put(SplunkSinkConnectorConfig.INDEX_CONF, "");
            config.put(SplunkSinkConnectorConfig.SOURCETYPE_CONF, "");
            config.put(SplunkSinkConnectorConfig.SOURCE_CONF, "");
        }

        SplunkSinkTask task = new SplunkSinkTask();
        HecMock hec = new HecMock(task);
        // success
        hec.setSendReturnResult(HecMock.success);
        task.setHec(hec);
        task.start(config);
        task.put(createSinkRecords(1000));
        Assert.assertEquals(10, hec.getBatches().size());
        if (raw && withMeta) {
            for (EventBatch batch: hec.getBatches()) {
                RawEventBatch rb = (RawEventBatch) batch;
                Assert.assertEquals("i1", rb.getIndex());
                Assert.assertEquals("s1", rb.getSourcetype());
                Assert.assertEquals("e1", rb.getSource());
            }
        }
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        offsets.put(new TopicPartition(uu.topics, 1), new OffsetAndMetadata(1000));
        Assert.assertEquals(offsets, task.preCommit(null));
        Assert.assertTrue(task.getTracker().getAndRemoveFailedRecords().isEmpty());
        task.stop();
    }

    private Collection<SinkRecord> createSinkRecords(int numOfRecords) {
        return createSinkRecords(numOfRecords, 0,"ni, hao");
    }

    private Collection<SinkRecord> createSinkRecords(int numOfRecords, String value) {
        return createSinkRecords(numOfRecords, 0, value);
    }

    private Collection<SinkRecord> createSinkRecords(int numOfRecords, int start, String value) {
        List<SinkRecord> records = new ArrayList<>();
        for (int i = start; i < start + numOfRecords; i++) {
            SinkRecord rec = new SinkRecord(new UnitUtil().topics, 1, null, null, null, value, i);
            records.add(rec);
        }
        return records;
    }
}
