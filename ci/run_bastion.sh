#!/bin/bash

sleep 70

if [ -f /etc/hosts2 ]; then
    cat /etc/hosts2 >> /etc/hosts
fi

HEC_URIS="https://idx1:8088"
for i in `seq 2 ${INDEX_CLUSTER_SIZE}`
do
    HEC_URIS="${HEC_URIS},https://idx${i}:8088"
done

# Create kafka connect task
while :
do
    curl http://kafkaconnect1:8083/connectors -X POST -H "content-type:application/json" -d '{
      "name": "splunk-sink",
      "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "splunk.hec.raw": "'"${KAFKA_CONNECT_RAW}"'",
        "splunk.hec.track.data": "true",
        "splunk.hec.http.connection.per.channel": "2",
        "topics": "'"${KAFKA_CONNECT_TOPICS}"'",
        "tasks.max": "'"${KAFKA_CONNECT_TASKS_MAX}"'",
        "splunk.indexes": "main",
        "splunk.sources": "perf",
        "splunk.sourcetypes": "perf",
        "splunk.hec.uri": "'"${HEC_URIS}"'",
        "splunk.hec.token": "00000000-0000-0000-0000-000000000000",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.ack.enabled": "false",
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
