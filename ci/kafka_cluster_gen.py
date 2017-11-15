#!/usr/bin/python

import argparse


class KafkaClusterYamlGen(object):

    def __init__(self, image, version='2'):
        self.image = image
        self.version = version

        self.num_of_zk = 5
        self.zk_prefix = 'zookeeper'
        self.zk_opts = [
            # 'ZOOKEEPER_myid=1',
            'ZOOKEEPER_initLimit=5',
            'ZOOKEEPER_syncLimit=2',
            'ZOOKEEPER_dataDir=/zookeeperdata',
            # 'ZOOKEEPER_servers=server.1=zookeeper1:2888:3888,server.2=zookeeper2:2888:3888,server.3=zookeeper3:2888:3888',
        ]

        self.num_of_broker = 5
        self.broker_prefix = 'kafka'
        self.broker_opts = [
            'KAFKA_listeners=PLAINTEXT://:9092',
            # 'KAFKA_advertised_listeners=PLAINTEXT://kafka1:9092',
            'KAFKA_log_dirs=/kafkadata',
            'KAFKA_num_partitions=3',
            'KAFKA_delete_topic_enable=true',
            'KAFKA_auto_create_topics_enable=true',
            # 'KAFKA_zookeeper_connect=zookeeper1:2181,zookeeper2:2181,zookeeper3:2181',
        ]

        self.max_jvm_memory = '6G'
        self.min_jvm_memory = '512M'

    def bootstrap_servers(self):
        return ','.join(
            '{prefix}{kid}:9092'.format(prefix=self.broker_prefix, kid=i + 1)
            for i in xrange(self.num_of_broker))

    def gen(self):
        '''
        @return: docker compose yaml string in version 2 or 3 format
        '''

        yaml_lines = self._do_gen()
        if self.version >= '3':
            for i, lin in enumerate(yaml_lines):
                if lin != '\n':
                    yaml_lines[i] = '  ' + lin

            yaml_lines.insert(0, 'version: \'{}\'\n'.format(self.version))
            yaml_lines.insert(0, 'services:\n')
        return '\n'.join(yaml_lines)

    def _do_gen(self):
        zk_yaml = self._do_gen_zk()
        broker_yaml = self._do_gen_broker()
        zk_yaml.extend(broker_yaml)
        return zk_yaml

    def _do_gen_zk(self):
        zk_servers = self._get_zk_servers()
        self.zk_opts.insert(0, 'RUN=zookeeper')
        self.zk_opts.insert(1, self._get_jvm_memory())
        self.zk_opts.append('ZOOKEEPER_servers={}'.format(zk_servers))

        def add_myid(service, service_idx):
            myid = '    - ZOOKEEPER_myid={}'.format(service_idx)
            service.append(myid)

        return gen_services(
            self.num_of_zk, self.zk_prefix, self.image, [2181, 2888, 3888],
            self.zk_opts, add_myid)

    def _do_gen_broker(self):
        def add_advertise_name_and_id(service, service_idx):
            adname = '    - KAFKA_advertised_listeners=PLAINTEXT://{}{}:9092'.format(
                self.broker_prefix, service_idx)
            service.append(adname)
            bid = '    - KAFKA_broker_id={}'.format(service_idx - 1)
            service.append(bid)

        self.broker_opts.insert(0, 'RUN=kafka')
        self.broker_opts.insert(1, self._get_jvm_memory())
        zk_connect = self._get_zk_connect_setting()
        self.broker_opts.append(
            'KAFKA_zookeeper_connect={}'.format(zk_connect))

        return gen_services(
            self.num_of_broker, self.broker_prefix, self.image, [9092],
            self.broker_opts, add_advertise_name_and_id)

    def _get_jvm_memory(self):
        return 'KAFKA_HEAP_OPTS=-Xmx{} -Xms{}'.format(
            self.max_jvm_memory, self.min_jvm_memory)

    def _get_zk_servers(self):
        zk_servers = []
        for i in xrange(self.num_of_zk):
            zk_server = 'server.{kid}={prefix}{kid}:2888:3888'.format(
                kid=i + 1, prefix=self.zk_prefix)
            zk_servers.append(zk_server)
        return ','.join(zk_servers)

    def _get_zk_connect_setting(self):
        zk_connect_settings = []
        for i in xrange(self.num_of_zk):
            zk_connect_settings.append(
                '{prefix}{kid}:2181'.format(prefix=self.zk_prefix, kid=i + 1))
        return ','.join(zk_connect_settings)


def gen_services(num, prefix, image, ports, envs, callback):
    services = []
    for i in xrange(1, num + 1):
        name = '{}{}'.format(prefix, i)
        service = [
            '{}:'.format(name),
            '  image: {}'.format(image),
            '  hostname: {}'.format(name),
            '  container_name: {}'.format(name),
        ]

        if ports:
            service.append('  ports:')

            # ports
            for port in ports:
                service.append('    - "{}"'.format(port))

        # envs
        if envs:
            service.append('  environment:')
            for env in envs:
                service.append('    - {}'.format(env))

        if callback is not None:
            callback(service, i)

        service.append('  restart: always')
        service.append('\n')
        services.extend(service)
    return services


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', default='2',
                        help='[2|3]. Docker compose file version 2 or 3')
    parser.add_argument('--image', default='zlchen/kafka-cluster:0.11',
                        help='Docker image')
    parser.add_argument('--broker_size', type=int, default=5,
                        help='number of kafka brokers')
    parser.add_argument('--zookeeper_size', type=int, default=5,
                        help='number of zookeeper')
    parser.add_argument('--max_jvm_memory', default="6G",
                        help='Max JVM memory, by default it is 6G')
    parser.add_argument('--min_jvm_memory', default="512M",
                        help='Min JVM memory, by default it is 512M')

    args = parser.parse_args()
    gen = KafkaClusterYamlGen(
        args.image, args.version)

    gen.num_of_zk = args.zookeeper_size
    gen.num_of_broker = args.broker_size

    gen.max_jvm_memory = args.max_jvm_memory
    gen.min_jvm_memory = args.min_jvm_memory

    yaml = gen.gen()

    yaml_file = 'kafka_cluster_gen.yaml'
    with open(yaml_file, 'w') as f:
        f.write(yaml)

    print 'finish generating kafka cluster yaml file in ', yaml_file


if __name__ == '__main__':
    main()
