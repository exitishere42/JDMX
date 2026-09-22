package de.exit.jdmx.ui.tabs;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Tab 1: 2D Stage & Fixtures.
 * Visual CAD rigging blueprint of U-Truss against the projection wall with vector beams.
 */
public class StageTab extends HBox {

    /**
     * Constructs the Stage Tab.
     */
    public StageTab() {
        setSpacing(12);
        getStyleClass().add("jdmx-bg");
        HBox.setHgrow(this, Priority.ALWAYS);

        // Stage Canvas Area
        VBox canvasArea = new VBox(8);
        HBox.setHgrow(canvasArea, Priority.ALWAYS);
        canvasArea.getStyleClass().addAll("jdmx-card", "stage-canvas-area");

        HBox topInfo = new HBox(12);
        topInfo.setAlignment(Pos.CENTER_LEFT);
        Label rigInfo = new Label("Rigging: U-Traverse gegen Projektionswand");
        rigInfo.getStyleClass().add("jdmx-section-title");
        topInfo.getChildren().add(rigInfo);

        Canvas canvas = new Canvas(800, 500);
        drawBlueprint(canvas);

        canvasArea.getChildren().addAll(topInfo, canvas);

        // Quick Inspector Right
        VBox inspector = new VBox(12);
        inspector.setPrefWidth(280);
        inspector.getStyleClass().addAll("jdmx-card", "inspector-panel");

        Label inspectorTitle = new Label("Fixture Quick Inspector");
        inspectorTitle.getStyleClass().add("jdmx-section-title");

        Label fixtureName = new Label("Moving Head 1 (Left Truss)");
        fixtureName.setStyle("-fx-text-fill: -jdmx-primary; -fx-font-weight: bold;");

        Label dmxAddr = new Label("Universe 1 | Start: CH 01 | 16 Kanäle");
        dmxAddr.setStyle("-fx-text-fill: -jdmx-text-medium; -fx-font-size: 11px;");

        inspector.getChildren().addAll(inspectorTitle, fixtureName, dmxAddr);

        getChildren().addAll(canvasArea, inspector);
    }

    /**
     * Draws the blueprint on the provided canvas.
     *
     * @param canvas The canvas to draw on.
     */
    private void drawBlueprint(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Background (0dp Base)
        gc.setFill(Color.web("#121212"));
        gc.fillRect(0, 0, w, h);

        // Wall representation at bottom (1dp Sheet)
        gc.setFill(Color.web("#1E1E1E"));
        gc.setStroke(Color.web("#2C2C2C"));
        gc.setLineWidth(2);
        gc.fillRect(100, h - 60, w - 200, 40);
        gc.strokeRect(100, h - 60, w - 200, 40);

        gc.setFill(Color.web("#9E9E9E"));
        gc.fillText("PROJEKTIONSWAND", w / 2 - 60, h - 35);

        // U-Truss Rigging (Primary Teal)
        gc.setStroke(Color.web("#03DAC6"));
        gc.setLineWidth(3);
        // Left arm
        gc.strokeLine(150, 60, 150, h - 140);
        // Cross truss
        gc.strokeLine(150, 60, w - 150, 60);
        // Right arm
        gc.strokeLine(w - 150, 60, w - 150, h - 140);

        // Fixture dots
        gc.setFill(Color.web("#ffff00"));
        gc.fillOval(142, 100, 16, 16);
        gc.fillOval(142, 200, 16, 16);

        gc.setFill(Color.web("#ff453a"));
        gc.fillOval(w - 158, 100, 16, 16);
        gc.fillOval(w - 158, 200, 16, 16);
    }
}
