package de.exit.jdmx.ui.stage;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * 2D Arbeitsbereich für Bühne, U-Traverse und Projektionswand.
 * <p>
 * Visualisiert die Positionen der Scheinwerfer (Moving Heads & Washlights)
 * an der U-Traverse und simuliert die Lichtkegel zur Projektionswand.
 * </p>
 */
public class StageCanvasView extends StackPane {

    private final Canvas canvas;

    public StageCanvasView() {
        getStyleClass().add("stage-canvas-view");
        setMinHeight(220.0);

        this.canvas = new Canvas(800, 400);
        getChildren().add(canvas);
        setAlignment(Pos.CENTER);

        // Responsive Canvas-Größenanpassung
        widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            renderStage();
        });
        heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            renderStage();
        });

        renderStage();
    }

    private void renderStage() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Hintergrund
        gc.setFill(Color.web("#121212"));
        gc.fillRect(0, 0, w, h);

        // 2. Dezent subtiles 40px Raster
        gc.setStroke(Color.web("#1A1A1A"));
        gc.setLineWidth(1);
        for (double x = 0; x < w; x += 40) {
            gc.strokeLine(x, 0, x, h);
        }
        for (double y = 0; y < h; y += 40) {
            gc.strokeLine(0, y, w, y);
        }

        // 3. Projektionswand oben
        double screenMargin = Math.max(40, w * 0.12);
        double screenW = w - (screenMargin * 2);
        double screenH = 14;
        double screenY = 28;

        // Leuchtender Glow-Effekt an der Wand
        gc.setFill(Color.web("#1E1E1E"));
        gc.fillRoundRect(screenMargin, screenY, screenW, screenH, 4, 4);
        gc.setStroke(Color.web("#383838"));
        gc.strokeRoundRect(screenMargin, screenY, screenW, screenH, 4, 4);

        gc.setFill(Color.web("#9CA3AF"));
        gc.fillText("PROJEKTIONSWAND", w / 2.0 - 55, screenY - 8);

        // 4. U-Traverse (Mitte & Schenkel nach unten)
        double trussY = screenY + Math.max(90, h * 0.38);
        double trussMargin = screenMargin + 40;
        double trussW = w - (trussMargin * 2);
        double legLen = Math.max(60, h * 0.22);

        gc.setStroke(Color.web("#404040"));
        gc.setLineWidth(6);

        // Quertraverse oben
        gc.strokeLine(trussMargin, trussY, trussMargin + trussW, trussY);
        // Linker Schenkel nach unten
        gc.strokeLine(trussMargin, trussY, trussMargin, trussY + legLen);
        // Rechter Schenkel nach unten
        gc.strokeLine(trussMargin + trussW, trussY, trussMargin + trussW, trussY + legLen);

        gc.setLineWidth(1);
        gc.setFill(Color.web("#6B7280"));
        gc.fillText("U - T R A V E R S E", w / 2.0 - 45, trussY + 22);

        // 5. Beispiel-Moving-Heads mit Lichtkegeln an die Wand
        int headCount = 4;
        double step = trussW / (headCount + 1);

        Color[] beamColors = {
                Color.web("#38BDF8", 0.18),
                Color.web("#A855F7", 0.18),
                Color.web("#10B981", 0.18),
                Color.web("#F59E0B", 0.18)
        };

        for (int i = 1; i <= headCount; i++) {
            double headX = trussMargin + (i * step);
            double headY = trussY;

            // Lichtkegel zur Wand
            double targetX = headX + ((i - 2.5) * 20);
            gc.setFill(beamColors[i - 1]);
            gc.beginPath();
            gc.moveTo(headX, headY);
            gc.lineTo(targetX - 25, screenY + screenH);
            gc.lineTo(targetX + 25, screenY + screenH);
            gc.closePath();
            gc.fill();

            // Scheinwerfer-Symbol auf der Traverse
            gc.setFill(Color.web("#222222"));
            gc.fillOval(headX - 10, headY - 10, 20, 20);
            gc.setStroke(Color.web("#D1D5DB"));
            gc.setLineWidth(1.5);
            gc.strokeOval(headX - 10, headY - 10, 20, 20);

            // Beschriftung
            gc.setFill(Color.web("#E5E7EB"));
            gc.fillText("MH " + i, headX - 12, headY - 14);
        }
    }
}
