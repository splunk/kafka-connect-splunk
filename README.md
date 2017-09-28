# kafka-connect-splunk

## Build
1. create a uber jar for cloudfwd-1.0-SNAPSHOT.jar and place it under `kafka-connect-splunk` directory
2. Run `mvn package` to build a uber JAR for kafka-connect-splunk
4. cp `kafka-connect-splunk-1.0-SNAPSHOT.jar` to `/tmp` directory which is target Kafka Connect plugin path by default


## Run Kafka Connect
1. Install Kafka Connect and add it to bin path
2. Start up Kafka Cluster locally
3. Inject some JSON events to Kafka Cluster to `perf` topic
4. Adjust kafka booststrap servers in `ci/connect-distributed.properties` if necessary
5. Adjust splunk HEC settings in `ci/connector.properties` if necessary
6. Run `connect-distributed ci/connect-distributed.properties`
7. Run the following command to create a connector

    ```
	curl localhost:8083/connectors -X POST -H "Content-Type: application/json" -d '{
   "name": "kafka-connect-splunk",
   "config": {
      "topics": "perf2",
      "tasks.max": "3",
      "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
      "splunk.hec.uri": "https://127.0.0.1:8088",
      "splunk.hec.token": "1B901D2B-576D-40CD-AF1E-98141B499534",
      "splunk.hec.raw": "false",
      "splunk.hec.ack.enabled": "true",
      "splunk.hec.ssl.validate.certs": "false",
      "name": "kafka-connect-splunk"
  }
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
	
	# Get kafka-connect-splunk connector task info
	curl http://localhost:8083/connectors/kafka-connect-splunk/tasks
	
	# Delete kafka-connect-splunk connector
	curl http://localhost:8083/connectors/kafka-connect-splunk -X DELETE
	```

8. Data should flow into Splunk
