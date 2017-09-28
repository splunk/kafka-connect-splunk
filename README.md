# kafka-connect-splunk
This is a Kafka Connect Sink for Splunk.
SplunkSinkConnector supports writing data from Kafka topics into Splunk via Splunk HTTP Event Collector.
 
 
## Build
1. Install [Apache Kafka](https://kafka.apache.org/)
2. Install Kafka Connect and add it to bin path
3. Build [Cloudfwd](https://github.com/splunk/cloudfwd) using `mvn clean install` and copy the created cloudfwd-1.0-SNAPSHOT.jar under `kafka-connect-splunk` directory ($HOME/kafka-connect-splunk/libs/com/splunk/cloudfwd/1.0-SNAPSHOT)
4. Run `mvn package` to build a uber JAR for kafka-connect-splunk
5. cp `kafka-connect-splunk-1.0-SNAPSHOT.jar` to `/tmp` directory (in kafka-connect env) which is target Kafka Connect plugin path by default
 
 
## Run Kafka Connect
1. [Start up](https://kafka.apache.org/quickstart) Kafka Cluster and Zookeeper locally and create a test topic (eg: `perf`)
2. Inject JSON events to `perf` topic using [Kafka data-gen-app](https://github.com/dtregonning/kafka-data-gen)
4. Adjust `ci/connect-distributed.properties` for your environment if necessary
5. Adjust Splunk HEC settings in `ci/connector.properties` if necessary
6. Run `$KAFKA_CONNECT_HOME/bin/connect-distributed.sh $HOME/kafka-connect-splunk/ci/connect-distributed.properties` to start Kafka Connect
7. Run the following command to create a connector
 
    ```
    curl localhost:8083/connectors -X POST -H "Content-Type: application/json" -d 
    '{
    "name": "kafka-connect-splunk",
    "config": {
       "topics": "<topics-to-consume>",
       "tasks.max": "3",
       "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
       "splunk.hec.uri": "https://127.0.0.1:8088",
       "splunk.hec.token": "<Splunk-HEC-Token>",
       "splunk.hec.raw": "false",
       "splunk.hec.ack.enabled": "true",
       "splunk.hec.ssl.validate.certs": "false",
       "name": "kafka-connect-splunk"}}
       }'
       
    ```
 
7. Run the following commands to check the status of connector and tasks
  
    ```
    # List active connectors
    curl http://localhost:8083/connectors
     
    # Get kafka-connect-splunk connector info
    curl http://localhost:8083/connectors/kafka-connect-splunk
     
    # Get kafka-connect-splunk connector config info
    curl http://localhost:8083/connectors/kafka-connect-splunk/config
 
    # Validate kafka-connect-splunk connector config
    curl http://localhost:8083/connectors/kafka-connect-splunk -X DELETE
     
    # Get kafka-connect-splunk connector task info
    curl http://localhost:8083/connectors/kafka-connect-splunk/tasks
     
    # Delete kafka-connect-splunk connector
    curl http://localhost:8083/connectors/kafka-connect-splunk -X DELETE
 
    Refer to [this](https://docs.confluent.io/current/connect/managing.html#common-rest-examples) for more examples.
    ```
 
8. Data should flow into Splunk