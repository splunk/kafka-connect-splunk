import logging
import sys
import pytest
from ..commonsplunk import check_events_from_splunk
from ..commonkafka import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
formatter = logging.Formatter('%(message)s')
handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(formatter)
logger.addHandler(handler)


class TestDataOnboarding:

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("raw_endpoint_no_ack", "sourcetype::raw_data-no-ack", 1),
        ("raw_endpoint_with_ack", "sourcetype::raw_data-ack", 1),
        ("event-endpoint-no-ack", "chars::data-onboarding-event-endpoint-no-ack", 1),
        ("event-endpoint-ack", "chars::data-onboarding-event-endpoint-ack", 1),
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
