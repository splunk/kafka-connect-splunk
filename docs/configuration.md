# Configure Splunk Connect for Kafka

Kafka Connect workers started with the same distributed-worker configuration form a cluster. Submit connector REST requests to any worker in that cluster; Kafka Connect stores and distributes the connector configuration and assigns its tasks across the workers.

See also:

- [Security configurations](security.md)
- [Load balancing configurations](load-balancing.md)
- [Index routing configurations](index-routing.md)
- [Configuration examples](configuration-examples.md)
- [Complete parameter reference](../README.md#parameters)

## Create a data collection connector

1. Start a distributed Kafka Connect worker:

   ```sh
   $KAFKA_HOME/bin/connect-distributed.sh config/connect-distributed.properties
   ```

2. Create the connector through the Kafka Connect REST API. Replace each placeholder with a value for your deployment:

   ```sh
   curl http://localhost:8083/connectors \
     -X POST \
     -H 'Content-Type: application/json' \
     -d '{
       "name": "kafka-connect-splunk",
       "config": {
         "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
         "tasks.max": "3",
         "topics": "<KAFKA_TOPIC>",
         "splunk.indexes": "<SPLUNK_DESTINATION_INDEX>",
         "splunk.hec.uri": "https://<SPLUNK_HEC_HOST>:8088",
         "splunk.hec.token": "<SPLUNK_HEC_TOKEN>",
         "splunk.hec.ssl.trust.store.path": "<ABSOLUTE_PATH_TO_TRUSTSTORE>",
         "splunk.hec.ssl.trust.store.type": "JKS",
         "splunk.hec.ssl.trust.store.password": "<TRUSTSTORE_PASSWORD>",
         "splunk.hec.raw": "true",
         "splunk.hec.ack.enabled": "false"
       }
     }'
   ```

   The essential deployment-specific values are:

   | Setting | Purpose |
   | --- | --- |
   | `topics` or `topics.regex` | Selects the Kafka records consumed by the connector. |
   | `splunk.indexes` | Selects the destination index when the HEC token does not supply the routing. |
   | `splunk.hec.uri` | Identifies one or more HEC endpoints. |
   | `splunk.hec.token` | Authenticates requests to HEC. |
   | `splunk.hec.ssl.trust.store.path` | Selects the truststore used to validate HTTPS HEC endpoints. An explicit path is required with the default certificate-validation setting. The type defaults to JKS but can be changed with `splunk.hec.ssl.trust.store.type`; supply `splunk.hec.ssl.trust.store.password` only when the store requires one. |
   | `splunk.hec.ack.enabled` | Must agree with the indexer-acknowledgment setting on the HEC token. |

3. To include connector context and source lines in Log4j 1 console output, set the following layout in `config/connect-log4j.properties`:

   ```properties
   log4j.appender.stdout.layout.ConversionPattern=[%d] %p %X{connector.context}%m (%c:%L)%n
   ```

   See [Enable verbose logging](troubleshooting.md#enable-verbose-logging) when DEBUG-level connector logs are required.

4. Search the configured Splunk index and verify that events are arriving.

## Configuration schema reference

The following template collects the connector settings covered by the Splunk Connect for Kafka 2.2 configuration guide. Most fields are optional; remove settings that do not apply instead of submitting placeholder values.

```json
{
  "name": "<CONNECTOR_NAME>",
  "config": {
    "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
    "tasks.max": "<NUMBER_OF_TASKS>",
    "topics": "<COMMA_SEPARATED_TOPICS>",
    "splunk.indexes": "<COMMA_SEPARATED_INDEXES>",
    "splunk.sources": "<COMMA_SEPARATED_SOURCES>",
    "splunk.sourcetypes": "<COMMA_SEPARATED_SOURCETYPES>",
    "splunk.hec.uri": "<COMMA_SEPARATED_HEC_URIS>",
    "splunk.hec.token": "<HEC_TOKEN>",
    "splunk.hec.raw": "<true|false>",
    "splunk.hec.raw.line.breaker": "<LINE_BREAKER>",
    "splunk.hec.json.event.enrichment": "<KEY_VALUE_PAIRS>",
    "splunk.hec.json.event.formatted": "<true|false>",
    "splunk.hec.auto.extract.timestamp": "<true|false>",
    "splunk.hec.use.record.timestamp": "<true|false>",
    "splunk.hec.ack.enabled": "<true|false>",
    "splunk.hec.ack.poll.interval": "<SECONDS>",
    "splunk.hec.ack.poll.threads": "<THREAD_COUNT>",
    "splunk.hec.ack.legacy.sticky.session.expiry.enabled": "<true|false>",
    "splunk.hec.ssl.enforced": "<true|false>",
    "splunk.hec.ssl.validate.certs": "<true|false>",
    "splunk.hec.ssl.trust.store.path": "<TRUSTSTORE_PATH>",
    "splunk.hec.ssl.trust.store.type": "<JKS_OR_PKCS12>",
    "splunk.hec.ssl.trust.store.password": "<TRUSTSTORE_PASSWORD>",
    "splunk.hec.http.keepalive": "<true|false>",
    "splunk.hec.max.http.connection.per.channel": "<CONNECTION_COUNT>",
    "splunk.hec.total.channels": "<CHANNEL_COUNT>",
    "splunk.hec.max.batch.size": "<RECORD_COUNT>",
    "splunk.hec.threads": "<THREAD_COUNT>",
    "splunk.hec.concurrent.queue.capacity": "<RECORD_COUNT>",
    "splunk.hec.event.timeout": "<SECONDS>",
    "splunk.hec.socket.timeout": "<SECONDS>",
    "splunk.hec.max.outstanding.events": "<RECORD_COUNT>",
    "splunk.hec.max.retries": "<RETRY_COUNT_OR_MINUS_ONE>",
    "splunk.hec.backoff.threshhold.seconds": "<SECONDS>",
    "splunk.hec.lb.poll.interval": "<SECONDS_OR_MINUS_ONE>",
    "splunk.hec.enable.compression": "<true|false>",
    "splunk.hec.track.data": "<true|false>",
    "splunk.flush.window": "<SECONDS>",
    "splunk.validation.disable": "<true|false>",
    "splunk.header.support": "<true|false>",
    "splunk.header.custom": "<COMMA_SEPARATED_HEADER_NAMES>",
    "splunk.header.index": "<INDEX_HEADER_NAME>",
    "splunk.header.source": "<SOURCE_HEADER_NAME>",
    "splunk.header.sourcetype": "<SOURCETYPE_HEADER_NAME>",
    "splunk.header.host": "<HOST_HEADER_NAME>",
    "value.converter": "<VALUE_CONVERTER_CLASS>",
    "value.converter.schema.registry.url": "<SCHEMA_REGISTRY_URL>",
    "value.converter.schemas.enable": "<true|false>",
    "key.converter": "<KEY_CONVERTER_CLASS>",
    "key.converter.schema.registry.url": "<SCHEMA_REGISTRY_URL>",
    "key.converter.schemas.enable": "<true|false>",
    "kerberos.user.principal": "<KERBEROS_PRINCIPAL>",
    "kerberos.keytab.path": "<KEYTAB_PATH>",
    "enable.timestamp.extraction": "<true|false>",
    "timestamp.regex": "<REGULAR_EXPRESSION_WITH_NAMED_TIME_GROUP>",
    "timestamp.regex.timeout.ms": "<MILLISECONDS>",
    "timestamp.format": "<TIME_FORMAT>",
    "timestamp.timezone": "<TIME_ZONE>"
  }
}
```

Most settings are optional. Remove fields that do not apply instead of submitting placeholder values, and use either `topics` or `topics.regex`, never both. See [Configuration examples](configuration-examples.md) for complete requests covering common deployment patterns.

### Start at the latest topic offsets

Before starting Kafka Connect, add this worker property and then restart the worker:

```properties
consumer.auto.offset.reset=latest
```

This setting affects partitions for which the connector's consumer group has no committed offset. It does not discard existing committed offsets.

## Scale the connector tier

First confirm that Kafka Connect, rather than Kafka brokers, the network, HEC, or Splunk indexing capacity, is the bottleneck. Then consider these options:

- Increase `tasks.max` when the workers have spare CPU and memory but connector throughput is too low.
- Add CPU or memory when the workers are resource constrained.
- Add Kafka Connect workers to the distributed cluster.

`tasks.max` should not exceed the number of source partitions because each partition can be assigned to only one task. A starting estimate is two tasks per worker CPU, capped by the partition count. For five 8-CPU workers and 200 partitions, that estimate is 80 tasks; with 60 partitions, cap it at 60.

Capacity-test with your own event sizes, transforms, acknowledgment mode, network, and Splunk deployment. The source guide reports approximately 50-60 MB/s for an 8-CPU, 16-GB worker when the downstream Splunk deployment is sized appropriately, but this is an example rather than a guarantee.

## Monitor data loss and latency

Tracking is available only with the HEC `/event` endpoint:

```json
{
  "splunk.hec.raw": "false",
  "splunk.hec.track.data": "true"
}
```

The connector adds the Kafka topic, partition, offset, and record timestamp to each event. Sequential offsets make gaps visible, while the record timestamp can be compared with Splunk index time to estimate end-to-end latency.

The following source queries can be used as starting points. Adjust the index and sourcetype and account for the starting offset of each partition in your interpretation.

Check partition counts after de-duplicating by offset:

```spl
index=main sourcetype="<sourcetype>"
| dedup kafka_offset kafka_partition
| stats count as observed, min(kafka_offset) as min_offset, max(kafka_offset) as max_offset by kafka_partition
| eval expected=max_offset-min_offset+1
| eval missing=expected-observed
```

Check distinct offsets without a separate de-duplication command:

```spl
index=main sourcetype="<sourcetype>"
| stats dc(kafka_offset) as observed, min(kafka_offset) as min_offset, max(kafka_offset) as max_offset by kafka_partition
| eval expected=max_offset-min_offset+1
| eval missing=expected-observed
```

These searches report gaps only within the offset range present in the selected time window. They cannot detect records missing before `min_offset` or after `max_offset`, and Kafka retention or compaction can make a gap intentional.

## Find malformed records

The connector treats a record as malformed when JSON data cannot be marshaled or byte data cannot be decoded as UTF-8. It logs the exception with topic, partition, and offset information and sends malformed-record metadata to Splunk. Find those records with:

```spl
type=malformed
```

## Source

Adapted from [Configure Splunk Connect for Kafka](https://help.splunk.com/en/splunk-cloud-platform/get-data-in/splunk-connect-for-kafka/2.2/configure/configure-splunk-connect-for-kafka) in the Splunk Connect for Kafka 2.2 manual.
