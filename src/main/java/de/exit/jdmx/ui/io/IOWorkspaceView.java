package de.exit.jdmx.ui.io;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

/**
 * Vollständiger Arbeitsbereich für Tab 5 (I/O Patch).
 * <p>
 * Architektur wie in Stage & Fixtures:
 * <ul>
 *     <li><b>Oben (Horizontales SplitPane):</b> Genau 2 verschiebbare Steuerungs-Docks
 *         ({@link InputDockView} und {@link OutputDockView}) im scharfkantigen {@code control-module-card} Stil.
 *         Die Trennlinie lässt sich horizontal stufenlos verschieben.</li>
 *     <li><b>Unten (Vertikales SplitPane):</b> Der 512-Kanal Universe Live-Monitor {@link DmxMatrixView}
 *         im 16-Spalten Mischpult-Raster mit Zero-Lag Dirty Checking.
 *         Die Höhe der oberen Docks lässt sich über die vertikale Dragbar beliebig anpassen.</li>
 * </ul>
 * </p>
 */
public class IOWorkspaceView extends SplitPane {

    /** Standard-Höhenanteil für die oberen Steuerungsdocks (ca. 24%). */
    public static final double DEFAULT_VERTICAL_RATIO = 0.24;

    private final InputDockView inputDock;
    private final OutputDockView outputDock;
    private final SplitPane docksSplitPane;
    private final DmxMatrixView matrixView;

    public IOWorkspaceView() {
        setOrientation(Orientation.VERTICAL);
        getStyleClass().add("io-workspace-view");

        // 1. Obere Hälfte: Horizontales SplitPane mit genau 2 Steuerungs-Docks
        this.inputDock = new InputDockView();
        this.outputDock = new OutputDockView();

        this.docksSplitPane = new SplitPane();
        docksSplitPane.setOrientation(Orientation.HORIZONTAL);
        docksSplitPane.getStyleClass().add("io-docks-split");
        docksSplitPane.getItems().addAll(inputDock, outputDock);
        docksSplitPane.setDividerPositions(0.50);

        // 2. Untere Hälfte: 512-Kanal Matrix (Zero-Lag)
        this.matrixView = new DmxMatrixView();

        getItems().addAll(docksSplitPane, matrixView);
        SplitPane.setResizableWithParent(docksSplitPane, false);
        setDividerPositions(DEFAULT_VERTICAL_RATIO);
    }

    public SplitPane getDocksSplitPane() {
        return docksSplitPane;
    }

    public InputDockView getInputDock() {
        return inputDock;
    }

    public OutputDockView getOutputDock() {
        return outputDock;
    }

    public DmxMatrixView getMatrixView() {
        return matrixView;
    }
}
