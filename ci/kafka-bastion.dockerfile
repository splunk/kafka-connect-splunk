FROM anapsix/alpine-java:8_jdk

RUN apk update && apk upgrade && apk add git && apk add openssh && apk add openssl && apk add curl && apk add python

ENV kafkaversion=0.11.0.2
RUN wget -q https://archive.apache.org/dist/kafka/${kafkaversion}/kafka_2.11-${kafkaversion}.tgz -P / && cd / && tar xzf kafka_2.11-${kafkaversion}.tgz && rm -f kafka_2.11-${kafkaversion}.tgz

RUN wget -q https://bootstrap.pypa.io/get-pip.py -P / && python get-pip.py && pip install requests

RUN mkdir -p /root/.ssh
ADD id_rsa /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa

ADD id_rsa.pub /root/.ssh/id_rsa.pub
ADD known_hosts /root/.ssh/known_hosts

RUN mkdir -p /kafka-bastion/
WORKDIR /kafka-bastion

ADD run_bastion.sh /kafka-bastion/run_bastion.sh

CMD ["/bin/bash", "-c", "/kafka-bastion/run_bastion.sh"]
