# Install Splunk Connect for Kafka

The installation procedure applies to Apache Kafka and Confluent Platform deployments.

Supported Apache Kafka and Kafka Connect release lines are 4.2.x, 4.3.x, and 4.4.x. Use Kafka 4.2.2, 4.3.2, or 4.4.0, or a newer patch in the corresponding release line.

The connector is compiled using `--release 17` against the Kafka 4.2.2 Connect API. CI builds once and runs unit tests with JDK 17 against Kafka 4.2.2, then uses the resulting connector JAR for end-to-end tests on Kafka 4.2.2, 4.3.2, and 4.4.0. Kafka Connect must run on Java 17 or later. The Connect API and Kafka clients are supplied by the worker at runtime and are not bundled in the connector JAR.

> **New deployments:** Splunk's current product guidance recommends Splunk OpenTelemetry Connector for Kafka (SOC4Kafka) for new Kafka-to-Splunk deployments. Use this connector when maintaining an existing Splunk Connect for Kafka deployment or when its behavior is specifically required. See the [official Splunk Connect for Kafka overview](https://help.splunk.com/en/splunk-cloud-platform/get-data-in/splunk-connect-for-kafka/2.2/overview/splunk-connect-for-kafka) before selecting an ingestion path.

## Install the connector

1. Download the required `splunk-kafka-connect-<VERSION>.jar` from the repository's [GitHub releases](https://github.com/splunk/kafka-connect-splunk/releases), or build it locally with `mvn package`.

2. Confirm that the Kafka cluster and Kafka Connect REST API are available:

   ```sh
   curl http://<KAFKA_CONNECT_HOST>:<KAFKA_CONNECT_PORT>
   ```

   The default local REST endpoint is `http://localhost:8083`.

3. Create or select a Kafka Connect plugin directory. The directory must be included in the worker's `plugin.path`.

4. Configure the properties file passed to each worker's startup command, for example, `$KAFKA_HOME/config/connect-distributed.properties`.

   ```properties
   bootstrap.servers=<BROKER_1>:9092,<BROKER_2>:9092,<BROKER_3>:9092
   plugin.path=<CONNECTOR_PLUGIN_DIRECTORY>

   key.converter=org.apache.kafka.connect.storage.StringConverter
   value.converter=org.apache.kafka.connect.storage.StringConverter
   ```

   Select converters that match the Kafka record format. Common alternatives are `org.apache.kafka.connect.json.JsonConverter`, `io.confluent.connect.avro.AvroConverter`, and `io.confluent.connect.protobuf.ProtobufConverter`. Avro and Protobuf converters require their libraries and schema registry configuration.

5. Place the connector JAR in the configured plugin directory on every Kafka Connect host. Keep one connector version in that plugin directory to avoid class-loading ambiguity.

6. Restart existing Kafka Connect services, or start a distributed worker:

   ```sh
   $KAFKA_HOME/bin/connect-distributed.sh config/connect-distributed.properties
   ```

7. Confirm that Kafka Connect discovered the plugin:

   ```sh
   curl http://localhost:8083/connector-plugins
   ```

   The response must contain:

   ```text
   com.splunk.kafka.connect.SplunkSinkConnector
   ```

8. For a new deployment, create a test topic and publish a few events with `kafka-console-producer` or another Kafka producer.

9. Create a connector using the [configuration guide](configuration.md), then search the destination Splunk index to verify ingestion.

`$KAFKA_HOME` is the Kafka or Kafka Connect installation directory on the worker host.

Kafka 4.x requires KRaft. Format its storage with `kafka-storage.sh` and start the brokers using the Kafka 4.x [KRaft deployment procedure](https://kafka.apache.org/44/operations/kraft/); do not start ZooKeeper. Use the storage tool from the same Kafka distribution as the broker.

## Install the Protobuf converter

> **Third-party software:** Starting with Splunk Connect for Kafka version 2.4.0, the Confluent Protobuf Converter and its dependencies are no longer bundled with the connector. To enable Protobuf schema support, install the converter separately. The Confluent Protobuf Converter is developed and distributed by Confluent and is subject to its own license terms. Install it from the official Confluent distribution using the version specified for your Kafka Connect runtime.

String and JSON ingestion use Kafka's built-in converters and do not require this package or Schema Registry.

Choose the converter for the **Connect worker** release. The release families follow [Confluent's Kafka compatibility table](https://docs.confluent.io/platform/8.4/installation/versions-interoperability.html). 

Use the corresponding 8.2.x, 8.3.x, or 8.4.x converter family for Kafka Connect 4.2.x, 4.3.x, or 4.4.x. A converter selected for Kafka 4.4 is not a substitute for the converter selected for older workers.

Find the converter in [Confluent Marketplace](https://www.confluent.io/hub/confluentinc/kafka-connect-protobuf-converter). Follow Confluent's [plugin installation](https://docs.confluent.io/platform/current/connect/userguide.html#install-a-connect-plugin) and [converter configuration](https://docs.confluent.io/platform/current/schema-registry/connect.html) documentation.


Keep the complete package, including `lib/`, licenses, and metadata.

Install the Splunk connector in a sibling directory and include their parent in `plugin.path`, preserving existing path entries:

```text
/opt/kafka/plugins/
  sc4kafka/
    splunk-kafka-connect-<VERSION>.jar
  confluentinc-kafka-connect-protobuf-converter-8.4.1/
    lib/
      kafka-connect-protobuf-converter-8.4.1.jar
      <all other supplied JARs>
```

```properties
plugin.path=/opt/kafka/plugins
```

Merge these properties into the Splunk connector configuration for string keys and Protobuf values:

```json
{
  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "io.confluent.connect.protobuf.ProtobufConverter",
  "value.converter.schema.registry.url": "https://schema-registry.example.com"
}
```

Use the appropriate key converter for the topic's key format. Configure Schema Registry authentication and TLS as required. The registry must contain the schemas referenced by the records, which must use Confluent's Protobuf serialization format.

Remove the old Splunk JAR containing the bundled converter before restarting workers. 

Check task health using the Connect REST endpoint and `jq`:

```sh
curl --fail --silent --show-error \
  "http://localhost:8083/connectors/<CONNECTOR_NAME>/status" |
  jq -e '.connector.state == "RUNNING" and (.tasks | length > 0) and all(.tasks[]; .state == "RUNNING")'
```

## Kafka Connect REST commands

The examples use the connector name `kafka-connect-splunk`.

| Operation | Command |
| --- | --- |
| List connectors | `curl http://localhost:8083/connectors` |
| Get connector information | `curl http://localhost:8083/connectors/kafka-connect-splunk` |
| Get connector status | `curl http://localhost:8083/connectors/kafka-connect-splunk/status` |
| Get connector configuration | `curl http://localhost:8083/connectors/kafka-connect-splunk/config` |
| Get connector tasks | `curl http://localhost:8083/connectors/kafka-connect-splunk/tasks` |
| Pause the connector | `curl -X PUT http://localhost:8083/connectors/kafka-connect-splunk/pause` |
| Resume the connector | `curl -X PUT http://localhost:8083/connectors/kafka-connect-splunk/resume` |
| Delete the connector | `curl -X DELETE http://localhost:8083/connectors/kafka-connect-splunk` |

> **Security:** The configuration endpoint returns sensitive values such as `splunk.hec.token`. Do not expose port `8083` publicly, and redact credentials before sharing command output.

See [Upgrade Splunk Connect for Kafka](upgrade.md) when replacing an existing connector version.
