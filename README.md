## kafka-connect-splunk
Splunk Kafka Connector is a Sink connector which supports pulling data from Kafka topics, do data transformation, enrichement and then inject the data to Splunk via Splunk HTTP Event Collector.

## Constraints
1. Only support Kafka version 0.9 above
2. Java8 above


## Build
1. Clone the https://github.com/splunk/kafka-connect-splunk
2. Make sure Java8 JRE or JDK is installed
3. Run `bash build.sh`. Build script will build Splunk Kafka Connector, download all dependencies, the final ".tar.gz" package is self-contained which means it contains Kafka Connect framework, 3rd party libs and Splunk Kafka Connector 


## Quick start in single machine
1. [Start up](https://kafka.apache.org/quickstart) Kafka Cluster and Zookeeper locally and create a test topic (eg: `perf`). If you have already had a cluster, then use the existing cluster is fine.
2. Inject events to `perf` topic for example using [Kafka data-gen-app](https://github.com/dtregonning/kafka-data-gen) or use kafka-console-consumer
3. Untar the package, `tar xzvf kafka-connect-splunk.tar.gz`
4. Enter `kafka-connect-splunk`
5. Adjust `bootstrap.servers` and `plugin.path` in `config/connect-distributed-quickstart.properties` for your environment if necessary. Default values should be fine for experiment
6. Run `./bin/connect-distributed.sh config/connect-distributed-quickstart.properties` to start Kafka Connect
7. Run the following command to create a connector tasks. Adjust the `topics`, `tasks.max`, `indexes`, `sources`, `sourcetypes` and `hec` etc settings if necessary

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
8. If everything is fine, data should flow to splunk
9. Run the following commands to check the status and manage the connector and tasks

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

    Refer to [this](https://docs.confluent.io/current/connect/managing.html#common-rest-examples) for more examples.
    ```

## Deployment
User can leverage any automation tools to deploy Splunk Kafka Connector. Depending on the tools used, details deployment steps will be probably different. Splunk Kafka Connector can run in containers or in virtual machines or in physical machines. 

And depending on if there is alraedy having Kafka Connect cluster running in production and if user would like to deploy Splunk Kafka Sink Connector to existing Kafka Connect cluster, there are 2 deployment considerations as described in detail below.

### Deploy Splunk Kafka Connector from scratch (recommended)
It is recommended to deploy Splunk Kafka Connector in a separated Kafka Cluster when the data volume is high in production. Detail steps are below.

1. Untar **kafka-connect-splunk.tar.gz** installation package and enter  **kafka-connect-splunk** directory
2. Revise the following settings in config/connect-distributed.properties according to production env, it is mandantory. 

	```
	bootstrap.servers=<broker1:9092,broker2:9092,...>  # adjust this setting to brokers IP/hostname port
	```
3. Revise other optional settings in **config/connect-distributed.properties** if necessary. Keeping them (replication factor and partition number) as default is generally good since they are recommended settings according to best practice. Please note these topics will be created by Kafka Connect cluster automatically if they are not in Kafka cluster. If Kafka Connect cluster has **no permission** to create these topics due to ACL control for instance, user needs create them in Kafka cluster before starting Kafka Connect Cluster.

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
4. Deploy **kafka-connect-splunk** whole directory to target hosts (virtual machine, physical box or container)
5. Start the connector on all target hosts by leveraging deployment tools by executing the following commands. Please pay attention to the **KAFKA_HEAP_OPTS** environment variable which controls how much memory the Kafka Connect can use. Setting the **KAFKA_HEAP_OPTS** with the value stated below is generally good.
	
	```
	cd kafka-connect-splunk
	export KAFKA_HEAP_OPTS="-Xmx6G -Xms2G" && ./bin/connect-distributed.sh config/connect-distributed.properties >> kafka-connect-splunk.log 2>&1 
	```

### Deploy Splunk Kafka Connetor to existing Kafka Connect Cluster

1. Untar **kafka-connect-splunk.tar.gz** installation package and enter **kafka-connect-splunk** directory
2. Copy the **conectors/kafka-connect-splunk-1.0-SNAPSHOT.jar** to plugin path sepcified by **plugin.path** in existing Kafka Connect on every host.
3. Copy **libs/commons-logging-1.2.jar** to **libs** of existing Kafka Connect on every host.
4. Restart Kafka Connect Cluster

## Configuration

After Kafka Connect is brought up on every host, all of the Kafka Connect instance will form a cluster automatically. User can talk to one of them for Splunk connector / task configuration. If there is a Load balancer setup in front of the Kafka Connect Cluster, then load balance hostname/ip can be used for configuration.

### Prequisitives

Splunk Http Event Collector (HEC) tokens should be configured in Splunk Indexers or Heavy Forwarders before hand. Please note: HEC token can be enabled with acknowledgements. When HEC token was enabled with acknowledgements, the task configuration parameters are a little bit different than the HEC without acknowledgements. Enabling acknowledgements will prevent potentional data loss but may be not optimal when injecting events. Please also note that, HEC token should be same on all Splunk Indexers or Heavy Forwarders.

### Configuration parameters

1. There are a bunch of settings user can specify. Depending on use cases, the exact configuration will be a little bit different. Following is a task JSON schema which contains all possible settings.  The meaning of the configuration parameters are explained afterwards below.
	
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
	* **name**: Mandatory setting. connector name. Connector name is a very important parameter, since it was used form a consumer group. Different connector names are forming different consumer groups. For example, when user creates a connector named `splunk-prod-financial` with **tasks.max** setting to 30, and there are 3 Kafka Connect in the cluster, then each of the Splunk Kafka Connector in each Kafka Connect will probably get 10 tasks, and all of the 30 tasks will be in the same consumer group even they are spread accross different boxes / containers. If one Kafka Connect is down, the tasks on that node will be redispatched to other 2 nodes automatically. 
	* **connector.class**: Mandatory setting. Entry point Java class which is used to do connector job. **com.splunk.kafka.connect.SplunkSinkConnector** is for this Splunk Kafka Connector.
	* **tasks.max**: Mandatory setting. Number of tasks are generated to handle data collection jobs in parrallel. The tasks will be spread (evenly) accross all Splunk Kafka Connectors. 
	* **splunk.hec.uri**: Mandatory setting. Splunk HEC URIs. Can be a list of them separated by ",". For example, https://hec1.splunk.com:8088,https://hec2.splunk.com:8088,https://hec3.splunk.com. Please construct a complete list of all HEC URIs on all Splunk indexers, then internally the Splunk Connector will round robin to this list of indexers. 
	* **splunk.hec.token**: Mandatory setting. Splunk HEC token.
	* **topics**: Mandatory setting. Kafka topics which are pulled data from. It can be a list of topics separated by ",". For example, prod-topic1,prod-topc2,prod-topic3
	* **splunk.indexes**: Splunk indexs which are injecting data to. It can be a list of indexes which shall be the same sequence / order as topics. For example, if user would like to inject data from prod-topic1,prod-topc2,prod-topic3 to index prod-index1,prod-index2,prod-index3, then prod-index1,prod-index2,prod-index3 can be setup. Anohter example is if user would like index all data from prod-topic1,prod-topc2,prod-topic3 to main index, then "main" index can be specified. Leaving it non-configured, the data will be routed to default index bound to the HEC token. Please note make sure the indexes configured here are in the index list of HEC token, otherwise Splunk HEC will reject the data. By default, this setting is empty.
	* **splunk.sources**: Splunk event source metadata for Kafka topic data. Same configuration rules as indexes can be applied here. Leaving it non-configured result in default source bound to HEC token. By default, this setting is empty.
	* **splunk.sourcetypes**: Splunk event source metadata for Kafka topic data. Same configuration rules as indexes can be applied here. Leaving it non-configured result in default source bound to HEC token. By default, this setting is empty.
	* **splunk.hec.raw**: Valid settings are "true" or "false". When setting to "true", it means use /raw HEC endpoint to do data injection, otherwise use /event HEC endpoint to do data injection. For question "when I should choose which HEC endpoint", please see the FAQ section. By default, this setting is "false".
	* **splunk.hec.raw.line.breaker**: This setting is only applicable to /raw HEC endpoint. It gives user a chance to specify a special line breaker to help Splunk break the event correctly. For example, user can specify "#####" as a specfical line breaker for some sourcetypes, internally Splunk Kafka Connector will append this line breaker to every Kafka record to form a clear event boundry and the connector does data injection in batch mode. On Splunk side, user can configure **props.conf** to setup line breaker for the sourcetypes, then Splunk will correctly break events for data flowing through /raw HEC endpoint. For question "when I should specify line breaker and how", please go to FAQ section. By default, this setting is empty.
	* **splunk.hec.json.event.enrichment**: This setting is only applicable to /event HEC endpoint. It gives user a chance to enrich their raw data. It contains a list of key value pairs separated by ",", for example, "org=fin,bu=sourth-east-us". The configured enrichement meta data will be indexed along with raw event in Splunk. Please note, data enrichment for /event HEC endpoint is only available in Splunk Enterprise 6.5 onwards. For more information, please refer to http://dev.splunk.com/view/event-collector/SP-CAAAE8Y#indexedfield. By default, this setting is empty.
	* **splunk.hec.ack.enabled**: Valid settings are "true" or "false". When setting to "true", it means Splunk Kafka Connector will poll event ACKs for POST events before checkpointing the Kafka offsets to prevent data loss. Please make sure the corresponding HEC token is also enabled with acknowledgements, otherwise the data injection will fail with duplicate data. When setting "false", it means Splunk Kafka Connector just does POST events to Splunk. After it recieves HTTP 200 OK, it assume the events are indexed by Splunk. Most of the time, this is true. But some cases like Splunk crashes, there will be some data loss. By default, this setting is "true".
	* **splunk.hec.ack.poll.interval**: This setting is only applicable to HEC token with acknowledgements enabled. Internally it controls the event ACKs polling internval. By default, this setting is 10 seconds.
	* **splunk.hec.ack.poll.threads**: This setting is used for performance tuning and only applicable to HEC token with acknowledgements enabled. It controls how many threads should be spawn to poll event ACKs. By default it is 1. For large indexer cluster env say 100 indexers, user may want bump up this number, say tune it to 4 threads to spead up the ACK polling
	* **splunk.hec.ssl.validate.certs**: Valid setting is "true" or "false". Enable or disalbe HTTPS cerfication validation. By default, this setting is "true".
	* **splunk.hec.http.keepalive**: Valid setting is "true" or "false". Enable or disable HTTP connection keepalive. By default, it is "true"
	* **splunk.hec.max.http.connection.per.channel**: Controls how many http connection should be created an cached in the HTTP pool for one HEC channel. By default, this setting is 2.
	* **splunk.hec.total.channels**: Controls total channels created to do HEC event POST. Usually it works in a loadbalancer env. Please refer to load balancer section for more details. By default, this setting is 2.
	* **splunk.hec.max.batch.size**: Max batch size when posting events to Splunk. The size is counting against Kafka event instead of the "bytes". By default, this setting is 100. 
	* **splunk.hec.threads**: Controls how many threads are spawn to do data injection to HEC in **single** connector task. Default default, this setting is 1.
	* **splunk.hec.event.timeout**: This setting is only applicable to HEC token with acknowledgements enabled. When events are POSTed to Splunk, before they are ACKed, how long time the connector wait before deciding the events are timed out and need resend them. By default, this setting is 120 seconds. 
	* **splunk.hec.socket.timeout**: Internal TCP socket timeout when connecting to Splunk. By default, this setting is 60 seconds.
	* **splunk.hec.track.data**:  Valid setting is "true" or "false". When it is setup as "true", data loss and data injection latency metadata will be indexed along with raw data. It only works with /event HEC endpoint. By default, this setting is "false".

2. **Create** a connector by using the above schema. In example below, it creates a connector called `splunk-prod-financial` for 10 topics with 10 in parallel tasks by using /event HEC endpoint with acknowledgements enabled. The data is injected to a 3 indexers cluster.

	
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

3. **Update** a connector with new configuration. In example below, it updates the connector configuration with 20 tasks.

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
	
4. **Delete** a connector can be done through a REST API. In example below, it deletes splunk-prod-financial connector.

	```
	 curl <hostname>:8083/connectors/splunk-prod-financial-X DELETE
	```
	
## Loadbanacer
If there is a load balancer set up infront of the indexer cluster or in front of a Heavy Forwarders in production. And end user would like to do data injection through the load balancer and if user at the same time needs to enable HEC with acknowledgements. Some spefical care need paid attention to for this scenario. For load balancer and HEC token without acknowledgements cases, we are fine.

There are 2 key things here:

1. Enable **sticky session** for load balancer. This is critical. Without **sticky session** enabled, the data injection will probably fail with duplicate data.
2. Do correct caculation for **splunk.hec.total.channels** setting. Assume there are N indexers or N Heavy Forwarders behind the load balancer. The **splunk.hec.total.channels** should be set to N or 2 * N. This way, internally the Splunk Kafka Connector will create this number of HEC channels and it can then route the HEC event POSTs to the indexers or Heavy Forwarders in a load balance way.

Using a load balancer is not quite recommended since **sticky session** impacts load balancer itself and sometime can cause duplicate date when load balancer extremely needs offload load to other hosts. 

## Sizing Guide

With the following testing bed and raw event setup, intial basic performance testing shows single Splunk Kafka Connector can reach around 32MB/second maximum. 

Hardware spec: EC2 c4.2xlarge, 8 vCPU and 31 GB memory
Splunk Cluster: 3 indexer cluster without Load balancer
Kafka Connect: JVM heapsize configuration is "-Xmx6G -Xms2G"
Kafka COnnect resource usage: ~6GB memory, ~3 vCPUs.


## Scale
In production, if user woud like to scale Splunk Kafka Connector to handle more load. There are 2 ways. Please note before scaling the connectors, please also make sure the bottleneck is in Splunk Kafka Connector. If the bottleneck is in Splunk (splunk provision is too small), we will need scale Splunk provision first.

1. Reconfigure with more tasks to scale up the data injection. If hardware resources which are allocated to run nk Splunk Kafka Connector is enough (say observe low CPU, memory usage and low data injection throughput), user can reconfigure the connector with more tasks as **Update** bulltin item as in **Configuration parameters** section mentioned.

2. If tasks are enough, and high CPU, memory usage are observed, user can scale out the data injection with more Kafka Connect instances by provisioning more hardware resources say more virtual machine, physical machine etc. 

## Data loss and latency monitoring

When creating Splunk Kafka Connector by using REST API, in the settings, user can specify **"splunk.hec.track.data": "true"** to enable data loss tracking and data collection latency monitoring. 

The machenism behind is enrich the raw data with **offset, timestamp, partition, topic** metadata of Kafka records, Splunk Kafka Connector uses offset to track data loss since offsets in a partition is sequential, if there is gap in Splunk, there is data loss. It use timestamp of the record to track the duration between the time a Kafka record was created and the time the record was indexed in Splunk.

Please note this data loss and latency monitoring is only applicable to /event HEC endpoint.

## FAQ
1. When should I use HEC acknowledgements ?

	If avoiding data loss is a must, please enable HEC acknowledgements for token. HEC without acknowledgements doesn't necessary mean there is always data loss, but at the event of Splunk crash, restart etc, there might slightly some data loss.

2. When should I use /raw HEC endpoint and /event HEC endpoint ?

	If your raw events need go through Splunk index time extraction like timestamp extraction, data manipulation etc for raw data, propbably you will need use /raw event. Please note that when using /raw HEC endpoint and when your raw data doesn't contain timestamp or contains multiple timestamps or carriage returns etc, you probably also would like to setup the **splunk.hec.raw.line.breaker** and setup a corresponding **props.conf** to honor this line breaker setting, this will help Splunk correctly do event breaking. Here is one example:
	
	In connectro configuration, it sets **"splunk.hec.raw.line.breaker":"####"** for sourcetype "s1"
	
	In **props.conf**, user can setup the line breaking as follows.
	
	```
	[s1] # sourcetye name
	LINE_BREAKER = (####)
	SHOULE_LINEMERGE = false
	```
	
	On the other side, if you don't care about the timestamp or by default the auto assigned timestamp is good enough, then stick to /event HEC endpoint.
	
4. How many tasks should I configure ?
	
	Depending on how many Kafka topic partitions the connector is consuming from and how many Kafka Connect in the cluster and the hardware spec which runs Splunk Kafka Connector. Generally speaking, creating 2 * CPU tasks per Splunk Kafka Connector is good to go but don't create more than number of partitions tasks. For example, assume there are 5 Kafka Connect running Splunk Kafka Connector. Each host is 8 CPUs and 16 GB memory. And there are 200 partitions to collect data from. max.tasks will be: max.tasks = 2 * CPUs/host * Kafka Connect instances = 2 * 8 * 5 = 80 tasks. On the other hand if there are only 60 partitions to consume from, then just setting max.tasks to 60, otherwise the other 20 will be pending there doing nothing.
	
	
5. How many Kafka Connect instances should I deploy ?

	High depending on how much volume per day Splunk Kafka Connector needs pull data and index to Splunk. In general, on a 8 CPUs, 16 GB memory box, it can possibly achieve 50 ~ 60 MB/s for pulling data from Kafka topic and then index them to Splunk if downstream Splunk cluster size is big enough.
	
6. How can I track data loss and data collection latency ?

	Please refer to **Data loss and latency monitoring** section
	
7. Is there a recommended deployment architecture ?

	There are 2 typical architectures. Setting up a Heavy forwarder layer has performance benifits for Splunk Search.
	
	* Setting up a Heavy Forwarder layer in front of Splunk Indexer Cluster to offload data injection load to Indexer Cluster
	
		Kafka Connect Cluster (in containers or virtual machines or physical machines) -> Heavy Forwarders (HEC) -> Splunk Indexer Cluster
	
	* Direct inject data to Splunk Indexer cluster

		Kafka Connect Cluster (in containers or virtual machines or physical machines) -> Splunk Indexer Cluster (HEC)
	

## Troubleshootings

Append **log4j.logger.com.splunk=DEBUG** to **config/connect-log4j.properties** file to enable more verbose logging for Splunk Kafka Connector