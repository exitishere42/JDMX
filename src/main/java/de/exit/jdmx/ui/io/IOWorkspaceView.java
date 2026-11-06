package de.exit.jdmx.ui.io;

import de.exit.jdmx.service.HardwareDiscoveryService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Arbeitsbereich für Tab 5 (I/O Patch).
 * <p>
 * Präsentiert die {@link UniversePatchTable} (Universum-Zuordnung im QLC+-Stil)
 * im oberen Drittel des Fensters und hält den unteren Bereich frei für
 * zukünftige DMX-Anzeigen oder Werkzeuge.
 * </p>
 */
public class IOWorkspaceView extends VBox {

    private final UniversePatchTable patchTable;
    private final Region expansionArea;

    /**
     * Erstellt den I/O-Patch Arbeitsbereich mit nativer Hardwareerkennung.
     */
    public IOWorkspaceView() {
        this(new HardwareDiscoveryService());
    }

    /**
     * Erstellt den I/O-Patch Arbeitsbereich mit bereitgestelltem HardwareDiscoveryService.
     *
     * @param hardwareService Hardware-Erkennungsdienst
     */
    public IOWorkspaceView(HardwareDiscoveryService hardwareService) {
        super(16);
        Objects.requireNonNull(hardwareService, "hardwareService darf nicht null sein");

        getStyleClass().add("io-workspace-view");
        // Spürbarer Abstand von der oberen Tableiste und dem linken Fensterrand
        setPadding(new Insets(28, 30, 24, 30));
        setAlignment(Pos.TOP_LEFT);

        // 1. Universum-Zuordnungstabelle im oberen Drittel
        this.patchTable = new UniversePatchTable(hardwareService);
        VBox.setVgrow(patchTable, Priority.NEVER);

        // 2. Unterer Platzhalterbereich für spätere Erweiterungen (z. B. DMX-Monitor)
        this.expansionArea = new Region();
        expansionArea.getStyleClass().add("io-expansion-area");
        VBox.setVgrow(expansionArea, Priority.ALWAYS);

        getChildren().addAll(patchTable, expansionArea);
    }

    public UniversePatchTable getPatchTable() {
        return patchTable;
    }

    public Region getExpansionArea() {
        return expansionArea;
    }
}
