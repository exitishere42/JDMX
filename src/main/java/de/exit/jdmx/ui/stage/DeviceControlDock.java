package de.exit.jdmx.ui.stage;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

/**
 * Das untere 24/7 Steuerungs-Dock für Tab 1 (Stage & Fixtures).
 * <p>
 * Beherbergt 3 nebeneinanderliegende, horizontal verschiebbare Steuerungskarten:
 * <ul>
 *     <li><b>Modul 1:</b> {@link IntensityModuleView} (Dimmer & Strobo)</li>
 *     <li><b>Modul 2:</b> {@link ColorModuleView} (HSV-Farbkreis & Presets)</li>
 *     <li><b>Modul 3:</b> {@link PositionModuleView} (Pan/Tilt 2D-Pad & Invert)</li>
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
        setMinHeight(180.0);

        this.intensityModule = new IntensityModuleView();
        this.colorModule = new ColorModuleView();
        this.positionModule = new PositionModuleView();

        // 3 Module hinzufügen
        getItems().addAll(intensityModule, colorModule, positionModule);

        // Standard-Teilung: ca. 30% Intensität, 35% Farbe, 35% Position
        setDividerPositions(0.30, 0.65);
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
