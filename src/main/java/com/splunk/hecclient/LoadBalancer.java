package com.splunk.hecclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */
public class LoadBalancer {
    private List<HecChannel> channels;
    private int index;

    public LoadBalancer() {
        channels = new ArrayList<>();
        index = 0;
    }

    public void add(HecChannel channel) {
        channels.add(channel);
    }

    public void remove(HecChannel channel) {
        doRemove(channel);
    }

    private void doRemove(HecChannel channel) {
        for (Iterator<HecChannel> iter = channels.listIterator(); iter.hasNext();) {
            HecChannel ch = iter.next();
            if (ch.equals(channel)) {
                iter.remove();
            }
        }
    }

    public void send(EventBatch batch) {
        HecChannel channel = channels.get(index);
        index = (index + 1) % channels.size();
        channel.send(batch);
    }

    public void sendAckPollRequests() {
        for (HecChannel channel: channels) {
            channel.sendAckPollRequests();
        }
    }
}