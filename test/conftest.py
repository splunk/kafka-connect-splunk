"""
Copyright 2018-2019 Splunk, Inc..

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import pytest
import sys
import os
import json
import logging
import requests
import yaml
import json
import time
from kafka import KafkaProducer

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
formatter = logging.Formatter('%(message)s')
handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(formatter)
logger.addHandler(handler)

with open('test/config.yaml', 'r') as yaml_file:
    config = yaml.load(yaml_file)

with open('test/connect_params.json', 'r') as json_file:
    connect_params = json.load(json_file)

@pytest.fixture()
def setup(request):
    return config

def pytest_configure(): 
    # Generate data
    producer = KafkaProducer(bootstrap_servers='localhost:9092', value_serializer=lambda v: json.dumps(v).encode('utf-8'))
    msg = {'foo': 'bar'} 
    producer.send(config["kafka_topic"], msg)
    producer.flush()

    # Launch all connectors for tests
    for param in connect_params:
        response = requests.post(url=config["kafka_connect_url"]+"/connectors", data=json.dumps(param),
                        headers={'Accept': 'application/json', 'Content-Type': 'application/json'})

        if response.status_code == 201:
            logger.info("Created connector successfully - " + json.dumps(param))   
        else:
            logger.error("failed to create connector", param)
            print(response)
    # wait for data to be ingested to Splunk
    time.sleep(30)

def pytest_unconfigure():
    pass