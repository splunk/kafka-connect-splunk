# Load balancing configurations for Splunk Connect for Kafka

Splunk Connect for Kafka supports two HEC load-balancing patterns:

- Give `splunk.hec.uri` a comma-separated list of HEC endpoints and let the connector distribute requests among them.
- Give `splunk.hec.uri` the address of a hardware or software load balancer that forwards requests to HEC-enabled indexers or heavy forwarders.

Both patterns can be used with HEC indexer acknowledgment.

For Splunk Cloud Platform, work with Splunk Support when a load balancer must be created or changed. A deployment that already uses an Elastic Load Balancing endpoint for the Splunk Add-on for Amazon Kinesis Firehose may be able to reuse that endpoint.

## Configure acknowledgment through a load balancer

When `splunk.hec.ack.enabled=true` and `splunk.hec.uri` points to a load balancer:

1. Configure cookie-based sticky sessions with the longest practical cookie lifetime. An acknowledgment poll must reach the same HEC backend that accepted the corresponding batch.
2. Configure multiple HEC channels with `splunk.hec.total.channels`. A useful starting point is one or more channels per backend; the Splunk 2.2 guide suggests up to two times the number of indexers behind the load balancer.
3. Tune `splunk.hec.lb.poll.interval`, which controls endpoint health polling. The default is 120 seconds. Increase it to reduce polling or decrease it to detect endpoint changes more quickly. Set it to `-1` only when polling must be disabled.
4. Save the connector configuration and observe the distribution of requests and acknowledgment latency.

Sticky sessions reduce acknowledgment-routing failures but cannot guarantee that a load balancer will never move a request to another backend. Failover or load-based routing can therefore still produce duplicate data.

## Internal endpoint list

```json
{
  "splunk.hec.uri": "https://idx1.example.com:8088,https://idx2.example.com:8088,https://idx3.example.com:8088",
  "splunk.hec.ack.enabled": "true",
  "splunk.hec.total.channels": "6"
}
```

## External load balancer

```json
{
  "splunk.hec.uri": "https://hec-lb.example.com:8088",
  "splunk.hec.ack.enabled": "true",
  "splunk.hec.total.channels": "6",
  "splunk.hec.lb.poll.interval": "120"
}
```

See [Configuration examples](configuration-examples.md#load-balancing) for complete connector requests.
