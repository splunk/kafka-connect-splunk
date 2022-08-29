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
_connector = 'kafka_connect'
_connector_ack = 'kafka_connect_ack'



if __name__ == '__main__':
    time.sleep(100)
    search_query_1 = f"index={config['splunk_index']} | search source::{_connector} sourcetype::upgraded_test"
    logger.debug(search_query_1)
    events_1 = check_events_from_splunk(start_time="-48h@h",
                                      url=config["splunkd_url"],
                                      user=config["splunk_user"],
                                      query=[f"search {search_query_1}"],
                                      password=config["splunk_password"])
    logger.info("Splunk received %s events", len(events_1))
    assert len(events_1) == 2000
    search_query_2 = f"index={config['splunk_index']} | search source::{_connector_ack} sourcetype::upgraded_test"
    logger.debug(search_query_2)
    events_2 = check_events_from_splunk(start_time="-48h@m",
                                        url=config["splunkd_url"],
                                        user=config["splunk_user"],
                                        query=[f"search {search_query_2}"],
                                        password=config["splunk_password"])
    logger.info("Splunk received %s events ", len(events_2))
    assert len(events_2) == 2000