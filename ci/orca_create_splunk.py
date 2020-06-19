from csmslib.client import Service
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
    cmd = "{}/python -m splunk_orca --cloud cloudworks --printer json create --idx 3 --sh 3 --shc".format(_env_var['PYTHON_PATH'])

    try:
        proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        output, error = proc.communicate()
        data = json.loads(output)
        stack_id = jsonpath.jsonpath(data, '$..stack_id')[0]
        if error:
            logger.error(error.strip())
        logger.info('The stack [{0}] is Creating.'.format(stack_id))
        return stack_id
    except OSError as e:
        logger.error(e)


def get_status(stack_id):
    cmd = "{0}/python -m splunk_orca --cloud cloudworks --printer json show containers --deployment-id {1}".\
        format(_env_var['PYTHON_PATH'], stack_id)

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
            logger.info('The stack [{0}] is Ready to use.'.format(stack_id))
            return True


if __name__ == '__main__':
    stack_id = create_cloud_stack()
    wait_until_stack_ready(stack_id)
    sys.stdout.write(stack_id)
