#!/bin/bash

# variables
kafkaversion=0.11.0.2
builddir=/tmp/kafka-connect-splunk-build/kafka-connect-splunk
packagename=kafka-connect-splunk.tar.gz

curdir=`pwd`

/bin/rm -rf ${builddir}
mkdir -p ${builddir}/connectors
mkdir -p ${builddir}/bin
mkdir -p ${builddir}/config
mkdir -p ${builddir}/libs

# Build the package
echo "Building the connector package ..."
mvn package -Dmaven.test.skip=true > /dev/null

# Copy over the pacakge
echo "Copy over kafka-connect-splunk jar ..."
cp target/kafka-connect-splunk-1.0-SNAPSHOT.jar ${builddir}/connectors
cp config/* ${builddir}/config
cp README.md ${builddir}
cp LICENSE ${builddir}

# Download kafka
echo "Downloading kafka_2.11-${kafkaversion} ..."
wget -q https://archive.apache.org/dist/kafka/${kafkaversion}/kafka_2.11-${kafkaversion}.tgz -P ${builddir}
cd ${builddir} && tar xzf kafka_2.11-${kafkaversion}.tgz

# Copy over kafka connect runtime
echo "Copy over kafka connect runtime ..."
cp kafka_2.11-${kafkaversion}/bin/connect-distributed.sh ${builddir}/bin
cp kafka_2.11-${kafkaversion}/bin/kafka-run-class.sh ${builddir}/bin
cp kafka_2.11-${kafkaversion}/config/connect-log4j.properties ${builddir}/config
cp kafka_2.11-${kafkaversion}/libs/*.jar ${builddir}/libs

# Download commons-logging jar
echo "Downloading commons-logging jar"
wget -q http://central.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar -P ${builddir}/libs/

# Clean up
echo "Clean up ..."
/bin/rm -rf kafka_2.11-${kafkaversion}
/bin/rm -f kafka_2.11-${kafkaversion}.tgz

# Package up
echo "Package ${packagename} ..."
cd .. && tar czf ${packagename} kafka-connect-splunk

echo "Copy package ${packagename} to ${curdir} ..."
cp ${packagename} ${curdir}

/bin/rm -rf kafka-connect-splunk ${packagename}
echo "Done with build & packaging"

echo

cat << EOP
To run the kafka-connect-splunk, do the following steps:
1. untar the package: tar xzf kafka-connect-splunk.tar.gz
2. config config/connect-distributed.properties according to your env
3. run: bash bin/connect-distributed.sh config/connect-distributed.properties
4. Use Kafka Connect REST api to create data collection tasks
EOP
