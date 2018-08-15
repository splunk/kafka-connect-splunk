#!/bin/bash

curdir=`pwd`
git clone git@github.com:splunk/kafka-connect-splunk.git
branch=${KAFKA_CONNECT_BRANCH:-issue-160/update-CI-for-Kafka2.0}
cd kafka-connect-splunk && git checkout ${branch}

duration=${SLEEP:-600}
sleep ${duration}

bash ${curdir}/kafka-connect-splunk/ci/fix_hosts.sh > /tmp/fixhosts 2>&1 &

python ${curdir}/kafka-connect-splunk/ci/perf.py

tail -f /dev/null
