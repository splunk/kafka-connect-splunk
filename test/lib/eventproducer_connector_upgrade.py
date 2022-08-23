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


def generate_kafka_events(num):
    topics = [_topic]
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



if __name__ == '__main__':

    time.sleep(20)
    logger.info("Generate Kafka events ...")
    thread_gen = threading.Thread(target=generate_kafka_events, args=(1000,), daemon=True)
    thread_gen.start()
    time.sleep(100)