import pytest
from lib.commonkafka import *
from lib.helper import get_test_folder
from lib.data_gen import generate_connector_content
from lib.commonsplunk import check_events_from_splunk

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger("test_case")


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
    def test_valid_crud_tasks(self, setup, test_input, expected):
        '''
        Test that valid kafka connect task can be created, updated, paused, resumed, restarted and deleted
        '''
        logger.info(f"testing test_valid_CRUD_tasks input={test_input} expected={expected} ")

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

        # Validate create task
        assert create_kafka_connector(setup, connector_definition, success=expected) == expected
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
        tasks = get_kafka_connector_tasks(setup, connector_definition,10)
        assert tasks == int(connector_definition["config"]["tasks.max"])

        # Validate pause task
        assert pause_kafka_connector(setup, connector_definition) == expected

        # Validate resume task
        assert resume_kafka_connector(setup, connector_definition) == expected

        # Validate restart task
        assert restart_kafka_connector(setup, connector_definition) == expected

        # Validate delete task
        assert delete_kafka_connector(setup, connector_definition) == expected

    @pytest.mark.parametrize("test_case, config_input, expected", [
        ("test_invalid_tasks_max", {"name": "test_invalid_tasks_max", "tasks_max": "dummy-string"}, False),
        ("test_invalid_splunk_hec_raw", {"name": "test_invalid_splunk_hec_raw", "splunk_hec_raw": "disable"}, False),
        ("test_invalid_topics", {"name": "test_invalid_topics", "topics": ""}, False)
    ])
    def test_invalid_crud_tasks(self, setup, test_case, config_input, expected):
        '''
        Test that invalid kafka connect task cannot be created
        '''
        logger.info(f"testing {test_case} input={config_input} expected={expected} ")

        connector_definition_invalid_tasks = generate_connector_content(config_input)
        setup['connectors'].append(test_case)

        assert create_kafka_connector(setup, connector_definition_invalid_tasks, success=expected) == expected

    @pytest.mark.parametrize("test_case, config_input, expected", [
        ("event_enrichment_non_key_value", {"name": "event_enrichment_non_key_value",
                                            "splunk_hec_json_event_enrichment": "testing-testing non KV"},
         ["FAILED"]),
        ("event_enrichment_non_key_value_3_tasks", {"name": "event_enrichment_non_key_value_3_tasks",
                                                    "tasks_max": "3",
                                                    "splunk_hec_json_event_enrichment": "testing-testing non KV"},
         ["FAILED", "FAILED", "FAILED"]),
        ("event_enrichment_not_separated_by_commas", {"name": "event_enrichment_not_separated_by_commas",
                                                      "splunk_hec_json_event_enrichment": "key1=value1 key2=value2"},
         ["FAILED"]),
        ("event_enrichment_not_separated_by_commas_3_tasks", {"name": "event_enrichment_not_separated_by_commas_3_tasks",
                                                              "tasks_max": "3",
                                                              "splunk_hec_json_event_enrichment": "key1=value1 key2=value2"},
         ["FAILED", "FAILED", "FAILED"])

    ])
    def test_invalid_crud_event_enrichment_tasks(self, setup, test_case, config_input, expected):
        '''
        Test that invalid event_enrichment kafka connect task can be created but task status should be FAILED
        and no data should enter splunk
        '''
        logger.info(f"testing {test_case} input={config_input} expected={expected} ")

        connector_definition_invalid_tasks = generate_connector_content(config_input)
        setup['connectors'].append(test_case)

        assert create_kafka_connector(setup, connector_definition_invalid_tasks) is True
        assert get_running_kafka_connector_task_status(setup, connector_definition_invalid_tasks) == expected
