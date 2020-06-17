#! /bin/bash

venv/bin/pip install -r test/requirements.txt
STACK_ID=`venv/bin/python ci/orca_create_splunk.py`
CI_SPLUNK_HOST="$STACK_ID.stg.splunkcloud.com"

chmod +x ci/install_splunk.sh && sh ci/install_splunk.sh $CI_SPLUNK_HOST
echo "-----------------update config.yaml-----------------"
sed -i "s/splunkd_url: https:\/\/127.0.0.1:8089/splunkd_url: https:\/\/$CI_SPLUNK_HOST:8089/g" /build/kafka-connect-splunk/test/config.yaml
sed -i "s/splunk_hec_url: https:\/\/127.0.0.1:8088/splunk_hec_url: https:\/\/$CI_SPLUNK_HOST:8088/g" /build/kafka-connect-splunk/test/config.yaml
sed -i "s/splunk_password: helloworld/splunk_password: ${CI_SPLUNK_PASSWORD}/g" /build/kafka-connect-splunk/test/config.yaml

cp /build/kafka-connect-splunk/test/config.yaml /build/kafka-connect-splunk/ci/config.yaml
