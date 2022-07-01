import argparse
import time
import os
import logging
import subprocess
import json
import jsonpath
import sys

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
_env_var = os.environ


def create_cloud_stack():
    cmd = f"python3 -m splunk_orca --cloud cloudworks --printer json create --splunk-version {_env_var['SPLUNK_VERSION']}"
    try:
        proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        output, error = proc.communicate()
        logger.info(output)
        data = json.loads(output)
        stack_id = jsonpath.jsonpath(data, '$..stack_id')[0]
        if error:
            logger.error(error.strip())
        logger.info(f'The stack [{stack_id}] is Creating.')
        return stack_id
    except OSError as e:
        logger.error(e)


def get_status(stack_id):
    cmd = f"python3 -m splunk_orca --cloud cloudworks --printer json show containers --deployment-id {stack_id}"

    try:
        proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        output, error = proc.communicate()
        data = json.loads(output)
        status = jsonpath.jsonpath(data, '$..status')[0]
        if error:
            logger.error(error.strip())
        return status
    except OSError as e:
        logger.error(e)


def wait_until_stack_ready(stack_id):
    t_end = time.time() + 3600
    while time.time() < t_end:
        status = get_status(stack_id)
        if status == 'READY':
            logger.info(f'The stack [{stack_id}] is Ready to use.')
            return
    logger.error(f"Time out when creating Splunk cloud stack: {stack_id}")


if __name__ == '__main__':
    stack_id = create_cloud_stack()
    wait_until_stack_ready(stack_id)
    sys.stdout.write(stack_id)
