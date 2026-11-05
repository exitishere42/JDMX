package de.exit.jdmx.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Scheinwerfer-Profil (Definition) eines Herstellers mit allen unterstützten DMX-Modi.
 */
public class FixtureDefinition {

    private final String id;
    private final String manufacturer;
    private final String model;
    private final ExplorerItemType type;
    private final List<DmxMode> modes;

    public FixtureDefinition(String id, String manufacturer, String model, ExplorerItemType type, List<DmxMode> modes) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.manufacturer = Objects.requireNonNull(manufacturer, "manufacturer must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        if (modes == null || modes.isEmpty()) {
            throw new IllegalArgumentException("modes must not be null or empty");
        }
        this.modes = Collections.unmodifiableList(new ArrayList<>(modes));
    }

    public String getId() {
        return id;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public ExplorerItemType getType() {
        return type;
    }

    public List<DmxMode> getModes() {
        return modes;
    }

    public DmxMode getDefaultMode() {
        return modes.getFirst();
    }

    public String getFullName() {
        return manufacturer + " - " + model;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
