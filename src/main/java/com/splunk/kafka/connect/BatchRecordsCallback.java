package com.splunk.kafka.connect;

import com.sun.scenario.effect.Offset;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.internals.Topic;

/**
 * Created by kchen on 9/24/17.
 */
public class BatchRecordsCallback { //implements ConnectionCallbacks {
/*    private TopicPartition partition;
    private SplunkSinkTask task;

    public BatchRecordsCallback(TopicPartition partition, SplunkSinkTask task) {
        this.partition = partition;
        this.task = task;
    }

    @Override
    public void acknowledged(EventBatch events) {
    }

    @Override
    public void failed(EventBatch events, Exception ex) {
    }

    @Override
    public void checkpoint(EventBatch events) {
        long offset = (long) events.getId(); // highest sequence number in the event batch
        task.commitOffset(partition, offset);
    }

    @Override
    public void systemError(Exception e) {

    }

    @Override
    public void systemWarning(Exception e) {

    }*/
}
