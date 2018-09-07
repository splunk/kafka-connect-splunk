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
3. Run `bash build.sh`. The build script will download all dependencies and build Splunk Connect for Kafka.

Note: The resulting "splunk-kafka-connect*.tar.gz" package is self-contained. Bundled within it are the Kafka Connect framework, all 3rd party libraries, and Splunk Connect for Kafka.

## Quick Start

1. [Start](https://kafka.apache.org/quickstart) your Kafka Cluster and confirm it is running.
2. If this is a new install, create a test topic (eg: `perf`). Inject events into the topic. This can be done using [Kafka data-gen-app](https://github.com/dtregonning/kafka-data-gen) or the Kafka bundle [kafka-console-producer](https://kafka.apache.org/quickstart#quickstart_send).
3. Untar the package created from the build script: `tar xzvf splunk-kafka-connect-*.tar.gz` (Default target location is /tmp/splunk-kafka-connect-build/kafka-connect-splunk).
4. Navigate to splunk-kafka-connect directory `cd splunk-kafka-connect`.
5. Adjust values for `bootstrap.servers` and `plugin.path` inside `config/connect-distributed-quickstart.properties` to fit your environment. Default values should work for experimentation.
6. Run `./bin/connect-distributed.sh config/connect-distributed-quickstart.properties` to start Kafka Connect.
7. Run the following command to create connector tasks. Adjust `topics` to set the topic, and  `splunk.hec.token`  to set your HEC token.

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

8. Verify that data is flowing into your Splunk platform instance by searching using the index, sourcetype or source from your configuration.
9. Use the following commands to check status, and manage connectors and tasks:

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
Splunk Connect for Kafka can run in containers, virtual machines or on physical machines.
You can leverage any automation tools for deployment.

Use the following connector deployment options:
* Splunk Connect for Kafka in a dedicated Kafka Connect Cluster (recommended)
* Splunk Connect for Kafka in an existing Kafka Connect Cluster

### Connector in a dedicated Kafka Connect Cluster 
Running Splunk Connect for Kafka in a dedicated Kafka Connect Cluster is recommended. Isolating the Splunk connector from other Kafka connectors results in significant performance benefits in high throughput environments.

1. Untar the **splunk-kafka-connect-*.tar.gz** package and navigate to the **splunk-kafka-connect** directory.

    ```
    tar xzvf splunk-kafka-connect-*.tar.gz
    cd splunk-kafka-connect
    ```

2. Update config/connect-distributed.properties to match your environment.

	```
	bootstrap.servers=<broker1:9092,broker2:9092,...>  # adjust this setting to the brokers' IP/hostname port
	```

3. Revise other optional settings in **config/connect-distributed.properties** as needed.

	> Note: Modify group ID name if needed.

    ```
	group.id=kafka-connect-splunk-hec-sink    # consumer group id of Kafka Connect, which is used to form a Kafka Connect cluster

	```

4. Deploy/Copy the **splunk-kafka-connect** directory to all target hosts (virtual machines, physical machines or containers).
5. Start Kafka Connect on all target hosts using the below commands:

	```
	cd kafka-connect-splunk
	export KAFKA_HEAP_OPTS="-Xmx6G -Xms2G" && ./bin/connect-distributed.sh config/connect-distributed.properties >> kafka-connect-splunk.log 2>&1
	```

	> Note: The **KAFKA\_HEAP\_OPTS** environment variable controls how much memory Kafka Connect can use. Set the **KAFKA\_HEAP\_OPTS** with the recommended value stated in the example above.

### Connector in an existing Kafka Connect Cluster

1. Navigate to Splunkbase and download the latest version of [Splunk Connect for Kafka](https://splunkbase.splunk.com/app/3862/).
 
2. Copy downloaded file onto every host running Kafka Connect into the directory that contains your other connectors or create a folder to store them in. (ex. `/opt/connectors/splunk-kafka-connect`)

3. The Splunk Connector requires the below worker properties to function correctly.

```
#These settings may already be configured if you have deployed a connector in your Kafka Connect Environment
bootstrap.servers=<BOOTSTRAP_SERVERS>
plugin.path=<PLUGIN_PATH>

#Required
key.converter=<org.apache.kafka.connect.storage.StringConverter|org.apache.kafka.connect.json.JsonConverter|io.confluent.connect.avro.AvroConverter>
value.converter=<org.apache.kafka.connect.storage.StringConverter|org.apache.kafka.connect.json.JsonConverter|io.confluent.connect.avro.AvroConverter>

- For StringConverter and JsonConverter only -
key.converter.schemas.enable=false
value.converter.schemas.enable=false

- For AvroConverter only -
key.converter.schema.registry.url=<Location of Avro schema registry>
value.converter.schema.registry.url=<Location of Avro schema registry>

internal.key.converter=org.apache.kafka.connect.json.JsonConverter
internal.value.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter.schemas.enable=false
internal.value.converter.schemas.enable=false
offset.flush.interval.ms=10000

#Recommended
group.id=kafka-connect-splunk-hec-sink

```  
> Note - For more information on the worker paramaters please refer to Kafka Connect [documentation](https://kafka.apache.org/documentation/#connect_running).

Please create or modify a Kafka Connect worker properties file to contain these parameters. The default worker properties file is `$KAFKA_CONNECT_HOME/config/connect-distrubuted.properties`. Ensure to replace `<BOOTSTRAP_SERVERS>` to point to your Kafka brokers (ex. `localhost:9092`) and ensure `<PLUGIN_PATH>` points to the top-level directory of where you are storing your connectors. (ex. `/opt/connectors/`).
    
4. Start/Restart Kafka Connect - (for ex. `$KAFKA_CONNECT_HOME/bin/connect-distributed.sh $KAFKA_CONNECT_HOME/config/connect-distrubuted.properties`).

5. Validate your connector deployment by running the following command curl `http://<KAFKA_CONNECT_HOST>:8083/connector-plugins`. Response should have an entry named `com.splunk.kafka.connect.SplunkSinkConnector`.

## Security
Splunk Connect for Kafka supports the following security mechanisms:
* `SSL`
* `SASL/GSSAPI (Kerberos)`
* `SASL/PLAIN`
* `SASL/SCRAM-SHA-256 and SASL/SCRAM-SHA-512`

See [Confluent's documentation](https://docs.confluent.io/current/connect/security.html#security) to understand the impact of using security within the Kafka Connect framework, specifically [ACL considerations](https://docs.confluent.io/current/connect/security.html#acl-considerations).

The following examples assume you're deploying to an [existing Kafka Connect cluster](#connector-in-an-existing-kafka-connect-cluster) or a [dedicated Kafka Connect cluster](#connector-in-a-dedicated-kafka-connect-cluster).


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
 "splunk.indexes":
"<list-of-indexes-for-topics-data-separated-by-comma>",
 "splunk.sources":
"<list-of-sources-for-topics-data-separated-by-comma>",
 "splunk.sourcetypes":
"<list-of-sourcetypes-for-topics-data-separated-by-comma>",
 "splunk.hec.uri": "<Splunk-HEC-URI>",
 "splunk.hec.token": "<Splunk-HEC-Token>",
 "splunk.hec.raw": "<true|false>",
 "splunk.hec.raw.line.breaker": "<line breaker separator>",
 "splunk.hec.json.event.enrichment": "<key value pairs separated by
comma, only applicable to /event HEC>",
 "splunk.hec.ack.enabled": "<true|false>",
 "splunk.hec.ack.poll.interval": "<event ack poll interval>",
 "splunk.hec.ack.poll.threads": "<number of threads used to poll
event acks>",
 "splunk.hec.ssl.validate.certs": "<true|false>",
 "splunk.hec.http.keepalive": "<true|false>",
 "splunk.hec.max.http.connection.per.channel": "<max number of http
connections per channel>",
 "splunk.hec.total.channels": "<total number of channels>",
 "splunk.hec.max.batch.size": "<max number of kafka records post in
one batch>",
 "splunk.hec.threads": "<number of threads to use to do HEC post for
single task>",
 "splunk.hec.event.timeout": "<timeout in seconds>",
 "splunk.hec.socket.timeout": "<timeout in seconds>",
 "splunk.hec.track.data": "<true|false, tracking data loss and
latency, for debugging lagging and data loss>",
 "splunk.header.support": "<true|false>",
 "splunk.header.custom": "<custom_header_1,custom_header_2,custom_header_3>",
 "splunk.header.index": "<reocrd header key to be used for index>",
 "splunk.header.source": "<reocrd header key to be used for source>",
 "splunk.header.sourcetype": "<reocrd header key to be used for sourcetype>",
 "splunk.header.host": "<reocrd header key to be used for host>",
 "splunk.hec.json.event.formatted": "<true|false>"
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
| `splunk.hec.json.event.formatted` | Valid settings are `true` or `false`. Allows HEC-formatted events to pass through as is. |`false`|
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


#### Configuration Examples
 Two parameters which affect that core functionality of how the Connector works are:
 `splunk.hec.raw` and `splunk.hec.ack.enabled`. Detailed below are 4 configuration examples which implement these settings

##### Splunk Indexing with Acknowledgment

1. Using HEC /raw endpoint:

	```
	curl <hostname>:8083/connectors -X POST -H "Content-Type: application/json" -d'{
	  "name": "splunk-prod-financial",
	    "config": {
	      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
	      "tasks.max": "10",
	      "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
	      "splunk.hec.uri": "https://idx1:8089,https://idx2:8089,https://idx3:8089",
	      "splunk.hec.token": "1B901D2B-576D-40CD-AF1E-98141B499534",
	      "splunk.hec.ack.enabled : "true",
	      "splunk.hec.ack.poll.interval" : "20",
	      "splunk.hec.ack.poll.threads" : "2",
	      "splunk.hec.event.timeout" : "300",
	      "splunk.hec.raw" : "true",
	      "splunk.hec.raw.line.breaker" : "#####"
	    }
	}'
	```

2. Using HEC /event endpoint:

   ```
   curl <hostname>:8083/connectors -X POST -H "Content-Type: application/json" -d'{
     "name": "splunk-prod-financial",
       "config": {
         "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
         "tasks.max": "10",
         "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
         "splunk.hec.uri": "https://idx1:8089,https://idx2:8089,https://idx3:8089",
         "splunk.hec.token": "1B901D2B-576D-40CD-AF1E-98141B499534",
         "splunk.hec.ack.enabled : "true",
         "splunk.hec.ack.poll.interval" : "20",
         "splunk.hec.ack.poll.threads" : "2",
         "splunk.hec.event.timeout" : "300",
         "splunk.hec.raw" : "false",
         "splunk.hec.json.event.enrichment" : "org=fin,bu=south-east-us",
         "splunk.hec.track.data" : "true"
       }
   }'
   ```

##### Splunk Indexing without Acknowledgment

3. Using HEC /raw endpoint:

    ```
    curl <hostname>:8083/connectors -X POST -H "Content-Type: application/json" -d'{
      "name": "splunk-prod-financial",
        "config": {
          "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
          "tasks.max": "10",
          "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
          "splunk.hec.uri": "https://idx1:8089,https://idx2:8089,https://idx3:8089",
          "splunk.hec.token": "1B901D2B-576D-40CD-AF1E-98141B499534"
          "splunk.hec.ack.enabled : "false",
          "splunk.hec.raw" : "true",
          "splunk.hec.raw.line.breaker" : "#####"
        }
    }'
    ```


4. Using HEC /event endpoint:

    ```
    curl <hostname>:8083/connectors -X POST -H "Content-Type: application/json" -d'{
      "name": "splunk-prod-financial",
        "config": {
          "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
          "tasks.max": "10",
          "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
          "splunk.hec.uri": "https://idx1:8089,https://idx2:8089,https://idx3:8089",
          "splunk.hec.token": "1B901D2B-576D-40CD-AF1E-98141B499534",
          "splunk.hec.ack.enabled : "false",
          "splunk.hec.raw" : "false",
          "splunk.hec.json.event.enrichment" : "org=fin,bu=south-east-us",
          "splunk.hec.track.data" : "true"

        }
    }'
    ```

* Use the below command to create a connector called `splunk-prod-financial` for 10 topics and 10 parallelized tasks. The connector will use the /event HEC endpoint with acknowledgements enabled. The data is injected into a 3-server Splunk platform indexer cluster.

	```
	 curl <hostname>:8083/connectors -X POST -H "Content-Type: application/json" -d'{
    "name": "splunk-prod-financial",
    "config": {
       "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
       "tasks.max": "10",
       "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
       "splunk.hec.uri": "https://idx1:8089,https://idx2:8089,https://idx3:8089",
       "splunk.hec.token": "1B901D2B-576D-40CD-AF1E-98141B499534"
      }
    }'
	```

* Use the command below to update the connector to use 20 parallelized tasks.

	```
	curl <hostname>:8083/connectors/splunk-prod-financial/config -X PUT -H "Content-Type: application/json" -d'{
	"name": "splunk-prod-financial",
	"config": {
	   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
	   "tasks.max": "20",
	   "topics": "t1,t2,t3,t4,t5,t6,t7,t8,t9,t10",
	   "splunk.hec.uri": "https://idx1:8089,https://idx2:8089,https://idx3:8089",
	   "splunk.hec.token": "1B901D2B-576D-40CD-AF1E-98141B499534"
	  }
	}'
	```

* Use the command below to delete the connector.

	```
	curl <hostname>:8083/connectors/splunk-prod-financial-X DELETE
	```

## Load balancing

A common architecture will include a load balancer in front of your Splunk platform indexer cluster or a collection of Splunk platform heavy forwarders. If configured in this manner and HEC acknowledgement is enabled (`splunk.hec.ack.enabled:true`), take care to ensure data ingestion behaves correctly:

1. Enable **sticky sessions** on the load balancer. Without this, data duplication may occur.
2. Set HEC channels (**splunk.hec.total.channels**) to multiple HEC endpoints (= indexers or 2 * indexers behind the load balancer). This will ensure the data flow is evenly load balanced across the Splunk platform indexers.

> Note: Data duplication may occur even with sticky sessions, when requests are offloaded to a different endpoint under load.

## Benchmark Results

A single instance of Splunk Connect for Kafka can reach maximum indexed throughput of **32 MB/second** with the following testbed and raw HEC endpoint in use:

Hardware specifications:

* **AWS:** EC2 c4.2xlarge, 8 vCPU and 31 GB Memory
* **Splunk Cluster:** 3 indexer cluster without load balancer
* **Kafka Connect:** JVM heap size configuration is "-Xmx6G -Xms2G"
* **Kafka Connect resource usage:** ~6GB memory, ~3 vCPUs.
* **Kafka records size**: 512 Bytes
* **Batch size**: Maximum 100 Kafka records per batch which is around 50KB per batch

## Scaling out your environment

Before scaling the Splunk Connect for Kafka tier, ensure the bottleneck is in the connector tier and not in another component.

Scaling out options:

1. Increase the number of parallel tasks. Only do this if the hardware is under-utilized (low CPU, low memory usage and low data injection throughput). The user can reconfigure the connector with more tasks. Example above in the **Configuration parameters**-**Update** section.

2. Increase hardware resources on cluster nodes in case of resource exhaustion, such as high CPU, or high memory usage.

3. Increase the number of Kafka Connect nodes.

## Data loss and latency monitoring

When creating an instance of Splunk Connect for Kafka using the REST API, `"splunk.hec.track.data": "true"` can be configured to allow data loss tracking and data collection latency monitoring.
This is accomplished by enriching the raw data with **offset, timestamp, partition, topic** metadata.

### Data Loss Tracking
Splunk Connect for Kafka uses offset to track data loss since offsets in a Kafka topic partition are sequential. If a gap is observed in the Splunk software, there is data loss.

### Data Latency Tracking
Splunk Connect for Kafka uses the timestamp of the record to track the time elapsed between the time a Kafka record was created and the time the record was indexed in Splunk.

> Note: This setting will only work in conjunction with /event HEC endpoint (`"splunk.hec.raw" : "false"`)

### Malformed data

If the raw data of the Kafka records is a JSON object but is not able to be marshaled, or if the raw data is in bytes but it is not UTF-8 encodable, Splunk Connect for Kafka considers these records malformed. It will log the exception with Kafka specific information (topic, partition, offset) for these records within the console, as well as the malformed records information will be indexed in Splunk. Users can search "type=malformed" within Splunk to return any malformed Kafka records encountered.

## Troubleshooting

1. Append the **log4j.logger.com.splunk=DEBUG** to **config/connect-log4j.properties** file to enable more verbose logging for Splunk Connect for Kafka.
2. Kafka connect encounters an "out of memory" error. Remember to export environment variable **KAFKA\_HEAP\_OPTS="-Xmx6G -Xms2G"**. Refer to the [Deployment](#deployment) section for more information.
3. Can't see any Connector information on third party UI. For example, Splunk Connect for Kafka is not shown on the Confluent Control Center. Make sure cross origin access is enabled for Kafka Connect. Append the following two lines to connect configuration, e.g. `connect-distributed.properties` or `connect-distributed-quickstart.properties` and then restart Kafka Connect.

	```
	access.control.allow.origin=*
	access.control.allow.methods=GET,OPTIONS,HEAD,POST,PUT,DELETE
	```
