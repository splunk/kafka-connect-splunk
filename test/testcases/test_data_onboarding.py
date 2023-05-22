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
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")
        search_query = f"index={setup['splunk_index']} | search timestamp=\"{setup['timestamp']}\" {test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last hour", len(events))
        assert len(events) == expected

    # @pytest.mark.parametrize("test_scenario, test_input, expected", [
    #     ("protobuf", "sourcetype::protobuf", 1),
    # ])
    # def test_proto_data_onboarding(self, setup, test_scenario, test_input, expected):
    #     logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")
    #     search_query = f"index={setup['splunk_index']} | search {test_input}"
    #     logger.info(search_query)
    #     events = check_events_from_splunk(start_time="-15m@m",
    #                                       url=setup["splunkd_url"],
    #                                       user=setup["splunk_user"],
    #                                       query=[f"search {search_query}"],
    #                                       password=setup["splunk_password"])
    #     logger.info("Splunk received %s events in the last hour", len(events))
    #     assert len(events) == expected

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("date_format", "latest=1365209605.000 sourcetype::date_format", "2010-06-13T23:11:52.454+00:00"),
        ("epoch_format", "latest=1565209605.000 sourcetype::epoch_format", "2019-04-14T02:40:05.000+00:00"),
    ])
    def test_extracted_timestamp_data_onboarding_date_format(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")
        search_query = f"index={setup['splunk_index']} {test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last hour", len(events))
        if(len(events)==1):
            assert events[0]["_time"] == expected
        else:
            assert False,"No event found or duplicate events found"
