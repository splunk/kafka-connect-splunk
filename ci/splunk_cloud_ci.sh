#! /bin/bash

pip3 install splunk_orca==1.1.0 -i https://repo.splunk.com/artifactory/api/pypi/pypi/simple --upgrade
splunk_orca --version
pip3 install -r test/requirements.txt
STACK_ID=`python3 ci/orca_create_splunk.py`
CI_SPLUNK_HOST="$STACK_ID.stg.splunkcloud.com"

chmod +x ci/setup_splunk_hec.sh && sh ci/setup_splunk_hec.sh $CI_SPLUNK_HOST
echo "-----------------update config.yaml-----------------"
sed -i "s/splunkd_url: https:\/\/127.0.0.1:8089/splunkd_url: https:\/\/$CI_SPLUNK_HOST:8089/g" /build/kafka-connect-splunk/test/config.yaml
sed -i "s/splunk_hec_url: https:\/\/127.0.0.1:8088/splunk_hec_url: https:\/\/$CI_SPLUNK_HOST:8088/g" /build/kafka-connect-splunk/test/config.yaml
sed -i "s/splunk_password: helloworld/splunk_password: ${CI_SPLUNK_PASSWORD}/g" /build/kafka-connect-splunk/test/config.yaml

cp /build/kafka-connect-splunk/test/config.yaml /build/kafka-connect-splunk/ci/config.yaml
