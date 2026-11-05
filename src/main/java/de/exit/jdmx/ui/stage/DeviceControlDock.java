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
 *     <li><b>Links (Fest):</b> {@link IntensityModuleView} mit fester Breite von 300px,
 *         vollständig entkoppelt vom SplitPane (horizontal starr arretiert mit sauberem Randabstand)</li>
 *     <li><b>Rechts (Dynamisch):</b> Horizontales {@link SplitPane} mit {@link ColorModuleView} und
 *         {@link PositionModuleView}, die sich den verbleibenden Platz stabil 50/50 teilen</li>
 * </ul>
 * Die Mindesthöhe ist fest auf 135px verankert, sodass das Dashboard niemals kleiner geschoben werden kann.
 * </p>
 */
public class DeviceControlDock extends HBox {

    /** Standard- und Mindesthöhe des unteren Dashboards. */
    public static final double DOCK_HEIGHT = 220;

    private final IntensityModuleView intensityModule;
    private final ColorModuleView colorModule;
    private final PositionModuleView positionModule;
    private final SplitPane dynamicSplitPane;

    public DeviceControlDock() {
        getStyleClass().add("device-control-dock");
        setMinHeight(DOCK_HEIGHT);
        setPrefHeight(DOCK_HEIGHT);

        this.intensityModule = new IntensityModuleView();
        this.colorModule = new ColorModuleView();
        this.positionModule = new PositionModuleView();

        // Dynamisches SplitPane für die beiden flexiblen Module rechts (Farbe & Position)
        this.dynamicSplitPane = new SplitPane();
        dynamicSplitPane.setOrientation(Orientation.HORIZONTAL);
        dynamicSplitPane.getStyleClass().add("device-control-dock-split");
        HBox.setHgrow(dynamicSplitPane, Priority.ALWAYS);

        dynamicSplitPane.getItems().addAll(colorModule, positionModule);
        dynamicSplitPane.setDividerPositions(0.50);

        // Links festes Intensitätsmodul (300px), rechts das flexible SplitPane
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
