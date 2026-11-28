package de.exit.jdmx.ui.io;

import de.exit.jdmx.service.HardwareDiscoveryService;
import de.exit.jdmx.ui.MainWindow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Arbeitsbereich für Tab 5 (I/O Routing).
 * <p>
 * Reines I/O-Routing und Hardware-Management im Studio-Dark-Design:
 * <ul>
 *   <li>Oben: {@link UniversePatchTable} für Universen 1–4 mit Input/Output/Merge-Routing.</li>
 *   <li>Unten: {@link InterfaceManagerView} mit den drei Hardware-Karten (Netzwerk, USB-DMX, MIDI).</li>
 * </ul>
 * </p>
 */
public class IOWorkspaceView extends VBox {

    private final UniversePatchTable patchTable;
    private final InterfaceManagerView interfaceManagerView;

    /**
     * Erstellt den I/O-Routing Arbeitsbereich mit nativer Hardwareerkennung.
     */
    public IOWorkspaceView() {
        this(new HardwareDiscoveryService(), null);
    }

    /**
     * Erstellt den I/O-Routing Arbeitsbereich mit bereitgestelltem HardwareDiscoveryService.
     *
     * @param hardwareService Hardware-Erkennungsdienst
     */
    public IOWorkspaceView(HardwareDiscoveryService hardwareService) {
        this(hardwareService, null);
    }

    /**
     * Erstellt den I/O-Routing Arbeitsbereich mit HardwareDiscoveryService und MainWindow.
     *
     * @param hardwareService Hardware-Erkennungsdienst
     * @param mainWindow      Hauptfenster für In-App Overlays
     */
    public IOWorkspaceView(HardwareDiscoveryService hardwareService, MainWindow mainWindow) {
        super(20);
        Objects.requireNonNull(hardwareService, "hardwareService darf nicht null sein");

        getStyleClass().add("io-workspace-view");
        setPadding(new Insets(24, 28, 24, 28));
        setAlignment(Pos.TOP_LEFT);

        // 1. Universum-Zuordnungstabelle im oberen Bereich
        this.patchTable = new UniversePatchTable(hardwareService, mainWindow);
        VBox.setVgrow(patchTable, Priority.NEVER);

        // 2. Hardware- & Schnittstellen-Manager (Netzwerk, USB-DMX, MIDI) im unteren Bereich
        this.interfaceManagerView = new InterfaceManagerView(hardwareService);
        VBox.setVgrow(interfaceManagerView, Priority.NEVER);

        // Verknüpfung: Refresh-Button aktualisiert Tabelle UND Hardware-Karten
        patchTable.getBtnRefresh().setOnAction(e -> {
            patchTable.refreshHardware();
            interfaceManagerView.refreshHardware();
        });

        getChildren().addAll(patchTable, interfaceManagerView);
    }

    /**
     * Verknüpft das Hauptfenster für In-App Modal Dialoge.
     *
     * @param mainWindow Hauptfenster
     */
    public void setMainWindow(MainWindow mainWindow) {
        patchTable.setMainWindow(mainWindow);
    }

    public MainWindow getMainWindow() {
        return patchTable.getMainWindow();
    }

    public UniversePatchTable getPatchTable() {
        return patchTable;
    }

    public InterfaceManagerView getInterfaceManagerView() {
        return interfaceManagerView;
    }

    /**
     * Abwärtskompatibler Getter für den unteren Bereich (liefert den InterfaceManagerView als Region).
     *
     * @return Unterer Inhaltsbereich
     */
    public Region getExpansionArea() {
        return interfaceManagerView;
    }
}
