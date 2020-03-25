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

import logging
import requests
import sys

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s -' +
                              ' %(levelname)s - %(message)s')
handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(formatter)
logger.addHandler(handler)

def create_kafka_connector(setup, params):
    '''
    send a search request to splunk and return the events from the result
    '''

    # sending get request and saving the response as response object
    r = requests.get(url=setup["kafka_connect_url"], params=params)

    # extracting data in json format
    data = r.json()

    # printing the output
    print(data)

    return events
