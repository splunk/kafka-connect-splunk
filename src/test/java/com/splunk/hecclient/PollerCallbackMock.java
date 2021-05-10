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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PollerCallbackMock implements PollerCallback {
    private final ConcurrentLinkedQueue<EventBatch> failed = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<EventBatch> committed = new ConcurrentLinkedQueue<>();

    public void onEventFailure(final List<EventBatch> failure, Exception ex) {
        failed.addAll(failure);
    }

    public void onEventCommitted(final List<EventBatch> commit) {
        committed.addAll(commit);
    }

    public List<EventBatch> getFailed() {
        return new ArrayList<>(failed);
    }

    public List<EventBatch> getCommitted() {
        return new ArrayList<>(committed);
    }
}
