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
package com.splunk.kafka.connect;

import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.HecException;
import com.splunk.hecclient.HecInf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HecMock implements HecInf {
    static final String success = "success";
    static final String successAndThenFailure = "successAndThenFailure";
    static final String failure = "failure";

    private final List<EventBatch> batches;
    private final SplunkSinkTask task;
    private String sentResult = "success";

    public HecMock(SplunkSinkTask task) {
        this.task = task;
        this.batches = new ArrayList<>();
    }

    @Override
    public void close() {
    }

    @Override
    public void send(final EventBatch batch) {
        batches.add(batch);
        if (sentResult.equals(success)) {
            batch.commit();
            task.onEventCommitted(Collections.singletonList(batch));
        } else if (sentResult.equals(failure)) {
            batch.fail();
            task.onEventFailure(Collections.singletonList(batch), new HecException("mockup"));
        } else {
            batch.fail();
            task.onEventFailure(Collections.singletonList(batch), new HecException("mockup"));
        }
    }

    public void setSendReturnResult(final String result) {
        sentResult = result;
    }

    public List<EventBatch> getBatches() {
        return batches;
    }
}
