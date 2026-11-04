package de.exit.jdmx.ui.icon;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Standardkonforme Vektor-Flaggen für Sprachauswahlen in JavaFX.
 * <p>
 * Zeichnet pixelgenaue, scharfe Miniaturflaggen ohne externe Bilddateien
 * und ohne Emojis für 100% konsistente Darstellung auf Windows und Linux.
 * </p>
 */
public final class FlagIcon {

    private static final double WIDTH = 18.0;
    private static final double HEIGHT = 12.0;

    private FlagIcon() {
        // Utility-Klasse
    }

    /**
     * Erzeugt eine Vektor-Flagge anhand des Sprach-Codes (de, en, ru).
     *
     * @param code Sprachcode ("de", "en", "ru")
     * @return Ein gestyltes JavaFX Node
     */
    public static Node create(String code) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        switch (code.toLowerCase()) {
            case "de" -> drawGermanFlag(gc);
            case "en" -> drawUkFlag(gc);
            case "ru" -> drawRussianFlag(gc);
            default -> drawGenericFlag(gc);
        }

        StackPane container = new StackPane(canvas);
        container.setMinSize(WIDTH, HEIGHT);
        container.setPrefSize(WIDTH, HEIGHT);
        container.setMaxSize(WIDTH, HEIGHT);
        container.setStyle("-fx-border-color: #383838; -fx-border-width: 0.5px; -fx-background-radius: 2px; -fx-border-radius: 2px;");
        return container;
    }

    private static void drawGermanFlag(GraphicsContext gc) {
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillRect(0, 0, WIDTH, 4);

        gc.setFill(Color.web("#DD0000"));
        gc.fillRect(0, 4, WIDTH, 4);

        gc.setFill(Color.web("#FFCE00"));
        gc.fillRect(0, 8, WIDTH, 4);
    }

    private static void drawRussianFlag(GraphicsContext gc) {
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillRect(0, 0, WIDTH, 4);

        gc.setFill(Color.web("#0039A6"));
        gc.fillRect(0, 4, WIDTH, 4);

        gc.setFill(Color.web("#D52B1E"));
        gc.fillRect(0, 8, WIDTH, 4);
    }

    private static void drawUkFlag(GraphicsContext gc) {
        // Blauer Grund
        gc.setFill(Color.web("#012169"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Weiße Diagonalen
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.4);
        gc.strokeLine(0, 0, WIDTH, HEIGHT);
        gc.strokeLine(WIDTH, 0, 0, HEIGHT);

        // Rote Diagonalen
        gc.setStroke(Color.web("#C8102E"));
        gc.setLineWidth(1.2);
        gc.strokeLine(0, 0, WIDTH, HEIGHT);
        gc.strokeLine(WIDTH, 0, 0, HEIGHT);

        // Weißes Kreuz
        gc.setFill(Color.WHITE);
        gc.fillRect(7, 0, 4, HEIGHT);
        gc.fillRect(0, 4, WIDTH, 4);

        // Rotes Kreuz
        gc.setFill(Color.web("#C8102E"));
        gc.fillRect(8, 0, 2, HEIGHT);
        gc.fillRect(0, 5, WIDTH, 2);
    }

    private static void drawGenericFlag(GraphicsContext gc) {
        gc.setFill(Color.web("#2A2A2A"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
    }
}
