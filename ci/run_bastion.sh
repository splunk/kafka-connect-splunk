#!/bin/bash

curdir=`pwd`
git clone git@github.com:splunk/splunk-kafka-connect.git
branch=${KAFKA_CONNECT_BRANCH:-develop}
cd splunk-kafka-connect && git checkout ${branch}

duration=${SLEEP:-600}
sleep ${duration}

bash ${curdir}/splunk-kafka-connect/ci/fix_hosts.sh > /tmp/fixhosts 2>&1 &

python ${curdir}/splunk-kafka-connect/ci/perf.py

tail -f /dev/null
