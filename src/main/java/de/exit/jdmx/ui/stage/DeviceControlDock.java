package de.exit.jdmx.ui.stage;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Das untere 24/7 Steuerungs-Dock für Tab 1 (Stage & Fixtures).
 * <p>
 * Robuste, jitterfreie Hybrid-Architektur (DMXControl-Standard):
 * <ul>
 *     <li><b>Links (Fest):</b> {@link IntensityModuleView} mit fester Breite von 270px,
 *         vollständig entkoppelt vom SplitPane (kann horizontal in keine Richtung verschoben werden)</li>
 *     <li><b>Rechts (Dynamisch):</b> Horizontales {@link SplitPane} mit {@link ColorModuleView} und
 *         {@link PositionModuleView}, die sich den verbleibenden Platz stabil 50/50 teilen und
 *         deren Teiler frei verschiebbar ist (ohne jedes gegenseitiges Verdünnen!)</li>
 * </ul>
 * </p>
 */
public class DeviceControlDock extends HBox {

    private final IntensityModuleView intensityModule;
    private final ColorModuleView colorModule;
    private final PositionModuleView positionModule;
    private final SplitPane dynamicSplitPane;

    public DeviceControlDock() {
        getStyleClass().add("device-control-dock");
        setMinHeight(115.0);
        setPrefHeight(135.0);

        this.intensityModule = new IntensityModuleView();
        this.colorModule = new ColorModuleView();
        this.positionModule = new PositionModuleView();

        // Dynamisches SplitPane für die restlichen Module (Farbe & Position)
        this.dynamicSplitPane = new SplitPane();
        dynamicSplitPane.setOrientation(Orientation.HORIZONTAL);
        dynamicSplitPane.getStyleClass().add("device-control-dock-split");
        HBox.setHgrow(dynamicSplitPane, Priority.ALWAYS);

        dynamicSplitPane.getItems().addAll(colorModule, positionModule);
        dynamicSplitPane.setDividerPositions(0.50);

        // Links festes Intensitätsmodul, rechts das flexible SplitPane
        getChildren().addAll(intensityModule, dynamicSplitPane);
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

    public SplitPane getDynamicSplitPane() {
        return dynamicSplitPane;
    }

    /**
     * Liefert die horizontale Orientierung des dynamischen Modulbereichs.
     */
    public Orientation getOrientation() {
        return dynamicSplitPane.getOrientation();
    }

    /**
     * Liefert die 3 Steuerungskarten des Docks für Test- und Inspektionszwecke.
     */
    public ObservableList<Node> getItems() {
        return FXCollections.observableArrayList(intensityModule, colorModule, positionModule);
    }
}
