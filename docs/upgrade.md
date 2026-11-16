# Upgrade Splunk Connect for Kafka

Use the following procedure to replace an existing Splunk Connect for Kafka plugin with a newer release.

1. Download the target `splunk-kafka-connect-<VERSION>.jar` from [GitHub releases](https://github.com/splunk/kafka-connect-splunk/releases).
2. Save the current connector configurations and review the target release notes for compatibility or configuration changes.
3. Stop the Kafka Connect workers that load the Splunk Connect for Kafka plugin.
4. Remove the previous connector JAR from the configured plugin directory or classpath. Do not leave multiple versions in the same plugin directory.
5. Install the new JAR on every worker as described in [Install Splunk Connect for Kafka](installation.md).
6. Start the Kafka Connect workers.
7. Verify that the plugin is discovered and that the connector and its tasks return to `RUNNING`:

   ```sh
   curl http://localhost:8083/connector-plugins
   curl http://localhost:8083/connectors/<CONNECTOR_NAME>/status
   ```

8. Confirm that events continue to arrive in the expected Splunk indexes and monitor connector lag and worker logs.

Kafka Connect stores distributed connector configurations and offsets in its internal Kafka topics. Replacing the plugin JAR does not require recreating the connector when the existing configuration is compatible with the new version.

## Changes to review for recent releases

- Version 2.2.7 rejects non-HTTPS HEC URIs by default. Set `splunk.hec.ssl.enforced=false` only for an intentionally isolated HTTP deployment. It also defaults `splunk.hec.max.retries` to `5`; after those retries are exhausted, the connector drops the failed batch. Set `-1` only when unbounded retries and the resulting lag are acceptable.
- Version 2.2.8 no longer treats every normal `Set-Cookie` response as a sticky-session expiry. `splunk.hec.ack.legacy.sticky.session.expiry.enabled=true` restores the deprecated behavior, but it can reset HEC channels and retry outstanding batches whenever a load balancer refreshes a cookie.

Review the [GitHub release notes](https://github.com/splunk/kafka-connect-splunk/releases) for all versions between the installed and target releases.
