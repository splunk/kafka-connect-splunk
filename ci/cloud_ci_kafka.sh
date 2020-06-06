#! /bin/bash

venv/bin/pip install -r test/requirements.txt
STACK_ID=`venv/bin/python ci/orca_create_splunk.py`
CI_SPLUNK_HOST="$STACK_ID.stg.splunkcloud.com"
CI_KAFKA_TOPIC=test-datagen
kafkaversion=2.5.0

chmod +x ci/install_splunk.sh && sh ci/install_splunk.sh $CI_SPLUNK_HOST

echo "=============install kafka=============="
wget -q https://bootstrap.pypa.io/get-pip.py -P / && python get-pip.py && pip install requests && pip install psutil

wget -q http://apache.claz.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -P /build && cd /build && tar xzf apache-maven-3.6.3-bin.tar.gz

export PATH=${PATH}:/build/apache-maven-3.6.3/bin
echo ${PATH}
mkdir /build/kafka
mkdir /build/logs

wget -q http://apache.mirrors.hoobly.com/kafka/${kafkaversion}/kafka_2.12-${kafkaversion}.tgz
tar -xf kafka_2.12-${kafkaversion}.tgz -C /build/kafka --strip-components 1
rm -f kafka_2.12-${kafkaversion}.tgz

echo "Building the connector package ..."
cd /build/kafka-connect-splunk && mvn package > /dev/null
sleep 2
cd /build
echo "Copy over splunk-kafka-connect jar ..."
cp kafka-connect-splunk/target/splunk-kafka-connect-*.jar /build/

sed -i 's/plugin.path=connectors/plugin.path=\/build/' /build/kafka-connect-splunk/config/connect-distributed-quickstart.properties
sed -i 's/key.converter=org.apache.kafka.connect.storage.StringConverter/key.converter=org.apache.kafka.connect.json.JsonConverter/' /build/kafka-connect-splunk/config/connect-distributed-quickstart.properties
sed -i 's/value.converter=org.apache.kafka.connect.storage.StringConverter/value.converter=org.apache.kafka.connect.json.JsonConverter/' /build/kafka-connect-splunk/config/connect-distributed-quickstart.properties

cd /build/kafka
echo "Start ZooKeeper"
bin/zookeeper-server-start.sh config/zookeeper.properties > /build/logs/zookeeper.txt 2>&1 &

echo "Start kafka server"
bin/kafka-server-start.sh config/server.properties > /build/logs/kafka.txt 2>&1 &

echo "Run connect"
./bin/connect-distributed.sh /build/kafka-connect-splunk/config/connect-distributed-quickstart.properties > /build/logs/kafka_connect.txt 2>&1 &
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic test-datagen

echo "=============setup testing environment=============="

sed -i "s/splunkd_url: https:\/\/127.0.0.1:8089/splunkd_url: https:\/\/$CI_SPLUNK_HOST:8089/g" /build/kafka-connect-splunk/test/config.yaml
sed -i "s/splunk_hec_url: https:\/\/127.0.0.1:8088/splunk_hec_url: https:\/\/$CI_SPLUNK_HOST:8088/g" /build/kafka-connect-splunk/test/config.yaml
sed -i "s/splunk_password: helloworld/splunk_password: ${CI_SPLUNK_PASSWORD}/g" /build/kafka-connect-splunk/test/config.yaml

cp /build/kafka-connect-splunk/test/config.yaml /build/kafka-connect-splunk/ci/config.yaml

echo "=============run test=============="
cd /build/kafka-connect-splunk
venv/bin/python ci/cloud_ci_test.py

echo "=============tear down=============="
echo "Terminating the cloud stack..."
venv/bin/splunk_orca --cloud cloudworks destroy ${STACK_ID}