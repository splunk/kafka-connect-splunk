package com.splunk.hecclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */
public final class LoadBalancer implements LoadBalancerInf {
    private List<HecChannel> channels;
    private int index;

    public LoadBalancer() {
        channels = new ArrayList<>();
        index = 0;
    }

    @Override
    public void add(HecChannel channel) {
        channels.add(channel);
    }

    @Override
    public void remove(HecChannel channel) {
        for (Iterator<HecChannel> iter = channels.listIterator(); iter.hasNext();) {
            HecChannel ch = iter.next();
            if (ch.equals(channel)) {
                iter.remove();
            }
        }
    }

    @Override
    public void send(final EventBatch batch) {
        if (channels.isEmpty()) {
            throw new HecException("No channels are available / registered with LoadBalancer");
        }

        for (int tried = 0; tried != channels.size(); tried++) {
            HecChannel channel = channels.get(index);
            index = (index + 1) % channels.size();
            if (!channel.hasBackPressure()) {
                channel.send(batch);
                return;
            }
        }

        // all indexers have back pressure
        throw new HecException("All channels have back pressure");
    }

    @Override
    public int size() {
        return channels.size();
    }
}
