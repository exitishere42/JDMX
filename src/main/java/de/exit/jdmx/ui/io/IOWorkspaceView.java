package de.exit.jdmx.ui.io;

import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Vollständiger Arbeitsbereich für Tab 5 (Input/Output & Universum-Patch nach QLC+ Vorbild).
 * <p>
 * Oben: Die kompakte Universum I/O Patch-Tabelle {@link UniversePatchTable}
 * mit Dropdown-Menüs für Input und Output pro Universum.<br>
 * Unten: Der 512-Kanal Universe Live-Monitor {@link DmxMatrixView}
 * im 16-Spalten Mischpult-Raster mit Zero-Lag Dirty Checking.
 * </p>
 */
public class IOWorkspaceView extends VBox {

    private final UniversePatchTable patchTable;
    private final DmxMatrixView matrixView;

    public IOWorkspaceView() {
        getStyleClass().add("io-workspace-view");
        setSpacing(8.0);
        setPadding(new Insets(8.0, 10.0, 10.0, 10.0));

        // 1. Obere Hälfte: QLC+-ähnliche Universum I/O Patch-Tabelle
        this.patchTable = new UniversePatchTable();

        // 2. Untere Hälfte: 512-Kanal Matrix (Zero-Lag)
        this.matrixView = new DmxMatrixView();
        VBox.setVgrow(matrixView, Priority.ALWAYS);

        getChildren().addAll(patchTable, matrixView);
    }

    public UniversePatchTable getPatchTable() {
        return patchTable;
    }

    public DmxMatrixView getMatrixView() {
        return matrixView;
    }
}
