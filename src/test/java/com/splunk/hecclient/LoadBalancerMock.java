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

public class LoadBalancerMock implements LoadBalancerInf {
    private List<EventBatch> batches = new ArrayList<>();
    private boolean throwOnSend = false;

    public void add(HecChannel channel) {
    }

    public void remove(HecChannel channel) {
    }

    public void send(final EventBatch batch) {
        if (throwOnSend) {
            throw new HecException("mocked up");
        }
        batches.add(batch);
    }

    public LoadBalancerMock setThrowOnSend(boolean throwOnSend) {
        this.throwOnSend = throwOnSend;
        return this;
    }

    public int size() {
        return 0;
    }

    public List<EventBatch> getBatches() {
        return batches;
    }
}
