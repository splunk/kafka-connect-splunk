#!/bin/bash

# variables
kafkaversion=2.0.0
builddir=/tmp/splunk-kafka-connect-build/splunk-kafka-connect

githash=`git rev-parse --short HEAD 2>/dev/null | sed "s/\(.*\)/@\1/"` # get current git hash
gitbranch=`git rev-parse --abbrev-ref HEAD` # get current git branch
gitversion=`git describe --abbrev=0 --tags 2>/dev/null` # returns the latest tag from current commit
jarversion=${gitversion}

# if no version found from git tag, it is a dev build
if [[ -z "$gitversion" ]]; then
  gitversion="dev"
  jarversion=${gitversion}-SNAPSHOT
fi

packagename=splunk-kafka-connect-${gitversion}.tar.gz

# record git info in version.properties file under resources folder
resourcedir='src/main/resources'
/bin/rm -f ${resourcedir}/version.properties
echo githash=${githash} >> ${resourcedir}/version.properties
echo gitbranch=${gitbranch} >> ${resourcedir}/version.properties
echo gitversion=${gitversion} >> ${resourcedir}/version.properties


curdir=`pwd`

/bin/rm -rf ${builddir}
mkdir -p ${builddir}/connectors
mkdir -p ${builddir}/bin
mkdir -p ${builddir}/config
mkdir -p ${builddir}/libs

# Build the package
echo "Building the connector package ..."
mvn versions:set -DnewVersion=${jarversion}
mvn package > /dev/null

# Copy over the pacakge
echo "Copy over splunk-kafka-connect jar ..."
cp target/splunk-kafka-connect-${jarversion}.jar ${builddir}/connectors
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

# Clean up
echo "Clean up ..."
/bin/rm -rf kafka_2.11-${kafkaversion}
/bin/rm -f kafka_2.11-${kafkaversion}.tgz

# Package up
echo "Package ${packagename} ..."
cd .. && tar czf ${packagename} splunk-kafka-connect

echo "Copy package ${packagename} to ${curdir} ..."
cp ${packagename} ${curdir}

/bin/rm -rf splunk-kafka-connect ${packagename}
echo "Done with build & packaging"

echo

cat << EOP
To run the splunk-kafka-connect, do the following steps:
1. untar the package: tar xzf splunk-kafka-connect.tar.gz
2. config config/connect-distributed.properties according to your env
3. run: bash bin/connect-distributed.sh config/connect-distributed.properties
4. Use Kafka Connect REST api to create data collection tasks
EOP
