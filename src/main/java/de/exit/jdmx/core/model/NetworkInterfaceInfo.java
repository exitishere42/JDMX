package de.exit.jdmx.core.model;

import java.util.Objects;

/**
 * Unveränderliches Datenmodell für eine erkannte Netzwerk-Schnittstelle.
 * <p>
 * Dient der Auswahl von Art-Net- und sACN-Netzwerkadaptern im I/O-Routing-Workspace.
 * </p>
 *
 * @param name             Systemname des Netzwerk-Adapters (z. B. "eth0", "wlan0")
 * @param displayName      Benutzerfreundlicher Anzeigename (z. B. "Ethernet", "Wi-Fi")
 * @param ipAddress        Zugeordnete IPv4-Adresse (z. B. "192.168.1.100")
 * @param subnetMask       Subnetzmaske (z. B. "255.255.255.0")
 * @param broadcastAddress Berechnete Broadcast-Adresse (z. B. "192.168.1.255")
 * @param isLoopback       Ob es sich um das lokale Loopback-Interface handelt
 */
public record NetworkInterfaceInfo(
        String name,
        String displayName,
        String ipAddress,
        String subnetMask,
        String broadcastAddress,
        boolean isLoopback
) {

    public NetworkInterfaceInfo {
        Objects.requireNonNull(name, "name darf nicht null sein");
        Objects.requireNonNull(displayName, "displayName darf nicht null sein");
        Objects.requireNonNull(ipAddress, "ipAddress darf nicht null sein");
    }

    /**
     * Liefert eine kompakte Zusammenfassung für Dropdown-Auswahlfelder.
     *
     * @return z. B. "Ethernet - 192.168.1.100"
     */
    public String getSummary() {
        return displayName + " - " + ipAddress;
    }
}
