#!/bin/bash

CI_KAFKA_TOPIC=test-datagen
# Checkout, build and run kafka-connect-splunk in the fight

curdir=`pwd`
git clone https://github.com/splunk/kafka-connect-splunk.git

branch=${BRANCH_NAME:-develop}
# build the package
cd kafka-connect-splunk && git checkout ${branch} && bash build.sh
cd /kafka-connect
cp kafka-connect-splunk/target/splunk-kafka-connect-v*.jar /kafka-connect/
yes | cp -rf config.yaml kafka-connect-splunk/test/config.yaml

sed -i 's/plugin.path=connectors/plugin.path=\/kafka-connect/' /kafka-connect/kafka-connect-splunk/config/connect-distributed-quickstart.properties
sed -i 's/key.converter=org.apache.kafka.connect.storage.StringConverter/key.converter=org.apache.kafka.connect.json.JsonConverter/' /kafka-connect/kafka-connect-splunk/config/connect-distributed-quickstart.properties
sed -i 's/value.converter=org.apache.kafka.connect.storage.StringConverter/value.converter=org.apache.kafka.connect.json.JsonConverter/' /kafka-connect/kafka-connect-splunk/config/connect-distributed-quickstart.properties

#debug=${KAFKA_CONNECT_LOGGING:-DEBUG}
#echo "log4j.logger.com.splunk=${debug}" >> config/connect-log4j.properties

cd kafka

echo "Start ZooKeeper"
bin/zookeeper-server-start.sh config/zookeeper.properties > /kafka-connect/logs/zookeeper.txt 2>&1 &

echo "Start kafka server"
bin/kafka-server-start.sh config/server.properties > /kafka-connect/logs/kafka.txt 2>&1 &

echo "Run connect"
./bin/connect-distributed.sh /kafka-connect/kafka-connect-splunk/config/connect-distributed-quickstart.properties > /kafka-connect/logs/kafka_connect.txt 2>&1 &

echo "-----------------run integration tests-----------------"
cd /kafka-connect/kafka-connect-splunk/test
pip install virtualenv
virtualenv venv
pip install -r requirements.txt
source venv/bin/activate
pytest
