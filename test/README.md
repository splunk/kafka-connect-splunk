
# Prerequsite
* Python version must be > 3.x

# Testing Instructions
0. Use a virtual environment for the test  
    `virtualenv --python=python3.7 venv`  
    `source venv/bin/activate`
1. Install the dependencies  
    `pip install -r test/requirements.txt`  
2. Provided required options at `test/config.yaml`
    **Options are:**  
    splunkd_url
    * Description: splunkd url used to send test data to. Eg: https://localhost:8089  

    splunk_user
    * Description: splunk username  

    splunk_password
    * Description: splunk user password  

    splunk_token
    * Description: splunk hec token  
  
    splunk_token_ack
    * Description: splunk hec token with ack enabled

    splunk_index
    * Description: splunk index   

    kafka_connect_url
    * Description: url used to interact with kafka connect  

    --kafka-topic
    * Description: kafka topic used to get data with kafka connect  

3. Start the test   
    `python -m pytest`