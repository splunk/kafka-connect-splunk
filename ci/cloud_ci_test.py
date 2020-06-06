from kafka.producer import KafkaProducer
from requests.packages.urllib3.util.retry import Retry
from requests.adapters import HTTPAdapter
import logging
import yaml
import os
import json
import requests
import time

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger('')

TIMEOUT = 500
_config_path = os.path.join(os.path.dirname(__file__), 'config.yaml')
with open(_config_path, 'r') as yaml_file:
    config = yaml.load(yaml_file)


def create_kafka_connecter(name):
    connector_definition = {
        "name": name,
        "config": {
            "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
            "tasks.max": "3",
            "topics": config["kafka_topic"],
            "splunk.indexes": config["splunk_index"],
            "splunk.hec.uri": config["splunk_hec_url"],
            "splunk.hec.token": config["splunk_token"],
            "splunk.hec.raw": "false",
            "splunk.hec.ack.enabled": "false",
            "splunk.hec.ssl.validate.certs": "false",
            "splunk.hec.json.event.enrichment": "chars=data-onboarding-event-endpoint-no-ack"
        }
    }
    msg = {"message": "kafka connector"}
    producer = KafkaProducer(bootstrap_servers=config["kafka_broker_url"],
                             value_serializer=lambda v: json.dumps(v).encode('utf-8'))
    producer.send(config["kafka_topic"], msg)
    producer.flush()

    create_kafka_connector(connector_definition)
    # wait for data to be ingested to Splunk
    time.sleep(60)


def create_kafka_connector(params, success=True):
    '''
    Create kafka connect connector using kafka connect REST API
    '''
    response = requests.post(url=config["kafka_connect_url"] + "/connectors", data=json.dumps(params),
                             headers={'Accept': 'application/json', 'Content-Type': 'application/json'})

    return response.status_code == 201


def check_events_from_splunk(start_time="-1h@h",
                             end_time="now",
                             url=None,
                             user=None,
                             query=None,
                             password=None):

    events = _collect_events(query, start_time, end_time, url, user, password)

    return events


def _collect_events(query, start_time, end_time, url="", user="", password=""):
    search_url = '{0}/services/search/jobs?output_mode=json'.format(
        url)
    logger.debug('requesting: %s', search_url)
    data = {
        'search': query,
        'earliest_time': start_time,
        'latest_time': end_time,
    }

    create_job = _requests_retry_session().post(
        search_url,
        auth=(user, password),
        verify=False, data=data)
    _check_request_status(create_job)

    json_res = create_job.json()
    job_id = json_res['sid']
    events = _wait_for_job_and_get_events(job_id, url, user, password)

    return events


def _wait_for_job_and_get_events(job_id, url="", user="", password=""):
    events = []
    job_url = '{0}/services/search/jobs/{1}?output_mode=json'.format(
        url, str(job_id))
    logger.debug('requesting: %s', job_url)

    for _ in range(TIMEOUT):
        res = _requests_retry_session().get(
            job_url,
            auth=(user, password),
            verify=False)
        _check_request_status(res)

        job_res = res.json()
        dispatch_state = job_res['entry'][0]['content']['dispatchState']

        if dispatch_state == 'DONE':
            events = _get_events(job_id, url, user, password)
            break
        if dispatch_state == 'FAILED':
            raise Exception('Search job: {0} failed'.format(job_url))
        time.sleep(1)
    return events


def _get_events(job_id, url="", user="", password=""):
    event_url = '{0}/services/search/jobs/{1}/events?output_mode=json'.format(
        url, str(job_id))
    logger.debug('requesting: %s', event_url)

    event_job = _requests_retry_session().get(
        event_url, auth=(user, password),
        verify=False)
    _check_request_status(event_job)

    event_job_json = event_job.json()
    events = event_job_json['results']
    logger.debug("Events from get_events method returned %s events",
                len(events))

    return events


def _check_request_status(req_obj):
    if not req_obj.ok:
        raise Exception('status code: {0} \n details: {1}'.format(
            str(req_obj.status_code), req_obj.text))


def _requests_retry_session(retries=10, backoff_factor=0.1, status_forcelist=(500, 502, 504)):
    session = requests.Session()
    retry = Retry(
        total=int(retries),
        backoff_factor=backoff_factor,
        method_whitelist=frozenset(['GET', 'POST']),
        status_forcelist=status_forcelist,
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount('http://', adapter)
    session.mount('https://', adapter)
    return session


def verify_event_on_splunk(test_input):
    search_query = "index={0} | search message=\"{1}\" {2}".format(config['splunk_index'],
                                                                   "kafka connector",
                                                                   test_input)
    events = check_events_from_splunk(start_time="-15m@m",
                                      url=config["splunkd_url"],
                                      user=config["splunk_user"],
                                      query=["search {}".format(search_query)],
                                      password=config["splunk_password"])
    logger.info("Splunk received %s events in the last hour", len(events))
    assert len(events) == 1


if __name__ == '__main__':
    create_kafka_connecter("kafka-connect-splunk")
    verify_event_on_splunk("chars::data-onboarding-event-endpoint-no-ack")