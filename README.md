## Kafka Connect Splunk
A Kafka Connect Sink for Splunk features:
* Data ingestion from Kafka topics into Splunk via [Splunk HTTP Event Collector(HEC)](http://dev.splunk.com/view/event-collector/SP-CAAAE6M).
* In-flight data transformation and enrichment.

## Requirements
1. Kafka version 0.9 above.
2. Java 8 and above.
3. Splunk configured with valid Http Event Collector (HEC) tokens.
* HEC token settings should be the same on all Splunk Indexers and Heavy Forwarders in your environment.
* Task configuration parameters will vary depending on acknowledgement setting (Refer to **configuration** section for details).

Note: HEC Acknowledgement prevents potential data loss but may slow down event ingestion. 


## Build
1. Clone the repo https://github.com/splunk/kafka-connect-splunk
2. Make sure Java8 JRE or JDK is installed
3. Run `bash build.sh`. The build script will download all dependencies and build the Splunk Kafka Connector.

Note: The resulting "kafka-connect-splunk.tar.gz" package is self-contained. Bundled within it are the Kafka Connect framework, all 3rd party libraries and the Splunk Kafka Connector.

## Quick Start
1. [Start](https://kafka.apache.org/quickstart) your Kafka Cluster and Zookeeper locally. Confirm both are running.
2. If this is a fresh install, create a test topic (eg: `perf`). Inject events into the topic. This can be done using [Kafka data-gen-app](https://github.com/dtregonning/kafka-data-gen) or the Kafka bundled [kafka-console-producer](https://kafka.apache.org/quickstart#quickstart_send).
3. Untar the package created from the build script: `tar xzvf kafka-connect-splunk.tar.gz` (Default target location is /tmp/kafka-connect-splunk-build/kafka-connect-splunk).
4. Go to kafka-connect-splunk directory `cd kafka-connect-splunk`.
5. Adjust values for `bootstrap.servers` and `plugin.path` inside `config/connect-distributed-quickstart.properties` for your environment. Default values should work for experimentation.
6. Run `./bin/connect-distributed.sh config/connect-distributed-quickstart.properties` to start Kafka Connect.
7. Run the following command to create connector tasks. Adjust `topics`, `tasks.max`, `indexes`, `sources`, `sourcetypes` and `hec` settings as needed.

    ```
    curl localhost:8083/connectors -X POST -H "Content-Type: application/json" -d '{
    "name": "kafka-connect-splunk",
    "config": {
       "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
       "tasks.max": "3",
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
      }
    }'

    ```
8. Data should be flowing into Splunk. To validate, search Splunk using the index, sourcetype or soure from your config.
9. Use the following commands to check status and manage connector and tasks:

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
    Refer to [the Confluent doucumentation](https://docs.confluent.io/current/connect/managing.html#common-rest-examples) for more REST examples.
    

## Deployment
Splunk Kafka Connector can run in containers, virtual machines or physical machines. 
You can leverage any automation tools for deployment. 

Consider the following connector deployment options:
* Splunk Kafka Connector in a dedicated Kafka Connect Cluster (recommended)
* Splunk Kafka Connector in an existing Kafka Connect Cluster

### Connector in a dedicated Kafka Connect Cluster
This is the recommended deployment option. Isolating the Splunk connector from other Kafka connectors is more robust and will have significant performance benefits in high throughput environments. 

1. Untar the **kafka-connect-splunk.tar.gz** package and go to **kafka-connect-splunk** directory.

    ``` 
       tar xzvf kafka-connect-splunk.tar.gz
       cd kafka-connect-splunk
    ```

2. Update config/connect-distributed.properties to match your environment.

	```
	bootstrap.servers=<broker1:9092,broker2:9092,...>  # adjust this setting to brokers IP/hostname port
	```
3. Revise other optional settings in **config/connect-distributed.properties** as needed. 

Note: Changing replication factor and partition number is not recommended at this time.

Note: The below topics should be created by Kafka Connect when deploying the Splunk Connector. If the Kafka Connect cluster **does not have permission** to create these topics, create these manually before starting Kafka Connect cluster.

	```
	group.id=kafka-connect-splunk-hec-sink    # consumer group id of Kafka Connect, which is used to form a Kafka Connect cluster
	
	config.storage.topic=__kafka-connect-splunk-task-configs # kafka topic used to persistent connector task configurations
	config.storage.replication.factor=3
	
	offset.storage.topic=__kafka-connect-splunk-offsets # kafka topic used to persistent task checkpoints
	offset.storage.replication.factor=3
	offset.storage.partitions=25
	
	status.storage.topic=__kafka-connect-splunk-statuses # kafka topic used to persistent task statuses
	status.storage.replication.factor=3
	status.storage.partitions=5
	```
4. Deploy/Copy the **kafka-connect-splunk** directory to all target hosts(virtual machine, physical machine or container).
5. Start Kafka Connect on all target hosts by executing the following commands:
	
	```
	cd kafka-connect-splunk
	export KAFKA_HEAP_OPTS="-Xmx6G -Xms2G" && ./bin/connect-distributed.sh config/connect-distributed.properties >> kafka-connect-splunk.log 2>&1 
	```
	**Note:** Please pay attention to the **KAFKA_HEAP_OPTS** environment variable which controls how much memory Kafka Connect can use. 
	Setting the **KAFKA_HEAP_OPTS** with the value stated above is recommended.

### Connector in an existing Kafka Connect Cluster
1. Untar the **kafka-connect-splunk.tar.gz** installation package and go to the **kafka-connect-splunk** directory.

    ``` 
       tar xzvf kafka-connect-splunk.tar.gz
       cd kafka-connect-splunk
    ```
2. Copy the **conectors/kafka-connect-splunk-1.0-SNAPSHOT.jar** to plugin path specified by **plugin.path** in existing Kafka Connect on every host.
3. Copy **libs/commons-logging-1.2.jar** to **libs** of existing Kafka Connect on every host.
4. Restart the Kafka Connect cluster.

## Configuration
After Kafka Connect is brought up on every host, all of the Kafka Connect instances will form a cluster automatically. 
Even in a load balanced environment, a REST call can be executed against one of the cluster instances and rest of the instances will pick up the task automiatically.

### Configuration schema structure

Use the below schema to configure Splunk Kafka Connector
	
	```
	'{
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
      }
    }'
	
	```

### Parameters
##### Required Parameters
* `name` - Connector name. A consumer group with this name will be created with tasks to be distributed evenly across the connector cluster nodes. 
* `connector.class` - The Java class used to perform connector jobs. Keep the default value **com.splunk.kafka.connect.SplunkSinkConnector** unless you modify the connector.
* `tasks.max` -  The number of tasks generated to handle data collection jobs in parallel. The tasks will be spread (evenly) across all Splunk Kafka Connector nodes. 
* `splunk.hec.uri` -  Splunk HEC URIs. Either a list of FQDNs or IP's of all Splunk indexers separated by "," (The connector will load balance to indexer using round robin) or a load balancer. Splunk Connector will round robin to this list of indexers. 
```https://hec1.splunk.com:8088,https://hec2.splunk.com:8088,https://hec3.splunk.com:8088``` 
* `splunk.hec.token` -  [Splunk Http Event Collector token] (http://docs.splunk.com/Documentation/SplunkCloud/6.6.3/Data/UsetheHTTPEventCollector#About_Event_Collector_tokens).
* `topics` -  Comma separated list of Kafka topics for Splunk to consume. `prod-topic1,prod-topc2,prod-topic3`

##### Optional Parameters
* `splunk.indexes` - Target Splunk indexes to send data to. It can be a list of indexes which shall be the same sequence / order as topics.
    > Note: It is possible to inject data from different kafka topics to different splunk indexes. For Example prod-topic1,prod-topic2,prod-topic3 can be sent to index prod-index1,prod-index2,prod-index3. If the user would like to index all data from multiple topics to the main index, then "main" can be specified. Leaving this setting unconfigured will result in data being routed to the default index configured against the HEC token being used. Please make sure the indexes configured here are in the index list of HEC token, otherwise Splunk HEC will drop the data. By default, this setting is empty.
* `splunk.sources` -  Splunk event source metadata for Kafka topic data. Same configuration rules as indexes can be applied here. Leaving it unconfigured will result in the default source bound to the HEC token. By default, this setting is empty.
* `splunk.sourcetypes` - Splunk event source metadata for Kafka topic data. Same configuration rules as indexes can be applied here. Leaving it unconfigured will result in default source bound to HEC token. By default, this setting is empty.
* `splunk.hec.raw` - Set to `true` to ingest data into Splunk using the the /raw HEC endpoint. Default is `false`, which will use the /event endpoint.
* `splunk.hec.raw.line.breaker` - Only applicable to /raw HEC endpoint. The setting is used to specify a custom line breaker to help Splunk separate the events correctly. 
    > Note: For example, user can specify "#####" as a special line breaker. Internally the Splunk Kafka Connector will append this line breaker to every Kafka record to form a clear event boundary and the connector does data injection in batch mode. On Splunk side, user can configure **props.conf** to setup line breaker for the sourcetypes, then Splunk will correctly break events for data flowing through /raw HEC endpoint. For question "when I should specify line breaker and how", please go to FAQ section. By default, this setting is empty.
* `splunk.hec.json.event.enrichment` -  Only applicable to /event HEC endpoint. This setting is used to enrich raw data with extra metadata fields. It contains a list of key value pairs separated by ",". The configured enrichment meta data will be indexed along with raw event in Splunk. Please note, data enrichment for /event HEC endpoint is only available in Splunk Enterprise 6.5 onwards ([Documentation](http://dev.splunk.com/view/event-collector/SP-CAAAE8Y#indexedfield)). By default, this setting is empty. 
    > Note: For example `org=fin,bu=south-east-us`
* `splunk.hec.ack.enabled` -  Valid settings are `true` or `false`. When set to `true` the Splunk Kafka Connector will poll event ACKs for POST events before check-pointing the Kafka offsets. This is used to prevent data loss as this setting implements guaranteed delivery. By default, this setting is `true`.
    > Note: If this setting is enabled to `true`, please ensure the corresponding HEC token is also enabled with index acknowledgements otherwise the data injection will fail with duplicate data. When set to `false` the Splunk Kafka Connector will simply POST events to Splunk. After it receives a HTTP 200 OK response, it assumes the events are indexed by Splunk. Most of the time this is true; however in cases where Splunk crashes there will be some data loss. 
* `splunk.hec.ack.poll.interval` - This setting is only applicable when `splunk.hec.ack.enabled` is set to `true`. Internally it controls the event ACKs polling interval. By default, this setting is 10 seconds.
* `splunk.hec.ack.poll.threads` - This setting is used for performance tuning and is only applicable when `splunk.hec.ack.enabled` is set to `true`. It controls how many threads should be spawn to poll event ACKs. By default it is set to 1. 
    > Note: For large Splunk indexer clusters (E.g. 100 indexers) users will want to increase this number. Recommended increase is 4 threads to speed up ACK polling.
* `splunk.hec.ssl.validate.certs` - Valid settings are `true` or `false`. Enables or disables HTTPS certification validation. By default, this setting is "true".
* `splunk.hec.http.keepalive` - Valid settings are `true` or `false`. Enables or disables HTTP connection keep-alive. By default, it is `true`
* `splunk.hec.max.http.connection.per.channel` - Controls how many HTTP connections should be created and cached in the HTTP pool for one HEC channel. By default this setting is 2.
* `splunk.hec.total.channels` - Controls the total channels created to perform HEC event POSTs. Please refer to the Load balancer section for more details. By default this setting is 2.
* `splunk.hec.max.batch.size` - Maximum batch size when posting events to Splunk. The size is the actual number of Kafka events, it is not related to byte size. By default this setting is 100. 
* `splunk.hec.threads` - Controls how many threads are spawned to do data injection via HEC in a **single** connector task. By default this setting is 1.
* `splunk.hec.event.timeout` - This setting is only applicable when `splunk.hec.ack.enabled` is set to `true`. When events are POSTed to Splunk and before they are ACKed, this setting determines how long the connector will wait before timing out and resending. By default this setting is 120 seconds. 
* `splunk.hec.socket.timeout` - Internal TCP socket timeout when connecting to Splunk. By default this setting is 60 seconds.
* `splunk.hec.track.data` -  Valid settings are `true` or `false`. When set to `true`, data loss and data injection latency metadata will be indexed along with raw data. This setting will only work in conjunction with /event HEC endpoint (`"splunk.hec.raw" : "false"`). By default this setting is `false`.

##### Configuration Example
* Use the command below to create a connector called `splunk-prod-financial` for 10 topics and 10 parallelized tasks. The connector will use the /event HEC endpoint with acknowledgements enabled. The data is injected into a 3-server Splunk Indexer cluster.

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

* Use the command bewlow to update the connector to use 20 parallelized tasks.

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
	
* Use the command bewlow to delete the connector.

	```
	 curl <hostname>:8083/connectors/splunk-prod-financial-X DELETE
	```
	
## Load balancing
A common architecture will include a load balancer in front of the Splunk indexer cluster or a collection of Splunk Heavy Forwarders. If configured in this manner and HEC acknowledgement is enabled (`splunk.hec.ack.enabled:true`), care must be taken to ensure data ingestion behaves correctly:

1. Enable **sticky sessions** on the load balancer. Without this, data duplication may occur.
2. Set HEC channels (**splunk.hec.total.channels**) to a multiple of HEC endpoints (=indexers behind the load balancer). This will ensure the data flow is evenly load balanced across the Splunk indexers. 

> Note: Data duplication may occur even with sticky sessions, when requests are offloaded to a different endpoint under load.

## Benchmark Results
With the following testbed and raw HEC endpoint in use, a single Splunk Kafka Connector can reach maximum indexed throughput of **32 MB/second**. 

Hardware Specification: 
* **AWS:** EC2 c4.2xlarge, 8 vCPU and 31 GB Memory
* **Splunk Cluster:** 3 indexer cluster without Load balancer
* **Kafka Connect:** JVM heap size configuration is "-Xmx6G -Xms2G"
* **Kafka Connect resource usage:** ~6GB memory, ~3 vCPUs.
* **Kafka recrods size**:** 512 Bytes

## Scaling out your environment
Before scaling the Splunk Kafka Connector tier, ensure the bottleneck is indeed in the connector tier and not in another component.

Scaling out options:

1. Increase the number of parallel tasks. Only do this, if the hardware is under-utilized (low CPU, low memory usage and low data injection throughput). The user can reconfigure the connector with more tasks. Example above in the **Configuration parameters**-**Update** section.

2. Increase hardware resources on cluster nodes in case of resource exhaustion, such as high CPU or high memory usage.

3. Increase the number of Kafka Connect nodes.

## Data loss and latency monitoring
When creating a Splunk Kafka Connector by using the REST API, `"splunk.hec.track.data": "true"` can be configured to allow data loss tracking and data collection latency monitoring. 
This is accomplished by enriching the raw data with **offset, timestamp, partition, topic** metadata.

##### Data Loss Tracking
The Splunk Kafka Connector uses offset to track data loss since offsets in a Kafka topic partition are sequential. If a gap is observed in Splunk, there is data loss.

##### Data Latency Tracking
The Splunk Kafka Connector uses the timestamp of the record to track the time elapsed between the time a Kafka record was created and the time the record was indexed in Splunk.

> Note: This setting will only work in conjunction with /event HEC endpoint (`"splunk.hec.raw" : "false"`)

## FAQ
1. When should I use HEC acknowledgements?

	If avoiding data loss is a must, enable HEC token acknowledgements. Without HEC acknowledgement, data loss may occur, especially in case of system restart or crash.

2. When should I use /raw HEC endpoint and /event HEC endpoint?

	If raw events need go through Splunk's index time extraction to use features like timestamp extraction, data manipulation etc for raw data, you will need use HEC's /raw event endpoint. 
	When using the /raw HEC endpoint and when your raw data does not contain timestamp or contains multiple timestamps or carriage returns, you may want to configure the **splunk.hec.raw.line.breaker** and setup a corresponding **props.conf** inside Splunk to honor this line breaker setting. This will assist Splunk to do event breaking. 
	Here is one example:
	
	In connection configuration, it sets **"splunk.hec.raw.line.breaker":"####"** for sourcetype "s1"
	
	In **props.conf**, user can setup the line breaking as follows.
	
	```
	[s1] # sourcetye name
	LINE_BREAKER = (####)
	SHOULE_LINEMERGE = false
	```
	
   If you don't care about the timestamp or by default the auto assigned timestamp is good enough, then stick to /event HEC endpoint.
	
4. How many tasks should I configure ?
	
	Generally speaking, creating 2 * CPU tasks per Splunk Kafka Connector is good but don't create more tasks than the number of partitions. 
	> Note: For example, assume there are 5 Kafka Connects running the Splunk Kafka Connector. Each host is 8 CPUs and 16 GB memory. And there are 200 partitions to collect data from. max.tasks will be: max.tasks = 2 * CPUs/host * Kafka Connect instances = 2 * 8 * 5 = 80 tasks. On the other hand if there are only 60 partitions to consume from, then just setting max.tasks to 60, otherwise the other 20 will be pending there doing nothing.
	
5. How many Kafka Connect instances should I deploy ?

	This is highly dependent on how much volume per day the Splunk Kafka Connector needs to index in Splunk. In general on an 8 CPU, 16 GB memory machine it can potentially achieve 50 ~ 60 MB/s throughput from Kafka into Splunk if Splunk is sized correctly. 
	
6. How can I track data loss and data collection latency ?

	Please refer to the **Data loss and latency monitoring** section.
	
7. Is there a recommended deployment architecture ?

	There are 2 typical architectures. 
	
	* Setting up a Heavy Forwarder layer in front of Splunk Indexer Cluster to offload data injection load to Indexer Cluster. Setting up a Heavy forwarder layer has performance benefits for Splunk Search.
	
		Kafka Connect Cluster (in containers or virtual machines or physical machines) -> Heavy Forwarders (HEC) -> Splunk Indexer Cluster
	
	* Direct inject data to Splunk Indexer cluster

		Kafka Connect Cluster (in containers or virtual machines or physical machines) -> Splunk Indexer Cluster (HEC)
	

## Troubleshooting
1. Append **log4j.logger.com.splunk=DEBUG** to **config/connect-log4j.properties** file to enable more verbose logging for Splunk Kafka Connector.
2. Kafka connect is encountering "out of memory" error. Remember to export environment variable **KAFKA\_HEAP\_OPTS="-Xmx6G -Xms2G"**. Refer to **Deploy** section for more details.
