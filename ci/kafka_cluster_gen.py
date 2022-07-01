#!/usr/bin/python

import argparse


class KafkaClusterYamlGen(object):

    DATA_DIR_ROOT = '/kafkadata'

    def __init__(self, image, version='2', volumes=None):
        self.image = image
        self.version = version
        self.volumes = volumes

        self.num_of_zk = 5
        self.zk_prefix = 'zookeeper'
        self.zk_opts = [
            # 'ZOOKEEPER_myid=1',
            'ZOOKEEPER_initLimit=5',
            'ZOOKEEPER_syncLimit=2',
            f'ZOOKEEPER_dataDir={self.DATA_DIR_ROOT}/zookeeper',
            # 'ZOOKEEPER_servers=server.1=zookeeper1:2888:3888,server.2=zookeeper2:2888:3888,server.3=zookeeper3:2888:3888',
        ]

        self.num_of_broker = 5
        self.num_of_partition = 300
        self.broker_prefix = 'kafka'
        self.broker_opts = [
            'KAFKA_listeners=PLAINTEXT://:9092',
            # 'KAFKA_advertised_listeners=PLAINTEXT://kafka1:9092',
            f'KAFKA_log_dirs={self.DATA_DIR_ROOT}/kafkadata',
            # 'KAFKA_num_partitions=3',
            'KAFKA_delete_topic_enable=true',
            'KAFKA_auto_create_topics_enable=true',
            # 'KAFKA_zookeeper_connect=zookeeper1:2181,zookeeper2:2181,zookeeper3:2181',
        ]

        self.max_jvm_memory = '6G'
        self.min_jvm_memory = '512M'

    def bootstrap_servers(self):
        return ','.join(
            f'{self.broker_prefix}{i + 1}:9092'
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

            yaml_lines.insert(0, f'version: \'{self.version}\'\n')
            yaml_lines.insert(0, 'services:\n')
        return '\n'.join(yaml_lines)

    def _do_gen(self):
        zk_yaml = self._do_gen_zk()
        broker_yaml = self._do_gen_broker()
        zk_yaml.extend(broker_yaml)
        return zk_yaml

    def _do_gen_zk(self):
        self.zk_opts.insert(0, 'RUN=zookeeper')
        self.zk_opts.insert(1, self._get_jvm_memory())

        def add_myid(service, service_idx):
            myid = f'    - ZOOKEEPER_myid={service_idx}'
            service.append(myid)
            zk_servers = self._get_zk_servers(service_idx)
            service.append(f'    - ZOOKEEPER_servers={zk_servers}')

        return gen_services(
            self.num_of_zk, self.zk_prefix, self.image, [2181, 2888, 3888],
            self.zk_opts, [], [2181, 2888, 3888], self.volumes, add_myid)

    def _do_gen_broker(self):
        def add_advertise_name_and_id(service, service_idx):
            adname = f'    - KAFKA_advertised_listeners=PLAINTEXT://{self.broker_prefix}{service_idx}:9092'
            service.append(adname)
            bid = f'    - KAFKA_broker_id={service_idx - 1}'
            service.append(bid)

        self.broker_opts.insert(0, 'RUN=kafka')
        self.broker_opts.insert(1, self._get_jvm_memory())
        self.broker_opts.append(
            f'KAFKA_num_partitions={self.num_of_partition}')
        zk_connect = self._get_zk_connect_setting()
        self.broker_opts.append(
            f'KAFKA_zookeeper_connect={zk_connect}')
        depends = [f'{self.zk_prefix}{i}'
                   for i in xrange(1, self.num_of_zk + 1)]

        return gen_services(
            self.num_of_broker, self.broker_prefix, self.image, [9092],
            self.broker_opts, depends, [9092], self.volumes,
            add_advertise_name_and_id)

    def _get_jvm_memory(self):
        return f'KAFKA_HEAP_OPTS=-Xmx{self.max_jvm_memory} -Xms{self.min_jvm_memory}'

    def _get_zk_servers(self, cur_idx):
        zk_servers = []
        for i in xrange(1, self.num_of_zk + 1):
            if i != cur_idx:
                hname = f'{self.zk_prefix}{i}'
            else:
                hname = '0.0.0.0'

            zk_server = f'server.{i}={hname}:2888:3888'
            zk_servers.append(zk_server)
        return ','.join(zk_servers)

    def _get_zk_connect_setting(self):
        zk_connect_settings = []
        for i in xrange(self.num_of_zk):
            zk_connect_settings.append(
                f'{self.zk_prefix}{i + 1}:2181')
        return ','.join(zk_connect_settings)


def gen_services(num, prefix, image, ports, envs,
                 depends, exposed_ports, volumes, callback):
    services = []
    for i in xrange(1, num + 1):
        name = f'{prefix}{i}'
        service = [
            f'{name}:',
            f'  image: {image}',
            f'  hostname: {name}',
            f'  container_name: {name}',
        ]

        # exposed ports
        if exposed_ports:
            service.append('  expose:')
            for port in exposed_ports:
                service.append(f'    - "{port}"')

        # ports
        if ports:
            service.append('  ports:')
            for port in ports:
                service.append(f'    - "{port}"')

        # depends
        if depends:
            service.append('  depends_on:')
            for dep in depends:
                service.append(f'    - {dep}')

        # volumes
        if volumes:
            service.append('  volumes:')
            for vol in volumes:
                service.append(f'    - {vol}')

        # envs
        if envs:
            service.append('  environment:')
            for env in envs:
                service.append(f'    - {env}')

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
                        help='Number of kafka brokers')
    parser.add_argument('--zookeeper_size', type=int, default=5,
                        help='Number of zookeeper')
    parser.add_argument('--default_partitions', type=int, default=300,
                        help='Default number of partitions for new topic')
    parser.add_argument('--max_jvm_memory', default="6G",
                        help='Max JVM memory, by default it is 6G')
    parser.add_argument('--min_jvm_memory', default="512M",
                        help='Min JVM memory, by default it is 512M')

    args = parser.parse_args()
    gen = KafkaClusterYamlGen(
        args.image, args.version)

    gen.num_of_zk = args.zookeeper_size
    gen.num_of_broker = args.broker_size
    gen.num_of_partition = args.default_partitions

    gen.max_jvm_memory = args.max_jvm_memory
    gen.min_jvm_memory = args.min_jvm_memory

    yaml = gen.gen()

    yaml_file = 'kafka_cluster_gen.yaml'
    with open(yaml_file, 'w') as f:
        f.write(yaml)

    print 'finish generating kafka cluster yaml file in', yaml_file


if __name__ == '__main__':
    main()
