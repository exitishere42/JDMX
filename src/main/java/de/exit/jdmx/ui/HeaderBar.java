package de.exit.jdmx.ui;

import de.exit.jdmx.ui.icons.LucideIcon;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Permanent global header bar for jdmx DMX Controller.
 * Features DMX status pill badges, Grand Master fader, Blackout, and Stop All controls.
 */
public class HeaderBar extends HBox {

    private final Slider masterSlider;
    private final Label masterValueLabel;
    private final Button blackoutButton;
    private final Button stopAllButton;
    private final Button settingsButton;

    /**
     * Constructs the global header bar.
     *
     * @param onStopAll        Runnable to execute when Stop All is pressed.
     * @param onBlackoutToggle Runnable to execute when Blackout is toggled.
     */
    public HeaderBar(Runnable onStopAll, Runnable onBlackoutToggle) {
        getStyleClass().add("global-header");
        setSpacing(16);
        setAlignment(Pos.CENTER_LEFT);

        // --- Left: Brand Logo & Pill Badges ---
        HBox leftBox = new HBox(12);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        Label brandLabel = new Label("jdmx");
        brandLabel.getStyleClass().add("brand-title");

        HBox userBadge = createPillBadge("Bediener:", "Admin");
        HBox universeBadge = createPillBadge("Universe:", "1 (512 CH)");
        HBox fpsBadge = createPillBadge("DMX FPS:", "44 Hz");

        leftBox.getChildren().addAll(brandLabel, userBadge, universeBadge, fpsBadge);

        // Spacer
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // --- Center: Grand Master Slider ---
        HBox masterBox = new HBox(8);
        masterBox.getStyleClass().add("master-group");
        masterBox.setAlignment(Pos.CENTER);

        Label masterLabel = new Label("MASTER");
        masterLabel.getStyleClass().add("master-label");

        masterSlider = new Slider(0, 100, 100);
        masterSlider.setPrefWidth(120);

        masterValueLabel = new Label("100%");
        masterValueLabel.getStyleClass().add("master-value-label");

        masterSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            masterValueLabel.setText(String.format("%d%%", newVal.intValue()));
        });

        Button btn100 = new Button("100%");
        btn100.getStyleClass().add("btn-minimal");
        btn100.setOnAction(e -> masterSlider.setValue(100));

        Button btn0 = new Button("0%");
        btn0.getStyleClass().add("btn-minimal");
        btn0.setOnAction(e -> masterSlider.setValue(0));

        masterBox.getChildren().addAll(masterLabel, masterSlider, masterValueLabel, btn100, btn0);

        // Spacer
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // --- Right: Actions (Blackout, STOP ALL, Settings) ---
        HBox rightBox = new HBox(10);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        blackoutButton = new Button("BLACKOUT");
        blackoutButton.getStyleClass().add("btn-blackout");
        blackoutButton.setGraphic(LucideIcon.create(LucideIcon.POWER, LucideIcon.COLOR_TEXT, 0.7));
        blackoutButton.setOnAction(e -> {
            boolean isActive = blackoutButton.getStyleClass().contains("active");
            if (isActive) {
                blackoutButton.getStyleClass().remove("active");
            } else {
                blackoutButton.getStyleClass().add("active");
            }
            if (onBlackoutToggle != null) onBlackoutToggle.run();
        });

        stopAllButton = new Button("STOP ALL");
        stopAllButton.getStyleClass().add("btn-stop-all");
        stopAllButton.setGraphic(LucideIcon.create(LucideIcon.SQUARE_X, Color.WHITE, 0.7));
        stopAllButton.setOnAction(e -> {
            if (onStopAll != null) onStopAll.run();
        });

        settingsButton = new Button();
        settingsButton.getStyleClass().add("btn-jdmx-icon");
        settingsButton.setGraphic(LucideIcon.create(LucideIcon.WRENCH, LucideIcon.COLOR_HANDLE, 0.75));

        rightBox.getChildren().addAll(blackoutButton, stopAllButton, settingsButton);

        getChildren().addAll(leftBox, spacer1, masterBox, spacer2, rightBox);
    }

    /**
     * Creates a pill badge.
     *
     * @param labelText The text for the label.
     * @param valueText The text for the value.
     * @return HBox representing the pill badge.
     */
    private HBox createPillBadge(String labelText, String valueText) {
        HBox pill = new HBox(6);
        pill.getStyleClass().add("jdmx-pill");
        pill.setAlignment(Pos.CENTER);

        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("jdmx-pill-label");

        Label val = new Label(valueText);
        val.getStyleClass().add("jdmx-pill-value");

        pill.getChildren().addAll(lbl, val);
        return pill;
    }

    /**
     * Retrieves the Grand Master slider.
     *
     * @return The master slider.
     */
    public Slider getMasterSlider() {
        return masterSlider;
    }
}
