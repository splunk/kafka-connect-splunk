#!/bin/bash

git clone https://github.com/dtregonning/kafka-data-gen.git
cd kafka-data-gen && gradle install

java -jar build/libs/kafka-data-gen.jar -message-count ${MESSAGE_COUNT} -message-size ${MESSAGE_SIZE} -topic ${KAFKA_TOPIC} -bootstrap.servers ${KAFKA_BOOTSTRAP_SERVERS} -EPS ${EPS}
