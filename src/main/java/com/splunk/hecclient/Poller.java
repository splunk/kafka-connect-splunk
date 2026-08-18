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
package com.splunk.hecclient;

public interface Poller {
    void start();
    void stop();
    void add(HecChannel channel, EventBatch batch, String response);
    void fail(HecChannel channel, EventBatch batch, Exception ex);

    /**
     * @deprecated Sticky-session changes are normally handled by the HTTP client's cookie store.
     *     This method is active only when legacy sticky-session expiry handling is enabled.
     */
    @Deprecated
    void stickySessionHandler(HecChannel channel);

    /**
     * @deprecated A {@code Set-Cookie} response does not normally indicate that a sticky session
     *     expired. This method is active only when legacy sticky-session expiry handling is enabled.
     */
    @Deprecated
    void setStickySessionToTrue();

    // minimum load channel
    HecChannel getMinLoadChannel();
    long getTotalOutstandingEventBatches();
}
