
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

    splunk_hec_url
    * Description: splunk HTTP Event Collector's address and port. Eg: https://127.0.0.1:8088

    splunk_user
    * Description: splunk username  

    splunk_token
    * Description: splunk hec token  
  
    splunk_token_ack
    * Description: splunk hec token with ack enabled

    splunk_index
    * Description: splunk index to ingest test data

    kafka_broker_url
    * address of kafka broker. Eg: 127.0.0.1:9092
    
    kafka_connect_url
    * Description: url used to interact with kafka connect  

    kafka-topic
    * Description: kafka topic used to get data with kafka connect  

3. Start the test
    `python -m pytest`