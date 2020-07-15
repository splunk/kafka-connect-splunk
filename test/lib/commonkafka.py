"""
Copyright 2018-2019 Splunk, Inc..

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
from lib.helper import get_test_folder
from kafka.admin import KafkaAdminClient, NewTopic
import logging.config
import requests
import os
import json
import time
import jsonpath

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger("kafka")


def create_kafka_connector(setup, params, success=True):
    '''
    Create kafka connect connector using kafka connect REST API
    '''
    response = requests.post(url=setup["kafka_connect_url"] + "/connectors", data=json.dumps(params),
                  headers={'Accept': 'application/json', 'Content-Type': 'application/json'})
    if not success:
        logger.info(response.content)
        return response.status_code == 201

    status = get_kafka_connector_status(setup, params, action='Create', state='RUNNING')

    if status:
        logger.info("Created connector successfully - " + json.dumps(params))
        return True
    else:
        return False


def update_kafka_connector(setup, params, success=True):
    '''
    Update kafka connect connector using kafka connect REST API
    '''
    response = requests.put(url=setup["kafka_connect_url"] + "/connectors/" + params["name"] + "/config",
                            data=json.dumps(params["config"]),
                            headers={'Accept': 'application/json', 'Content-Type': 'application/json'})

    if not success:
        return response.status_code == 200

    status = get_kafka_connector_status(setup, params, action='Update', state='RUNNING')

    if status:
        logger.info("Updated connector successfully - " + json.dumps(params))
        return True
    else:
        return False


def delete_kafka_connector(setup, connector):
    '''
    Delete kafka connect connector using kafka connect REST API
    '''
    if not isinstance(connector, str):
        connector = connector['name']
    response = requests.delete(url=setup["kafka_connect_url"] + "/connectors/" + connector,
                               headers={'Accept': 'application/json', 'Content-Type': 'application/json'})
    if response.status_code == 204:
        logger.info("Deleted connector successfully - " + connector)
        return True

    logger.error("Failed to delete connector: {0}, response code - {1}".format(connector, response.status_code))
    return False


def get_kafka_connector_tasks(setup, params):
    '''
    Get kafka connect connector tasks using kafka connect REST API
    '''

    t_end = time.time() + 10
    while time.time() < t_end:
        response = requests.get(url=setup["kafka_connect_url"] + "/connectors/" + params["name"] + "/tasks",
                                headers={'Accept': 'application/json', 'Content-Type': 'application/json'})
        status = response.status_code
        if status == 200:
            return len(response.json())

    return 0


def get_kafka_connector_status(setup, params, action, state):
    '''
    Get kafka connect connector tasks using kafka connect REST API
    '''
    t_end = time.time() + 10
    while time.time() < t_end:
        response = requests.get(url=setup["kafka_connect_url"] + "/connectors/" + params["name"] + "/status",
                                headers={'Accept': 'application/json', 'Content-Type': 'application/json'})
        content = response.json()
        if content.get('connector'):
            if content['connector']['state'] == state:
                return True

    logger.error("Failed to {} connector and tasks are not in a {} state after 10 seconds".format(action, state))
    return False


def get_running_kafka_connector_task_status(setup, params):
    '''
    Get running kafka connect connector tasks status using kafka connect REST API
    '''
    response = requests.get(url=setup["kafka_connect_url"] + "/connectors/" + params["name"] + "/status",
                            headers={'Accept': 'application/json', 'Content-Type': 'application/json'})
    content = response.json()

    if content.get('connector'):
        if content['connector']['state'] == 'RUNNING':
            task_status = jsonpath.jsonpath(content, '$.tasks.*.state')
            return task_status




def pause_kafka_connector(setup, params, success=True):
    '''
    Pause kafka connect connector using kafka connect REST API
    '''
    response = requests.put(url=setup["kafka_connect_url"] + "/connectors/" + params["name"] + "/pause",
                            headers={'Accept': 'application/json', 'Content-Type': 'application/json'})
    if not success:
        return response.status_code != 202

    status = get_kafka_connector_status(setup, params, action='Pause', state='PAUSED')

    if status:
        logger.info("Paused connector successfully - " + json.dumps(params))
        return True
    else:
        return False


def resume_kafka_connector(setup, params, success=True):
    '''
    Resume kafka connect connector using kafka connect REST API
    '''
    response = requests.put(url=setup["kafka_connect_url"] + "/connectors/" + params["name"] + "/resume",
                            headers={'Accept': 'application/json', 'Content-Type': 'application/json'})
    if not success:
        return response.status_code == 202

    status = get_kafka_connector_status(setup, params, action='Resume', state='RUNNING')

    if status:
        logger.info("Resumed connector successfully - " + json.dumps(params))
        return True
    else:
        return False


def restart_kafka_connector(setup, params, success=True):
    '''
    Restart kafka connect connector using kafka connect REST API
    '''
    response = requests.post(url=setup["kafka_connect_url"] + "/connectors/" + params["name"] + "/restart",
                             headers={'Accept': 'application/json', 'Content-Type': 'application/json'})

    if not success:
        return response.status_code == 202 or response.status_code == 204

    status = get_kafka_connector_status(setup, params, action='Restart', state='RUNNING')

    if status:
        logger.info("Restarted connector successfully - " + json.dumps(params))
        return True
    else:
        return False


def get_running_connector_list(setup):
    # Get the list of running connectors
    content = requests.get(url=setup["kafka_connect_url"] + "/connectors",
                           headers={'Accept': 'application/json', 'Content-Type': 'application/json'})
    content_text = content.text[1:-1].replace('\"', '')
    if not content_text:
        return []

    connectors = content_text.split(',')
    return connectors


def create_kafka_topics(config, topics):
    client = KafkaAdminClient(bootstrap_servers=config["kafka_broker_url"], client_id='test')
    broker_topics = client.list_topics()
    topic_list = []
    for topic in topics:
        if topic not in broker_topics:
            topic_list.append(NewTopic(name=topic, num_partitions=1, replication_factor=1))
    client.create_topics(new_topics=topic_list, validate_only=False)
