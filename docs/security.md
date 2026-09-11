# Security configurations for Splunk Connect for Kafka

Splunk Connect for Kafka can operate with Kafka deployments secured by TLS, SASL/GSSAPI (Kerberos), SASL/PLAIN, or SASL/SCRAM-SHA-256/512. HEC can also be protected with TLS and certificate validation.

> **Important:** Protect the Kafka Connect REST API. Connector configurations can contain the HEC token and truststore password, and an unauthenticated REST API can expose or modify those values. Keep port `8083` on a trusted management network and apply access controls outside this connector.

## Configure TLS for HEC

Use a certificate issued by a trusted certificate authority in production. The following self-signed flow is suitable only for development or as a starting point for an internally managed CA.

1. Create a working directory and generate a certificate and private key:

   ```sh
   mkdir cert
   cd cert
   openssl req -newkey rsa:2048 -nodes \
     -keyout kafka_connect.key \
     -x509 -days 365 \
     -out kafka_connect.crt
   openssl x509 -in kafka_connect.crt \
     -out kafka_connect.pem \
     -outform PEM
   ```

2. Create a Java truststore for the connector and import the certificate:

   ```sh
   keytool -genkeypair -keyalg RSA -keystore keystore.jks
   keytool -importcert -trustcacerts \
     -file kafka_connect.crt \
     -alias splunk-hec \
     -keystore keystore.jks
   ```

3. Copy the PEM certificate and private key to a protected location under `$SPLUNK_HOME/etc/auth`.

4. Configure HEC in `$SPLUNK_HOME/etc/apps/splunk_httpinput/local/inputs.conf`:

   ```ini
   [http]
   disabled = 0
   enableSSL = 1
   serverCert = <ABSOLUTE_PATH_TO_CERTIFICATE_PEM>
   privKeyPath = <ABSOLUTE_PATH_TO_PRIVATE_KEY>
   sslPassword = <PRIVATE_KEY_PASSWORD>
   ```

5. Restart Splunk:

   ```sh
   $SPLUNK_HOME/bin/splunk restart
   ```

6. Configure the connector to validate the HEC certificate:

   ```json
   {
     "splunk.hec.uri": "https://<SPLUNK_HEC_HOST>:8088",
     "splunk.hec.ssl.enforced": "true",
     "splunk.hec.ssl.validate.certs": "true",
     "splunk.hec.ssl.trust.store.path": "<ABSOLUTE_PATH_TO_KEYSTORE_JKS>",
     "splunk.hec.ssl.trust.store.password": "<KEYSTORE_PASSWORD>"
   }
   ```

Use an absolute truststore path that is readable by every worker that might run the connector task.

## Configure TLS for Kafka brokers and Kafka Connect

Create a CA and issue certificates for each broker and client. Every broker and client truststore must contain the CA certificate. The resulting files commonly include:

- `kafka.server.keystore.jks` and `kafka.server.truststore.jks` for brokers
- `kafka.client.keystore.jks` and `kafka.client.truststore.jks` for Kafka Connect and command-line clients

An illustrative keytool/OpenSSL sequence is:

```sh
# Create a CA. Protect ca-key and its password as production secrets.
openssl req -new -x509 -keyout ca-key -out ca-cert -days 365

# Create and sign the broker keypair.
keytool -keystore kafka.server.keystore.jks \
  -alias <BROKER_DNS_NAME> -validity 365 -genkeypair
keytool -keystore kafka.server.truststore.jks \
  -alias CARoot -importcert -file ca-cert
keytool -keystore kafka.server.keystore.jks \
  -alias <BROKER_DNS_NAME> -certreq -file broker-cert-request
openssl x509 -req -CA ca-cert -CAkey ca-key \
  -in broker-cert-request -out broker-cert-signed \
  -days 365 -CAcreateserial
keytool -keystore kafka.server.keystore.jks \
  -alias CARoot -importcert -file ca-cert
keytool -keystore kafka.server.keystore.jks \
  -alias <BROKER_DNS_NAME> -importcert -file broker-cert-signed

# Create and sign the client keypair.
keytool -keystore kafka.client.keystore.jks \
  -alias kafka-connect -validity 365 -genkeypair
keytool -keystore kafka.client.truststore.jks \
  -alias CARoot -importcert -file ca-cert
keytool -keystore kafka.client.keystore.jks \
  -alias kafka-connect -certreq -file client-cert-request
openssl x509 -req -CA ca-cert -CAkey ca-key \
  -in client-cert-request -out client-cert-signed \
  -days 365 -CAcreateserial
keytool -keystore kafka.client.keystore.jks \
  -alias CARoot -importcert -file ca-cert
keytool -keystore kafka.client.keystore.jks \
  -alias kafka-connect -importcert -file client-cert-signed
```

Include the broker DNS names or IP addresses in the certificate subject alternative names. Repeat the broker steps for each broker rather than sharing one private key among hosts.

### Broker properties

Add TLS listener and keystore settings to `config/server.properties`:

```properties
listeners=SSL://localhost:9092
security.inter.broker.protocol=SSL
ssl.enabled.protocols=TLSv1.3,TLSv1.2
ssl.client.auth=none
ssl.keystore.type=JKS
ssl.keystore.location=<ABSOLUTE_PATH_TO_SERVER_KEYSTORE>
ssl.keystore.password=<KEYSTORE_PASSWORD>
ssl.key.password=<KEY_PASSWORD>
ssl.truststore.type=JKS
ssl.truststore.location=<ABSOLUTE_PATH_TO_SERVER_TRUSTSTORE>
ssl.truststore.password=<TRUSTSTORE_PASSWORD>
```

Set `ssl.client.auth=required` when brokers must authenticate clients with certificates.

### Kafka Connect worker and sink-consumer properties

Kafka Connect has two relevant clients: the worker's internal Kafka clients and the consumer used by sink tasks. Worker settings are unprefixed; sink-consumer overrides begin with `consumer.`.

Add both groups to `config/connect-distributed.properties`:

```properties
bootstrap.servers=localhost:9092

# Worker security
security.protocol=SSL
ssl.key.password=<KEY_PASSWORD>
ssl.keystore.location=<ABSOLUTE_PATH_TO_CLIENT_KEYSTORE>
ssl.keystore.password=<KEYSTORE_PASSWORD>
ssl.truststore.location=<ABSOLUTE_PATH_TO_CLIENT_TRUSTSTORE>
ssl.truststore.password=<TRUSTSTORE_PASSWORD>
ssl.enabled.protocols=TLSv1.3,TLSv1.2
ssl.truststore.type=JKS

# Sink consumer security
consumer.security.protocol=SSL
consumer.ssl.key.password=<KEY_PASSWORD>
consumer.ssl.keystore.location=<ABSOLUTE_PATH_TO_CLIENT_KEYSTORE>
consumer.ssl.keystore.password=<KEYSTORE_PASSWORD>
consumer.ssl.truststore.location=<ABSOLUTE_PATH_TO_CLIENT_TRUSTSTORE>
consumer.ssl.truststore.password=<TRUSTSTORE_PASSWORD>
consumer.ssl.enabled.protocols=TLSv1.3,TLSv1.2
consumer.ssl.truststore.type=JKS
```

When broker-side client authentication is disabled, the client keystore settings can be omitted; the truststore is still needed for a private CA.

Kafka Connect cannot set these sink-consumer properties separately for each connector through this connector's configuration. If separate client identities are required, run connectors on workers with the appropriate worker configuration.

Start Kafka and Kafka Connect after saving the settings:

```sh
$KAFKA_HOME/bin/zookeeper-server-start.sh config/zookeeper.properties
$KAFKA_HOME/bin/kafka-server-start.sh config/server.properties
$KAFKA_HOME/bin/connect-distributed.sh config/connect-distributed.properties
```

For Kafka deployments that do not use ZooKeeper, start the cluster using its KRaft-specific procedure.

### Secure command-line clients

Create `client.properties` and restrict its permissions:

```properties
security.protocol=SSL
ssl.keystore.location=<ABSOLUTE_PATH_TO_CLIENT_KEYSTORE>
ssl.keystore.password=<KEYSTORE_PASSWORD>
ssl.key.password=<KEY_PASSWORD>
ssl.truststore.location=<ABSOLUTE_PATH_TO_CLIENT_TRUSTSTORE>
ssl.truststore.password=<TRUSTSTORE_PASSWORD>
```

```sh
chmod 0600 client.properties
$KAFKA_HOME/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic <TOPIC> \
  --consumer.config client.properties
$KAFKA_HOME/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic <TOPIC> \
  --producer.config client.properties
```

## SASL/GSSAPI (Kerberos)

Configure both the worker and the sink consumer in `connect-distributed.properties`:

```properties
# Worker security
security.protocol=SASL_PLAINTEXT
sasl.mechanism=GSSAPI
sasl.kerberos.service.name=kafka

# Sink consumer security
consumer.security.protocol=SASL_PLAINTEXT
consumer.sasl.mechanism=GSSAPI
consumer.sasl.kerberos.service.name=kafka
```

Prefer `SASL_SSL` when TLS is available. Supply the Kerberos and JAAS files to the Kafka Connect JVM, for example:

```sh
export KAFKA_OPTS="-Djava.security.krb5.conf=/etc/krb5.conf -Djava.security.auth.login.config=/etc/kafka/kafka_connect_jaas.conf"
```

For connection diagnostics, temporarily append `-Dsun.security.krb5.debug=true`.

Example JAAS configuration:

```text
KafkaClient {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  storeKey=true
  keyTab="/etc/security/keytabs/connect.keytab"
  principal="connect/<HOST>@<REALM>";
};
```

Update the keytab and principal for the deployment, protect the JAAS file, and start Kafka Connect normally.

## SASL/PLAIN

> **Caution:** Do not use SASL/PLAIN without TLS in production because the credentials are otherwise exposed on the network.

Set the worker and sink consumer protocol and mechanism:

```properties
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
consumer.security.protocol=SASL_SSL
consumer.sasl.mechanism=PLAIN
```

Pass a protected JAAS file to Kafka Connect through `KAFKA_OPTS`. Example JAAS content:

```text
KafkaClient {
  org.apache.kafka.common.security.plain.PlainLoginModule required
  username="<USERNAME>"
  password="<PASSWORD>";
};
```

## SASL/SCRAM-SHA-256 and SASL/SCRAM-SHA-512

Choose the SCRAM mechanism provisioned on the Kafka cluster and configure it for both clients:

```properties
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-512
consumer.security.protocol=SASL_SSL
consumer.sasl.mechanism=SCRAM-SHA-512
```

Example JAAS content:

```text
KafkaClient {
  org.apache.kafka.common.security.scram.ScramLoginModule required
  username="<USERNAME>"
  password="<PASSWORD>";
};
```

Pass the JAAS file to the worker JVM and start Kafka Connect:

```sh
export KAFKA_OPTS="-Djava.security.auth.login.config=/etc/kafka/kafka_connect_jaas.conf"
$KAFKA_HOME/bin/connect-distributed.sh config/connect-distributed.properties
```
