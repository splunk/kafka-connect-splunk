import pytest
from lib.commonkafka import *
from lib.helper import get_test_folder
from lib.data_gen import generate_connector_content
from lib.commonsplunk import check_events_from_splunk
from kafka.producer import KafkaProducer

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger("test_case")


class TestConfigurations:

    @pytest.fixture(scope='class', autouse=True)
    def setup_class(self, setup):
        setup['connectors'] = []
        yield
        running_connectors = get_running_connector_list(setup)
        for connector in setup['connectors']:
            if connector in running_connectors:
                delete_kafka_connector(setup, connector)

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("test_tasks_max_1_hec_raw_true", "sourcetype::raw_data-tasks_max-1", 1),
        ("test_tasks_max_1_hec_raw_false", "chars::test_tasks_max_1_hec_raw_false", 3),
        # ("test_tasks_max_3_hec_raw_true", "sourcetype::raw_data-tasks_max_3", 1),
        # ("test_tasks_max_3_hec_raw_false", "chars::test_tasks_max_3_hec_raw_false", 3),
        # ("test_tasks_max_null", "chars::test_tasks_max_null", 0)
    ])
    def test_tasks_max(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")

        search_query = f"index={setup['splunk_index']} | search timestamp=\"{setup['timestamp']}\" {test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last 15m", len(events))

        assert len(events) == expected

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("test_1_source_hec_raw_true", "source::test_1_source_hec_raw_true", 1),
        ("test_1_source_hec_raw_false", "source::test_1_source_hec_raw_false", 3),
        ("test_2_sources_hec_raw_true-1", "source::source_1_raw", 1),
        ("test_2_sources_hec_raw_true-2", "source::source_2_raw", 1),
        ("test_2_sources_hec_raw_false-1", "source::source_1_event", 3),
        ("test_2_sources_hec_raw_false-2", "source::source_2_event", 3)
    ])
    def test_splunk_sources(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")

        search_query = f"index={setup['splunk_index']} | search timestamp=\"{setup['timestamp']}\" {test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last 15m", len(events))

        assert len(events) == expected

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("test_1_sourcetype_hec_raw_true", "sourcetype::test_1_sourcetype_hec_raw_true", 1),
        ("test_1_sourcetype_hec_raw_false", "sourcetype::test_1_sourcetype_hec_raw_false", 3),
        ("test_2_sourcetypes_hec_raw_true-1", "sourcetype::sourcetype_1_raw", 1),
        ("test_2_sourcetypes_hec_raw_true-2", "sourcetype::sourcetype_2_raw", 1),
        ("test_2_sourcetypes_hec_raw_false-1", "sourcetype::sourcetype_1_event", 3),
        ("test_2_sourcetypes_hec_raw_false-2", "sourcetype::sourcetype_2_event", 3)
    ])
    def test_splunk_sourcetypes(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")

        search_query = f"index={setup['splunk_index']} | search timestamp=\"{setup['timestamp']}\" {test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last 15m", len(events))
        assert len(events) == expected

    # @pytest.mark.parametrize("test_scenario, test_input, expected", [
    #     ("test_ssl_validate_certs_true", "source::ssl_validate_certs_true", 1)
    # ])
    # def test_splunk_ssl_validate_certs_true(self, setup, test_scenario, test_input, expected):
    #     logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")
    #
    #     search_query = f"index={setup['splunk_index']} | search timestamp=\"{setup['timestamp']}\" {test_input}"
    #     logger.info(search_query)
    #     events = check_events_from_splunk(start_time="-15m@m",
    #                                       url=setup["splunkd_url"],
    #                                       user=setup["splunk_user"],
    #                                       query=[f"search {search_query}"],
    #                                       password=setup["splunk_password"])
    #     logger.info("Splunk received %s events in the last 15m", len(events))
    #     assert len(events) == expected

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("test_header_support_false_event_data", "source::test_header_support_false_event", 9),
        ("test_header_support_false_raw_data", "source::test_header_support_false_raw", 1)
    ])
    def test_header_support_false_event_data(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")

        search_query = f"index={setup['splunk_index']} | search timestamp=\"{setup['timestamp']}\" {test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last 15m", len(events))
        assert len(events) == expected

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("test_kafka_header_source_event", "source::kafka_header_source_event", 3),
        ("test_kafka_header_sourcetype_event", "sourcetype::kafka_header_sourcetype_event", 3),
        ("test_kafka_header_host_event", "host=kafkahostevent.com", 3),
        ("test_kafka_custom_header_source_event",
         "source::kafka_custom_header_source chars::test_header_support_true_custom_event", 3),
        ("test_kafka_header_source_raw", "source::kafka_header_source_raw", 1),
        ("test_kafka_header_sourcetype_raw", "sourcetype::kafka_header_sourcetype_raw", 1),
        ("test_kafka_header_host_raw", "host=kafkahostraw.com", 1),
        ("test_kafka_custom_header_source_raw", "sourcetype::kafka_custom_header_sourcetype NOT chars=\"*\"", 1)
    ])
    def test_header_support_true_event_data(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")

        search_query = f"index={setup['kafka_header_index']} | search \"{setup['timestamp']}\" {test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last 15m", len(events))
        assert len(events) == expected

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("test_splunk_hec_json_event_formatted_true_event_data",
         "chars::test_splunk_hec_json_event_formatted_true_event_data", 3),
        ("test_splunk_hec_json_event_formatted_false_event_data",
         "chars::test_splunk_hec_json_event_formatted_false_event_data", 3),
        ("test_splunk_hec_json_event_formatted_true_raw_data",
         "sourcetype::test_splunk_hec_json_event_formatted_true_raw_data", 1),
        ("test_splunk_hec_json_event_formatted_false_raw_data",
         "sourcetype::test_splunk_hec_json_event_formatted_false_raw_data", 1)
    ])
    def test_splunk_hec_json_event_formatted(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")

        search_query = f"index={setup['splunk_index']} | search timestamp=\"{setup['timestamp']}\" {test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last 15m", len(events))
        assert len(events) == expected

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("test_splunk_hec_empty_event", "NOT message chars::hec_empty_events", 1),
        ("test_splunk_hec_malformed_events", "\"&&\"=null chars::hec_malformed_events", 1)
    ])
    def test_splunk_hec_malformed_events(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")

        search_query = f"index={setup['splunk_index']} | search {test_input}_{setup['timestamp']}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last 15m", len(events))
        assert len(events) == expected
