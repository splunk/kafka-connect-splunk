FROM anapsix/alpine-java:8_jdk

RUN apk update && apk upgrade && apk add git && apk add openssh && apk add openssl

RUN mkdir -p /bin/gradle

ENV GRADLE_VERSION=4.3.1
ENV GRADLE_HOME=/bin/gradle/gradle-${GRADLE_VERSION}
ENV PATH=${PATH}:${GRADLE_HOME}/bin

RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /bin/gradle \
        && cd /bin/gradle && unzip gradle-${GRADLE_VERSION}-bin.zip \
        && rm gradle-${GRADLE_VERSION}-bin.zip

RUN mkdir -p /kafka-data-gen
WORKDIR /kafka-data-gen

RUN mkdir -p /root/.ssh
ADD known_hosts /root/.ssh/known_hosts

ADD fix_hosts.sh /fix_hosts.sh
ADD run_data_gen.sh /kafka-data-gen/run_data_gen.sh

CMD ["/bin/bash", "-c", "/kafka-data-gen/run_data_gen.sh"]
