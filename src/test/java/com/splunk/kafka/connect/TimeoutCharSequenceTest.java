/*
 * Copyright 2026 Splunk, Inc..
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

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TimeoutCharSequenceTest {
    @Test
    public void doesNotTimeoutBeforeMillisecondsDeadline() {
        TimeoutCharSequence sequence = new TimeoutCharSequence("a", 500);

        for (int i = 0; i < 1_000_000; i++) {
            Assert.assertEquals('a', sequence.charAt(0));
        }
    }

    @Test(expected = RegexTimeoutException.class)
    public void throwsWhenDeadlineHasElapsedAtTheCheckInterval() {
        TimeoutCharSequence sequence = new TimeoutCharSequence(
                "a", System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(501), TimeUnit.MILLISECONDS.toNanos(500));

        for (int i = 0; i < 1_000_000; i++) {
            sequence.charAt(0);
        }
    }
}
