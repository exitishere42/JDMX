package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

import java.util.Objects;

/**
 * Linke Track-Header-Box in der Show-Timeline (FL Studio Playlist-Stil).
 * <p>
 * Enthält:
 * <ul>
 *     <li>Einen runden Mute-Knopf (●) zum Stummschalten/Aktivieren der Spur</li>
 *     <li>Den Track-Namen mit Inline-Editierung bei Doppelklick</li>
 * </ul>
 * Reagiert reaktiv auf Zustandsänderungen im {@link TimelineService}.
 * </p>
 */
public class TimelineTrackHeaderBox extends HBox {

    /** Standardbreite der Track-Header-Box in Pixeln (verhindert Zusammenstauchen). */
    public static final double DEFAULT_HEADER_WIDTH = 160.0;

    private final TimelineTrack track;
    private final TimelineService service;

    private final Button btnMute;
    private final Label lblName;
    private final TextField txtRename;
    private final StackPane nameContainer;

    private boolean editing = false;

    /**
     * Erstellt eine neue Track-Header-Box für die gegebene Spur.
     *
     * @param track   die zugehörige Spur (darf nicht {@code null} sein)
     * @param service der Timeline-Service (darf nicht {@code null} sein)
     */
    public TimelineTrackHeaderBox(TimelineTrack track, TimelineService service) {
        super(8);
        this.track = Objects.requireNonNull(track, "track must not be null");
        this.service = Objects.requireNonNull(service, "service must not be null");

        getStyleClass().add("timeline-track-header-box");
        setAlignment(Pos.CENTER_LEFT);
        setMinHeight(track.getHeight());
        setPrefHeight(track.getHeight());
        setMaxHeight(track.getHeight());
        setMinWidth(DEFAULT_HEADER_WIDTH);
        setPrefWidth(DEFAULT_HEADER_WIDTH);
        setMaxWidth(DEFAULT_HEADER_WIDTH);

        // 1. Runder Mute-Knopf (●)
        this.btnMute = new Button();
        btnMute.getStyleClass().add("timeline-track-mute-btn");
        updateMuteStateVisual();
        btnMute.setTooltip(new Tooltip("Mute"));
        btnMute.setOnAction(e -> {
            service.toggleTrackMute(track.getId());
            updateMuteStateVisual();
        });

        // 2. Name-Label
        this.lblName = new Label(track.getName());
        lblName.getStyleClass().add("timeline-track-name-lbl");
        lblName.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        lblName.setEllipsisString("...");
        Tooltip nameTip = new Tooltip(track.getName());
        Tooltip.install(lblName, nameTip);
        HBox.setHgrow(lblName, Priority.ALWAYS);
        lblName.setMaxWidth(Double.MAX_VALUE);

        // Doppelklick zum Umbenennen
        lblName.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                startEditing();
                event.consume();
            }
        });

        // 3. Inline TextField zum Umbenennen
        this.txtRename = new TextField(track.getName());
        txtRename.getStyleClass().add("timeline-track-rename-txt");
        txtRename.setVisible(false);
        txtRename.setManaged(false);

        txtRename.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                commitEditing();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                cancelEditing();
            }
        });

        txtRename.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (Boolean.FALSE.equals(isFocused) && editing) {
                commitEditing();
            }
        });

        this.nameContainer = new StackPane(lblName, txtRename);
        nameContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameContainer, Priority.ALWAYS);

        getChildren().addAll(nameContainer, btnMute);

        // Kontextmenü & Drag & Drop
        setupContextMenu();
        setupDragAndDrop();
    }

    private void setupContextMenu() {
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();

        javafx.scene.control.MenuItem itemAssign = new javafx.scene.control.MenuItem("Song zuweisen...");
        itemAssign.setOnAction(e -> handleAssignSongViaChooser());

        javafx.scene.control.MenuItem itemRename = new javafx.scene.control.MenuItem("Spur umbenennen");
        itemRename.setOnAction(e -> startEditing());

        javafx.scene.control.MenuItem itemRemoveSong = new javafx.scene.control.MenuItem("Song entfernen");
        itemRemoveSong.setOnAction(e -> {
            track.setSongFile(null);
            track.setSongName(null);
            track.setWaveformData(new float[0]);
            service.notifyChange();
        });

        javafx.scene.control.MenuItem itemAddTrack = new javafx.scene.control.MenuItem("Track hinzufügen");
        itemAddTrack.setOnAction(e -> service.addTrack());

        javafx.scene.control.MenuItem itemDeleteTrack = new javafx.scene.control.MenuItem("Track löschen");
        itemDeleteTrack.setOnAction(e -> {
            if (service.getTracks().size() > 1) {
                service.removeTrack(track.getId());
            }
        });

        contextMenu.getItems().addAll(itemAssign, itemRename, itemRemoveSong, new javafx.scene.control.SeparatorMenuItem(), itemAddTrack, itemDeleteTrack);

        setOnContextMenuRequested(e -> {
            itemRemoveSong.setDisable(!track.hasSong());
            itemDeleteTrack.setDisable(service.getTracks().size() <= 1);
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
        });
    }

    /**
     * Öffnet einen Dateidialog, um der Spur direkt eine Audiodatei zuzuweisen.
     */
    public void handleAssignSongViaChooser() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Song für " + track.getName() + " auswählen");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Audiodateien (*.mp3, *.wav)", "*.mp3", "*.wav"));
        java.io.File chosen = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (chosen != null) {
            service.assignSongToTrack(track.getId(), chosen);
            lblName.setText(track.getName());
            Tooltip.install(lblName, new Tooltip(track.getName()));
        }
    }

    private void setupDragAndDrop() {
        setOnDragOver(e -> {
            javafx.scene.input.Dragboard db = e.getDragboard();
            java.util.Optional<java.io.File> audioOpt = TimelineDragHelper.extractAudioFile(db, service);
            if (audioOpt.isPresent()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY_OR_MOVE);
                if (!getStyleClass().contains("timeline-track-drop-target")) {
                    getStyleClass().add("timeline-track-drop-target");
                }
            }
            e.consume();
        });

        setOnDragExited(e -> {
            getStyleClass().remove("timeline-track-drop-target");
            e.consume();
        });

        setOnDragDropped(e -> {
            getStyleClass().remove("timeline-track-drop-target");
            javafx.scene.input.Dragboard db = e.getDragboard();
            java.util.Optional<java.io.File> audioOpt = TimelineDragHelper.extractAudioFile(db, service);

            if (audioOpt.isPresent()) {
                java.io.File targetFile = audioOpt.get();
                service.assignSongToTrack(track.getId(), targetFile);
                lblName.setText(track.getName());
                Tooltip.install(lblName, new Tooltip(track.getName()));
                e.setDropCompleted(true);
            }
            e.consume();
        });
    }

    /**
     * Startet die Inline-Editierung des Track-Namens.
     */
    public void startEditing() {
        editing = true;
        txtRename.setText(track.getName());
        lblName.setVisible(false);
        lblName.setManaged(false);
        txtRename.setVisible(true);
        txtRename.setManaged(true);
        txtRename.selectAll();
        txtRename.requestFocus();
    }

    /**
     * Bestätigt und speichert den neuen Namen im {@link TimelineService}.
     */
    public void commitEditing() {
        if (!editing) return;
        editing = false;
        String newName = txtRename.getText();
        if (newName != null && !newName.trim().isEmpty()) {
            service.renameTrack(track.getId(), newName.trim());
            lblName.setText(newName.trim());
        }
        txtRename.setVisible(false);
        txtRename.setManaged(false);
        lblName.setVisible(true);
        lblName.setManaged(true);
    }

    /**
     * Bricht die Editierung ab, ohne den Namen zu verändern.
     */
    public void cancelEditing() {
        editing = false;
        txtRename.setVisible(false);
        txtRename.setManaged(false);
        lblName.setVisible(true);
        lblName.setManaged(true);
    }

    /**
     * Aktualisiert die visuelle Darstellung des Mute-Knopfs.
     */
    public void updateMuteStateVisual() {
        btnMute.getStyleClass().removeAll("active", "muted");
        if (track.isMuted()) {
            btnMute.getStyleClass().add("muted");
        } else {
            btnMute.getStyleClass().add("active");
        }
    }

    /**
     * Liefert die zugehörige Spur.
     *
     * @return die {@link TimelineTrack}-Instanz
     */
    public TimelineTrack getTrack() {
        return track;
    }

    /**
     * Liefert den Mute-Button.
     *
     * @return der Mute-Button
     */
    public Button getBtnMute() {
        return btnMute;
    }

    /**
     * Liefert das Namens-Label.
     *
     * @return das Namens-Label
     */
    public Label getLblName() {
        return lblName;
    }

    /**
     * Liefert das Umbenennungs-Textfeld.
     *
     * @return das Textfeld
     */
    public TextField getTxtRename() {
        return txtRename;
    }

    /**
     * Prüft, ob sich die Trackbox gerade im Editiermodus befindet.
     *
     * @return {@code true}, wenn im Editiermodus, sonst {@code false}
     */
    public boolean isEditing() {
        return editing;
    }
}
