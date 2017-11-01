package com.splunk.hecclient;

import java.util.*;

/**
 * Created by kchen on 10/19/17.
 */

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