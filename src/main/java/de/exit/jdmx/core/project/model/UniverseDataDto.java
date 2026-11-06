package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO zur persistenten Speicherung der Universums- und Art-Net-Konfiguration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UniverseDataDto(
        int universeIndex,
        String inputSource,
        String outputTarget,
        String artNetIp,
        int artNetUniverse,
        int artNetSubnet,
        int artNetNet
) {
    public static UniverseDataDto of(int index, String in, String out, String ip, int uni, int subnet, int net) {
        return new UniverseDataDto(index, in != null ? in : "Kein Input", out != null ? out : "Kein Output", ip != null ? ip : "127.0.0.1", uni, subnet, net);
    }
}
