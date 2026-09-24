# Troubleshoot Splunk Connect for Kafka

Start with the connector and task status, then compare the observed log signature with the sections below:

```sh
curl http://localhost:8083/connectors
curl http://localhost:8083/connectors/<CONNECTOR_NAME>/status
curl http://localhost:8083/connectors/<CONNECTOR_NAME>/config
```

Do not paste the output of the `/config` endpoint into tickets or public logs without removing `splunk.hec.token` and other credentials.

## No events are arriving in Splunk

1. Confirm that the connector and every task report `RUNNING`.
2. Check that `splunk.hec.ack.enabled` matches the indexer-acknowledgment setting on the HEC token.
3. If the HEC token has no default index, provide `splunk.indexes`.
4. Confirm that the token is allowed to write to the selected index.
5. Test network reachability from each Kafka Connect worker to the configured HEC URI and inspect worker logs for HEC responses.
6. Search the destination index and review the time range used by the search.

## Enable verbose logging

For a Log4j 1 worker configuration, add this line to `config/connect-log4j.properties` and restart Kafka Connect:

```properties
log4j.logger.com.splunk=DEBUG
```

Kafka distributions that use Log4j 2 require the equivalent logger entry in their Log4j 2 configuration. DEBUG output can include event metadata and endpoint details, so restore the normal level after collecting diagnostics.

## Connector is missing from a third-party UI

Confluent Control Center and other browser-based tools may require cross-origin access to the Kafka Connect REST API. In `connect-distributed.properties` or `connect-distributed-quickstart.properties`, configure the allowed origin and methods, then restart Kafka Connect:

```properties
access.control.allow.origin=<CONTROL_CENTER_ORIGIN>
access.control.allow.methods=GET,OPTIONS,HEAD,POST,PUT,DELETE
```

You can use `access.control.allow.origin=*`, but prefer the exact trusted UI origin because the Connect REST API exposes administrative operations and connector configuration.

## Malformed data

JSON that cannot be marshaled or bytes that cannot be decoded as UTF-8 are treated as malformed. The worker log includes the Kafka topic, partition, and offset, and malformed-record information is sent to Splunk. Search for it with:

```spl
type=malformed
```

Inspect the producer serialization and the worker's `value.converter` when malformed records appear repeatedly.

## Detect HEC backpressure

HEC backpressure commonly starts with an HTTP `503` response and is followed by channel backpressure messages:

```text
failed to post events resp={"text":"Server busy","code":1}, status=503
Still in Backpressure window 1523:60000
com.splunk.hecclient.HecException: All channels have back pressure
```

The two numbers in `Still in Backpressure window` are the elapsed time and configured backoff window in milliseconds. A successful recovery is reported as:

```text
Clearing Backpressure
```

With HEC acknowledgment enabled, the connector can also pause Kafka consumption when too many events are awaiting acknowledgment:

```text
max outstanding events 1000000 have reached, pause the pull for a while
```

To detect and troubleshoot backpressure:

1. Search the Kafka Connect worker logs for `status=503`, `Backpressure`, `All channels have back pressure`, `max outstanding events`, `attempting to resend`, and `dropping EventBatch`.
2. Check the connector consumer group's lag. The connector may remain `RUNNING` while consumption is paused or batches are retried, so increasing lag is an important secondary signal.
3. Check HEC health, Splunk indexing latency and queues, ingestion limits, and the network or load balancer between the workers and HEC.
4. Review `splunk.hec.backoff.threshhold.seconds`, `splunk.hec.max.outstanding.events`, and `splunk.hec.max.retries`. Increasing these values can delay failure but does not remove the underlying bottleneck.

When you see this message, it means that the backpressure caused a data loss:

```text
dropping EventBatch <BATCH_ID> with <EVENT_COUNT> events after reaching maximum retries <MAX_RETRIES>
```

## Performance declines after several minutes

A sudden throughput drop accompanied by task rebalances and `CommitFailedException` can mean a poll or event batch took long enough for Kafka to remove the consumer from the group. A typical signature is:

```text
Commit of offsets threw an unexpected exception
org.apache.kafka.clients.consumer.CommitFailedException:
Commit cannot be completed since the group has already rebalanced
```

Check the following:

- Increase `splunk.hec.event.timeout` when normal HEC acknowledgment latency can exceed the current value.
- Reduce `max.poll.records` so one poll creates a batch the task can finish before consumer timeouts. Set the corresponding worker consumer property or connector consumer override supported by the deployment.
- Inspect HEC and Splunk indexing latency, worker CPU and memory, and network saturation.
- Confirm that Splunk license limits or ingestion quotas are not slowing indexing.

Change one limit at a time and observe rebalance frequency, acknowledgment latency, and connector lag.

## Duplicate data at the start of collection

On a new connector with no committed offsets, duplicates can appear when `tasks.max` exceeds the number of topic partitions. Do not configure more active tasks than the number of partitions available to the connector.

Also verify that the connector name and consumer group are stable between restarts and that HEC acknowledgment timeouts are not forcing healthy batches to be resent.

## Acknowledgments never complete

The final, partially filled Splunk indexing buffers can take longer to acknowledge than full buffers. Logs can repeat `acks` values of `false` and then show messages such as:

```text
no ackIds are ready for channel=<CHANNEL> on indexer=<HEC_URI>
timed out event batch after 60 seconds not acked
detected event batches timedout
```

Increase `splunk.hec.event.timeout` above the normal worst-case HEC acknowledgment latency. It is not recommended to use values below two minutes for this scenario; the repository default is documented in the [acknowledgment parameter table](../README.md#use-ack).

Confirm that sticky sessions are configured when HEC is behind a load balancer. See [Load balancing configurations](load-balancing.md).

## Serialization errors stop tasks

A converter mismatch commonly produces this signature:

```text
org.apache.kafka.connect.errors.DataException:
Converting byte[] to Kafka Connect data failed due to serialization error
```

Configure the worker's key and value converters for the record format:

```properties
key.converter=<STRING_JSON_AVRO_OR_PROTOBUF_CONVERTER_CLASS>
value.converter=<STRING_JSON_AVRO_OR_PROTOBUF_CONVERTER_CLASS>
```

For `StringConverter` and schemaless `JsonConverter`:

```properties
key.converter.schemas.enable=false
value.converter.schemas.enable=false
```

For Avro or Protobuf, set the appropriate schema registry URL and ensure that the converter JARs and their dependencies are present on every worker:

```properties
key.converter.schema.registry.url=<SCHEMA_REGISTRY_URL>
value.converter.schema.registry.url=<SCHEMA_REGISTRY_URL>
```

Compare the producer serializer, the bytes stored in Kafka, and the Kafka Connect converter. These are separate settings and must describe compatible formats.

## I/O exception

A typical signature starts with:

```text
ERROR encountered io exception (com.splunk.hecclient.Indexer:...)
java.net.SocketException: Socket closed
```

- Intermittent errors can indicate that HEC is overloaded or a proxy/load balancer is closing connections.
- Repeated errors can be reduced by lowering the request rate or increasing batch size so fewer requests are made.
- If every request fails, verify DNS, routing, firewall rules, TLS settings, and reachability from the Kafka Connect worker to HEC.

Check both ends of the connection before increasing retries; unlimited retries can turn a persistent outage into unbounded connector lag.

## HTTP 409 conflicting operation

Kafka Connect can return:

```json
{"error_code":409,"message":"Cannot complete request because of a conflicting operation (e.g. worker rebalance)"}
```

Wait for the rebalance or concurrent connector operation to finish and retry. If independent Kafka Connect clusters accidentally share internal topics or a connector name, correct that topology rather than repeatedly resubmitting the request.

## Out of memory

Inspect the heap allocated to Kafka Connect through `KAFKA_HEAP_OPTS` and compare it with process and container limits. Increase heap only when the host has enough physical memory, for example:

```sh
export KAFKA_HEAP_OPTS='-Xms2G -Xmx16G'
```

Restart Kafka Connect after changing it. Also inspect batch size, task count, outstanding acknowledgments, and heap dumps or GC logs; a larger heap can postpone rather than fix excessive retention.

## Sink tasks require a list of topics

Signature:

```text
org.apache.kafka.connect.errors.ConnectException: Sink tasks require a list of topics.
```

Set either `topics` or `topics.regex` in the connector configuration. Do not place this subscription setting only in the worker properties.

## Invalid HEC token

Signature:

```text
ERROR failed to post events resp={"text":"Invalid token","code":4}, status=403
```

Replace `splunk.hec.token` with an active HEC token for the target Splunk deployment. Verify that whitespace or quoting was not included when the secret was supplied and that the token is enabled.

## Connection timed out

Signature:

```text
org.apache.http.conn.HttpHostConnectException:
Connect to <HEC_HOST>:8088 failed: Connection timed out
```

The worker cannot establish a connection to the HEC host. Verify the URI and port, DNS, routes, proxies, load balancer health, security groups, and firewalls from the worker network.

## Invalid enrichment

Signature:

```text
org.apache.kafka.common.config.ConfigException:
Invalid enrichment: lucky. Expect key value pairs and separated by comma.
```

Set `splunk.hec.json.event.enrichment` to comma-separated `key=value` pairs, for example:

```json
{"splunk.hec.json.event.enrichment":"org=finance,region=eu-central"}
```

This setting applies to the HEC `/event` endpoint.

## Unrecognized SSL message

Signature:

```text
javax.net.ssl.SSLException: Unrecognized SSL message, plaintext connection?
```

The connector usually attempted TLS against a plaintext HTTP endpoint. Confirm whether HEC is configured for HTTPS and make the URI scheme and port match. Prefer enabling HTTPS. If a trusted private deployment intentionally uses HTTP, set `splunk.hec.ssl.enforced=false` and keep the traffic isolated.

## Unable to find a valid certification path

### HTTPS with an untrusted certificate

Signature:

```text
javax.net.ssl.SSLHandshakeException:
PKIX path building failed: unable to find valid certification path to requested target
```

When `splunk.hec.ssl.validate.certs=true`, the worker must trust the CA that issued the HEC certificate. Provide a valid truststore with:

```json
{
  "splunk.hec.ssl.validate.certs": "true",
  "splunk.hec.ssl.trust.store.path": "<ABSOLUTE_PATH_TO_TRUSTSTORE>",
  "splunk.hec.ssl.trust.store.password": "<TRUSTSTORE_PASSWORD>"
}
```

Also verify the certificate chain, expiration, and hostname or subject alternative name.

### Validation enabled without a truststore

Connector validation can fail with a configuration error when the URI is HTTPS, validation is enabled, and no truststore path is available. Install the CA in the JVM's default truststore or configure a connector truststore as shown above.

Disabling `splunk.hec.ssl.validate.certs` removes server identity validation and is not recommended for production. Use it only as a temporary diagnostic on a trusted network.

## HEC reports that acknowledgment is disabled

Signature:

```text
failed to poll ack for channel=<CHANNEL> on indexer=<HEC_URI>
failed to post events resp={"text":"ACK is disabled","code":14}, status=400
```

The connector is polling acknowledgments but the HEC token does not support them. Either enable indexer acknowledgment for the HEC token or set `splunk.hec.ack.enabled=false` in the connector. Keep the two sides consistent.
