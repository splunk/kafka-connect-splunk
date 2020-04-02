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
from .connect_params import connect_params
from .commonkafka import create_kafka_connector, delete_kafka_connector

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
formatter = logging.Formatter('%(message)s')
handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(formatter)
logger.addHandler(handler)

with open('test/config.yaml', 'r') as yaml_file:
    config = yaml.load(yaml_file)

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
        create_kafka_connector(config, param)
    # wait for data to be ingested to Splunk
    time.sleep(60)

def pytest_unconfigure():
    # Delete launched connectors
    for param in connect_params:
        delete_kafka_connector(config, param)