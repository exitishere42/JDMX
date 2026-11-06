package de.exit.jdmx.core.model;

import java.util.Objects;

/**
 * Unveränderliches Datenmodell für die Konfiguration eines DMX-Universums.
 * <p>
 * Beinhaltet Routing-Ziele, Art-Net 4 Netzwerkparameter (IP, Port, Subnet, Universe)
 * sowie Hardware-Timing.
 * </p>
 *
 * @param universeIndex  Null-basierter Index (0 bis 3)
 * @param universeNumber 1-basierte Anzeigennummer (1 bis 4)
 * @param artNetIp       Ziel-IP für Art-Net UDP Pakete
 * @param artNetPort     UDP-Port (Standard 6454)
 * @param artNetNet      Art-Net Net Switch (0 bis 127)
 * @param artNetSubnet   Art-Net Subnet Switch (0 bis 15)
 * @param artNetUniverse Art-Net Universe Switch (0 bis 15)
 * @param isBroadcast    Ob Art-Net im Broadcast-Modus gesendet werden soll
 * @param refreshRateHz  DMX Frame-Rate in Hertz (Standard 40 Hz)
 * @param inputSource    Aktuell gewählte Eingangsquelle
 * @param outputTarget   Aktuell gewähltes Ausgabeziel
 */
public record UniverseConfig(
        int universeIndex,
        int universeNumber,
        String artNetIp,
        int artNetPort,
        int artNetNet,
        int artNetSubnet,
        int artNetUniverse,
        boolean isBroadcast,
        int refreshRateHz,
        String inputSource,
        String outputTarget
) {

    public UniverseConfig {
        Objects.requireNonNull(artNetIp, "artNetIp darf nicht null sein");
        Objects.requireNonNull(inputSource, "inputSource darf nicht null sein");
        Objects.requireNonNull(outputTarget, "outputTarget darf nicht null sein");
    }

    /**
     * Erzeugt eine Standard-Konfiguration für das gegebene Universum.
     *
     * @param index Null-basierter Index (0..3)
     * @return Standard-Konfiguration
     */
    public static UniverseConfig defaultFor(int index) {
        int uniNumber = index + 1;
        String defaultOutput = (index == 0)
                ? "Art-Net (127.0.0.1, Uni 0)"
                : "Kein Output";

        return new UniverseConfig(
                index,
                uniNumber,
                "127.0.0.1",
                6454,
                0,
                0,
                index,
                false,
                40,
                "Kein Input",
                defaultOutput
        );
    }

    public UniverseConfig withArtNetIp(String ip) {
        return new UniverseConfig(universeIndex, universeNumber, ip, artNetPort, artNetNet, artNetSubnet, artNetUniverse, isBroadcast, refreshRateHz, inputSource, outputTarget);
    }

    public UniverseConfig withArtNetPort(int port) {
        return new UniverseConfig(universeIndex, universeNumber, artNetIp, port, artNetNet, artNetSubnet, artNetUniverse, isBroadcast, refreshRateHz, inputSource, outputTarget);
    }

    public UniverseConfig withArtNetSubnet(int subnet) {
        return new UniverseConfig(universeIndex, universeNumber, artNetIp, artNetPort, artNetNet, subnet, artNetUniverse, isBroadcast, refreshRateHz, inputSource, outputTarget);
    }

    public UniverseConfig withArtNetUniverse(int universe) {
        return new UniverseConfig(universeIndex, universeNumber, artNetIp, artNetPort, artNetNet, artNetSubnet, universe, isBroadcast, refreshRateHz, inputSource, outputTarget);
    }

    public UniverseConfig withBroadcast(boolean broadcast) {
        return new UniverseConfig(universeIndex, universeNumber, artNetIp, artNetPort, artNetNet, artNetSubnet, artNetUniverse, broadcast, refreshRateHz, inputSource, outputTarget);
    }

    public UniverseConfig withRefreshRateHz(int hz) {
        return new UniverseConfig(universeIndex, universeNumber, artNetIp, artNetPort, artNetNet, artNetSubnet, artNetUniverse, isBroadcast, hz, inputSource, outputTarget);
    }

    public UniverseConfig withInputSource(String input) {
        return new UniverseConfig(universeIndex, universeNumber, artNetIp, artNetPort, artNetNet, artNetSubnet, artNetUniverse, isBroadcast, refreshRateHz, input, outputTarget);
    }

    public UniverseConfig withOutputTarget(String output) {
        return new UniverseConfig(universeIndex, universeNumber, artNetIp, artNetPort, artNetNet, artNetSubnet, artNetUniverse, isBroadcast, refreshRateHz, inputSource, output);
    }
}
