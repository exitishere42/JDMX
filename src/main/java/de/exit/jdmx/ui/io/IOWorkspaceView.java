package de.exit.jdmx.ui.io;

import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Vollständiger Arbeitsbereich für Tab 5 (Input/Output & Signal-Routing).
 * <p>
 * Oben: Die kompakte, horizontale Signalkette {@link SignalRoutingPane}
 * ([INPUT] ════► [JDMX PROZESSOR] ════► [OUTPUT]).<br>
 * Unten: Der 512-Kanal Universe Live-Monitor {@link DmxMatrixView}
 * mit grüner Viereck-Matrix, Universum-Umschaltung und Auslastungsanzeige.
 * </p>
 */
public class IOWorkspaceView extends VBox {

    private final SignalRoutingPane routingPane;
    private final DmxMatrixView matrixView;

    public IOWorkspaceView() {
        getStyleClass().add("io-workspace-view");
        setSpacing(6.0);
        setPadding(new Insets(6.0, 10.0, 10.0, 10.0));

        // 1. Obere Hälfte: Signalkette (flach)
        this.routingPane = new SignalRoutingPane();

        // 2. Untere Hälfte: 512-Kanal Matrix
        this.matrixView = new DmxMatrixView();
        VBox.setVgrow(matrixView, Priority.ALWAYS);

        getChildren().addAll(routingPane, matrixView);
    }

    public SignalRoutingPane getRoutingPane() {
        return routingPane;
    }

    public DmxMatrixView getMatrixView() {
        return matrixView;
    }
}
