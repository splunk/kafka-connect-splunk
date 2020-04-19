# Common functions used in this project

import os


def get_test_folder():
    """
    returns the test folder
    """
    return os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))

