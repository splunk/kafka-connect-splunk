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
logger = logging.getLogger('eventproducer_connector_upgrade')

_config_path = os.path.join(get_test_folder(), 'config.yaml')
with open(_config_path, 'r') as yaml_file:
    config = yaml.load(yaml_file)
now = datetime.now()
_time_stamp = str(datetime.timestamp(now))
_topic = 'kafka_connect_upgrade'


def check_events_from_topic(target):
   
    t_end = time.time() + 100
    time.sleep(5)
    while time.time() < t_end:  
        output1 = subprocess.getoutput(" echo $(/usr/local/kafka/bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list 'localhost:9092' --topic kafka_connect_upgrade  --time -1 | grep -e ':[[:digit:]]*:' | awk -F  ':' '{sum += $3} END {print sum}')")
        output2 = subprocess.getoutput("echo $(/usr/local/kafka/bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list 'localhost:9092' --topic kafka_connect_upgrade --time -2 | grep -e ':[[:digit:]]*:' | awk -F  ':' '{sum += $3} END {print sum}')")
        time.sleep(5)
        if (int(output1)-int(output2))==target:
            logger.info("Events in the topic :" + str(int(output1)-int(output2)))
            break
        elif (int(output1)-int(output2))>2000:
            logger.info("Events in the topic :" + str(int(output1)-int(output2)))
    logger.info("Events in the topic :" + str(int(output1)-int(output2)))

def generate_kafka_events(num):
    # Generate message data
    topics = [_topic]
    client = KafkaAdminClient(bootstrap_servers=config["kafka_broker_url"], client_id='test')
    broker_topics = client.list_topics()
    logger.info(broker_topics)
    if _topic not in broker_topics:
        create_kafka_topics(config, topics)
    producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"],
                             value_serializer=lambda v: json.dumps(v).encode('utf-8'))

    for i in range(num):
        msg = f'timestamp={_time_stamp} count={i+1}\n'
        producer.send(_topic, msg)
        time.sleep(0.05)
        producer.flush()

if __name__ == '__main__':

    time.sleep(20)
    logger.info("Generate Kafka events ...")
    thread_gen = threading.Thread(target=generate_kafka_events, args=(1000,), daemon=True)
    thread_gen.start()
    check_events_from_topic(int(sys.argv[1]))
    time.sleep(50)