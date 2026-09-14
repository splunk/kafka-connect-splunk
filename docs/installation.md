# Install Splunk Connect for Kafka

The installation procedure applies to Apache Kafka and Confluent Platform deployments.

> **New deployments:** Splunk's current product guidance recommends Splunk OpenTelemetry Connector for Kafka (SOC4Kafka) for new Kafka-to-Splunk deployments. Use this connector when maintaining an existing Splunk Connect for Kafka deployment or when its behavior is specifically required. See the [official Splunk Connect for Kafka overview](https://help.splunk.com/en/splunk-cloud-platform/get-data-in/splunk-connect-for-kafka/2.2/overview/splunk-connect-for-kafka) before selecting an ingestion path.

## Install the connector

1. Download the required `splunk-kafka-connect-<VERSION>.jar` from the repository's [GitHub releases](https://github.com/splunk/kafka-connect-splunk/releases), or build it locally with `mvn package`.

2. Confirm that the Kafka cluster and Kafka Connect REST API are available:

   ```sh
   curl http://<KAFKA_CONNECT_HOST>:<KAFKA_CONNECT_PORT>
   ```

   The default local REST endpoint is `http://localhost:8083`.

3. Create or select a Kafka Connect plugin directory. The directory must be included in the worker's `plugin.path`.

4. Configure every distributed worker in `$KAFKA_HOME/config/connect-distributed.properties`:

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