## Kafka Connect Splunk

A Kafka Connect Sink for Splunk features:

* Data ingestion from Kafka topics into Splunk via [Splunk HTTP Event Collector(HEC)](http://dev.splunk.com/view/event-collector/SP-CAAAE6M).
* In-flight data transformation and enrichment.

## Requirements
1. Kafka version 0.9 and above.
2. Java 8 and above.
3. A Splunk platform instance configured with valid HTTP Event Collector (HEC) tokens.
	
	* HEC token settings should be the same on all Splunk Indexers and Heavy Forwarders in your environment.
	* Task configuration parameters will vary depending on acknowledgement setting (See the [Configuration](#configuration) section for details).

	Note: HEC Acknowledgement prevents potential data loss but may slow down event ingestion. 


## Build

1. Clone the repo from https://github.com/splunk/kafka-connect-splunk
2. Verify that Java8 JRE or JDK is installed.
3. Run `bash build.sh`. The build script will download all dependencies and build the Splunk Kafka Connector.

Note: The resulting "kafka-connect-splunk.tar.gz" package is self-contained. Bundled within it are the Kafka Connect framework, all 3rd party libraries, and the Splunk Kafka Connector.

## Quick Start

1. [Start](https://kafka.apache.org/quickstart) your Kafka Cluster and Zookeeper on your local host. Confirm both are running.
2. If this is a new install, create a test topic (eg: `perf`). Inject events into the topic. This can be done using [Kafka data-gen-app](https://github.com/dtregonning/kafka-data-gen) or the Kafka bundle [kafka-console-producer](https://kafka.apache.org/quickstart#quickstart_send).
3. Untar the package created from the build script: `tar xzvf kafka-connect-splunk.tar.gz` (Default target location is /tmp/kafka-connect-splunk-build/kafka-connect-splunk).
4. Navigate to kafka-connect-splunk directory `cd kafka-connect-splunk`.
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
   "splunk.hec.event.timeout": "60",
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
Splunk Kafka Connector can run in containers, virtual machines or on physical machines. 
You can leverage any automation tools for deployment. 

Use the following connector deployment options:
* Splunk Kafka Connector in a dedicated Kafka Connect Cluster (recommended)
* Splunk Kafka Connector in an existing Kafka Connect Cluster

### Connector in a dedicated Kafka Connect Cluster
Running the Splunk Kafka Connector in a dedicated Kafka Connect Cluster is recommended. Isolating the Splunk connector from other Kafka connectors results in significant performance benefits in high throughput environments. 

1. Untar the **kafka-connect-splunk.tar.gz** package and navigate to the **kafka-connect-splunk** directory.

    ``` 
    tar xzvf kafka-connect-splunk.tar.gz
    cd kafka-connect-splunk
    ```

2. Update config/connect-distributed.properties to match your environment.

	```
	bootstrap.servers=<broker1:9092,broker2:9092,...>  # adjust this setting to the brokers' IP/hostname port
	```
3. Revise other optional settings in **config/connect-distributed.properties** as needed. 

	> Note: Do not change the replication factor and partition number at this time.
	
	> Note: The below topics should be created by Kafka Connect when deploying the Splunk Connector. If the Kafka Connect cluster **does not have permission** to create these topics, create these manually before starting Kafka Connect cluster.

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
	
4. Deploy/Copy the **kafka-connect-splunk** directory to all target hosts (virtual machines, physical machines or containers).
5. Start Kafka Connect on all target hosts using the below commands:
	
	```
	cd kafka-connect-splunk
	export KAFKA_HEAP_OPTS="-Xmx6G -Xms2G" && ./bin/connect-distributed.sh config/connect-distributed.properties >> kafka-connect-splunk.log 2>&1 
	```
	> Note: The **KAFKA\_HEAP\_OPTS** environment variable controls how much memory Kafka Connect can use. Set the **KAFKA\_HEAP\_OPTS** with the recommended value stated in the example above.

### Connector in an existing Kafka Connect Cluster
1. Untar the **kafka-connect-splunk.tar.gz** installation package and go to the **kafka-connect-splunk** directory.

    ``` 
    tar xzvf kafka-connect-splunk.tar.gz
    cd kafka-connect-splunk
    ```
2. Copy the **conectors/kafka-connect-splunk-1.0-SNAPSHOT.jar** to the plugin path specified by **plugin.path** in the existing Kafka Connect on every host.
3. Copy **libs/commons-logging-1.2.jar** to **libs** of the existing Kafka Connect on each host.
4. Restart the Kafka Connect cluster.

## Security
The Kafka Connect Splunk Sink supports the following security mechanisms
* `SSL`
* `SASL/GSSAPI (Kerberos)` - starting at version 0.9.0.0
* `SASL/PLAIN` - starting at version 0.10.0.0
* `SASL/SCRAM-SHA-256 and SASL/SCRAM-SHA-512` - starting at version 0.10.2.0

See [Confluent's documentation](https://docs.confluent.io/current/connect/security.html#security) to understand the impact of using security within the Kafka Connect framework, specifically [ACL considerations](https://docs.confluent.io/current/connect/security.html#acl-considerations).

The following examples assume you're deploying to an [existing Kafka Connect cluster](#connector-in-an-existing-kafka-connect-cluster) or a [dedicated Kafka Connect cluster](#connector-in-a-dedicated-kafka-connect-cluster).

If you are using [Quick Start](#quick-start), adjust the config file to **config/connect-distributed-quickstart.properties**.

### SSL
This section documents how to configure Kafka Connect if your Kafka Cluster is secured using [SSL](http://kafka.apache.org/documentation/#security_ssl).

Configure the Kafka Connect worker and consumer settings to use SSL in **config/connect-distributed.properties**

```
# Worker security are located at the top level
security.protocol=SSL
ssl.truststore.location=/var/private/ssl/kafka.client.truststore.jks
ssl.truststore.password=test1234

# Sink security settings are prefixed with "consumer."
consumer.security.protocol=SSL
consumer.ssl.truststore.location=/var/private/ssl/kafka.client.truststore.jks
consumer.ssl.truststore.password=test1234
```

> Note: You will need to adjust the settings **consumer.ssl.truststore.location** and **ssl.truststore.password** to reflect your setup.

> Note: As of now, there is no way to change the configuration for connectors individually, but if your server supports client authentication over SSL, it is possible to use a separate principal for the worker and the connectors. See [Confluent's documentation on configuring workers and connectors with security](https://docs.confluent.io/current/connect/security.html#configuring-connectors-with-security) for more information.

Start Kafka Connect

```
./bin/connect-distributed.sh config/connect-distributed-quickstart.properties
```

Workers and sink tasks should work with your SSL secured cluster.

### SASL/GSSAPI (Kerberos)
This section documents how to configure Kafka Connect if your Kafka Cluster is secured using [Kerberos](http://kafka.apache.org/documentation/#security_sasl_kerberos). 

Configure the Kafka Connect worker and consumer settings to use Kerberos in **config/connect-distributed.properties**

```
# Worker security are located at the top level
security.protocol=SASL_PLAINTEXT
sasl.mechanism=GSSAPI

# Sink security settings are prefixed with "consumer."
consumer.sasl.mechanism=GSSAPI
consumer.security.protocol=SASL_PLAINTEXT
sasl.kerberos.service.name=kafka
```

Modify **bin/connect-distributed.sh** by editing the `EXTRA_ARGS` environment variable. Pass in the location of the JAAS conf file. Optionally, you can specify the path to your Kerberos config file and set Kerberos debugging to true for troubleshooting connection issues.

```
EXTRA_ARGS=${EXTRA_ARGS-'-name connectDistributed -Djava.security.krb5.conf=/etc/krb5.conf -Djava.security.auth.login.config=/root/kafka_connect_jaas.conf -Dsun.security.krb5.debug=true'}
```

See [Confluent's documentation](https://docs.confluent.io/current/kafka/sasl.html#sasl-configuration-for-kafka-clients) for more information on configuring Kafka Connect using JAAS.

For example, a Kafka Client JAAS file using the principal `connect`.

```
KafkaClient {
	com.sun.security.auth.module.Krb5LoginModule required
	useKeyTab=true
	storeKey=true
	keyTab="/etc/security/keytabs/connect.keytab"
	principal="connect/_HOST@REALM";
};

```
> Note: Modify the **keyTab** and **principal** settings to reflect your environment.

Start Kafka Connect 

```
./bin/connect-distributed.sh config/connect-distributed.properties
```

Workers and sink tasks should work with your Kerberos secured cluster.

### SASL/PLAIN
> Warning: Do not run SASL/PLAIN in produciton without SSL. See [Confluent's documentation](https://docs.confluent.io/current/kafka/sasl.html#use-of-sasl-plain-in-production) for details.

This section documents how to configure Kafka Connect if your Kafka Cluster is secured using [SASL/PLAIN](http://kafka.apache.org/documentation/#security_sasl_plain).

Configure the Kafka Connect worker and consumer settings to use SASL/PLAIN in **config/connect-distributed.properties**

```
# Worker security are located at the top level
security.protocol=SASL_SSL
sasl.mechanism=PLAIN

# Sink security settings are prefixed with "consumer."
consumer.security.protocol=SASL_SSL
consumer.sasl.mechanism=PLAIN
```

Modify **bin/connect-distributed.sh** by editing the `EXTRA_ARGS` environment variable. Pass in the location of the JAAS conf file.

```
EXTRA_ARGS=${EXTRA_ARGS-'-name connectDistributed -Djava.security.auth.login.config=/root/kafka_connect_jaas.conf'}
```

See [Confluent's documentation](https://docs.confluent.io/current/kafka/sasl.html#sasl-configuration-for-kafka-clients) for more information on configuring Kafka Connect using JAAS.

For example, a Kafka Client JAAS file for SASL/PLAIN.

```
KafkaClient {
  org.apache.kafka.common.security.plain.PlainLoginModule required
  username="alice"
  password="alice-secret";
};
```

Start Kafka Connect

```
./bin/connect-distributed.sh config/connect-distributed.properties
```

Workers and sink tasks should work with your SASL/PLAIN secured cluster.

### SASL/SCRAM-SHA-256 and SASL/SCRAM-SHA-512

This section documents how to configure Kafka Connect if your Kafka Cluster is secured using [SASL/SCRAM](http://kafka.apache.org/documentation/#security_sasl_scram). 

Configure the Kafka Connect worker and consumer settings to use SASL/SCRAM in **config/connect-distributed.properties**

```
# Worker security are located at the top level
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-256 (or SCRAM-SHA-512)

# Sink security settings are prefixed with "consumer."
consumer.security.protocol=SASL_SSL
consumer.sasl.mechanism=SCRAM-SHA-256 (or SCRAM-SHA-512)
```
Modify **bin/connect-distributed.sh** by editing the `EXTRA_ARGS` environment variable. Pass in the location of the JAAS conf file.

```
EXTRA_ARGS=${EXTRA_ARGS-'-name connectDistributed -Djava.security.auth.login.config=/root/kafka_connect_jaas.conf'}
```

See [Confluent's documentation](https://docs.confluent.io/current/kafka/sasl.html#sasl-configuration-for-kafka-clients) for more information on configuring Kafka Connect using JAAS.

For example, a Kafka Client JAAS file for SASL/SCRAM.

```
KafkaClient {
  org.apache.kafka.common.security.scram.ScramLoginModule required
  username="alice"
  password="alice-secret";
};
```

Start Kafka Connect

```
./bin/connect-distributed.sh config/connect-distributed.properties
```

Workers and sink tasks should work with your SASL/SCRAM secured cluster.

## Configuration

After Kafka Connect is brought up on every host, all of the Kafka Connect instances will form a cluster automatically. 
Even in a load balanced environment, a REST call can be executed against one of the cluster instances, and rest of the instances will pick up the task automatically.

### Configuration schema structure
Use the below schema to configure Splunk Kafka Connector

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
  }
}
```

### Parameters

#### Required Parameters
* `name` - Connector name. A consumer group with this name will be created with tasks to be distributed evenly across the connector cluster nodes. 
* `connector.class` - The Java class used to perform connector jobs. Keep the default value **com.splunk.kafka.connect.SplunkSinkConnector** unless you modify the connector.
* `tasks.max` -  The number of tasks generated to handle data collection jobs in parallel. The tasks will be spread evenly across all Splunk Kafka Connector nodes. 
* `splunk.hec.uri` -  Splunk HEC URIs. Either a list of FQDNs or IPs of all Splunk indexers, separated with a ",", or a load balancer. The connector will load balance to indexers using round robin. Splunk Connector will round robin to this list of indexers. 
```https://hec1.splunk.com:8088,https://hec2.splunk.com:8088,https://hec3.splunk.com:8088``` 
* `splunk.hec.token` -  [Splunk Http Event Collector token] (http://docs.splunk.com/Documentation/SplunkCloud/6.6.3/Data/UsetheHTTPEventCollector#About_Event_Collector_tokens).
* `topics` -  Comma separated list of Kafka topics for Splunk to consume. `prod-topic1,prod-topc2,prod-topic3`

#### General Optional Parameters
* `splunk.indexes` - Target Splunk indexes to send data to. It can be a list of indexes which shall be the same sequence / order as topics.
    > Note: It is possible to inject data from different kafka topics to different splunk indexes. For example, prod-topic1,prod-topic2,prod-topic3 can be sent to index prod-index1,prod-index2,prod-index3. If you would like to index all data from multiple topics to the main index, then "main" can be specified. Leaving this setting unconfigured will result in data being routed to the default index configured against the HEC token being used. Verify the indexes configured here are in the index list of HEC tokens, otherwise Splunk HEC will drop the data. By default, this setting is empty.
* `splunk.sources` -  Splunk event source metadata for Kafka topic data. The same configuration rules as indexes can be applied. If left unconfigured, the default source binds to the HEC token. By default, this setting is empty.
* `splunk.sourcetypes` - Splunk event source metadata for Kafka topic data. The same configuration rules as indexes can be applied here. If left unconfigured, the default source binds to the HEC token. By default, this setting is empty.
* `splunk.hec.ssl.validate.certs` - Valid settings are `true` or `false`. Enables or disables HTTPS certification validation. By default, it is set to `true`.
* `splunk.hec.http.keepalive` - Valid settings are `true` or `false`. Enables or disables HTTP connection keep-alive. By default, it is set to `true`
* `splunk.hec.max.http.connection.per.channel` - Controls how many HTTP connections will be created and cached in the HTTP pool for one HEC channel. By default, it is set to `2`.
* `splunk.hec.total.channels` - Controls the total channels created to perform HEC event POSTs. See the Load balancer section for more details. By default, it is set to `2`.
* `splunk.hec.max.batch.size` - Maximum batch size when posting events to Splunk. The size is the actual number of Kafka events, and not byte size. By default, it is set to `100`. 
* `splunk.hec.threads` - Controls how many threads are spawned to do data injection via HEC in a **single** connector task. By default, it is set to `1`.
* `splunk.hec.socket.timeout` - Internal TCP socket timeout when connecting to Splunk. By default, it is set to 60 seconds.

### Acknowledgement Parameters
##### Use Ack
* `splunk.hec.ack.enabled` -  Valid settings are `true` or `false`. When set to `true` the Splunk Kafka Connector will poll event ACKs for POST events before check-pointing the Kafka offsets. This is used to prevent data loss, as this setting implements guaranteed delivery. By default, this setting is set to `true`.
    > Note: If this setting is set to `true`, verify that the corresponding HEC token is also enabled with index acknowledgements, otherwise the data injection will fail, due to duplicate data. When set to `false`, the Splunk Kafka Connector will only POST events to your Splunk platform instance. After it receives a HTTP 200 OK response, it assumes the events are indexed by Splunk. Note: In cases where the Splunk platform crashes, there may be some data loss.
* `splunk.hec.ack.poll.interval` - This setting is only applicable when `splunk.hec.ack.enabled` is set to `true`. Internally it controls the event ACKs polling interval. By default, this setting is 10 seconds.
* `splunk.hec.ack.poll.threads` - This setting is used for performance tuning and is only applicable when `splunk.hec.ack.enabled` is set to `true`. It controls how many threads should be spawned to poll event ACKs. By default, it is set to `1`. 
    > Note: For large Splunk indexer clusters (For example, 100 indexers) you need to increase this number. Recommended increase to speed up ACK polling is 4 threads.
* `splunk.hec.event.timeout` - This setting is applicable when `splunk.hec.ack.enabled` is set to `true`. When events are POSTed to Splunk and before they are ACKed, this setting determines how long the connector will wait before timing out and resending. By default, it is set to 120 seconds. 

#### Endpoint Parameters
* `splunk.hec.raw` - Set to `true` in order for Splunk software to ingest data using the the /raw HEC endpoint. Default is `false`, which will use the /event endpoint.

##### /raw endpoint only
* `splunk.hec.raw.line.breaker` - Only applicable to /raw HEC endpoint. The setting is used to specify a custom line breaker to help Splunk separate the events correctly. 
    > Note: For example, you can specify "#####" as a special line breaker. Internally, the Splunk Kafka Connector will append this line breaker to every Kafka record to form a clear event boundary. The connector performs data injection in batch mode. On the Splunk platform side, you can configure **props.conf** to set up line breaker for the sourcetypes. Then the Splunk software will correctly break events for data flowing through /raw HEC endpoint. For questions on how and when to specify line breaker, go to the FAQ section. By default, this setting is empty.

##### /event endpoint only
* `splunk.hec.json.event.enrichment` -  Only applicable to /event HEC endpoint. This setting is used to enrich raw data with extra metadata fields. It contains a list of key value pairs separated by ",". The configured enrichment metadata will be indexed along with raw event data by Splunk software. Note: Data enrichment for /event HEC endpoint is only available in Splunk Enterprise 6.5 and above. By default, this setting is empty. See ([Documentation](http://dev.splunk.com/view/event-collector/SP-CAAAE8Y#indexedfield)) for more information.
    > Note: For example, `org=fin,bu=south-east-us`
* `splunk.hec.track.data` -  Valid settings are `true` or `false`. When set to `true`, data loss and data injection latency metadata will be indexed along with raw data. This setting only works in conjunction with /event HEC endpoint (`"splunk.hec.raw" : "false"`). By default, it is set to `false`.

#### Configuration Examples
 Two parameters which affect that core functionality of how the Connector works are:
 `splunk.hec.raw` and `splunk.hec.ack.enabled`. Detailed below are 4 configuration examples which implement these settings

##### Splunk Indexing with Acknowledgment 
    
1)  Using HEC /raw endpoint:
    
    
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
          "splunk.hec.event.timeout" : "120",
          "splunk.hec.raw" : "true",
          "splunk.hec.raw.line.breaker" : "#####"
        }
    }'
    ```

2) Using HEC /event endpoint:
    
   
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
             "splunk.hec.event.timeout" : "120",
             "splunk.hec.raw" : "false",
             "splunk.hec.json.event.enrichment" : "org=fin,bu=south-east-us",
             "splunk.hec.track.data" : "true"
           }
       }'
   ```
 
##### Splunk Indexing without Acknowledgment 

3)  Using HEC /raw endpoint:
 
   
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


4)  Using HEC /event endpoint:


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

A single Splunk Kafka Connector can reach maximum indexed throughput of **32 MB/second** with the following testbed and raw HEC endpoint in use:

Hardware specifications: 

* **AWS:** EC2 c4.2xlarge, 8 vCPU and 31 GB Memory
* **Splunk Cluster:** 3 indexer cluster without load balancer
* **Kafka Connect:** JVM heap size configuration is "-Xmx6G -Xms2G"
* **Kafka Connect resource usage:** ~6GB memory, ~3 vCPUs.
* **Kafka records size**: 512 Bytes
* **Batch size**: Maximum 100 Kafka records per batch which is around 50KB per batch

## Scaling out your environment

Before scaling the Splunk Kafka Connector tier, ensure the bottleneck is in the connector tier and not in another component.

Scaling out options:

1. Increase the number of parallel tasks. Only do this if the hardware is under-utilized (low CPU, low memory usage and low data injection throughput). The user can reconfigure the connector with more tasks. Example above in the **Configuration parameters**-**Update** section.

2. Increase hardware resources on cluster nodes in case of resource exhaustion, such as high CPU, or high memory usage.

3. Increase the number of Kafka Connect nodes.

## Data loss and latency monitoring

When creating a Splunk Kafka Connector using the REST API, `"splunk.hec.track.data": "true"` can be configured to allow data loss tracking and data collection latency monitoring. 
This is accomplished by enriching the raw data with **offset, timestamp, partition, topic** metadata.

### Data Loss Tracking
The Splunk Kafka Connector uses offset to track data loss since offsets in a Kafka topic partition are sequential. If a gap is observed in the Splunk software, there is data loss.

### Data Latency Tracking
The Splunk Kafka Connector uses the timestamp of the record to track the time elapsed between the time a Kafka record was created and the time the record was indexed in Splunk.

> Note: This setting will only work in conjunction with /event HEC endpoint (`"splunk.hec.raw" : "false"`)

## FAQ

1. When should I use HEC acknowledgements?

	Enable HEC token acknowledgements to avoid data loss. Without HEC token acknowledgement, data loss may occur, especially in case of a system restart or crash.

2. When should I use /raw HEC endpoint and /event HEC endpoint?

	If raw events need go through Splunk's index time extraction to use features like timestamp extraction or data manipulation for raw data, you will need use HEC's /raw event endpoint. 
	When using the /raw HEC endpoint and when your raw data does not contain a timestamp or contains multiple timestamps or carriage returns, you may want to configure the **splunk.hec.raw.line.breaker** and setup a corresponding **props.conf** inside your Splunk platform to honor this line breaker setting. This will assist Splunk to do event breaking. 
	Example:
	
	In connection configuration, set **"splunk.hec.raw.line.breaker":"####"** for sourcetype "s1"
	
	In **props.conf**, you can set up the line breaker as follows.
	
	```
	[s1] # sourcetye name
	LINE_BREAKER = (####)
	SHOULE_LINEMERGE = false
	```
	
   If you don't care about the timestamp, or by default, the auto assigned timestamp is good enough, then stick to the /event HEC endpoint.
	
4. How many tasks should I configure?
	
	Do not create more tasks than the number of partitions. Generally speaking, creating 2 * CPU tasks per Splunk Kafka Connector is a safe estimate.
	> Note: For example, assume there are 5 Kafka Connects running the Splunk Kafka Connector. Each host is 8 CPUs with 16 GB memory. And there are 200 partitions to collect data from. `max.tasks` will be: `max.tasks` = 2 * CPUs/host * Kafka Connect instances = 2 * 8 * 5 = 80 tasks. Alternatively, if there are only 60 partitions to consume from, then just set max.tasks to 60. Otherwise, the remaining 20 will be pending, doing nothing.
	
5. How many Kafka Connect instances should I deploy?

	This is highly dependent on how much volume per day the Splunk Kafka Connector needs to index in Splunk. In general an 8 CPU, 16 GB memory machine, can potentially achieve 50 - 60 MB/s throughput from Kafka into Splunk if Splunk is sized correctly. 
	
6. How can I track data loss and data collection latency?

	Please refer to the **Data loss and latency monitoring** section.
	
7. Is there a recommended deployment architecture?

	There are two typical architectures. 
	
	* Setting up a heavy forwarder layer in front of a Splunk platform indexer cluster in order to offload the data injection load to your Splunk platform indexer cluster. Setting up a heavy forwarder layer has performance benefits for the Splunk search app.
	
		Kafka Connect Cluster (in containers or virtual machines or physical machines) -> Heavy Forwarders (HEC) -> Splunk Indexer Cluster
	
	* Direct inject data to Splunk Indexer cluster

		Kafka Connect Cluster (in containers or virtual machines or physical machines) -> Splunk Indexer Cluster (HEC)
	

## Troubleshooting

1. Append the **log4j.logger.com.splunk=DEBUG** to **config/connect-log4j.properties** file to enable more verbose logging for Splunk Kafka Connector.
2. Kafka connect encounters an "out of memory" error. Remember to export environment variable **KAFKA\_HEAP\_OPTS="-Xmx6G -Xms2G"**. Refer to the **Deploy** section for more information.
