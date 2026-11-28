package de.exit.jdmx.ui.io;

import de.exit.jdmx.service.HardwareDiscoveryService;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.Objects;

/**
 * Schnittstellen- und Hardware-Manager im unteren Bereich des I/O-Routing Workspace.
 * <p>
 * Präsentiert drei modulare Studio-Karten nebeneinander:
 * <ul>
 *   <li>{@link NetworkRoutingCard}: Art-Net & Netzwerk-Schnittstellen</li>
 *   <li>{@link UsbDmxRoutingCard}: Physische USB-DMX Interfaces und Hardware-Test</li>
 *   <li>{@link MidiRoutingCard}: MIDI-Controller und Live-Signal-Monitor</li>
 * </ul>
 * </p>
 */
public class InterfaceManagerView extends HBox {

    private final NetworkRoutingCard networkCard;
    private final UsbDmxRoutingCard usbDmxCard;
    private final MidiRoutingCard midiCard;

    public InterfaceManagerView(HardwareDiscoveryService hardwareService) {
        super(16);
        Objects.requireNonNull(hardwareService, "hardwareService darf nicht null sein");

        getStyleClass().add("interface-manager-view");
        setFillHeight(true);

        this.networkCard = new NetworkRoutingCard(hardwareService);
        this.usbDmxCard = new UsbDmxRoutingCard(hardwareService);
        this.midiCard = new MidiRoutingCard(hardwareService);

        HBox.setHgrow(networkCard, Priority.ALWAYS);
        HBox.setHgrow(usbDmxCard, Priority.ALWAYS);
        HBox.setHgrow(midiCard, Priority.ALWAYS);

        getChildren().addAll(networkCard, usbDmxCard, midiCard);
    }

    /**
     * Aktualisiert die Hardware-Erkennung in allen drei Modulkarten.
     */
    public void refreshHardware() {
        networkCard.refreshHardware();
        usbDmxCard.refreshHardware();
        midiCard.refreshHardware();
    }

    public NetworkRoutingCard getNetworkCard() {
        return networkCard;
    }

    public UsbDmxRoutingCard getUsbDmxCard() {
        return usbDmxCard;
    }

    public MidiRoutingCard getMidiCard() {
        return midiCard;
    }
}
