package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.ChaserDefinition;
import de.exit.jdmx.core.model.ChaserDirection;
import de.exit.jdmx.core.model.ChaserStep;
import de.exit.jdmx.core.model.SceneDefinition;
import de.exit.jdmx.service.ChaserService;
import de.exit.jdmx.service.SceneService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Objects;

/**
 * Integrierter Chaser-Editor für das Project-Explorer-Dock (rechte Seitenleiste).
 * <p>
 * Ermöglicht das Bearbeiten von Chaser-Schritten (Szenen, Hold- und Fade-Zeiten),
 * BPM, Tap-Tempo und Loop-Modi direkt in der Leiste, ohne die 2D-Bühne zu verdecken.
 * </p>
 */
public class ChaserDockEditorView extends VBox {

    private final SceneService sceneService;
    private final ChaserService chaserService;

    private final Button btnBack;
    private final Label lblTitle;
    private final Button btnPlay;
    private final Button btnPause;
    private final Button btnStop;

    private final Spinner<Integer> spinnerBpm;
    private final Button btnTap;
    private final ComboBox<ChaserDirection> comboDirection;

    private final VBox stepListContainer;
    private final Button btnAddStep;

    private ChaserDefinition currentChaser;
    private int currentHighlightedStep = -1;
    private long lastTapTime = 0;

    private final ChaserService.ChaserListener chaserListener = new ChaserService.ChaserListener() {
        @Override
        public void onStepChanged(String chaserId, int stepIndex) {
            if (currentChaser != null && currentChaser.getId().equals(chaserId)) {
                Platform.runLater(() -> highlightStep(stepIndex));
            }
        }

        @Override
        public void onStateChanged(String chaserId, boolean isPlaying) {
            if (currentChaser != null && currentChaser.getId().equals(chaserId)) {
                Platform.runLater(() -> updatePlayState(isPlaying));
            }
        }
    };

    public ChaserDockEditorView(SceneService sceneService, ChaserService chaserService) {
        this.sceneService = Objects.requireNonNull(sceneService, "sceneService must not be null");
        this.chaserService = Objects.requireNonNull(chaserService, "chaserService must not be null");

        setSpacing(6);
        setPadding(new Insets(6));
        getStyleClass().add("chaser-dock-editor");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Header (Zurück-Button, Titel, Play/Pause/Stop)
        this.btnBack = new Button();
        btnBack.getStyleClass().add("chaser-dock-back-btn");
        btnBack.setGraphic(new LucideIcon("arrow-left", 14, Color.web("#9CA3AF")));
        btnBack.setTooltip(new Tooltip(I18n.get("chaser.editor.back")));

        this.lblTitle = new Label("Chaser");
        lblTitle.getStyleClass().add("chaser-dock-title");
        lblTitle.setMaxWidth(110);

        this.btnPlay = new Button();
        btnPlay.setGraphic(new LucideIcon("play", 13, StudioTheme.COLOR_ICON_TIMELINE));
        btnPlay.getStyleClass().add("chaser-control-btn");
        btnPlay.setTooltip(new Tooltip("Play"));
        btnPlay.setOnAction(e -> {
            if (currentChaser != null) {
                chaserService.play(currentChaser.getId());
            }
        });

        this.btnPause = new Button();
        btnPause.setGraphic(new LucideIcon("pause", 13, Color.web("#F59E0B")));
        btnPause.getStyleClass().add("chaser-control-btn");
        btnPause.setTooltip(new Tooltip("Pause"));
        btnPause.setOnAction(e -> {
            if (currentChaser != null) {
                chaserService.pause(currentChaser.getId());
            }
        });

        this.btnStop = new Button();
        btnStop.setGraphic(new LucideIcon("square", 13, Color.web("#EF4444")));
        btnStop.getStyleClass().add("chaser-control-btn");
        btnStop.setTooltip(new Tooltip("Stop"));
        btnStop.setOnAction(e -> {
            if (currentChaser != null) {
                chaserService.stop(currentChaser.getId());
            }
        });

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        HBox header = new HBox(4, btnBack, lblTitle, spacerHeader, btnPlay, btnPause, btnStop);
        header.setAlignment(Pos.CENTER_LEFT);

        // 2. Tempo & Modus Leiste
        Label lblBpm = new Label("BPM:");
        lblBpm.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        this.spinnerBpm = new Spinner<>(20, 400, 120);
        spinnerBpm.setPrefWidth(65);
        spinnerBpm.setEditable(true);
        spinnerBpm.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (currentChaser != null && newVal != null) {
                currentChaser.setBpm(newVal);
            }
        });

        this.btnTap = new Button("Tap");
        btnTap.getStyleClass().add("chaser-tap-btn");
        btnTap.setOnAction(e -> handleTapTempo());

        this.comboDirection = new ComboBox<>();
        comboDirection.getItems().addAll(ChaserDirection.values());
        comboDirection.setValue(ChaserDirection.FORWARD);
        comboDirection.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (currentChaser != null && newVal != null) {
                currentChaser.setDirection(newVal);
            }
        });

        HBox controlsBar = new HBox(4, lblBpm, spinnerBpm, btnTap, comboDirection);
        controlsBar.setAlignment(Pos.CENTER_LEFT);

        // 3. Schritte-Header & Liste
        Label lblSteps = new Label(I18n.get("chaser.editor.steps") + ":");
        lblSteps.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #E4E4E7;");

        Region spacerSteps = new Region();
        HBox.setHgrow(spacerSteps, Priority.ALWAYS);

        this.btnAddStep = new Button();
        btnAddStep.getStyleClass().add("chaser-add-step-btn");
        btnAddStep.setGraphic(new LucideIcon("plus", 12, StudioTheme.COLOR_ICON_TIMELINE));
        btnAddStep.setText(I18n.get("chaser.editor.add_step"));
        btnAddStep.setOnAction(e -> showAddSceneMenu());

        HBox stepsHeader = new HBox(4, lblSteps, spacerSteps, btnAddStep);
        stepsHeader.setAlignment(Pos.CENTER_LEFT);

        this.stepListContainer = new VBox(4);
        ScrollPane scrollPane = new ScrollPane(stepListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("chaser-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, controlsBar, stepsHeader, scrollPane);

        chaserService.addChaserListener(chaserListener);
    }

    public void setChaser(ChaserDefinition chaser) {
        this.currentChaser = chaser;
        if (chaser == null) {
            lblTitle.setText("Chaser");
            stepListContainer.getChildren().clear();
            return;
        }

        lblTitle.setText(chaser.getName());
        spinnerBpm.getValueFactory().setValue((int) Math.round(chaser.getBpm()));
        comboDirection.setValue(chaser.getDirection());

        refreshStepsList();
        updatePlayState(chaserService.isPlaying(chaser.getId()));
        highlightStep(chaserService.getCurrentStepIndex(chaser.getId()));
    }

    public void refreshStepsList() {
        stepListContainer.getChildren().clear();
        if (currentChaser == null) return;

        List<ChaserStep> steps = currentChaser.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            final int stepIdx = i;
            ChaserStep step = steps.get(i);
            SceneDefinition scene = sceneService.getScene(step.getSceneId());
            String sceneName = scene != null ? scene.getName() : "Szene (" + step.getSceneId() + ")";

            VBox card = new VBox(3);
            card.getStyleClass().add("chaser-step-card");
            card.setPadding(new Insets(5, 6, 5, 6));

            if (stepIdx == currentHighlightedStep) {
                card.setStyle("-fx-background-color: #27272A; -fx-border-color: #38BDF8; -fx-border-width: 1.5px; -fx-border-radius: 4px;");
            } else {
                card.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #333333; -fx-border-width: 1px; -fx-border-radius: 4px;");
            }

            // Zeile 1: # Index, Icon, Name, Verschieben & Löschen
            Label lblNum = new Label("#" + (stepIdx + 1));
            lblNum.setStyle("-fx-font-weight: bold; -fx-text-fill: #9CA3AF; -fx-font-size: 11px;");

            LucideIcon iconBm = new LucideIcon("bookmark", 11, Color.web("#F59E0B"));
            Label lblScene = new Label(sceneName);
            lblScene.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 11px; -fx-font-weight: 500;");
            lblScene.setMaxWidth(90);

            Region spacerCard = new Region();
            HBox.setHgrow(spacerCard, Priority.ALWAYS);

            Button btnUp = new Button();
            btnUp.setGraphic(new LucideIcon("arrow-up", 10, Color.web("#9CA3AF")));
            btnUp.getStyleClass().add("chaser-step-action-btn");
            btnUp.setOnAction(e -> {
                if (stepIdx > 0) {
                    currentChaser.moveStep(stepIdx, stepIdx - 1);
                    refreshStepsList();
                }
            });

            Button btnDown = new Button();
            btnDown.setGraphic(new LucideIcon("arrow-down", 10, Color.web("#9CA3AF")));
            btnDown.getStyleClass().add("chaser-step-action-btn");
            btnDown.setOnAction(e -> {
                if (stepIdx < currentChaser.getStepCount() - 1) {
                    currentChaser.moveStep(stepIdx, stepIdx + 1);
                    refreshStepsList();
                }
            });

            Button btnDel = new Button();
            btnDel.setGraphic(new LucideIcon("trash-2", 10, Color.web("#EF4444")));
            btnDel.getStyleClass().add("chaser-step-action-btn");
            btnDel.setOnAction(e -> {
                currentChaser.removeStep(stepIdx);
                refreshStepsList();
            });

            HBox rowTop = new HBox(3, lblNum, iconBm, lblScene, spacerCard, btnUp, btnDown, btnDel);
            rowTop.setAlignment(Pos.CENTER_LEFT);

            // Zeile 2: Hold & Fade Spinner
            Label lblHold = new Label(I18n.get("chaser.editor.hold") + ":");
            lblHold.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #71717A;");

            Spinner<Integer> spinHold = new Spinner<>(20, 10000, step.getHoldTimeMs(), 50);
            spinHold.setPrefWidth(55);
            spinHold.setEditable(true);
            spinHold.valueProperty().addListener((obs, o, n) -> {
                if (n != null) step.setHoldTimeMs(n);
            });

            Label lblFade = new Label(I18n.get("chaser.editor.fade") + ":");
            lblFade.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #71717A;");

            Spinner<Integer> spinFade = new Spinner<>(0, 5000, step.getFadeTimeMs(), 50);
            spinFade.setPrefWidth(50);
            spinFade.setEditable(true);
            spinFade.valueProperty().addListener((obs, o, n) -> {
                if (n != null) step.setFadeTimeMs(n);
            });

            HBox rowBottom = new HBox(3, lblHold, spinHold, lblFade, spinFade);
            rowBottom.setAlignment(Pos.CENTER_LEFT);

            card.getChildren().addAll(rowTop, rowBottom);
            stepListContainer.getChildren().add(card);
        }
    }

    private void showAddSceneMenu() {
        if (currentChaser == null) return;
        List<SceneDefinition> allScenes = sceneService.getAllScenes();
        if (allScenes.isEmpty()) {
            return;
        }

        ContextMenu menu = new ContextMenu();
        for (SceneDefinition sc : allScenes) {
            MenuItem mi = new MenuItem(sc.getName(), new LucideIcon("bookmark", 12, Color.web("#F59E0B")));
            mi.setOnAction(e -> {
                chaserService.addStep(currentChaser.getId(), sc.getId(), 500, sc.getDefaultFadeTimeMs());
                refreshStepsList();
            });
            menu.getItems().add(mi);
        }
        menu.show(btnAddStep, javafx.geometry.Side.BOTTOM, 0, 2);
    }

    private void handleTapTempo() {
        long now = System.currentTimeMillis();
        if (lastTapTime > 0) {
            long diff = now - lastTapTime;
            if (diff >= 150 && diff <= 3000) {
                double bpm = 60000.0 / diff;
                spinnerBpm.getValueFactory().setValue((int) Math.round(bpm));
            }
        }
        lastTapTime = now;
    }

    private void highlightStep(int stepIndex) {
        this.currentHighlightedStep = stepIndex;
        for (int i = 0; i < stepListContainer.getChildren().size(); i++) {
            if (stepListContainer.getChildren().get(i) instanceof VBox card) {
                if (i == stepIndex) {
                    card.setStyle("-fx-background-color: #27272A; -fx-border-color: #38BDF8; -fx-border-width: 1.5px; -fx-border-radius: 4px;");
                } else {
                    card.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #333333; -fx-border-width: 1px; -fx-border-radius: 4px;");
                }
            }
        }
    }

    private void updatePlayState(boolean isPlaying) {
        if (isPlaying) {
            btnPlay.setStyle("-fx-background-color: #10B981;");
        } else {
            btnPlay.setStyle("");
        }
    }

    public Button getBtnBack() {
        return btnBack;
    }

    public Button getBtnPlay() {
        return btnPlay;
    }

    public Button getBtnStop() {
        return btnStop;
    }

    public Button getBtnAddStep() {
        return btnAddStep;
    }

    public ChaserDefinition getCurrentChaser() {
        return currentChaser;
    }
}
