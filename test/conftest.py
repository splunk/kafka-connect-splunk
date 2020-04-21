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
from lib.commonkafka import *
from lib.connect_params import *
from kafka.producer import KafkaProducer
from datetime import datetime
from lib.helper import get_test_folder
from lib.data_gen import generate_connector_content
import pytest
import yaml


logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger(__name__)


_config_path = os.path.join(os.path.dirname(__file__), 'config.yaml')
with open(_config_path, 'r') as yaml_file:
    config = yaml.load(yaml_file)


@pytest.fixture(scope="class")
def setup(request):
    return config


def pytest_configure():
    # Generate data
    now = datetime.now()
    msg = {"timestamp": str(datetime.timestamp(now))}
    producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"],
                             value_serializer=lambda v: json.dumps(v).encode('utf-8'))
    producer.send(config["kafka_topic"], msg)
    producer.flush()
    config['timestamp'] = msg["timestamp"]

    # Launch all connectors for tests
    for param in connect_params:
        connector_content = generate_connector_content(param)
        create_kafka_connector(config, connector_content)
    # wait for data to be ingested to Splunk
    time.sleep(60)


def pytest_unconfigure():
    # Delete launched connectors
    for param in connect_params:
        delete_kafka_connector(config, param)
