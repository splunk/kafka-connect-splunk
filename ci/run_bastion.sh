#!/bin/bash

sleep 600

INDEX_CLUSTER_SIZE=${INDEX_CLUSTER_SIZE:-1}

HEC_URIS="https://idx1:8088"
for i in `seq 2 ${INDEX_CLUSTER_SIZE}`
do
    HEC_URIS="${HEC_URIS},https://idx${i}:8088"
done

KAFKA_CONNECT_RAW=${KAFKA_CONNECT_RAW:-"false"}
KAFKA_CONNECT_LINE_BREAKER=${KAFKA_CONNECT_LINE_BREAKER:-"@@@@"}
KAFKA_CONNECT_BATCH_SIZE=${KAFKA_CONNECT_BATCH_SIZE:-"500"}

bash /fix_hosts.sh > /tmp/fixhosts 2>&1 &

# Create kafka connect task
while :
do
    curl http://kafkaconnect1:8083/connectors -X POST -H "content-type:application/json" -d '{
      "name": "splunk-sink",
      "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "topics": "'"${KAFKA_CONNECT_TOPICS}"'",
        "tasks.max": "'"${KAFKA_CONNECT_TASKS_MAX}"'",
        "splunk.indexes": "main",
        "splunk.sources": "perf",
        "splunk.sourcetypes": "perf",
        "splunk.hec.uri": "'"${HEC_URIS}"'",
        "splunk.hec.token": "00000000-0000-0000-0000-000000000000",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.raw": "'"${KAFKA_CONNECT_RAW}"'",
        "splunk.hec.raw.line.breaker": "'"${KAFKA_CONNECT_LINE_BREAKER}"'",
        "splunk.hec.max.batch.size": "'"${KAFKA_CONNECT_BATCH_SIZE}"'",
        "splunk.hec.track.data": "true",
        "splunk.hec.http.connection.per.channel": "2",
        "name": "splunk-sink"
      }
    }'

    if [ $? == 0 ]; then
        break
    else
        sleep 2
    fi
done

tail -f /dev/null
