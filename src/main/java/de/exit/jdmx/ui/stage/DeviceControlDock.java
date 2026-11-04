package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

/**
 * Das untere 24/7 Steuerungs-Dock für Tab 1 (Stage & Fixtures).
 * <p>
 * Beherbergt 3 nebeneinanderliegende Steuerungskarten im DMXControl-Stil:
 * <ul>
 *     <li><b>Modul 1 (Intensität):</b> Feste Breite von 245px, horizontal in beide Richtungen
 *         vollständig gesperrt (weder nach links noch nach rechts verschiebbar)</li>
 *     <li><b>Modul 2 (Farbe):</b> Großer dynamischer HSV-Farbkreis mit anpassbaren Preset-Buttons</li>
 *     <li><b>Modul 3 (Position):</b> Großes 2D XY-Pad mit zentriertem Pan/Tilt und Home-Button</li>
 * </ul>
 * Der Teiler zwischen Farbe und Position bleibt frei horizontal verschiebbar.
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

        // Intensitätsmodul darf bei Fenster-Resizing horizontal nicht mitwachsen/schrumpfen
        SplitPane.setResizableWithParent(intensityModule, false);

        // Initiale Teilung: Intensitätsmodul fest auf FIXED_WIDTH (245px)
        Platform.runLater(this::lockIntensityDivider);

        // Divider 0 (nach Intensität) in beide Richtungen starr sperren
        if (!getDividers().isEmpty()) {
            SplitPane.Divider firstDivider = getDividers().getFirst();
            firstDivider.positionProperty().addListener((obs, oldVal, newVal) -> {
                double totalWidth = getWidth();
                if (totalWidth > 0) {
                    double targetRatio = IntensityModuleView.FIXED_WIDTH / totalWidth;
                    if (Math.abs(newVal.doubleValue() - targetRatio) > 0.001) {
                        firstDivider.setPosition(targetRatio);
                    }
                }
            });
        }

        // Bei Fensterbreiten-Änderungen den ersten Teiler stets exakt auf 245px halten
        widthProperty().addListener((obs, oldW, newW) -> lockIntensityDivider());
    }

    private void lockIntensityDivider() {
        double totalWidth = getWidth();
        if (totalWidth > IntensityModuleView.FIXED_WIDTH && getDividers().size() >= 2) {
            double div0 = IntensityModuleView.FIXED_WIDTH / totalWidth;
            double remaining = 1.0 - div0;
            double div1 = div0 + (remaining * 0.50);
            setDividerPositions(div0, div1);
        }
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
