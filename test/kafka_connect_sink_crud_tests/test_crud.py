import pytest
import logging
import sys
import time
from ..commonkafka import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
formatter = logging.Formatter('%(message)s')
handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(formatter)
logger.addHandler(handler)


class TestCrud:

    @pytest.fixture(scope='class', autouse=True)
    def setup_class(self, setup):
        setup['connectors'] = []
        yield
        running_connectors = get_running_connector_list(setup)
        for connector in setup['connectors']:
            if connector in running_connectors:
                delete_kafka_connector(setup, connector)

    @pytest.mark.parametrize("test_input,expected", [
        ("test_valid_CRUD_tasks", True)
    ])
    def test_valid_CRUD_tasks(self, setup, test_input, expected):
        '''
        Test that valid kafka connect task can be created, updated, paused, resumed, restarted and deleted
        '''
        logger.info("testing test_valid_CRUD_tasks input={0} expected={1} ".format(
            test_input, expected))

        # defining a connector definition dict for the parameters to be sent to the API
        connector_definition = {
                "name": "kafka-connect-splunk",
                "config": {
                   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
                   "tasks.max": "3",
                   "topics": setup["kafka_topic"],
                   "splunk.indexes": setup["splunk_index"],
                   "splunk.hec.uri": setup["splunkd_url"],
                   "splunk.hec.token": setup["splunk_token"],
                   "splunk.hec.raw": "false",
                   "splunk.hec.ack.enabled": "false",
                   "splunk.hec.ssl.validate.certs": "false"
                  }
                }

        #Validate create task
        assert create_kafka_connector(setup, connector_definition) == expected
        setup['connectors'].append("kafka-connect-splunk")

        # updating the definition to use 5 tasks instead of 3
        connector_definition = {
                "name": "kafka-connect-splunk",
                "config": {
                   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
                   "tasks.max": "5",
                   "topics": setup["kafka_topic"],
                   "splunk.indexes": setup["splunk_index"],
                   "splunk.hec.uri": setup["splunkd_url"],
                   "splunk.hec.token": setup["splunk_token"],
                   "splunk.hec.raw": "false",
                   "splunk.hec.ack.enabled": "false",
                   "splunk.hec.ssl.validate.certs": "false"
                  }
                }

        # Validate update task
        assert update_kafka_connector(setup, connector_definition) == expected

        # Validate get tasks
        tasks = get_kafka_connector_tasks(setup, connector_definition)
        assert tasks == int(connector_definition["config"]["tasks.max"])

        # Validate pause task
        assert pause_kafka_connector(setup, connector_definition) == expected

        # Validate resume task
        assert resume_kafka_connector(setup, connector_definition) == expected

        # Validate restart task
        assert restart_kafka_connector(setup, connector_definition) == expected

        # Validate delete task
        assert delete_kafka_connector(setup, connector_definition) == expected


    @pytest.mark.parametrize("test_input,expected", [
        ("test_invalid_CRUD_tasks", False)
    ])
    def test_invalid_CRUD_tasks(self, setup, test_input, expected):
        '''
        Test that invalid kafka connect task cannot be created
        '''
        logger.info("testing test_invalid_CRUD_tasks input={0} expected={1} ".format(
            test_input, expected))

        # connector definition with tasks.max invalid(not number)
        connector_definition_invalid_tasks = {
                "name": "kafka-connect-splunk-invalid",
                "config": {
                   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
                   "tasks.max": "dummy-string",
                   "topics": setup["kafka_topic"],
                   "splunk.indexes": setup["splunk_index"],
                   "splunk.hec.uri": setup["splunkd_url"],
                   "splunk.hec.token": setup["splunk_token"],
                   "splunk.hec.raw": "false",
                   "splunk.hec.ack.enabled": "false",
                   "splunk.hec.ssl.validate.certs": "false"
                  }
                }

        assert create_kafka_connector(setup, connector_definition_invalid_tasks) == expected

        # connector definition with splunk.hec.raw invalid(not boolean)
        connector_definition_invalid_tasks = {
                "name": "kafka-connect-splunk-invalid",
                "config": {
                   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
                   "tasks.max": "3",
                   "topics": setup["kafka_topic"],
                   "splunk.indexes": setup["splunk_index"],
                   "splunk.hec.uri": setup["splunkd_url"],
                   "splunk.hec.token": setup["splunk_token"],
                   "splunk.hec.raw": "disable",
                   "splunk.hec.ack.enabled": "false",
                   "splunk.hec.ssl.validate.certs": "false"
                  }
                }

        assert create_kafka_connector(setup, connector_definition_invalid_tasks) == expected

        # connector definition with topics invalid(empty string)
        connector_definition_invalid_tasks = {
                "name": "kafka-connect-splunk-invalid",
                "config": {
                   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
                   "tasks.max": "3",
                   "topics": "",
                   "splunk.indexes": setup["splunk_index"],
                   "splunk.hec.uri": setup["splunkd_url"],
                   "splunk.hec.token": setup["splunk_token"],
                   "splunk.hec.raw": "false",
                   "splunk.hec.ack.enabled": "false",
                   "splunk.hec.ssl.validate.certs": "false"
                  }
                }

        assert create_kafka_connector(setup, connector_definition_invalid_tasks) == expected

        # connector definition with splunk.hec.json.event.enrichment invalid(non key value pairs)
        connector_definition_invalid_tasks = {
                "name": "kafka-connect-splunk-invalid",
                "config": {
                   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
                   "tasks.max": "3",
                   "topics": setup["kafka_topic"],
                   "splunk.indexes": setup["splunk_index"],
                   "splunk.hec.uri": setup["splunkd_url"],
                   "splunk.hec.token": setup["splunk_token"],
                   "splunk.hec.raw": "false",
                   "splunk.hec.ack.enabled": "false",
                   "splunk.hec.ssl.validate.certs": "false",
                   "splunk.hec.json.event.enrichment": "testing-testing non KV"
                  }
                }

        assert create_kafka_connector(setup, connector_definition_invalid_tasks) == expected

        # connector definition with splunk.hec.json.event.enrichment invalid(key value pairs not separated by commas)
        connector_definition_invalid_tasks = {
                "name": "kafka-connect-splunk-invalid",
                "config": {
                   "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
                   "tasks.max": "3",
                   "topics": setup["kafka_topic"],
                   "splunk.indexes": setup["splunk_index"],
                   "splunk.hec.uri": setup["splunkd_url"],
                   "splunk.hec.token": setup["splunk_token"],
                   "splunk.hec.raw": "false",
                   "splunk.hec.ack.enabled": "false",
                   "splunk.hec.ssl.validate.certs": "false",
                    "splunk.hec.json.event.enrichment": "key1=value1 key2=value2"
                  }
                }

        assert create_kafka_connector(setup, connector_definition_invalid_tasks) == expected
