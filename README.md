# kafka-connect-splunk

## Build
1. create a uber jar for cloudfwd-1.0-SNAPSHOT.jar and place it under `kafka-connect-splunk` directory
2. Run `mvn package` to build a uber JAR for kafka-connect-splunk
3. Run `bash ci/repackage.sh` to repackage the kafka-connect-splunk jar ball
4. The repacakged jar will be placed to `/tmp` directory which is target Kafka Connect plugin path by default


## Run Kafka Connect
1. Install Kafka Connect and add it to bin path
2. Start up Kafka Cluster locally
3. Inject some JSON events to Kafka Cluster to `perf` topic
4. Adjust kafka booststrap servers in `ci/connect-distributed.properties` if necessary
5. Adjust splunk HEC settings in `ci/connector.properties` if necessary
6. Run `connect-distributed ci/connect-distributed.properties`
7. Run `confluent load kafka-connect-splunk -d ci/connector.properties
8. Data should flow into Splunk
