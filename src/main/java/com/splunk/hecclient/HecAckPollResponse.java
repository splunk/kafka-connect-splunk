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

import java.util.*;

final class HecAckPollResponse {
    // {"acks":{"1":true,"2":true,"3":true,"4":false,"5":false,"6":false}}
    private final SortedMap<String, Boolean> acks = new TreeMap<>();

    public Collection<Long> getSuccessIds() {
        Set<Long> successful = new HashSet<>();
        for (Map.Entry<String,Boolean> e: acks.entrySet()) {
            if (e.getValue()) { // was 'true' in json, meaning it succeeded
                successful.add(Long.parseLong(e.getKey()));
            }
        }
        return successful;
    }

    public Map<String, Boolean> getAcks() {
        return acks;
    }
}
