FROM anapsix/alpine-java:8_jdk

RUN apk update && apk upgrade && apk add git && apk add openssh

RUN wget -q http://apache.claz.org/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz -P /bin && cd /bin && tar xzf apache-maven-3.5.2-bin.tar.gz

ENV PATH=$PATH:/bin/apache-maven-3.5.2/bin

RUN mkdir -p /root/.ssh
ADD id_rsa /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa

ADD id_rsa.pub /root/.ssh/id_rsa.pub
ADD known_hosts /root/.ssh/known_hosts

RUN mkdir -p /kafka-connect/
WORKDIR /kafka-connect

ADD run_kafka_connect.sh /kafka-connect/run_kafka_connect.sh

CMD ["/bin/bash", "-c", "/kafka-connect/run_kafka_connect.sh"]
