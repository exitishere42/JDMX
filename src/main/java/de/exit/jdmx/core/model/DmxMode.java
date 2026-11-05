package de.exit.jdmx.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Ein DMX-Kanalmodus eines Scheinwerfers (z. B. "11-Kanal-Modus" oder "14CH").
 */
public class DmxMode {

    private final String name;
    private final List<ChannelDefinition> channels;

    public DmxMode(String name, List<ChannelDefinition> channels) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.channels = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(channels, "channels must not be null")));
    }

    public String getName() {
        return name;
    }

    public int getChannelCount() {
        return channels.size();
    }

    public List<ChannelDefinition> getChannels() {
        return channels;
    }

    @Override
    public String toString() {
        return name + " (" + getChannelCount() + "CH)";
    }
}
