#!/usr/bin/python

import argparse
import kafka_cluster_gen as kcg

DATA_GEN_IMAGE = 'repo.splunk.com/kafka-data-gen:0.1'
KAFKA_IMAGE = 'repo.splunk.com/kafka-cluster:0.11'
KAFKA_CONNECT_IMAGE = 'repo.splunk.com/kafka-connect-splunk:1.0'


class KafkaDataGenYaml(object):

    def __init__(self, image, bootstrap_servers, topic='perf'):
        self.image = image
        self.boostrap_servers = bootstrap_servers
        self.topic = topic
        self.num_of_gen = 1
        self.total_messages = 100 * 1000 * 1000
        self.eps = 5000
        self.message_size = 512
        self.max_jvm_memory = '2G'
        self.min_jvm_memory = '512M'

    def gen(self):
        envs = [
            'KAFKA_BOOTSTRAP_SERVERS={}'.format(self.boostrap_servers),
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


class KafkaOrcaYamlGen(object):

    def __init__(self, args):
        self.args = args

    def _create_data_gen(self, bootstrap_servers):
        data_gen_yaml_gen = KafkaDataGenYaml(
            self.args.data_gen_image, bootstrap_servers,
            self.args.kafka_topic)

        data_gen_yaml_gen.num_of_gen = self.args.data_gen_size
        data_gen_yaml_gen.total_messages = self.args.data_gen_total_events
        data_gen_yaml_gen.eps = self.args.data_gen_eps

        return data_gen_yaml_gen

    def _create_kafka_gen(self):
        kafka_yaml_gen = kcg.KafkaClusterYamlGen(
            self.args.kafka_image, version='2')

        kafka_yaml_gen.num_of_zk = self.args.zookeeper_size
        kafka_yaml_gen.num_of_broker = self.args.broker_size

        kafka_yaml_gen.max_jvm_memory = self.args.max_jvm_memory
        kafka_yaml_gen.min_jvm_memory = self.args.min_jvm_memory

        return kafka_yaml_gen

    def gen(self):
        kafka_yaml_gen = self._create_kafka_gen()
        data_gen_yaml_gen = self._create_data_gen(
            kafka_yaml_gen.bootstrap_servers())

        data_gen_yaml = data_gen_yaml_gen.gen()
        kafka_yaml = kafka_yaml_gen.gen()

        return data_gen_yaml + kafka_yaml


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

    service_file = 'kafka-connect-ci.yaml'
    with open(service_file, 'w') as f:
        f.write(orca_services)

    print 'finish generating orca service yaml file in', service_file


if __name__ == '__main__':
    main()
