package de.exit.jdmx.ui.io;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.service.HardwareDiscoveryService;
import de.exit.jdmx.ui.component.CustomDropdownButton;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
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
 * Harmonisch abgestimmt auf das Studio-Dark Design (identischer Rahmen #2E2E2E,
 * 4px Eckenradius, einheitliches Teal-Header-Icon und dezenter Status-Punkt).
 * </p>
 */
public class MidiRoutingCard extends VBox {

    private final HardwareDiscoveryService hardwareService;
    private final Label titleLabel;
    private final Circle statusDot;
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
        setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #2E2E2E; -fx-border-width: 1px; " +
                "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 14 16;");
        setMinWidth(240);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);

        // 1. Header (Teal-Icon, einheitlich zum I/O-Tab)
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon icon = new LucideIcon("sliders", 15, StudioTheme.COLOR_ICON_IO);
        this.titleLabel = new Label(I18n.get("io.card.midi"));
        titleLabel.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px; -fx-font-weight: 700;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Dezenter Status-Indikator (Punkt + Text wie in UniversePatchTable)
        HBox statusBox = new HBox(6);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        this.statusDot = new Circle(4.5, Color.web("#10B981"));
        this.badgeLabel = new Label(I18n.get("io.midi.status"));
        badgeLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: 600;");
        statusBox.getChildren().addAll(statusDot, badgeLabel);

        header.getChildren().addAll(icon, titleLabel, spacer, statusBox);

        // 2. Dropdown & Details
        this.midiDropdown = new CustomDropdownButton(I18n.get("io.midi.no_device"), List.of());
        midiDropdown.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
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
            statusDot.setFill(Color.web("#10B981"));
            badgeLabel.setText(I18n.get("io.midi.status"));
            badgeLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: 600;");
            statusLabel.setText(I18n.get("io.midi.status"));
            statusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: 600;");
        } else {
            midiDropdown.selectItem(I18n.get("io.midi.no_device"));
            statusDot.setFill(Color.web("#6B7280"));
            badgeLabel.setText(I18n.get("io.status.inactive"));
            badgeLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-weight: 500;");
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

        signalLed.setFill(Color.web("#10B981"));
        lastSignalLabel.setText(eventText);
        lastSignalLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: 600;");

        PauseTransition pause = new PauseTransition(Duration.millis(250));
        pause.setOnFinished(ev -> {
            signalLed.setFill(Color.web("#4B5563"));
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
