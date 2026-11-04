package de.exit.jdmx.ui.stage;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

/**
 * Das untere 24/7 Steuerungs-Dock für Tab 1 (Stage & Fixtures).
 * <p>
 * Beherbergt 3 nebeneinanderliegende, horizontal verschiebbare Steuerungskarten
 * im kompakten DMXControl-Stil:
 * <ul>
 *     <li><b>Modul 1:</b> {@link IntensityModuleView} (Dimmer, Strobo – vollflächig & ohne Shutter)</li>
 *     <li><b>Modul 2:</b> {@link ColorModuleView} (Großer HSV-Farbkreis mit je 4 Preset-Buttons links & rechts)</li>
 *     <li><b>Modul 3:</b> {@link PositionModuleView} (Großes Pan/Tilt 2D-Pad & Home-Button)</li>
 * </ul>
 * Jedes Modul besitzt eine feste Mindestbreite gegen versehentliches Kollabieren.
 * </p>
 */
public class DeviceControlDock extends SplitPane {

    private final IntensityModuleView intensityModule;
    private final ColorModuleView colorModule;
    private final PositionModuleView positionModule;

    public DeviceControlDock() {
        setOrientation(Orientation.HORIZONTAL);
        getStyleClass().add("device-control-dock");
        setMinHeight(115.0);
        setPrefHeight(135.0);

        this.intensityModule = new IntensityModuleView();
        this.colorModule = new ColorModuleView();
        this.positionModule = new PositionModuleView();

        // 3 Module hinzufügen
        getItems().addAll(intensityModule, colorModule, positionModule);

        // Standard-Teilung: ca. 32% Intensität, 36% Farbe, 32% Position
        setDividerPositions(0.32, 0.68);
    }

    public IntensityModuleView getIntensityModule() {
        return intensityModule;
    }

    public ColorModuleView getColorModule() {
        return colorModule;
    }

    public PositionModuleView getPositionModule() {
        return positionModule;
    }
}
