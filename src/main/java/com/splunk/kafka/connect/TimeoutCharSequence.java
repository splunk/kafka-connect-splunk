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

import java.util.concurrent.TimeUnit;

public class TimeoutCharSequence implements CharSequence {

    private final CharSequence input;
    private final long startNanos;
    private final long timeoutNanos;
    private long noCalls;
    private static final long TIMEOUT_CHECK_INTERVAL = 1_000_000;

    public TimeoutCharSequence(CharSequence input, long timeoutMillis) {
        this(input, System.nanoTime(), TimeUnit.MILLISECONDS.toNanos(timeoutMillis));
    }

    public TimeoutCharSequence(CharSequence input, long startNanos, long timeoutNanos) {
        this.input = input;
        this.startNanos = startNanos;
        this.timeoutNanos = timeoutNanos;
        this.noCalls = 0;
    }

    @Override
    public int length() {
        return input.length();
    }

    @Override
    public char charAt(int index) {
        this.noCalls += 1;
        if (this.noCalls % TIMEOUT_CHECK_INTERVAL == 0) {
            checkTimeout();
        }
        return input.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new TimeoutCharSequence(input.subSequence(start, end), startNanos, timeoutNanos);
    }

    @Override
    public String toString() {
        return input.toString();
    }

    private void checkTimeout() {
        if (System.nanoTime() - startNanos > timeoutNanos) {
            throw new RegexTimeoutException("Timeout");
        }
    }
}
