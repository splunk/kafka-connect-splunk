import pytest
import logging
import sys
from ..commonkafka import create_kafka_connector, delete_kafka_connector

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
formatter = logging.Formatter('%(message)s')
handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(formatter)
logger.addHandler(handler)

@pytest.mark.parametrize("test_input,expected", [
    ("create_and_delete_valid_task", True)
])
def test_create_and_delete_valid_task(setup, test_input, expected):
    '''
    Test that valid kafka connect task can be created
    '''
    logger.info("testing create_and_delete_valid_task input={0} expected={1} ".format(
        test_input, expected))

    # defining a connector definition dict for the parameters to be sent to the API
    connector_definition = {
            "name": "kafka-connect-splunk",
            "config": {
               "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
               "tasks.max": "3",
               "topics": "test-datagen",  # set kafka topic later
               "splunk.indexes": setup["kafka_topic"],
               "splunk.hec.uri": setup["splunkd_url"],
               "splunk.hec.token": setup["splunk_token"],
               "splunk.hec.raw": "false",
               "splunk.hec.ack.enabled": "false",
               "splunk.hec.ssl.validate.certs": "true"
              }
            }

    assert create_kafka_connector(setup, connector_definition) == expected

    assert delete_kafka_connector(setup, connector_definition) == expected