package de.exit.jdmx.ui.stage;

import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

/**
 * Arbeitsbereich für die Bühne (Tab 1: Stage & Fixtures).
 * <p>
 * Bietet die leere Arbeitsfläche für die zukünftige schrittweise Bühnenentwicklung.
 * </p>
 */
public class StageCanvasView extends StackPane {

    public StageCanvasView() {
        getStyleClass().add("stage-canvas-view");
        setMinHeight(150.0);
        setAlignment(Pos.CENTER);
    }
}
