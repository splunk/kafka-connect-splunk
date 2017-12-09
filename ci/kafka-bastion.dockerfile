FROM anapsix/alpine-java:8_jdk

RUN apk update && apk upgrade && apk add git && apk add openssh && apk add openssl && apk add curl && apk add python

ENV kafkaversion=0.11.0.2
RUN wget -q https://archive.apache.org/dist/kafka/${kafkaversion}/kafka_2.11-${kafkaversion}.tgz -P / && cd / && tar xzf kafka_2.11-${kafkaversion}.tgz && rm -f kafka_2.11-${kafkaversion}.tgz

RUN wget -q https://bootstrap.pypa.io/get-pip.py -P / && python get-pip.py && pip install requests

ADD fix_hosts.sh /fix_hosts.sh
ADD run_bastion.sh /run_bastion.sh
ADD perf.py /perf.py

WORKDIR /

CMD ["/bin/bash", "-c", "/run_bastion.sh"]
