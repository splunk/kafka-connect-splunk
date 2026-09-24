# Index routing configurations for Splunk Connect for Kafka

Index routing is optional and can be performed in either of two places:

- In the connector, when a Kafka topic maps naturally to a Splunk index.
- In Splunk, when routing depends on fields or patterns inside each event.

## Route by Kafka topic

Use `topics` together with `splunk.indexes`. The same positional mapping also applies to `splunk.sources` and `splunk.sourcetypes`.

To send three topics to one index:

```json
{
  "topics": "test-1,test-2,test-3",
  "splunk.indexes": "kafka"
}
```

To map each topic to a separate index, provide the same number of comma-separated values in the same order:

```json
{
  "topics": "test-1,test-2,test-3",
  "splunk.indexes": "kafka-1,kafka-2,kafka-3"
}
```

When `topics.regex` is used, topic-to-index lists are not applied. Supply metadata through Kafka headers or use the default index and sourcetype configured for the HEC token.

HEC token configuration can override event metadata. Ensure every target index is allowed for the token and that its settings permit the connector-supplied index when connector-side routing is required.

## Route by event content in Splunk

Configure index-time routing on the Splunk indexers or heavy forwarders that parse the incoming events. The examples below use this representative AWS CloudWatch event:

```json
{
  "owner": "123456789012",
  "logGroup": "CloudTrail",
  "logStream": "123456789012_CloudTrail_us-east-1",
  "subscriptionFilters": ["Destination"],
  "messageType": "DATA_MESSAGE",
  "logEvents": {
    "id": "31953106606966983378809025079804211143289615424298221570",
    "timestamp": 1432826855000,
    "message": {
      "eventVersion": "1.03",
      "userIdentity": {
        "type": "Root"
      }
    }
  }
}
```

### Route an owner ID to a production index

Add a transform reference for the incoming sourcetype in `props.conf`:

```ini
[kafka:events]
TRANSFORMS-index_routing = route_data_to_index_by_field_owner_id
```

Define the transform in `transforms.conf`:

```ini
[route_data_to_index_by_field_owner_id]
REGEX = "(\w+)":"123456789012"
DEST_KEY = _MetaData:Index
FORMAT = prod
```

### Route a CloudWatch region to a regional index

In `props.conf`:

```ini
[kafka:events]
TRANSFORMS-index_routing = route_data_to_index_by_aws_region
```

In `transforms.conf`:

```ini
[route_data_to_index_by_aws_region]
REGEX = "logStream":"(.*us-east-1)"
DEST_KEY = _MetaData:Index
FORMAT = aws-cloudwatch-us-east-1
```

Save the files and deploy them to the parsing tier. In an indexer cluster, distribute the same `props.conf` and `transforms.conf` configuration to every indexer through the cluster manager.

Make sure each destination index exists before enabling the transform. Test routing with representative events because an overly broad regular expression can redirect unrelated data.