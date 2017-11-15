#!/bin/bash

git clone https://github.com/dtregonning/kafka-data-gen.git
cd kafka-data-gen && gradle install

sleep 60

while :
do
    java -Xmx${JVM_MAX_HEAP:-4G} -Xms${JVM_MIN_HEAP:-512M} -jar build/libs/kafka-data-gen.jar -message-count ${MESSAGE_COUNT} -message-size ${MESSAGE_SIZE} -topic ${KAFKA_TOPIC} -bootstrap.servers ${KAFKA_BOOTSTRAP_SERVERS} -EPS ${EPS}
    sleep 1
done
