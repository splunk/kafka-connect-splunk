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


def start_old_connector():
    cmds = ["test -f {0}/{1} && echo {0}/{1}".format(config["connector_path"], config["old_connector_name"]),
            "cd {}".format(config["kafka_home"]),
            "sudo ./bin/connect-distributed.sh {}/config/connect-distributed-quickstart.properties &".
                format(config["kafka_connect_home"])]

    cmd = "\n".join(cmds)
    try:
        proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        proc.wait()
    except OSError as e:
        logger.error(e)


def generate_kafka_events(num):
    # Generate message data
    topics = ["kafka_data_gen"]
    connector_content = {
        "name": "kafka_connect",
        "config": {
            "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
            "tasks.max": "1",
            "splunk.indexes": config["splunk_index"],
            "topics": "kafka_data_gen",
            "splunk.hec.ack.enabled": "false",
            "splunk.hec.uri": config["splunk_hec_url"],
            "splunk.hec.ssl.validate.certs": "false",
            "splunk.hec.token": config["splunk_token"]
        }
    }
    create_kafka_connector(config, connector_content)
    connector_content_ack = {
        "name": "kafka_connect_ack",
        "config": {
            "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
            "tasks.max": "1",
            "splunk.indexes": config["splunk_index"],
            "topics": "kafka_data_gen",
            "splunk.hec.ack.enabled": "true",
            "splunk.hec.uri": config["splunk_hec_url"],
            "splunk.hec.ssl.validate.certs": "false",
            "splunk.hec.token": config["splunk_token_ack"]
        }
    }
    create_kafka_connector(config, connector_content_ack)
    create_kafka_topics(config, topics)
    producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"],
                             value_serializer=lambda v: json.dumps(v).encode('utf-8'))

    for _ in range(num):
        msg = {"timestamp": _time_stamp}
        producer.send("kafka_data_gen", msg)
        time.sleep(0.05)
        producer.flush()


def upgrade_connector():
    cmds = ["sudo kill $(sudo lsof -t -i:8083) && sleep 2",
            "sudo rm {}/{} && sleep 2".format(config["connector_path"], config["old_connector_name"]),
            "sudo cp {0}/splunk-kafka-connect*.jar {1} && sleep 2".format(config["connector_build_target"],
                                                                          config["connector_path"]),
            "cd {}".format(config["kafka_home"]),
            "sudo ./bin/connect-distributed.sh {}/config/connect-distributed-quickstart.properties &".
                format(config["kafka_connect_home"])]

    cmd = "\n".join(cmds)
    try:
        proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        output, error = proc.communicate()
        logger.info(output)
    except OSError as e:
        logger.error(e)


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
    thread_upgrade = threading.Thread(target=upgrade_connector, daemon=True)
    thread_upgrade.start()
    time.sleep(100)
    search_query = "index={0} | search timestamp=\"{1}\"".format(config['splunk_index'], _time_stamp)
    logger.info(search_query)
    events = check_events_from_splunk(start_time="-15m@m",
                                      url=config["splunkd_url"],
                                      user=config["splunk_user"],
                                      query=["search {}".format(search_query)],
                                      password=config["splunk_password"])
    logger.info("Splunk received %s events in the last 15m", len(events))
    assert len(events) == 4000
