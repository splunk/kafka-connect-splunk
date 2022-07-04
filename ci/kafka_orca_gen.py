#!/usr/bin/python

import argparse
import json
import kafka_cluster_gen as kcg

DATA_GEN_IMAGE = 'repo.splunk.com/kafka-data-gen:0.4'
KAFKA_IMAGE = 'repo.splunk.com/kafka-cluster:0.12'
KAFKA_CONNECT_IMAGE = 'repo.splunk.com/kafka-connect-splunk:1.8'
KAFKA_BASTION_IMAGE = 'repo.splunk.com/kafka-bastion:1.8'


def gen_depends_from(bootstrap_servers):
    return [sp.split(':')[0].strip()
            for sp in bootstrap_servers.split(',')]


class KafkaDataGenYamlGen(object):
    DATA_GEN_PER_HOST = 50

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
        if self.num_of_gen < self.DATA_GEN_PER_HOST:
            data_gen_size = self.num_of_gen
            num_of_host = 1
        else:
            data_gen_size = self.DATA_GEN_PER_HOST
            num_of_host = self.num_of_gen / self.DATA_GEN_PER_HOST

        envs = [
            f'KAFKA_BOOTSTRAP_SERVERS={self.bootstrap_servers}',
            f'KAFKA_TOPIC={self.topic}',
            f'MESSAGE_COUNT={self.total_messages}',
            f'EPS={self.eps}',
            f'MESSAGE_SIZE={self.message_size}',
            f'JVM_MAX_HEAP=2G',
            f'JVM_MIN_HEAP=512M',
            f'KAFKA_DATA_GEN_SIZE={data_gen_size}',
        ]
        depends = gen_depends_from(self.bootstrap_servers)
        services = kcg.gen_services(
            num_of_host, 'kafkagen', self.image, [], envs, depends, [8080],
            None, None)
        return '\n'.join(services)


class KafkaConnectYamlGen(object):
    prefix = 'kafkaconnect'

    def __init__(self, image, bootstrap_servers):
        self.image = image
        self.bootstrap_servers = bootstrap_servers
        self.branch = 'develop'
        self.logging_level = 'DEBUG'
        self.num_of_connect = 3
        self.max_jvm_memory = '6G'
        self.min_jvm_memory = '512M'

    def gen(self):
        jvm_mem = f'KAFKA_HEAP_OPTS=-Xmx{self.max_jvm_memory} -Xms{self.min_jvm_memory}'

        envs = [
            f'KAFKA_BOOTSTRAP_SERVERS={self.bootstrap_servers}',
            jvm_mem,
            f'KAFKA_CONNECT_LOGGING={self.logging_level}',
            f'KAFKA_CONNECT_BRANCH={self.branch}',
            # for proc monitor
            'SPLUNK_HOST=https://heclb1:8088',
            'SPLUNK_TOKEN=00000000-0000-0000-0000-000000000000',
            'TARGETS=java',
        ]
        depends = gen_depends_from(self.bootstrap_servers)
        services = kcg.gen_services(
            self.num_of_connect, self.prefix, self.image,
            [8083], envs, depends, [8083], None, None)
        return '\n'.join(services)


class KafkaBastionYamlGen(object):

    def __init__(self, image, num_of_indexer, num_of_connect):
        self.image = image
        self.num_of_indexer = num_of_indexer
        self.num_of_connect = num_of_connect
        self.branch = 'develop'
        self.batch_size = 500
        self.line_breaker = '@@@@'
        self.hec_mode = 'event'
        self.ack_mode = 'no_ack'
        self.topic = 'perf'
        self.metric_dest_hec_uri = ''
        self.metric_dest_hec_token = ''
        self.jvm_size = '8G'

    def gen(self):
        envs = [
            f'INDEX_CLUSTER_SIZE={self.num_of_indexer}',
            f'KAFKA_CONNECT_HEC_MODE={self.hec_mode.lower()}',
            f'KAFKA_CONNECT_ACK_MODE={self.ack_mode.lower()}',
            f'KAFKA_CONNECT_TOPICS={self.topic}',
            f'KAFKA_CONNECT_LINE_BREAKER={self.line_breaker}',
            f'JVM_HEAP_SIZE={self.jvm_size}',
            f'KAFKA_CONNECT_BRANCH={self.branch}',
            f'CONNECT_PERF_METRIC_DEST_HEC={self.metric_dest_hec_uri}',
            f'CONNECT_PERF_METRIC_TOKEN={self.metric_dest_hec_token}',
        ]

        depends = [f'{KafkaConnectYamlGen.prefix}{i}'
                   for i in xrange(1, self.num_of_connect + 1)]
        services = kcg.gen_services(
            1, 'kafkabastion', self.image, [], envs, depends,
            [8080], None, None)
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
        gen = kcg.KafkaClusterYamlGen(
            self.args.kafka_image, version='2',
            volumes=json.loads(self.args.volumes))

        gen.num_of_zk = self.args.zookeeper_size
        gen.num_of_broker = self.args.broker_size
        gen.num_of_partition = self.args.default_partitions

        gen.max_jvm_memory = self.args.kafka_max_jvm_memory
        gen.min_jvm_memory = self.args.kafka_min_jvm_memory

        return gen

    def _create_kafka_connect_gen(self, bootstrap_servers):
        gen = KafkaConnectYamlGen(
            self.args.kafka_connect_image, bootstrap_servers)

        gen.num_of_connect = self.args.kafka_connect_size
        gen.max_jvm_memory = self.args.kafka_connect_max_jvm_memory
        gen.min_jvm_memory = self.args.kafka_connect_min_jvm_memory
        gen.branch = self.args.kafka_connect_branch
        gen.logging_level = self.args.kafka_connect_logging

        return gen

    def _create_kafka_bastion_gen(self):
        gen = KafkaBastionYamlGen(
            self.args.kafka_bastion_image, self.args.indexer_size,
            self.args.kafka_connect_size)

        gen.hec_mode = self.args.kafka_connect_hec_mode
        gen.ack_mode = self.args.kafka_connect_ack_mode
        gen.jvm_size = self.args.kafka_connect_max_jvm_memory
        gen.topic = self.args.kafka_topic
        gen.line_breaker = self.args.kafka_connect_line_breaker
        gen.branch = self.args.kafka_connect_branch
        gen.metric_dest_hec_uri = self.args.metric_dest_hec_uri
        gen.metric_dest_hec_token = self.args.metric_dest_hec_token

        return gen

    def gen(self):
        kafka_yaml_gen = self._create_kafka_gen()
        data_gen_yaml_gen = self._create_data_gen(
            kafka_yaml_gen.bootstrap_servers())
        kafka_connect_yaml_gen = self._create_kafka_connect_gen(
            kafka_yaml_gen.bootstrap_servers())
        kafka_bastion_yaml_gen = self._create_kafka_bastion_gen()

        kafka_yaml = kafka_yaml_gen.gen()
        data_gen_yaml = data_gen_yaml_gen.gen()
        kafka_connect_yaml = kafka_connect_yaml_gen.gen()
        kafka_bastion_yaml = kafka_bastion_yaml_gen.gen()

        return kafka_yaml + data_gen_yaml + kafka_connect_yaml + kafka_bastion_yaml


def _gen_service_file(args, service_file):
    gen = KafkaOrcaYamlGen(args)

    orca_services = gen.gen()

    with open(service_file, 'w') as f:
        f.write(orca_services)

    print 'finish generating orca service yaml file in', service_file


def _gen_orca_file(args, service_file):
    lines = []
    with open('orca.conf', 'w') as f:
        lines.append('[kafka-connect]')
        lines.append('hec_load_balancers = 1')
        lines.append('search_heads = 1')
        lines.append(f'indexers = {args.indexer_size}')
        lines.append('log_token = 00000000-0000-0000-0000-000000000000')
        if args.perf == 1:
            lines.append('perf = true')
        lines.append(f'services = {service_file}')
        f.write('\n'.join(lines))

    print 'finish generating orca.conf'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--perf', type=int, default=1,
                        help='[0|1] Perf mode or not')
    parser.add_argument('--indexer_size', type=int, default=30,
                        help='Indexer cluster size')
    parser.add_argument('--data_gen_image', default=DATA_GEN_IMAGE,
                        help='Kafka data gen docker image')
    parser.add_argument('--data_gen_eps', type=int, default=100000,
                        help='Event per second for Kafka data gen')
    parser.add_argument('--data_gen_total_events', type=int, default=1000000000,
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
    parser.add_argument('--kafka_connect_branch', default='develop',
                        help='Code repo branch')
    parser.add_argument('--kafka_connect_logging', default='DEBUG',
                        help='Logging level')
    parser.add_argument('--kafka_connect_size', type=int, default=3,
                        help='number of Kafka connect')
    parser.add_argument('--kafka_connect_max_jvm_memory', default="8G",
                        help='Max JVM memory, by default it is 8G')
    parser.add_argument('--kafka_connect_min_jvm_memory', default="512M",
                        help='Min JVM memory, by default it is 512M')

    parser.add_argument('--kafka_bastion_image', default=KAFKA_BASTION_IMAGE,
                        help='Kafka bastion docker image')
    parser.add_argument('--kafka_connect_hec_mode', default='event',
                        help='[raw|event|raw_and_event] test /raw or /event or both')
    parser.add_argument('--kafka_connect_ack_mode', default='ack',
                        help='[ack|no_ack|ack_and_no_ack] test HEC with ack')
    parser.add_argument('--kafka_connect_line_breaker', default='@@@@',
                        help='/raw event line breaker')

    parser.add_argument('--kafka_max_jvm_memory', default='8G',
                        help='Max JVM memory, by default it is 8G')
    parser.add_argument('--kafka_min_jvm_memory', default="512M",
                        help='Min JVM memory, by default it is 512M')

    parser.add_argument('--metric_dest_hec_uri', required=True,
                        help='Splunk HEC destintion where to export the perf metrics')
    parser.add_argument('--metric_dest_hec_token', required=True,
                        help='Splunk HEC destintion token')


    volumes = f'["{kcg.KafkaClusterYamlGen.DATA_DIR_ROOT}"]'
    parser.add_argument('--volumes', default=volumes, help='Volumes to mount')

    args = parser.parse_args()
    service_file = 'kafka-connect-ci.yml'
    _gen_service_file(args, service_file)
    _gen_orca_file(args, service_file)


if __name__ == '__main__':
    main()
