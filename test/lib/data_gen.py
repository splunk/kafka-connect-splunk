from lib.helper import get_test_folder
import json
import jinja2
import yaml
import os

_config_path = os.path.join(get_test_folder(), 'config.yaml')
with open(_config_path, 'r') as yaml_file:
    config = yaml.load(yaml_file)


def generate_connector_content(input_disc=None):
    default_disc = \
        {
            "name": "",
            "connector_class": "com.splunk.kafka.connect.SplunkSinkConnector",
            "tasks_max": "1",
            "topics": config["kafka_topic"],
            "splunk_indexes": config["splunk_index"],
            "splunk_sources": "kafka",
            "splunk_hec_uri": config["splunk_hec_url"],
            "splunk_hec_token": config["splunk_token"],
            "splunk_hec_raw": "false",
            "splunk_hec_raw_line_breaker": None,
            "splunk_hec_ack_enabled": "false",
            "splunk_hec_ssl_validate_certs": "false",
            "splunk_hec_json_event_enrichment": None,
            "splunk_header_support": None,
            "splunk_header_custom": None,
            "splunk_header_index": None,
            "splunk_header_source": None,
            "splunk_header_sourcetype": None,
            "splunk_header_host": None,
            "splunk_hec_json_event_formatted": None,
            "splunk_sourcetypes": "kafka",
            "value_converter": "org.apache.kafka.connect.storage.StringConverter",
            "value_converter_schema_registry_url": "",
            "value_converter_schemas_enable": "false",
            "enable_timestamp_extraction": "false",
            "regex": "",
            "timestamp_format": ""
        }

    if input_disc:
        default_disc.update(input_disc)
    data = generate_content(default_disc)
    json_data = json.loads(data, strict=False)
    return json_data


def generate_content(input_dict):
    """
    Use jinja2 template to generate connector content
    """
    env = jinja2.Environment(loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
                             trim_blocks=True)
    config_template = env.get_template('connector.template')
    export = config_template.render(input_dict)
    return export
