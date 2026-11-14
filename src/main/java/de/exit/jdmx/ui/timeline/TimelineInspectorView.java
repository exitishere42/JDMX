package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Inspektor-Seitenleiste für die Show-Timeline (Tab 4).
 * <p>
 * Zeigt Eigenschaften des aktuell in der Timeline ausgewählten Elements
 * (Track, Clip, Beat-Marker) an und erlaubt präzise Anpassungen.
 * </p>
 */
public class TimelineInspectorView extends VBox {

    private final Label lblTitle;
    private final Label lblEmptyMessage;
    private final StackPane contentContainer;

    /**
     * Erstellt eine neue Inspektor-Ansicht mit Standard-Platzhalter.
     */
    public TimelineInspectorView() {
        super(0);
        getStyleClass().add("timeline-inspector-view");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Header (Titel)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("timeline-inspector-header");

        this.lblTitle = new Label(I18n.get("timeline.inspector.title"));
        lblTitle.getStyleClass().add("timeline-inspector-title");
        header.getChildren().add(lblTitle);

        // 2. Platzhalter für leere Auswahl
        this.contentContainer = new StackPane();
        contentContainer.getStyleClass().add("timeline-inspector-content");
        VBox.setVgrow(contentContainer, Priority.ALWAYS);

        this.lblEmptyMessage = new Label(I18n.get("timeline.inspector.empty"));
        lblEmptyMessage.getStyleClass().add("timeline-inspector-placeholder");
        lblEmptyMessage.setGraphic(new LucideIcon("sliders-horizontal", 24, StudioTheme.COLOR_TEXT_LOW));
        lblEmptyMessage.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
        lblEmptyMessage.setGraphicTextGap(10);
        contentContainer.getChildren().add(lblEmptyMessage);
        StackPane.setAlignment(lblEmptyMessage, Pos.CENTER);

        getChildren().addAll(header, contentContainer);

        // Sprachwechsel reaktiv
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });
    }

    /**
     * Aktualisiert Texte bei Sprachwechsel.
     */
    public void updateLocalization() {
        lblTitle.setText(I18n.get("timeline.inspector.title"));
        lblEmptyMessage.setText(I18n.get("timeline.inspector.empty"));
    }

    /**
     * Liefert das Titel-Label.
     *
     * @return das {@link Label}
     */
    public Label getLblTitle() {
        return lblTitle;
    }

    /**
     * Liefert das Platzhalter-Label.
     *
     * @return das Platzhalter-{@link Label}
     */
    public Label getLblEmptyMessage() {
        return lblEmptyMessage;
    }
}
