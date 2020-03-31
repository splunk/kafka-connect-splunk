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

import pytest

def pytest_addoption(parser):
    parser.addoption("--splunk-url",
                     help="splunk url used to send test data to. \
                          Eg: https://localhost:8089",
                     default="https://localhost:8089")
    parser.addoption("--splunk-user",
                     help="splunk username",
                     default="admin")
    parser.addoption("--splunk-password",
                     help="splunk user password",
                     default="password")
    parser.addoption("--splunk-token",
                     help="splunk hec token")
    parser.addoption("--splunk-index",
                     help="splunk index",
                     default="main")
    parser.addoption("--kafka-connect-url",
                     help="url used to interact with kafka connect. \
                          Eg: http://localhost:8083",
                     default="http://localhost:8083")
    parser.addoption("--kafka-topic",
                     help="kafka topic used to get data with kafka connect")


@pytest.fixture(scope="function")
def setup(request):
    config = {}
    config["splunk_url"] = request.config.getoption("--splunk-url")
    config["splunk_user"] = request.config.getoption("--splunk-user")
    config["splunk_password"] = request.config.getoption("--splunk-password")
    config["splunk_token"] = request.config.getoption("--splunk-token")
    config["splunk_index"] = request.config.getoption("--splunk-index")
    config["kafka_connect_url"] = request.config.getoption("--kafka-connect-url")
    config["kafka_topic"] = request.config.getoption("--kafka-topic")

    return config