package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Kompakter vertikaler Kanalstreifen für den LightDesk (Tab 2).
 * <p>
 * Enthält Funktionstext, DMX-Wertanzeige (0-255), Fader mit silbergrauer Pultkappe,
 * Kanalnummer und Schnell-Reset-Taste.
 * </p>
 */
public class DeskChannelStrip extends VBox {

    public static final double STRIP_WIDTH = 46.0;

    private final int channelNumber;
    private final Label lblFunction;
    private final Label lblValue;
    private final Slider slider;
    private final Label lblChannel;
    private final Button btnReset;
    private final Region fixtureIndicator;

    private ChannelType channelType;
    private String fixtureName;
    private IntConsumer onValueChanged;

    /**
     * Erstellt einen neuen Kanalstreifen für die angegebene 1-basierte Kanalnummer (1 bis 512).
     *
     * @param channelNumber 1-basierte DMX-Kanalnummer
     */
    public DeskChannelStrip(int channelNumber) {
        super(4);
        this.channelNumber = channelNumber;

        setAlignment(Pos.CENTER);
        setPrefWidth(STRIP_WIDTH);
        setMinWidth(STRIP_WIDTH);
        setMaxWidth(STRIP_WIDTH);
        getStyleClass().add("desk-channel-strip");

        // 1. Funktionstext (z. B. Dimmer, Pan, Tilt, Rot)
        this.lblFunction = new Label("-");
        lblFunction.getStyleClass().add("desk-channel-function");
        lblFunction.setMinWidth(Region.USE_PREF_SIZE);

        // 2. DMX Wert-Label (0 bis 255)
        this.lblValue = new Label("0");
        lblValue.getStyleClass().add("desk-channel-value");

        // 3. Vertikaler Fader-Slider (0 bis 255) mit etabliertem Pult-Design
        this.slider = new Slider(0.0, 255.0, 0.0);
        slider.setOrientation(Orientation.VERTICAL);
        slider.getStyleClass().add("fader-slider");
        slider.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(slider, Priority.ALWAYS);

        // 4. Kanalnummer (Kanal 1, 2...)
        this.lblChannel = new Label(String.valueOf(channelNumber));
        lblChannel.getStyleClass().add("desk-channel-number");

        // 5. Reset-Button [ x ]
        this.btnReset = new Button();
        btnReset.getStyleClass().add("desk-reset-btn");
        btnReset.setGraphic(new LucideIcon("x", 11, StudioTheme.COLOR_ICON_RED));
        btnReset.setOnAction(e -> reset());

        // 6. Dezenter Fixture-Indikatorpunkt / Balken am Fuß
        this.fixtureIndicator = new Region();
        fixtureIndicator.setMinSize(24, 2);
        fixtureIndicator.setPrefSize(24, 2);
        fixtureIndicator.setMaxSize(24, 2);
        fixtureIndicator.getStyleClass().add("desk-fixture-dot");
        fixtureIndicator.setVisible(false);

        getChildren().addAll(lblFunction, lblValue, slider, lblChannel, btnReset, fixtureIndicator);

        // Listener für Slider-Wert
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int dmx = (int) Math.round(newVal.doubleValue());
            lblValue.setText(String.valueOf(dmx));
            if (onValueChanged != null) {
                onValueChanged.accept(dmx);
            }
        });
    }

    /**
     * Weist dem Kanal ein gepatchtes Fixture-Attribut zu.
     *
     * @param fixtureName Name des Scheinwerfers
     * @param type        Attributtyp (z. B. DIMMER, PAN, TILT, RED)
     * @param dotColor    Akzentfarbe des Scheinwerfers
     */
    public void assignFixtureChannel(String fixtureName, ChannelType type, Color dotColor) {
        this.fixtureName = fixtureName;
        this.channelType = type;

        if (type != null) {
            lblFunction.setText(formatChannelTypeName(type));
            applyTypeColor(type);
        } else {
            lblFunction.setText("-");
            lblFunction.setStyle("");
        }

        if (dotColor != null) {
            fixtureIndicator.setVisible(true);
            String hex = String.format("#%02X%02X%02X",
                    (int) (dotColor.getRed() * 255),
                    (int) (dotColor.getGreen() * 255),
                    (int) (dotColor.getBlue() * 255));
            fixtureIndicator.setStyle("-fx-background-color: " + hex + ";");
        } else {
            fixtureIndicator.setVisible(false);
        }
    }

    /**
     * Entfernt die Scheinwerfer-Zuweisung und setzt den Kanal auf Standard zurück.
     */
    public void clearFixtureAssignment() {
        this.fixtureName = null;
        this.channelType = null;
        lblFunction.setText("-");
        lblFunction.setStyle("");
        fixtureIndicator.setVisible(false);
    }

    private String formatChannelTypeName(ChannelType type) {
        return switch (type) {
            case DIMMER -> "Dimmer";
            case PAN -> "Pan";
            case PAN_FINE -> "Pan F.";
            case TILT -> "Tilt";
            case TILT_FINE -> "Tilt F.";
            case RED -> "Rot";
            case GREEN -> "Grün";
            case BLUE -> "Blau";
            case WHITE -> "Weiß";
            case AMBER -> "Amber";
            case UV -> "UV";
            case SHUTTER -> "Strobe";
            case COLOR_WHEEL -> "Farbrad";
            case GOBO -> "Gobo";
            case PRISM -> "Prisma";
            case SPEED -> "Speed";
            case EFFECT -> "Effekt";
            case MAINTENANCE -> "Control";
            default -> "Kanal";
        };
    }

    private void applyTypeColor(ChannelType type) {
        switch (type) {
            case DIMMER -> lblFunction.setStyle("-fx-text-fill: #FEDC94; -fx-font-weight: bold;"); // Warm Amber
            case PAN, PAN_FINE, TILT, TILT_FINE -> lblFunction.setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold;"); // Cyan
            case RED -> lblFunction.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;"); // Rot
            case GREEN -> lblFunction.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;"); // Grün
            case BLUE -> lblFunction.setStyle("-fx-text-fill: #60A5FA; -fx-font-weight: bold;"); // Blau
            case WHITE -> lblFunction.setStyle("-fx-text-fill: #F3F4F6; -fx-font-weight: bold;"); // Weiß
            case SHUTTER -> lblFunction.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;"); // Amber
            default -> lblFunction.setStyle("-fx-text-fill: #9CA3AF;");
        }
    }

    /**
     * Setzt den DMX-Wert dieses Kanalstreifens auf 0 zurück.
     */
    public void reset() {
        slider.setValue(0.0);
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public Label getChannelFunctionLabel() {
        return lblFunction;
    }

    public Label getValueLabel() {
        return lblValue;
    }

    public Slider getSlider() {
        return slider;
    }

    public Label getChannelNumberLabel() {
        return lblChannel;
    }

    public Button getBtnReset() {
        return btnReset;
    }

    public void setOnValueChanged(IntConsumer onValueChanged) {
        this.onValueChanged = onValueChanged;
    }
}
