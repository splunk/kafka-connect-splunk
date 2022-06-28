from kafka.producer import KafkaProducer
import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir)))

from lib.commonsplunk import check_events_from_splunk
from lib.commonkafka import *
from lib.helper import *
from datetime import datetime
import threading
import logging.config
import yaml
import subprocess
import logging
import time

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger('connector_upgrade')

_config_path = os.path.join(get_test_folder(), 'config.yaml')
with open(_config_path, 'r') as yaml_file:
    config = yaml.load(yaml_file)
now = datetime.now()
_time_stamp = str(datetime.timestamp(now))
_topic = 'kafka_connect_upgrade'
_connector = 'kafka_connect'
_connector_ack = 'kafka_connect_ack'


def start_old_connector():
    cmds = [f"test -f {config['connector_path']}/{config['old_connector_name']} && echo {config['connector_path']}/{config['old_connector_name']}",
            f"cd {config['kafka_home']}",
            f"sudo {config['kafka_home']}/bin/connect-distributed.sh {os.environ.get('GITHUB_WORKSPACE')}/config/connect-distributed-quickstart.properties &"]

    cmd = "\n".join(cmds)
    try:
        proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        proc.wait()
    except OSError as e:
        logger.error(e)


def generate_kafka_events(num):
    # Generate message data
    topics = [_topic]
    connector_content = {
        "name": _connector,
        "config": {
            "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
            "tasks.max": "1",
            "splunk.indexes": config["splunk_index"],
            "topics": _topic,
            "splunk.hec.ack.enabled": "false",
            "splunk.hec.uri": config["splunk_hec_url"],
            "splunk.hec.ssl.validate.certs": "false",
            "splunk.hec.token": config["splunk_token"],
            "splunk.sources": _connector
        }
    }
    create_kafka_connector(config, connector_content)
    connector_content_ack = {
        "name": _connector_ack,
        "config": {
            "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
            "tasks.max": "1",
            "splunk.indexes": config["splunk_index"],
            "topics": _topic,
            "splunk.hec.ack.enabled": "true",
            "splunk.hec.uri": config["splunk_hec_url"],
            "splunk.hec.ssl.validate.certs": "false",
            "splunk.hec.token": config["splunk_token_ack"],
            "splunk.sources": _connector_ack
        }
    }
    create_kafka_connector(config, connector_content_ack)
    client = KafkaAdminClient(bootstrap_servers=config["kafka_broker_url"], client_id='test')
    broker_topics = client.list_topics()
    logger.info(broker_topics)
    if _topic not in broker_topics:
        create_kafka_topics(config, topics)
    producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"],
                             value_serializer=lambda v: json.dumps(v).encode('utf-8'))

    for _ in range(num):
        msg = {"timestamp": _time_stamp}
        producer.send(_topic, msg)
        time.sleep(0.05)
        producer.flush()


def upgrade_connector_plugin():
    cmds = ["sudo kill $(sudo lsof -t -i:8083) && sleep 2",
            f"sudo rm {config['connector_path']}/{config['old_connector_name']} && sleep 2",
            f"sudo cp {config['connector_build_target']}/splunk-kafka-connect*.jar {config['connector_path']} && sleep 2",
            f"sudo {config['kafka_home']}/bin/connect-distributed.sh {os.environ.get('GITHUB_WORKSPACE')}/config/connect-distributed-quickstart.properties &"]

    cmd = "\n".join(cmds)
    try:
        proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        output, error = proc.communicate()
        logger.debug(output)
        time.sleep(2)
        update_kafka_connectors()
    except OSError as e:
        logger.error(e)


def update_kafka_connectors():
    logger.info("Update kafka connectors ...")
    connector_content = {
        "name": _connector,
        "config": {
            "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
            "tasks.max": "1",
            "splunk.indexes": config["splunk_index"],
            "topics": _topic,
            "splunk.hec.ack.enabled": "false",
            "splunk.hec.uri": config["splunk_hec_url"],
            "splunk.hec.ssl.validate.certs": "false",
            "splunk.hec.token": config["splunk_token"],
            "splunk.sources": _connector,
            "splunk.hec.json.event.formatted": "true",
            "splunk.hec.raw": True
        }
    }
    create_kafka_connector(config, connector_content)
    connector_content_ack = {
        "name": _connector_ack,
        "config": {
            "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
            "tasks.max": "1",
            "splunk.indexes": config["splunk_index"],
            "topics": _topic,
            "splunk.hec.ack.enabled": "true",
            "splunk.hec.uri": config["splunk_hec_url"],
            "splunk.hec.ssl.validate.certs": "false",
            "splunk.hec.token": config["splunk_token_ack"],
            "splunk.sources": _connector_ack,
            "splunk.hec.json.event.formatted": "true",
            "splunk.hec.raw": True
        }
    }
    create_kafka_connector(config, connector_content_ack)


if __name__ == '__main__':
    logger.info("Start old Kafka connector ...")
    thread_old_connect = threading.Thread(target=start_old_connector, daemon=True)
    thread_old_connect.start()
    time.sleep(10)
    logger.info("Generate Kafka events ...")
    thread_gen = threading.Thread(target=generate_kafka_events, args=(2000,), daemon=True)
    thread_gen.start()
    time.sleep(50)
    logger.info("Upgrade Kafka connector ...")
    thread_upgrade = threading.Thread(target=upgrade_connector_plugin, daemon=True)
    thread_upgrade.start()
    time.sleep(100)
    search_query_1 = f"index={config['splunk_index']} | search timestamp=\"{_time_stamp}\" source::{_connector}"
    logger.debug(search_query_1)
    events_1 = check_events_from_splunk(start_time="-15m@m",
                                      url=config["splunkd_url"],
                                      user=config["splunk_user"],
                                      query=[f"search {search_query_1}"],
                                      password=config["splunk_password"])
    logger.info("Splunk received %s events in the last 15m", len(events_1))
    assert len(events_1) == 2000
    search_query_2 = f"index={config['splunk_index']} | search timestamp=\"{_time_stamp}\" source::{_connector_ack}"
    logger.debug(search_query_2)
    events_2 = check_events_from_splunk(start_time="-15m@m",
                                        url=config["splunkd_url"],
                                        user=config["splunk_user"],
                                        query=[f"search {search_query_2}"],
                                        password=config["splunk_password"])
    logger.info("Splunk received %s events in the last 15m", len(events_2))
    assert len(events_2) == 2000
