package com.splunk.hecclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */
final class LoadBalancer {
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
        for (Iterator<HecChannel> iter = channels.listIterator(); iter.hasNext();) {
            HecChannel ch = iter.next();
            if (ch.equals(channel)) {
                iter.remove();
            }
        }
    }

    public boolean send(final EventBatch batch) {
        HecChannel channel = channels.get(index);
        index = (index + 1) % channels.size();
        return channel.send(batch);
    }
}