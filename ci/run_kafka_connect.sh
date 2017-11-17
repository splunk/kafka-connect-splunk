#!/bin/bash

# Checkout, build and run kafka-connect-splunk in the fight

git clone git@github.com:splunk/kafka-connect-splunk.git

# build the package
cd kafka-connect-splunk && bash build.sh

# untar the package
tar xzf kafka-connect-splunk.tar.gz
cd kafka-connect-splunk

sed -i"" "s@bootstrap.servers=.*@bootstrap.servers=$KAFKA_BOOTSTRAP_SERVERS@g" config/connect-distributed.properties

sleep 300

bash /fix_hosts.sh > /tmp/fixhosts 2>&1 &

while :
do
    ./bin/connect-distributed.sh config/connect-distributed.properties
    sleep 1
done
