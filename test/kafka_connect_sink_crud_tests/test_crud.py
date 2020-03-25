import pytest
import requests
import logging
import sys

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
formatter = logging.Formatter('%(message)s')
handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(formatter)
logger.addHandler(handler)

@pytest.mark.parametrize("test_input,expected", [
    ("create_valid_task", True)
])
def test_create_valid_task(setup, test_input, expected):
    '''
    Test that valid kafka connect task can be created
    '''
    logger.info("testing create_valid_task input={0} expected={1} ".format(
        test_input, expected))

    print(setup)

    # defining a params dict for the parameters to be sent to the API
    PARAMS = {
            "name": "kafka-connect-splunk",
            "config": {
               "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
               "tasks.max": "3",
               "topics": "test-datagen",  # set kafka topic later
               "splunk.indexes": setup["kafka_topic"],
               "splunk.hec.uri": setup["splunkd_url"],
               "splunk.hec.token": setup["splunkd_token"],
               "splunk.hec.raw": "false",
               "splunk.hec.ack.enabled": "false",
               "splunk.hec.ssl.validate.certs": "true"
              }
            }

    # sending get request and saving the response as response object
    r = requests.get(url=setup["kafka_connect_url"], params=PARAMS)

    # extracting data in json format
    data = r.json()

    # printing the output
    print(data)

    assert r.json != None

    logger.info("Created valid task successfully")