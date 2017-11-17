FROM anapsix/alpine-java:8_jdk

RUN apk update && apk upgrade && apk add git && apk add openssh && apk add curl

ENV kafkaversion=0.11.0.1
RUN wget -q http://mirror.olnevhost.net/pub/apache/kafka/${kafkaversion}/kafka_2.11-${kafkaversion}.tgz -P / && cd / && tar xzf kafka_2.11-${kafkaversion}.tgz && rm -f kafka_2.11-${kafkaversion}.tgz

ADD fix_hosts.sh /fix_hosts.sh
ADD run_bastion.sh /run_bastion.sh

CMD ["/bin/bash", "-c", "/run_bastion.sh"]
