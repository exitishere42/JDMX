package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.ChannelType;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Leichtgewichtige Zustandsdaten eines DMX-Kanalbalkens im {@link CanvasDeskView}.
 * <p>
 * Kapselt Kanalnummer, aktuellen DMX-Wert (0–255), Funktionstyp, Balkenfarbe
 * und zugehörige Scheinwerfer-Metadaten ohne Scenegraph-Overhead.
 * </p>
 */
public class DeskChannelData {

    /** Standard-Balkenfarbe für ungepatchte Kanäle (Unser Primär-Hellgrau). */
    public static final Color DEFAULT_BAR_COLOR = Color.web("#D1D5DB");

    private final int channelNumber;
    private int value;
    private String functionName = "-";
    private ChannelType channelType;
    private Color barColor = DEFAULT_BAR_COLOR;
    private String fixtureName;
    private Color fixtureColor;
    private int fixtureChannelIndex = 1;
    private String customChannelName;

    /**
     * Erstellt einen neuen Kanalzustand für die angegebene 1-basierte Kanalnummer.
     *
     * @param channelNumber 1-basierte DMX-Kanalnummer (1 bis 512)
     */
    public DeskChannelData(int channelNumber) {
        if (channelNumber < 1 || channelNumber > 512) {
            throw new IllegalArgumentException("channelNumber must be between 1 and 512, got: " + channelNumber);
        }
        this.channelNumber = channelNumber;
        this.value = 0;
    }

    /**
     * Liefert die 1-basierte Kanalnummer.
     *
     * @return Kanalnummer (1–512)
     */
    public int getChannelNumber() {
        return channelNumber;
    }

    /**
     * Liefert den aktuellen DMX-Wert (0 bis 255).
     *
     * @return DMX-Wert
     */
    public int getValue() {
        return value;
    }

    /**
     * Setzt den DMX-Wert (geclampt auf den Bereich 0 bis 255).
     *
     * @param value Neuer DMX-Wert
     */
    public void setValue(int value) {
        this.value = Math.max(0, Math.min(255, value));
    }

    /**
     * Liefert den Funktionstext (z. B. "Dimmer", "Pan", "Rot" oder "-").
     *
     * @return Funktionstext
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Liefert den zugewiesenen Kanaltyp oder null bei ungepatchten Kanälen.
     *
     * @return ChannelType oder null
     */
    public ChannelType getChannelType() {
        return channelType;
    }

    /**
     * Liefert die Zeichenfarbe für den Pegelbalken.
     *
     * @return Color des Balkens
     */
    public Color getBarColor() {
        return barColor;
    }

    /**
     * Liefert den Namen des zugewiesenen Scheinwerfers oder null.
     *
     * @return Scheinwerfername oder null
     */
    public String getFixtureName() {
        return fixtureName;
    }

    /**
     * Liefert die Akzentfarbe des zugewiesenen Scheinwerfers oder null.
     *
     * @return Scheinwerfer-Akzentfarbe oder null
     */
    public Color getFixtureColor() {
        return fixtureColor;
    }

    /**
     * Weist dem Kanal ein gepatchtes Scheinwerfer-Attribut zu und leitet die passende Balkenfarbe ab.
     *
     * @param fixtureName  Name des Scheinwerfers
     * @param type         Attributtyp
     * @param fixtureColor Akzentfarbe des Scheinwerfers
     */
    public void assignFixture(String fixtureName, ChannelType type, Color fixtureColor) {
        assignFixture(fixtureName, type, fixtureColor, 1, null);
    }

    /**
     * Weist dem Kanal ein gepatchtes Scheinwerfer-Attribut mit relativem Kanalindex und Bezeichnung zu.
     *
     * @param fixtureName         Name des Scheinwerfers
     * @param type                Attributtyp
     * @param fixtureColor        Akzentfarbe des Scheinwerfers
     * @param fixtureChannelIndex 1-basierter Kanalindex innerhalb des Geräts
     * @param customChannelName   Spezifischer Kanalname laut Fixture-Definition (oder null)
     */
    public void assignFixture(String fixtureName, ChannelType type, Color fixtureColor, int fixtureChannelIndex, String customChannelName) {
        this.fixtureName = fixtureName;
        this.channelType = type;
        this.fixtureColor = fixtureColor;
        this.fixtureChannelIndex = Math.max(1, fixtureChannelIndex);
        this.customChannelName = customChannelName;

        if (type != null) {
            this.functionName = formatChannelTypeName(type);
            this.barColor = resolveBarColor(type, fixtureColor);
        } else {
            clearAssignment();
        }
    }

    /**
     * Liefert den 1-basierten relativen Kanalindex innerhalb des zugewiesenen Scheinwerfers.
     *
     * @return 1-basierter Fixture-Kanalindex
     */
    public int getFixtureChannelIndex() {
        return fixtureChannelIndex;
    }

    /**
     * Liefert die individuelle Kanalbezeichnung aus der Geräte-Definition oder null.
     *
     * @return Kanalname oder null
     */
    public String getCustomChannelName() {
        return customChannelName;
    }

    /**
     * Hebt die Scheinwerfer-Zuweisung auf und setzt den Kanal auf den Standardzustand zurück.
     */
    public void clearAssignment() {
        this.fixtureName = null;
        this.channelType = null;
        this.fixtureColor = null;
        this.fixtureChannelIndex = 1;
        this.customChannelName = null;
        this.functionName = "-";
        this.barColor = DEFAULT_BAR_COLOR;
    }

    private String formatChannelTypeName(ChannelType type) {
        return switch (type) {
            case DIMMER -> "Dimmer";
            case PAN -> "Pan";
            case PAN_FINE -> "Pan F.";
            case TILT -> "Tilt";
            case TILT_FINE -> "Tilt F.";
            case RED -> "Rot";
            case GREEN -> "Grün";
            case BLUE -> "Blau";
            case WHITE -> "Weiß";
            case AMBER -> "Amber";
            case UV -> "UV";
            case SHUTTER -> "Strobe";
            case COLOR_WHEEL -> "Farbrad";
            case GOBO -> "Gobo";
            case PRISM -> "Prisma";
            case SPEED -> "Speed";
            case EFFECT -> "Effekt";
            case MAINTENANCE -> "Control";
            default -> "Kanal";
        };
    }

    private Color resolveBarColor(ChannelType type, Color fallback) {
        return switch (type) {
            case DIMMER -> Color.web("#FEDC94"); // Warm Amber
            case PAN, PAN_FINE, TILT, TILT_FINE -> Color.web("#38BDF8"); // Cyan
            case RED -> Color.web("#EF4444"); // Signalrot
            case GREEN -> Color.web("#10B981"); // Smaragdgrün
            case BLUE -> Color.web("#60A5FA"); // Helles Blau
            case WHITE -> Color.web("#F3F4F6"); // Soft White
            case AMBER -> Color.web("#FBBF24"); // Amber
            case UV -> Color.web("#A855F7"); // Purple UV
            case SHUTTER -> Color.web("#F59E0B"); // Orange
            default -> fallback != null ? fallback : DEFAULT_BAR_COLOR;
        };
    }
}
