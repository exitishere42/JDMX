package de.exit.jdmx.core.model;

import java.util.Objects;

/**
 * Eine konkret gepatchte Scheinwerfer-Instanz auf der Bühne mit DMX-Adresse und Koordinaten.
 */
public class FixtureInstance {

    private final String id;
    private String name;
    private final FixtureDefinition definition;
    private DmxMode activeMode;
    private int universe;
    private int startAddress;
    private double stageX;
    private double stageY;

    public FixtureInstance(String id, String name, FixtureDefinition definition, DmxMode activeMode, int universe, int startAddress, double stageX, double stageY) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
        this.activeMode = Objects.requireNonNull(activeMode, "activeMode must not be null");
        if (universe < 1) {
            throw new IllegalArgumentException("universe must be >= 1");
        }
        if (startAddress < 1 || startAddress > 512) {
            throw new IllegalArgumentException("startAddress must be between 1 and 512");
        }
        this.universe = universe;
        this.startAddress = startAddress;
        this.stageX = stageX;
        this.stageY = stageY;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public FixtureDefinition getDefinition() {
        return definition;
    }

    public DmxMode getActiveMode() {
        return activeMode;
    }

    public void setActiveMode(DmxMode activeMode) {
        this.activeMode = Objects.requireNonNull(activeMode, "activeMode must not be null");
    }

    public int getUniverse() {
        return universe;
    }

    public void setUniverse(int universe) {
        if (universe < 1) {
            throw new IllegalArgumentException("universe must be >= 1");
        }
        this.universe = universe;
    }

    public int getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(int startAddress) {
        if (startAddress < 1 || startAddress > 512) {
            throw new IllegalArgumentException("startAddress must be between 1 and 512");
        }
        this.startAddress = startAddress;
    }

    public int getEndAddress() {
        return startAddress + activeMode.getChannelCount() - 1;
    }

    public double getStageX() {
        return stageX;
    }

    public void setStageX(double stageX) {
        this.stageX = stageX;
    }

    public double getStageY() {
        return stageY;
    }

    public void setStageY(double stageY) {
        this.stageY = stageY;
    }

    /**
     * Prüft, ob diese Scheinwerfer-Instanz eine bestimmte Adresse im Universum belegt.
     */
    public boolean occupiesAddress(int checkUniverse, int checkAddress) {
        if (this.universe != checkUniverse) {
            return false;
        }
        return checkAddress >= startAddress && checkAddress <= getEndAddress();
    }
}
