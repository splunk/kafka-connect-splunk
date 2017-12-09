#!/usr/bin/python

import os
import time
import logging
import requests

logging.basicConfig(format='%(asctime)-15s %(message)s', level=logging.INFO)

# envs
# HEC_ACK_MODE
# INDEX_CLUSTER_SIZE
# CONNECTOR_PERF_DURATION

CONNECTOR_URI = 'http://kafkaconnect1:8083/connectors'
CONNECTOR_URI = 'http://localhost:8083/connectors'
TOPIC = 'perf'

PERF_CASES = [
    {
        'tasks.max': '1',
        'splunk.hec.threads': '1',
        'splunk.hec.max.batch.size': '500',
    },
    {
        'tasks.max': '1',
        'splunk.hec.threads': '2',
        'splunk.hec.max.batch.size': '500',
    },
    {
        'tasks.max': '1',
        'splunk.hec.threads': '4',
        'splunk.hec.max.batch.size': '500',
    },
    {
        'tasks.max': '1',
        'splunk.hec.threads': '8',
        'splunk.hec.max.batch.size': '500',
    },
    {
        'tasks.max': '1',
        'splunk.hec.threads': '16',
        'splunk.hec.max.batch.size': '500',
    },
    {
        'tasks.max': '1',
        'splunk.hec.threads': '1',
        'splunk.hec.max.batch.size': '1000',
    },
    {
        'tasks.max': '1',
        'splunk.hec.threads': '2',
        'splunk.hec.max.batch.size': '1000',
    },
    {
        'tasks.max': '1',
        'splunk.hec.threads': '4',
        'splunk.hec.max.batch.size': '1000',
    },
    {
        'tasks.max': '1',
        'splunk.hec.threads': '8',
        'splunk.hec.max.batch.size': '1000',
    },
    {
        'tasks.max': '1',
        'splunk.hec.threads': '16',
        'splunk.hec.max.batch.size': '1000',
    },
    {
        'tasks.max': '2',
        'splunk.hec.threads': '1',
        'splunk.hec.max.batch.size': '500',
    },
    {
        'tasks.max': '4',
        'splunk.hec.threads': '1',
        'splunk.hec.max.batch.size': '500',
    },
    {
        'tasks.max': '8',
        'splunk.hec.threads': '1',
        'splunk.hec.max.batch.size': '500',
    },
    {
        'tasks.max': '16',
        'splunk.hec.threads': '1',
        'splunk.hec.max.batch.size': '500',
    },
    {
        'tasks.max': '32',
        'splunk.hec.threads': '1',
        'splunk.hec.max.batch.size': '500',
    },
]


def get_hec_uris():
    # calculate HEC URIs
    indxer_cluster_size = int(os.environ.get('INDEX_CLUSTER_SIZE', '1'))
    endpoints = []
    for i in xrange(1, indxer_cluster_size):
        endpoints.append('https://idx{}:8088'.format(i))

    return ','.join(endpoints)


def _get_connector_config(hec_uris, hec_raw, hec_ack, test_case):
    token = '00000000-0000-0000-0000-000000000000'
    if hec_ack:
        token = '00000000-0000-0000-0000-000000000001'

    source = 'connector-perf:raw_endpoint={raw}:use_ack={ack}'.format(
        raw=hec_raw, ack=hec_ack)

    params = ':'.join('{}={}'.format(k, v) for k, v in test_case.iteritems())
    sourcetype = 'connector-perf:{params}'.format(params=params)
    connector_name = 'splunk-sink-{}'.format(int(time.time() * 1000))

    connector_config = {
        'name': connector_name,
        'config': {
            'connector.class': 'com.splunk.kafka.connect.SplunkSinkConnector',
            'topics': TOPIC,
            'tasks.max': test_case['tasks.max'],
            'splunk.indexes': 'main',
            'splunk.sources': source,
            'splunk.sourcetypes': sourcetype,
            'splunk.hec.uri': hec_uris,
            'splunk.hec.token': token,
            'splunk.hec.ack.enabled': hec_ack,
            'splunk.hec.raw': hec_raw,
            'splunk.hec.max.batch.size': test_case['splunk.hec.max.batch.size'],
            'splunk.hec.track.data': 'true',
            'splunk.hec.ssl.validate.certs': 'false',
            'splunk.hec.raw.line.breaker': '@@@@',
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
    time.sleep(int(os.environ.get('CONNECTOR_PERF_DURATION', 3600)))


def _do_perf(hec_uris, hec_raw, hec_ack):
    for test_case in PERF_CASES:
        config = _get_connector_config(hec_uris, hec_raw, hec_ack, test_case)
        logging.info(
            'handling perf case: %s %s',
            config['config']['splunk.sources'],
            config['config']['splunk.sourcetypes'])
        create_connector(CONNECTOR_URI, config)
        wait_for_connector_do_data_collection_injection()
        delete_connector(CONNECTOR_URI, config)


def perf():
    mode = os.environ.get('HEC_ACK_MODE')
    if mode == 'no_ack':
        ack_modes = ['false']
    elif mode == 'ack':
        ack_modes = ['true']
    else:
        ack_modes = ['true', 'false']

    hec_uris = get_hec_uris()
    # raw=true/false, ack=true/false
    hec_configs = {
        'false': ack_modes,
        'true': ack_modes,
    }

    for hec_raw, hec_ack_enabled_settings in hec_configs.iteritems():
        for hec_ack_enabled in hec_ack_enabled_settings:
            _do_perf(hec_uris, hec_raw, hec_ack_enabled)


if __name__ == '__main__':
    perf()
