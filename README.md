## Splunk Connect for Kafka

Splunk Connect for Kafka is a Kafka Connect Sink for Splunk with the following features:

* Data ingestion from Kafka topics into Splunk via [Splunk HTTP Event Collector (HEC)](https://help.splunk.com/en/data-management/collect-http-event-data/use-hec-in-splunk-enterprise/set-up-and-use-http-event-collector-in-splunk-web).
* In-flight data transformation and enrichment.
* Indexed Kafka events can be searched directly or used as contextual data alongside other Splunk data.

Splunk Connect for Kafka runs as a Kafka Connect plugin and does not add views to Splunk Web. Release artifacts are available from [GitHub Releases](https://github.com/splunk/kafka-connect-splunk/releases) and [Splunkbase](https://splunkbase.splunk.com/app/3862).

> **New deployments:** Splunk's current product guidance recommends Splunk OpenTelemetry Connector for Kafka (SOC4Kafka). Use this connector when maintaining an existing Splunk Connect for Kafka deployment or when its behavior is specifically required. Review the [official Splunk Connect for Kafka overview](https://help.splunk.com/en/splunk-cloud-platform/get-data-in/splunk-connect-for-kafka/2.2/overview/splunk-connect-for-kafka) before selecting an ingestion path.

## Documentation

* [Install Splunk Connect for Kafka](docs/installation.md)
* [Upgrade Splunk Connect for Kafka](docs/upgrade.md)
* [Configure Splunk Connect for Kafka](docs/configuration.md)
* [Security configurations](docs/security.md)
* [Load balancing configurations](docs/load-balancing.md)
* [Index routing configurations](docs/index-routing.md)
* [Configuration examples](docs/configuration-examples.md)
* [Troubleshooting](docs/troubleshooting.md)

## Requirements

* Kafka Connect running with Kafka 1.0.0 or later.
  * Tested versions: 3.5.1, 3.6.2, 3.7.2, 3.8.1, and 3.9.0
* Java 8 or later.
* Splunk platform 8.0.0 or later with a valid HTTP Event Collector (HEC) token.
  * Tested versions: 9.4.4 and 10.0.0.
  * HEC token settings should be the same on all Splunk Indexers and Heavy Forwarders in your environment.
  * Task configuration parameters will vary depending on acknowledgement setting (See the [Configuration](#configuration) section for details).

Note: HEC Acknowledgement prevents potential data loss but may slow down event ingestion.


## Supported technologies

Splunk Connect for Kafka lets you subscribe to a Kafka topic and stream the data to the Splunk HTTP event collector on the following technologies:

* Apache Kafka
* Amazon Managed Streaming for Apache Kafka (Amazon MSK)
* Confluent Platform

## Build

1. Clone the repo from https://github.com/splunk/kafka-connect-splunk
2. Verify that Java 8 JRE or JDK is installed.
3. Verify that maven is installed.
4. Run `mvn package`. This will build the jar in the /target directory. The name will be `splunk-kafka-connect-[VERSION].jar`.

## Quick Start

1. [Start](https://kafka.apache.org/quickstart) your Kafka Cluster and confirm it is running.
2. If this is a new install, create a test topic (eg: `perf`). Inject events into the topic. This can be done using [Kafka data-gen-app](https://github.com/dtregonning/kafka-data-gen) or the Kafka-bundled [kafka-console-producer](https://kafka.apache.org/quickstart#quickstart_send).
3. Within your Kafka Connect deployment adjust the values for `bootstrap.servers` and `plugin.path` inside the `$KAFKA_HOME/config/connect-distributed.properties` file. `bootstrap.servers` should be configured to point to your Kafka Brokers. `plugin.path` should be configured to point to the install directory of your Kafka Connect Sink and Source Connectors. For more information on installing Kafka Connect plugins please refer to the [Confluent Documentation.](https://docs.confluent.io/current/connect/userguide.html#id3)
4. Place the jar file created by `mvn package` (`splunk-kafka-connect-[VERSION].jar`) in or under the location specified in `plugin.path`
5. Run `.$KAFKA_HOME/bin/connect-distributed.sh $KAFKA_HOME/config/connect-distributed.properties` to start Kafka Connect.
6. Run the following command to create connector tasks. Adjust `topics` to configure the Kafka topic to be ingested, `splunk.indexes` to set the destination Splunk indexes, `splunk.hec.token` to set your HTTP Event Collector (HEC) token. For more information on Splunk HEC configuration refer to [Splunk Documentation.](https://help.splunk.com/en/data-management/collect-http-event-data/use-hec-in-splunk-enterprise/set-up-and-use-http-event-collector-in-splunk-web)

```
  curl localhost:8083/connectors -X POST -H "Content-Type: application/json" -d '{
    "name": "kafka-connect-splunk",
    "config": {
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "tasks.max": "3",
      "splunk.indexes": "<SPLUNK_INDEXES>",
      "topics":"<YOUR_TOPIC>",
      "splunk.hec.uri": "https://<SPLUNK_HEC_HOST>:8088",
      "splunk.hec.token": "<YOUR_TOKEN>"
    }
  }'
```

7. Verify that data is flowing into your Splunk platform instance by searching using the index specified in the configuration.
8. Use the following commands to check status, and manage connectors and tasks:

```
    # List active connectors
    curl http://localhost:8083/connectors

    # Get kafka-connect-splunk connector info
    curl http://localhost:8083/connectors/kafka-connect-splunk

    # Get kafka-connect-splunk connector config info
    curl http://localhost:8083/connectors/kafka-connect-splunk/config

    # Delete kafka-connect-splunk connector
    curl http://localhost:8083/connectors/kafka-connect-splunk -X DELETE

    # Get kafka-connect-splunk connector task info
    curl http://localhost:8083/connectors/kafka-connect-splunk/tasks
```

See the [the Confluent doucumentation](https://docs.confluent.io/current/connect/managing.html#common-rest-examples) for additional REST examples.

## Deployment

See [Install Splunk Connect for Kafka](docs/installation.md) for installation and administration commands, [Upgrade Splunk Connect for Kafka](docs/upgrade.md) for version replacement, and [Configure Splunk Connect for Kafka](docs/configuration.md) for deployment and scaling guidance.

## Security

> **Important:** This connector does not configure authentication or authorization for the Kafka Connect REST API. In deployments where that API is unauthenticated, anyone who can reach it can create, inspect, update, or delete connector configurations. Connector configurations contain sensitive values such as `splunk.hec.token`.

### Restrict access to the Kafka Connect REST API

- Never expose Kafka Connect's REST API port `8083` directly to the public internet.
- Bind the REST listener to a private management interface or network where possible.
- Restrict inbound access to trusted administrative hosts and networks with firewalls, cloud security groups, or equivalent controls.

These controls are part of the Kafka Connect deployment and cannot be enforced by this connector.

If `splunk.hec.ssl.enforced=false`, the connector sends event data and the HEC token without TLS protection. Restrict that traffic to a trusted private network in addition to restricting access to port `8083`.

See [Security configurations](docs/security.md) for TLS, Kerberos, SASL/PLAIN, and SASL/SCRAM guidance.

## Configuration

After Kafka Connect is brought up on every host, all of the Kafka Connect instances will form a cluster automatically.
A REST call can be executed against one of the cluster instances, and the configuration will automatically propagate to all instances in the cluster.

For the complete workflow and worked configurations, see [Configure Splunk Connect for Kafka](docs/configuration.md) and [Configuration examples](docs/configuration-examples.md).

### Configuration schema structure
Use the following valid JSON template to configure Splunk Connect for Kafka.

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

Use either `topics` or `topics.regex`, never both. See [Configuration examples](docs/configuration-examples.md) for complete requests covering common deployment patterns.

### Parameters

#### Required Parameters
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
|`name` | Connector name. A consumer group with this name will be created with tasks to be distributed evenly across the connector cluster nodes.|
| `connector.class` | The Java class used to perform connector jobs. Keep the default unless you modify the connector.|`com.splunk.kafka.connect.SplunkSinkConnector`|
| `tasks.max` |  The number of tasks generated to handle data collection jobs in parallel. The tasks will be spread evenly across all Splunk Kafka Connector nodes.||
| `splunk.hec.uri` | Splunk HEC URIs. Either a list of FQDNs or IPs of all Splunk indexers, separated with a ",", or a load balancer. The connector will load balance to indexers using round robin. Splunk Connector will round robin to this list of indexers. `https://hec1.splunk.com:8088,https://hec2.splunk.com:8088,https://hec3.splunk.com:8088`||
| `splunk.hec.token` | [Splunk HTTP Event Collector token](https://help.splunk.com/en/data-management/collect-http-event-data/use-hec-in-splunk-enterprise/set-up-and-use-http-event-collector-in-splunk-web).||
| `topics` or `topics.regex` |  For **topics**: Comma separated list of Kafka topics for Splunk to consume. `prod-topic1,prod-topc2,prod-topic3` <br/> For **topics.regex**: Use for declaring topic subscriptions as name pattern, instead of specifying each topic in a list. `^prod-topic[0-9]$`<br/> **NOTE:** <br/> 1) If "topics.regex" is specified, the "topics" parameter must be omitted.<br/> 2) With "topics.regex", the Splunk meta fields("splunk.indexes", "splunk.sourcetypes", "splunk.sources") are ignored and should be omitted.<br/> 3) With "topics.regex" the Splunk metadata must either be defined on a per-event basis by using Kafka Header Fields("splunk.header.index", "splunk.header.sourcetype", etc.), OR it can be defined by the HEC token default index and sourcetype values.
 
#### General Optional Parameters
| Name                                         | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Default Value |
|----------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `splunk.indexes`                             | Target Splunk indexes to send data to. It can be a list of indexes which shall be the same sequence / order as topics. It is possible to inject data from different kafka topics to different splunk indexes. For example, prod-topic1,prod-topic2,prod-topic3 can be sent to index prod-index1,prod-index2,prod-index3. In that case, the configuration `topics` count must match the `splunk.indexes` count. If you would like to index all data from multiple topics to the main index, then "main" can be specified. Leaving this setting unconfigured will result in data being routed to the default index configured against the HEC token being used. Verify the indexes configured here are in the index list of HEC tokens, otherwise Splunk HEC will drop the data. | `""`          |
| `splunk.sources`                             | Splunk event source metadata for Kafka topic data. The same configuration rules as indexes can be applied. If left unconfigured, the default source binds to the HEC token.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | `""`          |
| `splunk.sourcetypes`                         | Splunk event sourcetype metadata for Kafka topic data. The same configuration rules as indexes can be applied here. If left unconfigured, the default sourcetype binds to the HEC token.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | `""`          |
| `splunk.flush.window`                        | The interval in seconds at which the events from kafka connect will be flushed to Splunk.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | `30`          |
| `splunk.validation.disable`                  | Disable validating splunk configurations before creating task.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | `false`       |
| `splunk.hec.ssl.enforced`                    | Requires every `splunk.hec.uri` value to use HTTPS. Set to `false` only for private-network HEC deployments without TLS.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | `true`        |
| `splunk.hec.ssl.validate.certs`              | Valid settings are `true` or `false`. Enables or disables HTTPS certification validation.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | `true`        |
| `splunk.hec.http.keepalive`                  | Valid settings are `true` or `false`. Enables or disables HTTP connection keep-alive.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | `true`        |
| `splunk.hec.max.http.connection.per.channel` | Controls how many HTTP connections will be created and cached in the HTTP pool for one HEC channel.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | `2`           |
| `splunk.hec.total.channels`                  | Controls the total channels created to perform HEC event POSTs. See the Load balancer section for more details.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | `2`           |
| `splunk.hec.max.batch.size`                  | Maximum batch size when posting events to Splunk. The size is the actual number of Kafka events, and not byte size.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | `500`         |
| `splunk.hec.threads`                         | Controls how many threads are spawned to do data injection via HEC in a **single** connector task.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | `1`           |
| `splunk.hec.concurrent.queue.capacity`       | Maximum records queued per connector task when `splunk.hec.threads` is greater than `1`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | `100`         |
| `splunk.hec.socket.timeout`                  | Internal TCP socket timeout when connecting to Splunk. Value is in seconds.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | `60`          |
| `splunk.hec.ssl.trust.store.path`            | Absolute path to the truststore used to validate HTTPS HEC endpoints. This connector requires an explicit path when certificate validation is enabled.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | `""`          |
| `splunk.hec.ssl.trust.store.type`            | Truststore format supported by the JVM, such as `JKS` or `PKCS12`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | `JKS`         |
| `splunk.hec.ssl.trust.store.password`        | Password for the configured truststore. Supply it when the truststore is password-protected.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | `""`          |
| `splunk.hec.json.event.formatted`            | Set to `true` for events that are already in HEC format. Valid settings are `true` or `false`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | `false`       |
| `splunk.hec.max.outstanding.events`          | Maximum amount of un-acknowledged events kept in memory by connector. Will trigger back-pressure event to slow down collection if reached.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | `1000000`     |
| `splunk.hec.max.retries`                     | Amount of times a failed batch will attempt to resend before dropping events completely. Warning: This will result in data loss after retries are exhausted. Default is `5`. If set to -1, the retries are never stopped.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | `5`           |
| `splunk.hec.backoff.threshhold.seconds`      | The amount of duration the Indexer object will be stopped after getting error code while posting the data.</br> **NOTE:** <br/>  Other Indexer won't get affected."                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | `60`          |
| `splunk.hec.lb.poll.interval`                | Specify this parameter(in seconds) to control the polling interval(increase to do less polling, decrease to do more frequent polling, set `-1` to disable polling)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | `120`         |
| `splunk.hec.enable.compression`              | Valid settings are true or false. Used for enable or disable gzip-compression.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | `false`       |
### Acknowledgement Parameters
#### Use Ack
| Name              | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Default Value  |
|--------           |----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| `splunk.hec.ack.enabled` | When set to `true` the Splunk Kafka Connector will poll event ACKs for POST events before check-pointing the Kafka offsets. This is used to prevent data loss, as this setting implements guaranteed delivery. In cases where the Splunk platform crashes, there may be some data loss. Valid settings are `true` or `false`. </br> **NOTE:** <br/> 1) If this setting is set to `true`, verify that the corresponding HEC token is also enabled with index acknowledgements, otherwise data injection fails.<br/> 2) When set to `false`, the Splunk Kafka Connector only POSTs events to the Splunk platform. After it receives an HTTP 200 response, it assumes the events are indexed. |`false`|
| `splunk.hec.ack.poll.interval` | This setting is only applicable when `splunk.hec.ack.enabled` is set to `true`. Internally it controls the event ACKs polling interval. Value is in seconds.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |`10`|
| `splunk.hec.ack.poll.threads` | This setting is used for performance tuning and is only applicable when `splunk.hec.ack.enabled` is set to `true`. It controls how many threads should be spawned to poll event ACKs. </br> **NOTE:** <br/> For large Splunk indexer clusters (For example, 100 indexers) you may need to increase this number.                                                                                                                                                                                                                                                                                                                                                                                                           | `2`|
| `splunk.hec.event.timeout` | This setting is applicable when `splunk.hec.ack.enabled` is set to `true`. When events are POSTed to Splunk and before they are ACKed, this setting determines how long the connector will wait before timing out and resending. Value is in seconds. Configure this timeout comfortably above the usual ACK latency observed under expected production load. If it is too short, healthy but slow batches can time out and be resent, resulting in duplicate events.                                                                                                                                                                                                                                                                              |`300`|
| `splunk.hec.ack.legacy.sticky.session.expiry.enabled` | Restores the deprecated behavior that treats every `Set-Cookie` response as a sticky-session expiry, resets the HEC channel, and retries its outstanding batches. Enabling this can disrupt deployments where a load balancer refreshes cookies on normal responses.                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |`false`|
#### Endpoint Parameters
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `splunk.hec.raw` | Set to `true` in order for Splunk software to ingest data using the the /raw HEC endpoint.`false` will use the /event endpoint |`false`|
##### /raw endpoint only
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `splunk.hec.raw.line.breaker` | Only applicable to /raw HEC endpoint. The setting is used to specify a custom line breaker to help Splunk separate the events correctly.</br> **NOTE:** <br/> For example, you can specify `"#####"` as a special line breaker. Internally, the Splunk Kafka Connector will append this line breaker to every Kafka record to form a clear event boundary. The connector performs data injection in batch mode. On the Splunk platform side, you can configure **`props.conf`** to set up line breaker for the sourcetypes. Then the Splunk software will correctly break events for data flowing through /raw HEC endpoint. For questions on how and when to specify line breaker, go to the FAQ section.|`""`|
##### /event endpoint only
| Name              | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Default Value |
|--------           |------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `splunk.hec.json.event.enrichment` | Only applicable to /event HEC endpoint. This setting is used to enrich raw data with extra metadata fields. It contains a list of key value pairs separated by ",". The configured enrichment metadata will be indexed along with raw event data by Splunk software. </br> **NOTE:** <br/> Data enrichment for /event HEC endpoint is only available in Splunk Enterprise 6.5 and above. By default, this setting is empty. See the [HEC documentation](https://help.splunk.com/en/data-management/collect-http-event-data/use-hec-in-splunk-enterprise/set-up-and-use-http-event-collector-in-splunk-web) for more information. <br/>**Example:** `org=fin,bu=south-east-us` | `""`          |
| `splunk.hec.track.data` | When set to `true`, data loss and data injection latency metadata will be indexed along with raw data. This setting only works in conjunction with /event HEC endpoint (`"splunk.hec.raw" : "false"`). Valid settings are `true` or `false`.                                                                                                                                                                                                                                                                                                                                             | `false`       |
| `splunk.hec.auto.extract.timestamp` | When set to `true`, it forces Splunk HEC to extract the timestamp from the event envelope/event data. See [/services/collector/event](https://docs.splunk.com/Documentation/Splunk/9.1.1/RESTREF/RESTinput#services.2Fcollector.2Fevent) for more details.                                                                                                                                                                                                                                                                                                                               | `unset`       |
| `splunk.hec.use.record.timestamp` | When set to `true`, use the Kafka record timestamp in the HEC event envelope unless explicit timestamp extraction overrides it. Only applies to the `/event` endpoint.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | `true`        |

### Headers Parameters
#### Use Headers
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `splunk.header.support` |   When set to `true` Splunk Connect for Kafka will parse Kafka headers for use as meta data in Splunk events. Valid settings are `true` or `false`. | `false` |
| `splunk.header.custom` | Custom headers are configured separated by comma for multiple headers. ex,  "custom_header_1,custom_header_2,custom_header_3". This setting will look for kafka record headers with these values and add them to each event if present. This setting is only applicable when `splunk.header.support` is set to `true`. <br/> **NOTE:** <br/> Only applicable to /event HEC endpoint. | `""` |
| `splunk.header.index` | This setting specifies the Kafka record header key which will determine the destination index for the Splunk event. This setting is only applicable when `splunk.header.support` is set to `true`. | `splunk.header.index` |
| `splunk.header.source` | This setting specifies the Kafka record header key which will determine the source value for the Splunk event. This setting is only applicable when `splunk.header.support` is set to `true`. | `splunk.header.source` |
| `splunk.header.sourcetype` | This setting specifies the Kafka record header key which will determine the sourcetype value for the Splunk event. This setting is only applicable when `splunk.header.support` is set to `true`. | `splunk.header.sourcetype` |
| `splunk.header.host` | This setting specifies the Kafka record header key which will determine the host value for the Splunk event. This setting is only applicable when `splunk.header.support` is set to `true`. | `splunk.header.host` |

### Kerberos Parameters
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `kerberos.user.principal` | Kerberos user principal used for SPNEGO authentication to a protected HEC endpoint. This does not configure Kafka broker authentication. | `""` |
| `kerberos.keytab.path` | Absolute keytab path used with `kerberos.user.principal` for outbound HEC HTTP authentication. | `""` |

### Protobuf Parameters
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `value.converter` |  Converter class used to convert between Kafka Connect format and the serialized form that is written to Kafka. This controls the format of the values in messages written to or read from Kafka. For using protobuf format ,set the value of this field to `io.confluent.connect.protobuf.ProtobufConverter` | `org.apache.kafka.connect.storage.StringConverter` |
| `value.converter.schema.registry.url` |  Schema Registry URL. | `""` |
| `value.converter.schemas.enable` | For using protobuf format ,set the value of this field to `true` | `false` |
| `key.converter` |  Converter class used to convert between Kafka Connect format and the serialized form that is written to Kafka. This controls the format of the keys in messages written to or read from Kafka. For using protobuf format ,set the value of this field to `io.confluent.connect.protobuf.ProtobufConverter` | `org.apache.kafka.connect.storage.StringConverter` |
| `key.converter.schema.registry.url` |  Schema Registry URL. | `""` |
| `key.converter.schemas.enable` | For using protobuf format ,set the value of this field to `true` | `false` |

### Timestamp extraction Parameters
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `enable.timestamp.extraction` |  To enable timestamp extraction ,set the value of this field to `true`. <br/> **NOTE:** <br/> Applicable only if `splunk.hec.raw` is `false` | `false` |
| `timestamp.regex` |  Regex for timestamp extraction. <br/> **NOTE:** <br/> Regex must have name captured group `"time"` For eg.: `\\\"time\\\":\\s*\\\"(?<time>.*?)\"` | `""` |
| `timestamp.regex.timeout.ms` | Maximum time in milliseconds for one timestamp regex evaluation. Must be a positive integer. When the timeout is reached, timestamp extraction is skipped for that event and the connector logs a warning. | `500` |
| `timestamp.format` |  Time-format for timestamp extraction .<br/>For eg.: <br/>If timestamp is `1555209605000` , set `timestamp.format` to `"epoch"` format.<br/> If timestamp is `Jun 13 2010 23:11:52.454 UTC` , set `timestamp.format` to `"MMM dd yyyy HH:mm:ss.SSS zzz".`. <br/> If timestamp is in ISO8601 format `2022-03-29'T'23:11:52.054` , set `timestamp.format` to `"yyyy-MM-dd'\''T'\''HH:mm:ss.SSS"` | `""` |
| `timestamp.timezone` | Timezone used for extracted timestamp. Defaults to local timezone if nothing is specified | `""` |

### Out-of-band Health Checks and In-band Health Checks
| Health Checks                | Description                | 
|--------               |----------------------------|
| `Out of  band health check` |  This health check targets Loadbalancer and aims to remove all the unhealthy channels from the pool; all unhealthy channels are released for the configurable period using the parameter `splunk.hec.lb.poll.interval`, Although this is configurable (by default 120 seconds), It may still get a 503 result code from the Splunk indexer. For that, there is another health check, and it can be called the in-band-health check. | 
| `In band healthcheck` | This health check targets Indexer object while posting data. If an error code is received, then it will trigger this health check. When this check fails, It will Pause the indexing from the Particular Indexer object for a configurable time using the parameter `Splunk.hec.backoff.threshhold.seconds` and trigger backpressure handling So that event that could not be indexed will be retried again.  | 

## Load balancing

See [Load balancing configurations](docs/load-balancing.md) for endpoint-list and external load-balancer guidance.

## Index routing

See [Index routing configurations](docs/index-routing.md) for topic-to-index mapping and Splunk index-time routing.

## Scale out your environment

See [Scale the connector tier](docs/configuration.md#scale-the-connector-tier) for task, worker, and capacity guidance.

## Data loss and latency monitoring

See [Monitor data loss and latency](docs/configuration.md#monitor-data-loss-and-latency) for tracking configuration and SPL examples.

## Troubleshooting

See [Troubleshoot Splunk Connect for Kafka](docs/troubleshooting.md) for symptoms, log signatures, and corrective actions.

## License

Splunk Connect for Kafka  is licensed under the Apache License 2.0. Details can be found in the file LICENSE.
