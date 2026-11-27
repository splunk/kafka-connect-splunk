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
from lib.commonsplunk import check_events_from_splunk
from lib.helper import get_test_folder
from lib.data_gen import generate_connector_content
import pytest
import yaml
import struct
import uuid

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger(__name__)

_config_path = os.path.join(os.path.dirname(__file__), 'config.yaml')
with open(_config_path, 'r') as yaml_file:
    config = yaml.load(yaml_file)

_active_connector_params = []


def pytest_addoption(parser):
    parser.addoption("--protobuf", action="store_true", default=False,
                     help="Test an externally installed Confluent Protobuf converter")


def pytest_collection_modifyitems(config, items):
    enabled = config.getoption("protobuf")
    for item in items:
        if item.get_closest_marker("protobuf") and not enabled:
            item.add_marker(pytest.mark.skip(reason="Requires --protobuf and an external converter"))


@pytest.fixture(scope="class")
def setup(request):
    return config


def pytest_configure(config):
    if not config.option.collectonly:
        _configure_environment(config.getoption("protobuf"))


def _configure_environment(protobuf_enabled):
    global _active_connector_params
    _active_connector_params = [p for p in connect_params
                                if protobuf_enabled or p['name'] != 'test_protobuf_events']
    # Generate message data
    topics = [config["kafka_topic"], config["kafka_topic_2"], config["kafka_header_topic"],
              "test_splunk_hec_malformed_events","epoch_format","date_format","record_key"]
    if protobuf_enabled:
        topics.append("prototopic")

    create_kafka_topics(config, topics)
    producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"],
                             value_serializer=lambda v: json.dumps(v).encode('utf-8'))
    raw_producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"])

    for _ in range(3):
        msg = {"timestamp": config['timestamp']}
        producer.send(config["kafka_topic"], msg)
        producer.send(config["kafka_topic_2"], msg)

        headers_to_send = [('header_index', b'kafka'), ('header_source_event', b'kafka_header_source_event'),
                           ('header_host_event', b'kafkahostevent.com'),
                           ('header_sourcetype_event', b'kafka_header_sourcetype_event')]
        producer.send(config["kafka_header_topic"], msg, headers=headers_to_send)

        headers_to_send = [('header_index', b'kafka'), ('header_source_raw', b'kafka_header_source_raw'),
                           ('header_host_raw', b'kafkahostraw.com'),
                           ('header_sourcetype_raw', b'kafka_header_sourcetype_raw')]
        producer.send(config["kafka_header_topic"], msg, headers=headers_to_send)

        headers_to_send = [('splunk.header.index', b'kafka'),
                           ('splunk.header.host', b'kafkahost.com'),
                           ('splunk.header.source', b'kafka_custom_header_source'),
                           ('splunk.header.sourcetype', b'kafka_custom_header_sourcetype')]
        producer.send(config["kafka_header_topic"], msg, headers=headers_to_send)
    producer.send("test_splunk_hec_malformed_events", {})
    producer.send("test_splunk_hec_malformed_events", {"&&": "null", "message": ["$$$$****////", 123, None]})
    producer.send("record_key",{"timestamp": config['timestamp']},b"{}")
    if protobuf_enabled:
        _publish_protobuf_record(raw_producer)
    raw_producer.send("date_format",b"{\"id\": \"19\",\"host\":\"host-01\",\"source\":\"bu\",\"fields\":{\"hn\":\"hostname\",\"CLASS\":\"class\",\"cust_id\":\"000013934\",\"time\": \"Jun 13 2010 23:11:52.454 UTC\",\"category\":\"IFdata\",\"ifname\":\"LoopBack7\",\"IFdata.Bits received\":\"0\",\"IFdata.Bits sent\":\"0\"}")
    raw_producer.send("epoch_format",b"{\"id\": \"19\",\"host\":\"host-01\",\"source\":\"bu\",\"fields\":{\"hn\":\"hostname\",\"CLASS\":\"class\",\"cust_id\":\"000013934\",\"time\": \"1555209605000\",\"category\":\"IFdata\",\"ifname\":\"LoopBack7\",\"IFdata.Bits received\":\"0\",\"IFdata.Bits sent\":\"0\"}")
    producer.flush()
    raw_producer.flush()

    # Launch only the connectors applicable to this worker's installed plugins.
    for param in _active_connector_params:
        connector_content = generate_connector_content(param)
        create_kafka_connector(config, connector_content)

    time.sleep(60)


def _publish_protobuf_record(producer):
    protobuf_params = next(p for p in connect_params if p['name'] == 'test_protobuf_events')
    schema = ('syntax = "proto3"; package com.mycorp.mynamespace; '
              'message MyRecord { string id = 1; float amount = 2; string customer_id = 3; }')
    response = requests.post(
        protobuf_params['value_converter_schema_registry_url'].rstrip('/')
        + '/subjects/' + protobuf_params['topics'] + '-value/versions',
        json={'schemaType': 'PROTOBUF', 'schema': schema}, timeout=30)
    response.raise_for_status()
    schema_id = response.json()['id']
    # A unique ID prevents old indexed records from satisfying this run's assertion.
    config['protobuf_record_id'] = uuid.uuid4().hex
    record_id = config['protobuf_record_id'].encode('utf-8')
    # Confluent header: magic byte, actual schema ID, message index [0].
    # Protobuf fields: string id, float amount, string customer_id.
    payload = (b'\x00' + struct.pack('>I', schema_id) + b'\x00'
               + b'\x0a' + bytes([len(record_id)]) + record_id
               + b'\x15' + struct.pack('<f', 1.9) + b'\x1a\x05Hello')
    producer.send(protobuf_params['topics'], value=payload).get(timeout=30)


def pytest_unconfigure():
    # Delete launched connectors
    for param in _active_connector_params:
        delete_kafka_connector(config, param)

def pytest_sessionfinish(session, exitstatus):
    if exitstatus != 0 and _active_connector_params:
        search_query = "index=*"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-24h@h",
                                            url=config["splunkd_url"],
                                            user=config["splunk_user"],
                                            query=[f"search {search_query}"],
                                            password=config["splunk_password"])
        myfile = open('events.txt', 'w+')
        for i in events:
            myfile.write("%s\n" % i)
        myfile.close()
