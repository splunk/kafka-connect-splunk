#!/bin/bash

# Checkout, build and run kafka-connect-splunk in the fight

curdir=`pwd`
git clone git@github.com:splunk/kafka-connect-splunk.git

branch=${KAFKA_CONNECT_BRANCH:-develop}
# build the package
cd kafka-connect-splunk && git checkout ${branch} && bash build.sh

# untar the package
tar xzf kafka-connect-splunk.tar.gz
cd kafka-connect-splunk

sed -i"" "s@bootstrap.servers=.*@bootstrap.servers=$KAFKA_BOOTSTRAP_SERVERS@g" config/connect-distributed.properties

debug=${KAFKA_CONNECT_LOGGING:-DEBUG}
echo "log4j.logger.com.splunk=${debug}" >> config/connect-log4j.properties

duration=${SLEEP:-300}
sleep ${duration}

bash ${curdir}/kafka-connect-splunk/ci/fix_hosts.sh > /tmp/fixhosts 2>&1 &

while :
do
    ./bin/connect-distributed.sh config/connect-distributed.properties
    sleep 1
done
