package de.exit.jdmx.core.model;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Ein Element im Project Explorer (entweder ein aufklappbarer Standard-Ordner
 * oder ein konkretes Scheinwerfer-, Gruppen-, Preset- oder Effekt-Item).
 */
public class ExplorerItem {

    private final String id;
    private String name;
    private final ExplorerItemType type;
    private final boolean standardFolder;
    private final String iconName;
    private final Color color;
    private FixtureInstance fixtureInstance;
    private final List<String> memberFixtureIds = new ArrayList<>();

    /**
     * Erstellt ein neues ExplorerItem.
     *
     * @param id             Eindeutige ID
     * @param name           Anzeigename
     * @param type           Elementtyp (z. B. FOLDER, FIXTURE, EFFECT)
     * @param standardFolder True, wenn es sich um einen unlöschbaren System-Ordner handelt
     * @param iconName       Lucide-Icon-Name
     * @param color          Farbakzent
     */
    public ExplorerItem(String id, String name, ExplorerItemType type, boolean standardFolder, String iconName, Color color) {
        this(id, name, type, standardFolder, iconName, color, null);
    }

    /**
     * Erstellt ein neues ExplorerItem mit verknüpfter Scheinwerfer-Instanz.
     *
     * @param id              Eindeutige ID
     * @param name            Anzeigename
     * @param type            Elementtyp (z. B. FOLDER, FIXTURE, EFFECT)
     * @param standardFolder  True, wenn es sich um einen unlöschbaren System-Ordner handelt
     * @param iconName        Lucide-Icon-Name
     * @param color           Farbakzent
     * @param fixtureInstance Die gepatchte DMX-Geräteinstanz oder null
     */
    public ExplorerItem(String id, String name, ExplorerItemType type, boolean standardFolder, String iconName, Color color, FixtureInstance fixtureInstance) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.standardFolder = standardFolder;
        this.iconName = iconName != null ? iconName : "circle";
        this.color = color != null ? color : Color.web("#D1D5DB");
        this.fixtureInstance = fixtureInstance;
    }

    public FixtureInstance getFixtureInstance() {
        return fixtureInstance;
    }

    public void setFixtureInstance(FixtureInstance fixtureInstance) {
        this.fixtureInstance = fixtureInstance;
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

    public ExplorerItemType getType() {
        return type;
    }

    public boolean isStandardFolder() {
        return standardFolder;
    }

    public String getIconName() {
        return iconName;
    }

    public Color getColor() {
        return color;
    }

    /**
     * Liefert eine unmodifizierbare Liste aller zugeordneten Scheinwerfer-IDs (für Gruppen).
     */
    public List<String> getMemberFixtureIds() {
        return Collections.unmodifiableList(memberFixtureIds);
    }

    /**
     * Setzt die Liste der zugeordneten Scheinwerfer-IDs.
     */
    public void setMemberFixtureIds(List<String> ids) {
        memberFixtureIds.clear();
        if (ids != null) {
            memberFixtureIds.addAll(ids);
        }
    }

    /**
     * Fügt eine Scheinwerfer-ID zur Gruppe hinzu.
     */
    public void addMemberFixtureId(String id) {
        if (id != null && !memberFixtureIds.contains(id)) {
            memberFixtureIds.add(id);
        }
    }

    /**
     * Entfernt eine Scheinwerfer-ID aus der Gruppe.
     */
    public void removeMemberFixtureId(String id) {
        memberFixtureIds.remove(id);
    }

    /**
     * Prüft, ob ein bestimmter Scheinwerfer Teil der Gruppe ist.
     */
    public boolean hasMemberFixture(String id) {
        return memberFixtureIds.contains(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExplorerItem that = (ExplorerItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
