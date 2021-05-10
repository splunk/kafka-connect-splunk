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
package com.splunk.hecclient.examples;

import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.PollerCallback;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class PrintIt implements PollerCallback {
    private final AtomicLong eventsFailed = new AtomicLong(0);
    private final AtomicLong events = new AtomicLong(0);

    @Override
    public void onEventFailure(List<EventBatch> batches, Exception ex) {
        eventsFailed.addAndGet(batches.size());
        System.out.println("Failed: " + eventsFailed.get());
    }

    @Override
    public void onEventCommitted(List<EventBatch> batches) {
        events.addAndGet(batches.size());
        System.out.println("committed: " + events.get());
    }

    public long getTotalEventsHandled() {
        return eventsFailed.get() + events.get();
    }
}
