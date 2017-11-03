/**
 * The hecclient lib is a Http Event Collector (HEC for short) library for Splunk which mainly covers the following features
 * 1. Post events to /event HEC endpoint
 *    a. When HEC token has "useACk = true" enabled, the library will do ACK polling and call callbacks provied by clients for acknowledged events or failed events.
 *    b. When HEC token has "useACK = false" enabled, the library will act just a HTTP client, however it still call callbacks provided by clients for POSTed events or failed events.
 *    c. It supports source, sourcetype, index etc metdata overrides by clients.
 *    d. It supports event data enrichment
 * 2. Post events to /raw HEC endpoint
 *    a. When HEC token has "useACk = true" enabled, the library will do ACK polling and call callbacks provied by clients for acknowledged events or failed events.
 *    b. When HEC token has "useACK = false" enabled, the library will act just a HTTP client, however it still call callbacks provided by clients for POSTed events or failed events.
 *    c. It supports source, sourcetype, index etc metdata overrides by clients.
 *    d. It doesn't supportievent data enrichment in this mode
 * 3. Concurrent Hec Client support
 * 4. HTTP connection pooling and reuse
 * 5. Cookies, stikcy session etc auto reuse with HTTP connection pooling
 */
package com.splunk.hecclient;
