#! /bin/bash

venv/bin/pip install -r test/requirements.txt
STACK_ID=`venv/bin/python ci/orca_create_splunk.py`
CI_SPLUNK_HOST="$STACK_ID.stg.splunkcloud.com"

chmod +x ci/install_splunk.sh && sh ci/install_splunk.sh $CI_SPLUNK_HOST

#venv/bin/splunk_orca --cloud cloudworks destroy ${STACK_ID}