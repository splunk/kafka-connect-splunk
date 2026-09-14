# Configuration examples for Splunk Connect for Kafka

These examples use placeholders and valid JSON. Replace hostnames, topics, metadata, and the HEC token before submitting a request.

Enable HEC indexer acknowledgment when delivery assurance is required. The connector and HEC token settings must agree. With acknowledgment disabled, a worker or Splunk failure after HEC accepts a request but before indexing can result in data loss.

Use the HEC `/raw` endpoint when events require Splunk index-time parsing. If a record can contain multiple timestamps, line breaks, or no timestamp, configure a distinct `splunk.hec.raw.line.breaker` and match it in `props.conf`:

```ini
[s1]
LINE_BREAKER = (####)
SHOULD_LINEMERGE = false
```

The HEC `/event` endpoint is appropriate when the event envelope supplies metadata or timestamp handling.

## Index with acknowledgment

### HEC `/raw`

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-prod-financial",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "10",
      "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
      "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ack.enabled": "true",
      "splunk.hec.ack.poll.interval": "20",
      "splunk.hec.ack.poll.threads": "2",
      "splunk.hec.event.timeout": "300",
      "splunk.hec.raw": "true",
      "splunk.hec.raw.line.breaker": "####"
    }
  }'
```

### HEC `/event`

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-prod-financial",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "10",
      "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
      "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ack.enabled": "true",
      "splunk.hec.ack.poll.interval": "20",
      "splunk.hec.ack.poll.threads": "2",
      "splunk.hec.event.timeout": "300",
      "splunk.hec.raw": "false",
      "splunk.hec.json.event.enrichment": "org=fin,bu=south-east-us",
      "splunk.hec.track.data": "true"
    }
  }'
```

## Index without acknowledgment

### HEC `/raw`

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-prod-financial-no-ack-raw",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "10",
      "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
      "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ack.enabled": "false",
      "splunk.hec.raw": "true",
      "splunk.hec.raw.line.breaker": "####"
    }
  }'
```

### HEC `/event`

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-prod-financial-no-ack-event",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "10",
      "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
      "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ack.enabled": "false",
      "splunk.hec.raw": "false",
      "splunk.hec.json.event.enrichment": "org=fin,bu=south-east-us",
      "splunk.hec.track.data": "true"
    }
  }'
```

## Kafka header support

When header support is enabled, the values of the configured settings are Kafka header names. The connector reads those headers from each record and uses their values as Splunk metadata.

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-header-routing",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "10",
      "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
      "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ack.enabled": "true",
      "splunk.hec.event.timeout": "120",
      "splunk.hec.raw": "false",
      "splunk.header.support": "true",
      "splunk.header.index": "destination_storage",
      "splunk.header.source": "event_source",
      "splunk.header.sourcetype": "event_sourcetype",
      "splunk.header.host": "event_host"
    }
  }'
```

## Custom Java truststore

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-custom-truststore",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "20",
      "topics": "t1",
      "splunk.hec.uri": "https://idx1.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ssl.validate.certs": "true",
      "splunk.hec.ssl.trust.store.path": "/opt/kafka-connect/certs/hec-truststore.jks",
      "splunk.hec.ssl.trust.store.password": "<TRUSTSTORE_PASSWORD>"
    }
  }'
```

The file must exist at the same path and be readable on every worker eligible to run the task.

## Events already formatted for HEC

Use `splunk.hec.json.event.formatted=true` when each Kafka record already contains a complete HEC `/event` payload.

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-preformatted-hec-events",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "20",
      "topics": "t1",
      "splunk.hec.uri": "https://idx1.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.raw": "false",
      "splunk.hec.json.event.formatted": "true"
    }
  }'
```

## Collected metrics

The Splunk 2.2 guide provides this pattern for sending collected data to a metrics index. Ensure the HEC token and sourcetype are configured for metrics in the target Splunk deployment.

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-collected-metrics",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "10",
      "topics": "collected",
      "splunk.sourcetypes": "collected_http",
      "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ack.enabled": "true",
      "splunk.hec.ack.poll.interval": "20",
      "splunk.hec.ack.poll.threads": "2",
      "splunk.hec.event.timeout": "120",
      "splunk.hec.raw": "true",
      "splunk.hec.raw.line.breaker": "####"
    }
  }'
```

## Parallel tasks

Create one connector with ten topics and up to ten parallel tasks:

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-prod-financial",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "10",
      "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
      "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ack.enabled": "true",
      "splunk.hec.raw": "false"
    }
  }'
```

Update the existing connector to allow up to twenty tasks by sending its complete configuration to the `/config` endpoint:

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors/splunk-prod-financial/config \
  -X PUT \
  -H 'Content-Type: application/json' \
  -d '{
    "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
    "tasks.max": "20",
    "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
    "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
    "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
    "splunk.hec.ack.enabled": "true",
    "splunk.hec.raw": "false"
  }'
```

The effective number of running tasks is limited by the number of assigned Kafka partitions.

## Load balancing

### Connector-managed list of HEC endpoints

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-hec-endpoint-list",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "1",
      "topics": "t1",
      "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ack.enabled": "true",
      "splunk.hec.raw": "true",
      "splunk.hec.raw.line.breaker": "####"
    }
  }'
```

### Preconfigured load balancer

```sh
curl http://<KAFKA_CONNECT_HOST>:8083/connectors \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "splunk-hec-load-balancer",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "1",
      "topics": "t1",
      "splunk.hec.uri": "https://hec-lb.example.com:8088",
      "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
      "splunk.hec.ack.enabled": "true",
      "splunk.hec.total.channels": "6",
      "splunk.hec.raw": "true",
      "splunk.hec.raw.line.breaker": "####"
    }
  }'
```

See [Load balancing configurations](load-balancing.md) for sticky-session and channel guidance.