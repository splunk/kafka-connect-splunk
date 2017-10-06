package com.splunk.kafka.connect;

import com.splunk.cloudfwd.*;
import com.splunk.cloudfwd.error.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kchen on 9/21/17.
 */
public class SplunkSinkTask extends SinkTask {
    private final Logger LOG = LoggerFactory.getLogger(SplunkSinkTask.class.getName());

    private SplunkSinkConnectorConfig connectorConfig;
    private Connection splunk;
    private String taskId;
    private int batchSize = 10000; //FIXME: Make configurable

    @Override
    public void start(Map<String, String> taskConfig) {
        connectorConfig = new SplunkSinkConnectorConfig(taskConfig);
        splunk = Connections.create(new BatchRecordsCallback(), connectorConfig.cloudfwdConnectionSettings());
        taskId = taskConfig.get("assigned_task_id");
        LOG.info("kafka-connect-splunk task={} starts with config={}", taskId, connectorConfig);
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        if (records.size() == 0) {
            LOG.info("Nothing to Post");
            return;
        }
        int batchSizeCount=0,totalPosted = 0;

        EventBatch batch = Events.createBatch();
        for (SinkRecord record: records ) {
            Event event = createCloudfwdEventFrom(record);
            batch.add(event);
            batchSizeCount++;

            if(batchSizeCount == batchSize ) { //FIXME: Flush on timer as well,  to ensure events dont go stale
                LOG.info("Posting {} events, {} events remain", batchSizeCount, (records.size() - totalPosted) );
               try {
                   int bytesSent = splunk.sendBatch(batch);
           /*    }catch(HecConnectionTimeoutException ex1) {
                   LOG.error("Caught HecConnectionTimeoutException Doing ...");
               }catch(HecNoValidChannelsException ex2) {
                   LOG.error("Caught HecNoValidChannelsException Doing ...");
               }
               */
               }
               catch(Exception ex) {

               }
                //FIXME: Flush EventBatch
                totalPosted += batchSize;
            }
        }
    }

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> meta) {
    }

    @Override
    public void stop() {
        this.LOG.info("kafka-connect-splunk task={} ends with config={}", this.taskId, this.connectorConfig);
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    static Event createCloudfwdEventFrom(SinkRecord record) {
        // FIXME, raw event and json event mode is configurable
        EventWithMetadata event = new EventWithMetadata(record.value(), record.kafkaOffset());

        // FIXME, other metadata overrides
        event.setHost("localhost");

        return event;
    }
}