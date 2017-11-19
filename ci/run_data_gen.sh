#!/bin/bash

git clone https://github.com/dtregonning/kafka-data-gen.git
cd kafka-data-gen && gradle install

sleep 600

bash /fix_hosts.sh > /tmp/fixhosts 2>&1 &

KAFKA_DATA_GEN_SIZE=${KAFKA_DATA_GEN_SIZE:-1}
KAFKA_DATA_GEN_SIZE=$(($KAFKA_DATA_GEN_SIZE - 1))

# gen an exec shell scrip on the fly

echo "#!/bin/bash" > do_run_data_gen.sh
echo "while :" >> do_run_data_gen.sh
echo "do" >> do_run_data_gen.sh
echo "    java -Xmx${JVM_MAX_HEAP:-4G} -Xms${JVM_MIN_HEAP:-512M} -jar build/libs/kafka-data-gen.jar -message-count ${MESSAGE_COUNT} -message-size ${MESSAGE_SIZE} -topic ${KAFKA_TOPIC} -bootstrap.servers ${KAFKA_BOOTSTRAP_SERVERS} -eps ${EPS}" >> do_run_data_gen.sh
echo "    sleep 1" >> do_run_data_gen.sh
echo "done" >> do_run_data_gen.sh

chmod +x do_run_data_gen.sh

for i in `seq ${KAFKA_DATA_GEN_SIZE}`
do
    bash do_run_data_gen.sh &
done

bash do_run_data_gen.sh
