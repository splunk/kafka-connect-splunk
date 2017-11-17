#!/usr/bin/python

import argparse
import kafka_cluster_gen as kcg

DATA_GEN_IMAGE = 'repo.splunk.com/kafka-data-gen:0.2'
KAFKA_IMAGE = 'repo.splunk.com/kafka-cluster:0.12'
KAFKA_CONNECT_IMAGE = 'repo.splunk.com/kafka-connect-splunk:1.1'
KAFKA_BASTION_IMAGE = 'repo.splunk.com/kafka-bastion:1.2'


def gen_depends_from(bootstrap_servers):
    return [sp.split(':')[0].strip()
            for sp in bootstrap_servers.split(',')]


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
            'KAFKA_DATA_GEN_SIZE={}'.format(self.num_of_gen / 2),
        ]
        depends = gen_depends_from(self.bootstrap_servers)
        services = kcg.gen_services(
            2, 'kafkagen', self.image, [], envs, depends, [8080], None)
        return '\n'.join(services)


class KafkaConnectYamlGen(object):
    prefix = 'kafkaconnect'

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
        depends = gen_depends_from(self.bootstrap_servers)
        services = kcg.gen_services(
            self.num_of_connect, self.prefix, self.image,
            [8083], envs, depends, [8083], None)
        return '\n'.join(services)


class KafkaBastionYamlGen(object):

    def __init__(self, image, num_of_indexer, num_of_connect):
        self.image = image
        self.num_of_indexer = num_of_indexer
        self.num_of_connect = num_of_connect
        self.batch_size = 500
        self.line_breaker = '@@@@'
        self.raw = False
        self.topic = 'perf'
        self.max_tasks = 30

    def gen(self):
        envs = [
            'INDEX_CLUSTER_SIZE={}'.format(self.num_of_indexer),
            'KAFKA_CONNECT_RAW={}'.format(str(self.raw).lower()),
            'KAFKA_CONNECT_TOPICS={}'.format(self.topic),
            'KAFKA_CONNECT_TASKS_MAX={}'.format(self.max_tasks),
            'KAFKA_CONNECT_LINE_BREAKER={}'.format(self.line_breaker),
            'KAFKA_CONNECT_BATCH_SIZE={}'.format(self.batch_size),
        ]

        depends = ['{}{}'.format(KafkaConnectYamlGen.prefix, i)
                   for i in xrange(1, self.num_of_connect + 1)]
        services = kcg.gen_services(
            1, 'kafkabastion', self.image, [], envs, depends, [8080], None)
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

    def _create_kafka_bastion_gen(self):
        gen = KafkaBastionYamlGen(
            self.args.kafka_bastion_image, self.args.indexer_size,
            self.args.kafka_connect_size)

        gen.raw = self.args.kafka_connect_raw == 1
        gen.max_tasks = self.args.kafka_connect_max_tasks
        gen.topic = self.args.kafka_topic
        gen.line_breaker = self.args.kafka_connect_line_breaker
        gen.batch_size = self.args.kafka_connect_batch_size

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
        lines.append('search_heads = 1')
        lines.append('indexers = {}'.format(args.indexer_size))
        lines.append('log_token = 00000000-0000-0000-0000-000000000000')
        if args.perf == 0:
            lines.append('memory = 8')
            lines.append('swap_memory = 20')
            lines.append('cpu = 8')
            lines.append('disk = fast')
        else:
            lines.append('perf = true')
        lines.append('services = {}'.format(service_file))
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

    parser.add_argument('--kafka_bastion_image', default=KAFKA_BASTION_IMAGE,
                        help='Kafka bastion docker image')
    parser.add_argument('--kafka_connect_raw', type=int, default=0,
                        help='[0|1] use /raw HEC endpoint')
    parser.add_argument('--kafka_connect_max_tasks', type=int, default=30,
                        help='Max number of data collection tasks')
    parser.add_argument('--kafka_connect_batch_size', type=int, default=500,
                        help='HEC batch size')
    parser.add_argument('--kafka_connect_line_breaker', default='@@@@',
                        help='/raw event line breaker')

    parser.add_argument('--max_jvm_memory', default="6G",
                        help='Max JVM memory, by default it is 6G')
    parser.add_argument('--min_jvm_memory', default="512M",
                        help='Min JVM memory, by default it is 512M')

    args = parser.parse_args()
    service_file = 'kafka-connect-ci.yml'
    _gen_service_file(args, service_file)
    _gen_orca_file(args, service_file)


if __name__ == '__main__':
    main()
