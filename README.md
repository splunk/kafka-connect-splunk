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
3. Run `mvn package`. This will build the jar in the /target directory. The name will be "splunk-kafka-connect-v*.jar".

## Quick Start

1. [Start](https://kafka.apache.org/quickstart) your Kafka Cluster and confirm it is running.
2. If this is a new install, create a test topic (eg: `perf`). Inject events into the topic. This can be done using [Kafka data-gen-app](https://github.com/dtregonning/kafka-data-gen) or the Kafka bundle [kafka-console-producer](https://kafka.apache.org/quickstart#quickstart_send).
3. Place the jar file created by the maven build (splunk-kafka-connect-v*.jar) in or under the location specified in `plugin.path` (see step 4).
4. Within Kafka Connect, adjust values for `bootstrap.servers` and `plugin.path` inside `config/connect-distributed.properties` to fit your environment. The following values are also required:

    ```
	key.converter=<org.apache.kafka.connect.storage.StringConverter|org.apache.kafka.connect.json.JsonConverter|io.confluent.connect.avro.AvroConverter>
	value.converter=<org.apache.kafka.connect.storage.StringConverter|org.apache.kafka.connect.json.JsonConverter|io.confluent.connect.avro.AvroConverter>
	internal.key.converter=org.apache.kafka.connect.json.JsonConverter
	internal.value.converter=org.apache.kafka.connect.json.JsonConverter
	internal.key.converter.schemas.enable=false
	internal.value.converter.schemas.enable=false
	offset.flush.interval.ms=10000

	- For StringConverter and JsonConverter only -
	key.converter.schemas.enable=false
	value.converter.schemas.enable=false

	- For AvroConverter only -
	key.converter.schema.registry.url=<Location of Avro schema registry>
	value.converter.schema.registry.url=<Location of Avro schema registry>

    ```

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

See [Splunk Docs](http://docs.splunk.com/Documentation/KafkaConnect) for information on the following:

* Deployment
* Security
* Configuration
* Load balancing
* Benchmark Results
* Scaling out your environment
* Data loss and latency monitoring
* Troubleshooting
