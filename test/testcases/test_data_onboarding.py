import pytest
from lib.commonsplunk import check_events_from_splunk
from lib.commonkafka import *
from lib.helper import get_test_folder

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger("test_case")


class TestDataOnboarding:

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("raw_endpoint_no_ack", "sourcetype::raw_data-no-ack", 1),
        ("raw_endpoint_with_ack", "sourcetype::raw_data-ack", 1),
        ("event-endpoint-no-ack", "chars::data-onboarding-event-endpoint-no-ack", 3),
        ("event-endpoint-ack", "chars::data-onboarding-event-endpoint-ack", 3),
    ])
    def test_data_onboarding(self, setup, test_scenario, test_input, expected):
        logger.info("testing {0} input={1} expected={2} event(s)".format(test_scenario, test_input, expected))
        search_query = "index={0} | search timestamp=\"{1}\" {2}".format(setup['splunk_index'],
                                                                         setup["timestamp"],
                                                                         test_input)
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=["search {}".format(search_query)],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last hour", len(events))
        assert len(events) == expected

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("protobuf", "sourcetype::protobuf", 1),
    ])
    def test_proto_data_onboarding(self, setup, test_scenario, test_input, expected):
        logger.info("testing {0} input={1} expected={2} event(s)".format(test_scenario, test_input, expected))
        search_query = "index={0} | search {1}".format(setup['splunk_index'],
                                                                         test_input)
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=["search {}".format(search_query)],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last hour", len(events))
        assert len(events) == expected
