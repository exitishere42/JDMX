package de.exit.jdmx.ui.io;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.service.HardwareDiscoveryService;
import de.exit.jdmx.ui.component.CustomDropdownButton;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;

/**
 * Modulare Hardware-Karte für MIDI-Controller im I/O-Routing Workspace.
 * <p>
 * Zeigt angeschlossene MIDI-Geräte, den Empfangsstatus, eine Live-Signal-LED
 * sowie den aktuellen Event-Monitor (CC, Note, Pitch) an.
 * </p>
 */
public class MidiRoutingCard extends VBox {

    private final HardwareDiscoveryService hardwareService;
    private final Label titleLabel;
    private final Label badgeLabel;
    private final CustomDropdownButton midiDropdown;

    private Label lblDeviceTitle;
    private Label lblStatusTitle;
    private Label lblSignalTitle;

    private final Label statusLabel;
    private final Label lastSignalLabel;
    private final Circle signalLed;

    public MidiRoutingCard(HardwareDiscoveryService hardwareService) {
        super(10);
        this.hardwareService = Objects.requireNonNull(hardwareService, "hardwareService darf nicht null sein");

        getStyleClass().add("io-hardware-card");
        setStyle("-fx-background-color: #1A1A1E; -fx-border-color: #2D2D32; -fx-border-width: 1px; " +
                "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 14;");
        setMinWidth(240);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);

        // 1. Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon icon = new LucideIcon("sliders", 15, Color.web("#A78BFA"));
        this.titleLabel = new Label(I18n.get("io.card.midi"));
        titleLabel.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px; -fx-font-weight: 700;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.badgeLabel = new Label(I18n.get("io.midi.status"));
        badgeLabel.setStyle("-fx-background-color: rgba(167, 139, 250, 0.15); -fx-text-fill: #A78BFA; " +
                "-fx-font-size: 10px; -fx-font-weight: 600; -fx-padding: 2 6; -fx-background-radius: 3px; -fx-border-color: #6D28D9; -fx-border-radius: 3px;");

        header.getChildren().addAll(icon, titleLabel, spacer, badgeLabel);

        // 2. Dropdown & Details
        this.midiDropdown = new CustomDropdownButton(I18n.get("io.midi.no_device"), List.of());
        midiDropdown.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 0, 0, 0));

        this.lblDeviceTitle = createFormLabel(I18n.get("io.midi.device"));
        this.lblStatusTitle = createFormLabel(I18n.get("io.col.status") + ":");
        this.lblSignalTitle = createFormLabel(I18n.get("io.midi.last_event"));

        this.statusLabel = createValueLabel(I18n.get("io.status.inactive"));

        // Signal-LED & Monitorzeile
        HBox signalBox = new HBox(6);
        signalBox.setAlignment(Pos.CENTER_LEFT);

        this.signalLed = new Circle(4.0, Color.web("#4B5563"));
        this.lastSignalLabel = createValueLabel(I18n.get("io.midi.waiting"));
        signalBox.getChildren().addAll(signalLed, lastSignalLabel);

        grid.add(lblDeviceTitle, 0, 0);
        grid.add(midiDropdown, 1, 0);

        grid.add(lblStatusTitle, 0, 1);
        grid.add(statusLabel, 1, 1);

        grid.add(lblSignalTitle, 0, 2);
        grid.add(signalBox, 1, 2);

        getChildren().addAll(header, grid);

        refreshHardware();

        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });
    }

    public void refreshHardware() {
        List<String> devices = hardwareService.getDetectedMidiInputs();
        midiDropdown.setItems(devices);

        if (!devices.isEmpty()) {
            midiDropdown.selectItem(devices.get(0));
            badgeLabel.setText(I18n.get("io.midi.status"));
            badgeLabel.setStyle("-fx-background-color: rgba(167, 139, 250, 0.15); -fx-text-fill: #A78BFA; " +
                    "-fx-font-size: 10px; -fx-font-weight: 600; -fx-padding: 2 6; -fx-background-radius: 3px; -fx-border-color: #6D28D9; -fx-border-radius: 3px;");
            statusLabel.setText(I18n.get("io.midi.status"));
            statusLabel.setStyle("-fx-text-fill: #A78BFA; -fx-font-size: 11px; -fx-font-weight: 600;");
        } else {
            midiDropdown.selectItem(I18n.get("io.midi.no_device"));
            badgeLabel.setText(I18n.get("io.status.inactive"));
            badgeLabel.setStyle("-fx-background-color: #242424; -fx-text-fill: #71717A; -fx-font-size: 10px; -fx-padding: 2 6; -fx-border-color: #333333; -fx-border-radius: 3px; -fx-background-radius: 3px;");
            statusLabel.setText(I18n.get("io.midi.no_device"));
            statusLabel.setStyle("-fx-text-fill: #71717A; -fx-font-size: 11px; -fx-font-weight: 500;");
        }
    }

    /**
     * Löst ein visuelles Blinken der Signal-LED und die Anzeige des MIDI-Events aus.
     *
     * @param eventText z. B. "CC 7: 127"
     */
    public void notifyMidiSignal(String eventText) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> notifyMidiSignal(eventText));
            return;
        }

        signalLed.setFill(Color.web("#10B981")); // Grün aufleuchten
        lastSignalLabel.setText(eventText);
        lastSignalLabel.setStyle("-fx-text-fill: #34D399; -fx-font-size: 11px; -fx-font-weight: 600;");

        PauseTransition pause = new PauseTransition(Duration.millis(250));
        pause.setOnFinished(ev -> {
            signalLed.setFill(Color.web("#4B5563")); // Zurück zu grau
            lastSignalLabel.setStyle("-fx-text-fill: #E5E7EB; -fx-font-size: 11px; -fx-font-weight: 500;");
        });
        pause.play();
    }

    public void updateLocalization() {
        titleLabel.setText(I18n.get("io.card.midi"));
        lblDeviceTitle.setText(I18n.get("io.midi.device"));
        lblStatusTitle.setText(I18n.get("io.col.status") + ":");
        lblSignalTitle.setText(I18n.get("io.midi.last_event"));
        refreshHardware();
    }

    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: 500; -fx-min-width: 85px;");
        return lbl;
    }

    private Label createValueLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #E5E7EB; -fx-font-size: 11px; -fx-font-weight: 500;");
        return lbl;
    }

    public CustomDropdownButton getMidiDropdown() {
        return midiDropdown;
    }

    public Circle getSignalLed() {
        return signalLed;
    }

    public Label getLastSignalLabel() {
        return lastSignalLabel;
    }
}
