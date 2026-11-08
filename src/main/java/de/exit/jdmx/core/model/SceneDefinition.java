package de.exit.jdmx.core.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Definition einer DMX-Lichtszene (Cue / Snapshot).
 * <p>
 * Speichert für konfigurierte DMX-Kanäle über mehrere Universen hinweg
 * die Zielwerte (0–255) sowie eine Standard-Überblendzeit in Millisekunden.
 * </p>
 */
public class SceneDefinition {

    private final String id;
    private String name;
    private int defaultFadeTimeMs;
    // universeIndex (0..3) -> Map<channel (1..512), value (0..255)>
    private final Map<Integer, Map<Integer, Integer>> channelValues;

    /**
     * Erstellt eine neue Szenendefinition.
     *
     * @param id                Eindeutige ID
     * @param name              Szenenname
     * @param defaultFadeTimeMs Standard-Überblendzeit in Millisekunden
     * @param channelValues     Map von Universums-Index auf Kanal-Wert-Zuordnungen
     */
    public SceneDefinition(String id, String name, int defaultFadeTimeMs, Map<Integer, Map<Integer, Integer>> channelValues) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name != null && !name.isBlank() ? name : "Scene";
        this.defaultFadeTimeMs = Math.max(0, defaultFadeTimeMs);
        this.channelValues = new HashMap<>();
        if (channelValues != null) {
            for (Map.Entry<Integer, Map<Integer, Integer>> entry : channelValues.entrySet()) {
                if (entry.getValue() != null) {
                    this.channelValues.put(entry.getKey(), new HashMap<>(entry.getValue()));
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null && !name.isBlank() ? name : this.name;
    }

    public int getDefaultFadeTimeMs() {
        return defaultFadeTimeMs;
    }

    public void setDefaultFadeTimeMs(int defaultFadeTimeMs) {
        this.defaultFadeTimeMs = Math.max(0, defaultFadeTimeMs);
    }

    /**
     * Liefert alle gespeicherten Universums-Kanalwerte als unveränderliche Map.
     *
     * @return Map von universeIndex auf Kanalwerte
     */
    public Map<Integer, Map<Integer, Integer>> getChannelValues() {
        Map<Integer, Map<Integer, Integer>> copy = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : channelValues.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Liefert die Kanalwerte für ein bestimmtes Universum (0-basiert).
     *
     * @param universeIndex Universum (0..3)
     * @return Map von Kanal auf DMX-Wert oder leere Map
     */
    public Map<Integer, Integer> getChannelValuesForUniverse(int universeIndex) {
        Map<Integer, Integer> vals = channelValues.get(universeIndex);
        return vals != null ? Collections.unmodifiableMap(vals) : Collections.emptyMap();
    }
}
