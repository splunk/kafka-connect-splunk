/*
 * Copyright 2017 Splunk, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
