#!/usr/bin/python

import argparse
import kafka_cluster_gen as kcg

DATA_GEN_IMAGE = 'repo.splunk.com/kafka-data-gen:0.1'
KAFKA_IMAGE = 'repo.splunk.com/kafka-cluster:0.11'
KAFKA_CONNECT_IMAGE = 'repo.splunk.com/kafka-connect-splunk:1.0'


class KafkaDataGenYamlGen(object):

    def __init__(self, image, bootstrap_servers, topic='perf'):
        self.image = image
        self.bootstrap_servers = bootstrap_servers
        self.topic = topic
        self.num_of_gen = 1
        self.total_messages = 100 * 1000 * 1000
        self.eps = 5000
        self.message_size = 512
        self.max_jvm_memory = '2G'
        self.min_jvm_memory = '512M'

    def gen(self):
        envs = [
            'KAFKA_BOOTSTRAP_SERVERS={}'.format(self.bootstrap_servers),
            'KAFKA_TOPIC={}'.format(self.topic),
            'MESSAGE_COUNT={}'.format(self.total_messages),
            'EPS={}'.format(self.eps),
            'MESSAGE_SIZE={}'.format(self.message_size),
            'JVM_MAX_HEAP=2G',
            'JVM_MIN_HEAP=512M',
        ]
        services = kcg.gen_services(
            self.num_of_gen, 'kafkagen', self.image, [], envs, None)
        return '\n'.join(services)


class KafkaConnectYamlGen(object):

    def __init__(self, image, bootstrap_servers):
        self.image = image
        self.bootstrap_servers = bootstrap_servers
        self.num_of_connect = 3
        self.max_jvm_memory = '6G'
        self.min_jvm_memory = '512M'

    def gen(self):
        jvm_mem = 'KAFKA_HEAP_OPTS=-Xmx{} -Xms{}'.format(
            self.max_jvm_memory, self.min_jvm_memory)

        envs = [
            'KAFKA_BOOTSTRAP_SERVERS={}'.format(self.bootstrap_servers),
            jvm_mem,
        ]
        services = kcg.gen_services(
            self.num_of_connect, 'kafkaconnect', self.image, [8083], envs, None)
        return '\n'.join(services)


class KafkaOrcaYamlGen(object):

    def __init__(self, args):
        self.args = args

    def _create_data_gen(self, bootstrap_servers):
        gen = KafkaDataGenYamlGen(
            self.args.data_gen_image, bootstrap_servers,
            self.args.kafka_topic)

        gen.num_of_gen = self.args.data_gen_size
        gen.total_messages = self.args.data_gen_total_events
        gen.eps = self.args.data_gen_eps

        return gen

    def _create_kafka_gen(self):
        gen = kcg.KafkaClusterYamlGen(self.args.kafka_image, version='2')

        gen.num_of_zk = self.args.zookeeper_size
        gen.num_of_broker = self.args.broker_size
        gen.num_of_partition = self.args.default_partitions

        gen.max_jvm_memory = self.args.max_jvm_memory
        gen.min_jvm_memory = self.args.min_jvm_memory

        return gen

    def _create_kafka_connect_gen(self, bootstrap_servers):
        gen = KafkaConnectYamlGen(
            self.args.kafka_connect_image, bootstrap_servers)

        gen.num_of_connect = self.args.kafka_connect_size
        gen.max_jvm_memory = self.args.max_jvm_memory
        gen.min_jvm_memory = self.args.min_jvm_memory

        return gen

    def gen(self):
        kafka_yaml_gen = self._create_kafka_gen()
        data_gen_yaml_gen = self._create_data_gen(
            kafka_yaml_gen.bootstrap_servers())
        kafka_connect_yaml_gen = self._create_kafka_connect_gen(
            kafka_yaml_gen.bootstrap_servers())

        data_gen_yaml = data_gen_yaml_gen.gen()
        kafka_yaml = kafka_yaml_gen.gen()
        kafka_connect_yaml = kafka_connect_yaml_gen.gen()

        return data_gen_yaml + kafka_yaml + kafka_connect_yaml


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--data_gen_image', default=DATA_GEN_IMAGE,
                        help='Kafka data gen docker image')
    parser.add_argument('--data_gen_eps', type=int, default=10000,
                        help='Event per second for Kafka data gen')
    parser.add_argument('--data_gen_total_events', type=int, default=100000000,
                        help='Total events for Kafka data gen')
    parser.add_argument('--data_gen_size', type=int, default=2,
                        help='number of Kafka data gen')
    parser.add_argument('--kafka_topic', default='perf',
                        help='Kafka topic to inject data for Kafka data gen')

    parser.add_argument('--kafka_image', default=KAFKA_IMAGE,
                        help='Kafka cluster docker image')
    parser.add_argument('--default_partitions', type=int, default=300,
                        help='Default number of partitions for new topic')
    parser.add_argument('--broker_size', type=int, default=5,
                        help='number of kafka brokers')
    parser.add_argument('--zookeeper_size', type=int, default=5,
                        help='number of zookeeper')

    parser.add_argument('--kafka_connect_image', default=KAFKA_CONNECT_IMAGE,
                        help='Kafka connect docker image')
    parser.add_argument('--kafka_connect_size', type=int, default=3,
                        help='number of Kafka connect')

    parser.add_argument('--max_jvm_memory', default="6G",
                        help='Max JVM memory, by default it is 6G')
    parser.add_argument('--min_jvm_memory', default="512M",
                        help='Min JVM memory, by default it is 512M')

    args = parser.parse_args()
    gen = KafkaOrcaYamlGen(args)

    orca_services = gen.gen()

    service_file = 'kafka-connect-ci.yml'
    with open(service_file, 'w') as f:
        f.write(orca_services)

    print 'finish generating orca service yaml file in', service_file


if __name__ == '__main__':
    main()
