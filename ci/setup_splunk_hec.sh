#! /bin/bash

echo "=============setup splunk HEC=============="

CI_SPLUNK_HOST=$1

echo "Enable HEC services ..."
curl -X POST -u ${CI_SPLUNK_USERNAME}:${CI_SPLUNK_PASSWORD} -k https://$CI_SPLUNK_HOST:${CI_SPLUNK_PORT}/servicesNS/nobody/splunk_httpinput/data/inputs/http/http/enable

echo "Create new HEC token ..."
curl -X POST -u ${CI_SPLUNK_USERNAME}:${CI_SPLUNK_PASSWORD} -k -d "name=splunk_hec_token&token=${CI_SPLUNK_HEC_TOKEN}" https://$CI_SPLUNK_HOST:${CI_SPLUNK_PORT}/servicesNS/nobody/splunk_httpinput/data/inputs/http

echo "Enable HEC new-token ..."
curl -k -X POST -u ${CI_SPLUNK_USERNAME}:${CI_SPLUNK_PASSWORD} https://$CI_SPLUNK_HOST:${CI_SPLUNK_PORT}/servicesNS/admin/splunk_httpinput/data/inputs/http/splunk_hec_token/enable

echo "Create new HEC token with ack ..."
curl -X POST -u ${CI_SPLUNK_USERNAME}:${CI_SPLUNK_PASSWORD} -k -d "name=splunk_hec_token_ack&token=${CI_SPLUNK_HEC_TOKEN_ACK}&useACK=1" https://$CI_SPLUNK_HOST:${CI_SPLUNK_PORT}/servicesNS/nobody/splunk_httpinput/data/inputs/http

echo "Enable HEC new-token ..."
curl -k -X POST -u ${CI_SPLUNK_USERNAME}:${CI_SPLUNK_PASSWORD} https://$CI_SPLUNK_HOST:${CI_SPLUNK_PORT}/servicesNS/admin/splunk_httpinput/data/inputs/http/splunk_hec_token_ack/enable

echo "Setup Indexes ..."
curl -X POST -u ${CI_SPLUNK_USERNAME}:${CI_SPLUNK_PASSWORD} -k -d "name=${CI_INDEX_EVENTS}&datatype=event" https://$CI_SPLUNK_HOST:${CI_SPLUNK_PORT}/servicesNS/-/search/data/indexes
curl -X POST -u ${CI_SPLUNK_USERNAME}:${CI_SPLUNK_PASSWORD} -k -d "name=${CI_KAFKA_HEADER_INDEX}&datatype=event" https://$CI_SPLUNK_HOST:${CI_SPLUNK_PORT}/servicesNS/-/search/data/indexes


curl -k -X POST -u ${CI_SPLUNK_USERNAME}:${CI_SPLUNK_PASSWORD} https://$CI_SPLUNK_HOST:${CI_SPLUNK_PORT}/services/server/control/restart
