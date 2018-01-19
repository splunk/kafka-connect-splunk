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
import java.util.Iterator;
import java.util.List;

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
