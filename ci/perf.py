#!/usr/bin/python

import os
import time
import logging
import requests
import export_data

try:
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
except:
    pass

try:
    from requests.packages.urllib3.exceptions import InsecureRequestWarning
    requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
except:
    pass

logging.basicConfig(
    format='%(asctime)-15s mod=%(module)s func=%(funcName)s line=%(lineno)d %(message)s',
    level=logging.INFO)

# envs
# KAFKA_CONNECT_HEC_MODE
# KAFKA_CONNECT_ACK_MODE
# KAFKA_CONNECT_TOPICS
# KAFKA_CONNECT_PERF_DURATION
# KAFKA_CONNECT_LINE_BREAKER
# INDEX_CLUSTER_SIZE
# JVM_HEAP_SIZE

CONNECTOR_URI = 'http://{}:8083/connectors'.format(
    os.environ.get('KAFKA_CONNECTOR_IP', 'kafkaconnect1'))
TOPIC = os.environ.get('KAFKA_CONNECT_TOPICS', 'perf')
JVM_HEAP_SIZE = os.environ.get('JVM_HEAP_SIZE', '8G')
LINE_BREAKER = os.environ.get('KAFKA_CONNECT_LINE_BREAKER', '@@@@')
TOKEN_WITHOUT_ACK = '00000000-0000-0000-0000-000000000000'
TOKEN_WITH_ACK = '00000000-0000-0000-0000-000000000001'
IDX_HOSTNAME_PREFIX = os.environ.get('IDX_HOSTNAME_PREFIX', 'idx')

PERF_CASES = [
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '1',
    #     'splunk.hec.max.batch.size': '500',
    # },
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '2',
    #     'splunk.hec.max.batch.size': '500',
    # },
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '4',
    #     'splunk.hec.max.batch.size': '500',
    # },
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '8',
    #     'splunk.hec.max.batch.size': '500',
    # },
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '16',
    #     'splunk.hec.max.batch.size': '500',
    # },
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '1',
    #     'splunk.hec.max.batch.size': '1000',
    # },
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '2',
    #     'splunk.hec.max.batch.size': '1000',
    # },
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '4',
    #     'splunk.hec.max.batch.size': '1000',
    # },
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '8',
    #     'splunk.hec.max.batch.size': '1000',
    # },
    # {
    #     'tasks.max': '1',
    #     'splunk.hec.threads': '16',
    #     'splunk.hec.max.batch.size': '1000',
    # },
    # {
    #     'tasks.max': '2',
    #     'splunk.hec.threads': '1',
    #     'splunk.hec.max.batch.size': '500',
    # },
    # {
    #     'tasks.max': '4',
    #     'splunk.hec.threads': '1',
    #     'splunk.hec.max.batch.size': '500',
    # },
    # {
    #     'tasks.max': '8',
    #     'splunk.hec.threads': '1',
    #     'splunk.hec.max.batch.size': '500',
    # },
    # {
    #     'tasks.max': '16',
    #     'splunk.hec.threads': '1',
    #     'splunk.hec.max.batch.size': '500',
    # },
    {
        'tasks.max': '32',
        'splunk.hec.threads': '2',
        'splunk.hec.max.batch.size': '500',
    },
]


def get_hec_uris():
    # calculate HEC URIs
    indxer_cluster_size = int(os.environ['INDEX_CLUSTER_SIZE'])
    endpoints = []
    for i in xrange(1, indxer_cluster_size + 1):
        endpoints.append('https://{}{}:8088'.format(IDX_HOSTNAME_PREFIX, i))

    return ','.join(endpoints)


def _get_connector_config(hec_uris, hec_raw, hec_ack, test_case):
    token = TOKEN_WITHOUT_ACK
    if hec_ack == 'true':
        token = TOKEN_WITH_ACK

    hec_settings = 'raw_endpoint={raw}:use_ack={ack}'.format(
        raw=hec_raw, ack=hec_ack)

    params = ':'.join('{}={}'.format(k, v) for k, v in test_case.iteritems())
    sourcetype = 'connector-perf:{hec_settings}:{params}:jvm_heap={jvm}'.format(
        hec_settings=hec_settings, params=params, jvm=JVM_HEAP_SIZE)
    connector_name = 'splunk-sink-{}'.format(int(time.time() * 1000))

    connector_config = {
        'name': connector_name,
        'config': {
            'connector.class': 'com.splunk.kafka.connect.SplunkSinkConnector',
            'topics': TOPIC,
            'tasks.max': test_case['tasks.max'],
            'splunk.indexes': 'main',
            'splunk.sources': 'connector-perf',
            'splunk.sourcetypes': sourcetype,
            'splunk.hec.uri': hec_uris,
            'splunk.hec.token': token,
            'splunk.hec.ack.enabled': hec_ack,
            'splunk.hec.raw': hec_raw,
            'splunk.hec.max.batch.size': test_case['splunk.hec.max.batch.size'],
            'splunk.hec.threads': test_case['splunk.hec.threads'],
            'splunk.hec.track.data': 'true',
            'splunk.hec.ssl.validate.certs': 'false',
            'splunk.hec.raw.line.breaker': LINE_BREAKER,
            'name': connector_name,
        }
    }
    return connector_config


def create_connector(connector_uri, connector_config):
    logging.info('create connector %s', connector_config['name'])
    while True:
        try:
            resp = requests.post(connector_uri, json=connector_config)
        except Exception:
            logging.exception('failed to create connector')
            time.sleep(2)
            continue
        else:
            if resp.ok:
                return
            else:
                logging.error('failed to create connector %s', resp.json)
                time.sleep(2)


def delete_connector(connector_uri, connector_config):
    logging.info('delete connector %s', connector_config['name'])
    uri = '{}/{}'.format(connector_uri, connector_config['name'])
    while True:
        try:
            resp = requests.delete(uri)
        except Exception:
            logging.exception('failed to delete connector')
            time.sleep(2)
        else:
            if resp.ok:
                return
            else:
                logging.error('failed to delete connector %s', resp.text)
                if resp.status_code == 404:
                    return

                time.sleep(2)


def wait_for_connector_do_data_collection_injection():
    time.sleep(int(os.environ.get('KAFKA_CONNECT_PERF_DURATION', 3600)))


def _do_perf(hec_uris, hec_raw, hec_ack):
    metric_exporter = _new_data_exporter()
    start_time = time.time()
    for test_case in PERF_CASES:
        config = _get_connector_config(hec_uris, hec_raw, hec_ack, test_case)
        logging.info(
            'handling perf case: %s %s',
            config['config']['splunk.sources'],
            config['config']['splunk.sourcetypes'])
        create_connector(CONNECTOR_URI, config)
        wait_for_connector_do_data_collection_injection()
        delete_connector(CONNECTOR_URI, config)

        _export_metric(metric_exporter, start_time)
        start_time = time.time()


def _get_hec_configs():
    mode = os.environ.get('KAFKA_CONNECT_ACK_MODE')
    if mode == 'no_ack':
        ack_modes = ['false']
    elif mode == 'ack':
        ack_modes = ['true']
    else:
        ack_modes = ['true', 'false']

    # raw=true/false, ack=true/false
    hec_mode = os.environ.get('KAFKA_CONNECT_HEC_MODE')
    if hec_mode == 'event':
        hec_configs = {
            'false': ack_modes,
        }
    elif hec_mode == 'raw':
        hec_configs = {
            'true': ack_modes,
        }
    else:
        hec_configs = {
            'false': ack_modes,
            'true': ack_modes,
        }

    return hec_configs


def _new_data_exporter():
    config = export_data.ExportParams(
        src_splunk_uri='https://sh1:8089',
        src_splunk_user='admin',
        src_splunk_password='Chang3d!',
        dest_splunk_hec_uri='{}'.format(os.environ['CONNECT_PERF_METRIC_DEST_HEC']),
        dest_splunk_hec_token='{}'.format(os.environ['CONNECT_PERF_METRIC_TOKEN']),
        src_index='',
        src_sourcetypes=[],
        timeout=600,
    )
    return export_data.ExportData(config)


def _export_metric(exporter, start_time):
    end_time = time.time()
    queries = [
        'search index="_internal" source="*metrics*.log" group=per_sourcetype_thruput series="connector-perf*"',
        'search index="main" source="proc_monitor" sourcetype="metric"',
    ]
    for query in queries:
        for _ in xrange(3):
            try:
                exporter.export(query, start_time, end_time)
            except Exception:
                logging.exception(
                    'failed to export perf metrics, query=%s', query)
            else:
                break


def perf():
    hec_configs = _get_hec_configs()
    hec_uris = get_hec_uris()
    for hec_raw, hec_ack_enabled_settings in hec_configs.iteritems():
        for hec_ack_enabled in hec_ack_enabled_settings:
            _do_perf(hec_uris, hec_raw, hec_ack_enabled)


def _do_post(uri, data, auth):
    logging.info('posting to %s', uri)
    while 1:
        try:
            resp = requests.post(uri, data=data, auth=auth, verify=False)
        except Exception:
            logging.exception('failed to post to %s', uri)
            time.sleep(2)
        else:
            if resp.ok:
                return

            if resp.status_code == 409:
                # already exists
                return

            logging.error('failed to post to %s, error=%s', uri, resp.text)
            time.sleep(2)


def splunk_request(uri_data_gen):
    auth = requests.auth.HTTPBasicAuth(
        os.environ.get('SPLUNK_USER', 'admin'),
        os.environ.get('SPLUNK_PASS', 'Chang3d!'))

    indxer_cluster_size = int(os.environ['INDEX_CLUSTER_SIZE'])
    for i in xrange(1, indxer_cluster_size + 1):
        uri, data = uri_data_gen(i)
        _do_post(uri, data, auth)


def create_hec_token_with_ack():
    data = {
        'name': 'hec-token-ack',
        'token': TOKEN_WITH_ACK,
        'index': 'main',
        'indexes': 'main',
        'useACK': '1',
        'disabled': '0',
    }

    def uri_data_gen(i):
        uri = 'https://{}{}:8089/servicesNS/nobody/splunk_httpinput/data/inputs/http?output_mode=json'.format(IDX_HOSTNAME_PREFIX, i)
        return uri, data

    return splunk_request(uri_data_gen)


def config_line_breaker():
    data = {
        'name': 'source::connector-perf...',
        'LINE_BREAKER': '({})'.format(LINE_BREAKER),
        'SHOULD_LINEMERGE': 'false',
    }

    def uri_data_gen(i):
        uri = 'https://{}{}:8089/servicesNS/nobody/splunk_httpinput/configs/conf-props?output_mode=json'.format(IDX_HOSTNAME_PREFIX, i)
        return uri, data

    return splunk_request(uri_data_gen)


if __name__ == '__main__':
    create_hec_token_with_ack()
    config_line_breaker()
    perf()
