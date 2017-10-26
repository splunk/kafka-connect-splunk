package com.splunk.hecclient;

import java.util.*;

/**
 * Created by kchen on 10/19/17.
 */

public final class HecAckPollResponse {
    // {"acks":[0,1,2,3]}
    private final SortedMap<String, Boolean> acks = new TreeMap<>();

    Collection<Long> getSuccessIds() {
        Set<Long> successful = new HashSet<>();
        for(Map.Entry<String,Boolean> e: acks.entrySet()){
            if(e.getValue()) { // was 'true' in json, meaning it succeeded
                successful.add(Long.parseLong(e.getKey()));
            }
        }
        return successful;
    }

    public Map<String, Boolean> getAcks() {
        return acks;
    }
}