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

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger(__name__)

_config_path = os.path.join(os.path.dirname(__file__), 'config.yaml')
with open(_config_path, 'r') as yaml_file:
    config = yaml.load(yaml_file)


@pytest.fixture(scope="class")
def setup(request):
    return config


def pytest_configure():
    # Generate message data
    topics = [config["kafka_topic"], config["kafka_topic_2"], config["kafka_header_topic"],"prototopic",
              "test_splunk_hec_malformed_events","epoch_format","date_format"]

    create_kafka_topics(config, topics)
    producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"],
                             value_serializer=lambda v: json.dumps(v).encode('utf-8'))
    protobuf_producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"])
    timestamp_producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"])

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
    protobuf_producer.send("prototopic",value=b'\x00\x00\x00\x00\x01\x00\n\x011\x12\r10-01-04-3:45\x18\x15%\x00\x00*C*\x02No:\x12\n\x011\x12\x04this\x1a\x07New oneB\x0c\n\x011\x12\x07shampooJ\x04Many')
    timestamp_producer.send("date_format",b"{\"id\": \"19\",\"host\":\"host-01\",\"source\":\"bu\",\"fields\":{\"hn\":\"hostname\",\"CLASS\":\"class\",\"cust_id\":\"000013934\",\"time\": \"Jun 13 2010 23:11:52.454 UTC\",\"category\":\"IFdata\",\"ifname\":\"LoopBack7\",\"IFdata.Bits received\":\"0\",\"IFdata.Bits sent\":\"0\"}")
    timestamp_producer.send("epoch_format",b"{\"id\": \"19\",\"host\":\"host-01\",\"source\":\"bu\",\"fields\":{\"hn\":\"hostname\",\"CLASS\":\"class\",\"cust_id\":\"000013934\",\"time\": \"1555209605000\",\"category\":\"IFdata\",\"ifname\":\"LoopBack7\",\"IFdata.Bits received\":\"0\",\"IFdata.Bits sent\":\"0\"}")
    producer.flush()
    protobuf_producer.flush()
    timestamp_producer.flush()
    
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

def pytest_sessionfinish(session, exitstatus):
    if exitstatus != 0:
        search_query = "index=*"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-24h@h",
                                            url=setup["splunkd_url"],
                                            user=setup["splunk_user"],
                                            query=[f"search {search_query}"],
                                            password=setup["splunk_password"])
        myfile = open('events.txt', 'w+')
        for i in events:
            myfile.write("%s\n" % i)
        myfile.close()