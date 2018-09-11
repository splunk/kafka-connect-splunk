## Splunk Connect for Kafka

Splunk Connect for Kafka is a Kafka Connect Sink for Splunk with the following features:

* Data ingestion from Kafka topics into Splunk via [Splunk HTTP Event Collector(HEC)](http://dev.splunk.com/view/event-collector/SP-CAAAE6M).
* In-flight data transformation and enrichment.

## Requirements
1. Kafka version 1.0.0 and above.
2. Java 8 and above.
3. A Splunk environment of version 6.5 and above, configured with valid HTTP Event Collector (HEC) tokens.

	* HEC token settings should be the same on all Splunk Indexers and Heavy Forwarders in your environment.
	* Task configuration parameters will vary depending on acknowledgement setting (See the [Configuration](#configuration) section for details).

	Note: HEC Acknowledgement prevents potential data loss but may slow down event ingestion.

## Build

1. Clone the repo from https://github.com/splunk/kafka-connect-splunk
2. Verify that Java8 JRE or JDK is installed.
3. Run `mvn package`. This will build the jar in the /target directory. The name will be `splunk-kafka-connect-[VERSION].jar`.

## Quick Start

1. [Start](https://kafka.apache.org/quickstart) your Kafka Cluster and confirm it is running.
2. If this is a new install, create a test topic (eg: `perf`). Inject events into the topic. This can be done using [Kafka data-gen-app](https://github.com/dtregonning/kafka-data-gen) or the Kafka bundle [kafka-console-producer](https://kafka.apache.org/quickstart#quickstart_send).
3. Place the jar file created by the maven build (splunk-kafka-connect-[VERSION].jar) in or under the location specified in `plugin.path` (see step 4).
4. Within Kafka Connect, adjust values for `bootstrap.servers` and `plugin.path` inside `config/connect-distributed.properties` to fit your environment. 
5. Run `./bin/connect-distributed.sh config/connect-distributed.properties` to start Kafka Connect.
6. Run the following command to create connector tasks. Adjust `topics` to set the topic, and  `splunk.hec.token`  to set your HEC token.

    ```
	curl localhost:8083/connectors -X POST -H "Content-Type: application/json" -d '{
	"name": "kafka-connect-splunk",
	"config": {
	   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
	   "tasks.max": "3",
	   "topics":"<YOUR_TOPIC>",
	   "splunk.indexes": "",
	   "splunk.sources": "",
	   "splunk.sourcetypes": "",
	   "splunk.hec.uri": "https://localhost:8088",
	   "splunk.hec.token": "<YOUR_TOKEN>",
	   "splunk.hec.raw": "true",
	   "splunk.hec.raw.line.breaker": "",
	   "splunk.hec.json.event.enrichment": "<key value pairs separated by comma, only applicable to /event HEC>",
	   "splunk.hec.ack.enabled": "true",
	   "splunk.hec.ack.poll.interval": "10",
	   "splunk.hec.ack.poll.threads": "2",
	   "splunk.hec.ssl.validate.certs": "false",
	   "splunk.hec.http.keepalive": "true",
	   "splunk.hec.max.http.connection.per.channel": "4",
	   "splunk.hec.total.channels": "8",
	   "splunk.hec.max.batch.size": "1000000",
	   "splunk.hec.threads": "2",
	   "splunk.hec.event.timeout": "300",
	   "splunk.hec.socket.timeout": "120",
	   "splunk.hec.track.data": "true"
	  }
	}'
    ```

7. Verify that data is flowing into your Splunk platform instance by searching using the index, sourcetype or source from your configuration.
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

See [Splunk Docs](https://docs.splunk.com/Documentation/KafkaConnect/latest/User/ConfigureSplunkKafkaConnect) for information on deployment.

## Security

See [Splunk Docs](https://docs.splunk.com/Documentation/KafkaConnect/latest/User/SecurityConfigurations) for information on security.

## Configuration

After Kafka Connect is brought up on every host, all of the Kafka Connect instances will form a cluster automatically.
Even in a load balanced environment, a REST call can be executed against one of the cluster instances, and rest of the instances will pick up the task automatically.

### Configuration schema structure
Use the below schema to configure Splunk Connect for Kafka

```
{
"name": "<connector-name>",
"config": {
   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
   "tasks.max": "<number-of-tasks>",
   "topics": "<list-of-topics-separated-by-comma>",
   "splunk.indexes": "<list-of-indexes-for-topics-data-separated-by-comma>",
   "splunk.sources": "<list-of-sources-for-topics-data-separated-by-comma>",
   "splunk.sourcetypes": "<list-of-sourcetypes-for-topics-data-separated-by-comma>",
   "splunk.hec.uri": "<Splunk-HEC-URI>",
   "splunk.hec.token": "<Splunk-HEC-Token>",
   "splunk.hec.raw": "<true|false>",
   "splunk.hec.raw.line.breaker": "<line breaker separator>",
   "splunk.hec.json.event.enrichment": "<key value pairs separated by comma, only applicable to /event HEC>",
   "splunk.hec.ack.enabled": "<true|false>",
   "splunk.hec.ack.poll.interval": "<event ack poll interval>",
   "splunk.hec.ack.poll.threads": "<number of threads used to poll event acks>",
   "splunk.hec.ssl.validate.certs": "<true|false>",
   "splunk.hec.http.keepalive": "<true|false>",
   "splunk.hec.max.http.connection.per.channel": "<max number of http connections per channel>",
   "splunk.hec.total.channels": "<total number of channels>",
   "splunk.hec.max.batch.size": "<max number of kafka records post in one batch>",
   "splunk.hec.threads": "<number of threads to use to do HEC post for single task>",
   "splunk.hec.event.timeout": "<timeout in seconds>",
   "splunk.hec.socket.timeout": "<timeout in seconds>",
   "splunk.hec.track.data": "<true|false, tracking data loss and latency, for debugging lagging and data loss>"
   "splunk.header.support": "<true|false>",
   "splunk.header.custom": "<list-of-custom-headers-to-be-used-from-kafka-headers-separated-by-comma>", 
   "splunk.header.index": "<header-value-to-be-used-as-splunk-index>",
   "splunk.header.source": "<header-value-to-be-used-as-splunk-source>",
   "splunk.header.sourcetype": "<header-value-to-be-used-as-splunk-sourcetype>",
   "splunk.header.host": "<header-value-to-be-used-as-splunk-host>",
   "splunk.hec.json.event.formatted": "<true|false>",
   "splunk.hec.ssl.trust.store.path": "<Java KeyStore location>",
   "splunk.hec.ssl.trust.store.password": "<Java KeyStore password>"
 }

  }
}
```

### Parameters

#### Required Parameters
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
|`name` | Connector name. A consumer group with this name will be created with tasks to be distributed evenly across the connector cluster nodes.||
| `connector.class` | The Java class used to perform connector jobs. Keep the default unless you modify the connector.|`com.splunk.kafka.connect.SplunkSinkConnector`|
| `tasks.max` |  The number of tasks generated to handle data collection jobs in parallel. The tasks will be spread evenly across all Splunk Kafka Connector nodes.||
| `splunk.hec.uri` | Splunk HEC URIs. Either a list of FQDNs or IPs of all Splunk indexers, separated with a ",", or a load balancer. The connector will load balance to indexers using round robin. Splunk Connector will round robin to this list of indexers.```https://hec1.splunk.com:8088,https://hec2.splunk.com:8088,https://hec3.splunk.com:8088```|
| `splunk.hec.token` |  [Splunk Http Event Collector token](http://docs.splunk.com/Documentation/SplunkCloud/6.6.3/Data/UsetheHTTPEventCollector#About_Event_Collector_tokens).||
| `topics` |  Comma separated list of Kafka topics for Splunk to consume. `prod-topic1,prod-topc2,prod-topic3`||
#### General Optional Parameters
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `splunk.indexes` | Target Splunk indexes to send data to. It can be a list of indexes which shall be the same sequence / order as topics. It is possible to inject data from different kafka topics to different splunk indexes. For example, prod-topic1,prod-topic2,prod-topic3 can be sent to index prod-index1,prod-index2,prod-index3. If you would like to index all data from multiple topics to the main index, then "main" can be specified. Leaving this setting unconfigured will result in data being routed to the default index configured against the HEC token being used. Verify the indexes configured here are in the index list of HEC tokens, otherwise Splunk HEC will drop the data. |`""`|
| `splunk.sources` |  Splunk event source metadata for Kafka topic data. The same configuration rules as indexes can be applied. If left unconfigured, the default source binds to the HEC token. | `""` |
| `splunk.sourcetypes` | Splunk event sourcetype metadata for Kafka topic data. The same configuration rules as indexes can be applied here. If left unconfigured, the default source binds to the HEC token. | `""` |
| `splunk.hec.ssl.validate.certs` | Valid settings are `true` or `false`. Enables or disables HTTPS certification validation. |`true`|
| `splunk.hec.http.keepalive` | Valid settings are `true` or `false`. Enables or disables HTTP connection keep-alive. |`true`|
| `splunk.hec.max.http.connection.per.channel` | Controls how many HTTP connections will be created and cached in the HTTP pool for one HEC channel. |`2`|
| `splunk.hec.total.channels` | Controls the total channels created to perform HEC event POSTs. See the Load balancer section for more details. |`2`|
| `splunk.hec.max.batch.size` | Maximum batch size when posting events to Splunk. The size is the actual number of Kafka events, and not byte size. |`100`|
| `splunk.hec.threads` | Controls how many threads are spawned to do data injection via HEC in a **single** connector task. |`1`|
| `splunk.hec.socket.timeout` | Internal TCP socket timeout when connecting to Splunk. Value is in seconds. |`60`|
| `splunk.hec.ssl.trust.store.path` | Location of Java KeyStore. |`""`|
| `splunk.hec.ssl.trust.store.password` | Password for Java KeyStore. |`""`|
| `splunk.hec.json.event.formatted` | Valid settings are `true` or `false`. Location of Java KeyStore. |`false`|
### Acknowledgement Parameters
#### Use Ack
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `splunk.hec.ack.enabled` -| Valid settings are `true` or `false`. When set to `true` the Splunk Kafka Connector will poll event ACKs for POST events before check-pointing the Kafka offsets. This is used to prevent data loss, as this setting implements guaranteed delivery. > Note: If this setting is set to `true`, verify that the corresponding HEC token is also enabled with index acknowledgements, otherwise the data injection will fail, due to duplicate data. When set to `false`, the Splunk Kafka Connector will only POST events to your Splunk platform instance. After it receives a HTTP 200 OK response, it assumes the events are indexed by Splunk. Note: In cases where the Splunk platform crashes, there may be some data loss.|`true`|
| `splunk.hec.ack.poll.interval` | This setting is only applicable when `splunk.hec.ack.enabled` is set to `true`. Internally it controls the event ACKs polling interval. Value is in seconds. |`10`|
| `splunk.hec.ack.poll.threads` | This setting is used for performance tuning and is only applicable when `splunk.hec.ack.enabled` is set to `true`. It controls how many threads should be spawned to poll event ACKs. > Note: For large Splunk indexer clusters (For example, 100 indexers) you need to increase this number. Recommended increase to speed up ACK polling is 4 threads.| `1`|
| `splunk.hec.event.timeout` | This setting is applicable when `splunk.hec.ack.enabled` is set to `true`. When events are POSTed to Splunk and before they are ACKed, this setting determines how long the connector will wait before timing out and resending. Value is in seconds. |`300`|
#### Endpoint Parameters
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `splunk.hec.raw` | Set to `true` in order for Splunk software to ingest data using the the /raw HEC endpoint.`false` will use the /event endpoint |`false`|
##### /raw endpoint only
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `splunk.hec.raw.line.breaker` | Only applicable to /raw HEC endpoint. The setting is used to specify a custom line breaker to help Splunk separate the events correctly.    > Note: For example, you can specify "#####" as a special line breaker. Internally, the Splunk Kafka Connector will append this line breaker to every Kafka record to form a clear event boundary. The connector performs data injection in batch mode. On the Splunk platform side, you can configure **props.conf** to set up line breaker for the sourcetypes. Then the Splunk software will correctly break events for data flowing through /raw HEC endpoint. For questions on how and when to specify line breaker, go to the FAQ section.|`""`|
##### /event endpoint only
| Name              | Description                | Default Value  |
|--------           |----------------------------|-----------------------|
| `splunk.hec.json.event.enrichment` |  Only applicable to /event HEC endpoint. This setting is used to enrich raw data with extra metadata fields. It contains a list of key value pairs separated by ",". The configured enrichment metadata will be indexed along with raw event data by Splunk software. Note: Data enrichment for /event HEC endpoint is only available in Splunk Enterprise 6.5 and above. By default, this setting is empty. See ([Documentation](http://dev.splunk.com/view/event-collector/SP-CAAAE8Y#indexedfield)) for more information.> Note: For example, `org=fin,bu=south-east-us`||
| `splunk.hec.track.data` |  Valid settings are `true` or `false`. When set to `true`, data loss and data injection latency metadata will be indexed along with raw data. This setting only works in conjunction with /event HEC endpoint (`"splunk.hec.raw" : "false"`).|`false`|

### Headers Parameters
#### Use Headers
* `splunk.header.support` -  Valid settings are `true` or `false`. When set to `true` Splunk Connect for Kafka will parse kafka headers for using meta data in generated splunk events. By default, this setting is set to `false`.
* `splunk.header.custom` - This setting is only applicable when `splunk.header.support` is set to `true`. Custom headers are configured separated by comma for multiple headers. ex,  "custom_header_1,custom_header_2,custom_header_3. This setting will look for kafka record headers with these values and add them to each event if present. By default, it is set to `""`.
* `splunk.header.index` - This setting is only applicable when `splunk.header.support` is set to `true`. This setting specifies the header to be used for splunk index. By default, it is set to `splunk.header.index`.
* `splunk.header.source` - This setting is only applicable when `splunk.header.support` is set to `true`. This setting specifies the source to be used for splunk source. By default, it is set to `splunk.header.source`.
* `splunk.header.sourcetype` - This setting is only applicable when `splunk.header.support` is set to `true`. This setting specifies the sourcetype to be used for splunk sourcetype. By default, it is set to `splunk.header.sourcetype`.
* `splunk.header.host` - This setting is only applicable when `splunk.header.support` is set to `true`. This setting specifies the host to be used for splunk host. By default, it is set to `splunk.header.host`.

## Load balancing

[Use load balancing in your deployment.](https://docs.splunk.com/Documentation/KafkaConnect/latest/User/LoadBalancing)

## Benchmark Results

[Information on benchmarking results.](https://docs.splunk.com/Documentation/KafkaConnect/latest/User/Planyourdeployment) 

## Scale out your environment

[Scale your environment.](https://docs.splunk.com/Documentation/KafkaConnect/latest/User/ConfigureSplunkKafkaConnect#Scale_your_environment)

## Data loss and latency monitoring

[Guidelines for tracking data loss and latency.](https://docs.splunk.com/Documentation/KafkaConnect/latest/User/ConfigureSplunkKafkaConnect#Data_loss_and_latency_monitoring)

## Troubleshooting

[Troubleshoot your deployment.](https://docs.splunk.com/Documentation/KafkaConnect/latest/User/Troubleshootyourdeployment)

## License

Splunk Connect for Kafka  is licensed under the Apache License 2.0. Details can be found in the file LICENSE.
