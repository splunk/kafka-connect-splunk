# Functional tests

Use Python 3.11, the version used by CI, and an isolated Kafka Connect environment with Splunk HEC enabled.

## Setup

```sh
python3.11 -m venv venv
source venv/bin/activate
pip install -r test/requirements.txt
```

Configure `test/config.yaml` for your environment:

| Setting | Purpose |
| --- | --- |
| `splunkd_url` | Splunk management API, for example, `https://localhost:8089`. |
| `splunk_hec_url` | HEC endpoint, for example, `https://localhost:8088`. |
| `splunk_user`, `splunk_password` | Credentials used to search indexed records. |
| `splunk_token`, `splunk_token_ack` | HEC tokens with ACK disabled and enabled, respectively. |
| `splunk_index` | Destination index for test records. |
| `kafka_broker_url` | Kafka bootstrap address, for example, `localhost:9092`. |
| `kafka_connect_url` | Worker REST endpoint, for example, `http://localhost:8083`. |
| `kafka_topic`, `kafka_topic_2`, `kafka_header_topic` | Topics used by the ingestion checks. |
| `schema_registry_url` | Registry endpoint for Protobuf mode, defaulting to `http://localhost:8081`. |

The fixture creates topics and connectors, publishes records, and deletes its connectors afterward. Use a dedicated environment when running the suite.

## Without the Protobuf converter (default)

Run a worker with only the Splunk plugin installed, using Kafka's built-in String or JSON converters. No Schema Registry is required.

```sh
python -m pytest test
```

This runs the existing ingestion and configuration checks without the Protobuf converter. The Protobuf ingestion test is skipped. Keep Confluent converters out of the worker's plugin path and global classpath for this mode.

## With an external Protobuf converter

Install the complete converter package for the **Connect worker** version, following [the installation guide](../docs/installation.md#install-the-protobuf-converter). Keep the converter and Splunk plugin in sibling plugin directories, then restart the worker. Start Schema Registry and set `schema_registry_url` if it differs from the default.

```sh
python -m pytest test --protobuf
```

The fixture registers its schema and uses the returned schema ID to publish a uniquely identified Protobuf record. The active regression test checks every Protobuf task's health and asserts the decoded indexed fields.

## CI coverage

CI runs all 12 combinations of Kafka 4.2.2, 4.3.2, and 4.4.0 with Splunk 9.4.16 and 10.6.0.5, with and without the external Protobuf converter.

The six Protobuf jobs install the matching converter, start Schema Registry, and run all 54 functional tests with `--protobuf`. The six converter-free jobs run the 53 non-Protobuf tests and skip Protobuf ingestion, without installing the converter or starting Schema Registry. All 12 jobs retain the existing JSON upgrade check.

To inspect collection without provisioning topics or connectors:

```sh
python -m pytest test --collect-only
python -m pytest test --collect-only --protobuf
```
