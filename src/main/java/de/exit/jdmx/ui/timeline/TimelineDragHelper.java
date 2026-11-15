package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.service.TimelineService;
import de.exit.jdmx.ui.stage.ProjectExplorerView;
import javafx.scene.input.Dragboard;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Zentraler Hilfsdienst für die robuste Extraktion von Audiodatei-Referenzen aus
 * Drag & Drop Vorgängen unter Windows und Linux (GNOME/Nautilus, X11, Wayland).
 * <p>
 * Löst Dateilisten, Linux-URI-Listen ({@code file://...}), direkte Dateipfade,
 * Explorer-Item-IDs und Songnamen zuverlässig in existierende {@link File}-Objekte auf.
 * </p>
 */
public final class TimelineDragHelper {

    private TimelineDragHelper() {
        // Hilfsklasse
    }

    /**
     * Prüft, ob eine gegebene Datei ein unterstütztes Audioformat (MP3 oder WAV) besitzt.
     *
     * @param file die zu prüfende Datei
     * @return {@code true}, wenn es sich um eine MP3- oder WAV-Audiodatei handelt
     */
    public static boolean isSupportedAudioFile(File file) {
        if (file == null) return false;
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp3") || name.endsWith(".wav");
    }

    /**
     * Extrahiert die erste gültige Audiodatei aus einem JavaFX {@link Dragboard}.
     *
     * @param db      das Dragboard (darf {@code null} sein)
     * @param service der {@link TimelineService} zur Namens- und ID-Auflösung (darf {@code null} sein)
     * @return {@link Optional} mit der gefundenen Datei oder leer
     */
    public static Optional<File> extractAudioFile(Dragboard db, TimelineService service) {
        List<File> files = extractAudioFiles(db, service);
        return files.isEmpty() ? Optional.empty() : Optional.of(files.get(0));
    }

    /**
     * Extrahiert alle gültigen Audiodateien aus einem JavaFX {@link Dragboard}.
     *
     * @param db      das Dragboard (darf {@code null} sein)
     * @param service der {@link TimelineService} zur Namens- und ID-Auflösung (darf {@code null} sein)
     * @return unveränderliche Liste aller gefundenen Audiodateien
     */
    public static List<File> extractAudioFiles(Dragboard db, TimelineService service) {
        if (db == null) {
            return Collections.emptyList();
        }

        List<File> result = new ArrayList<>();

        // 1. Native Dateiliste (Windows Explorer / native Drag-Drop)
        if (db.hasFiles() && db.getFiles() != null) {
            for (File file : db.getFiles()) {
                if (file != null && file.exists() && isSupportedAudioFile(file)) {
                    result.add(file);
                }
            }
            if (!result.isEmpty()) {
                return Collections.unmodifiableList(result);
            }
        }

        // 2. Direkte URL (z. B. file://...)
        if (db.hasUrl() && db.getUrl() != null) {
            File parsedUrl = parseFileUri(db.getUrl());
            if (parsedUrl != null && parsedUrl.exists() && isSupportedAudioFile(parsedUrl)) {
                result.add(parsedUrl);
                return Collections.unmodifiableList(result);
            }
        }

        // 3. String-Inhalt (Linux GNOME/Nautilus text/uri-list, absolute Pfade oder Songnamen)
        if (db.hasString() && db.getString() != null) {
            String raw = db.getString().trim();
            List<File> fromString = parseStringContent(raw, service);
            if (!fromString.isEmpty()) {
                return Collections.unmodifiableList(fromString);
            }
        }

        // 4. Benutzerdefiniertes Format des Project-Explorers
        if (db.hasContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT)) {
            Object content = db.getContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT);
            if (content instanceof String strId) {
                List<File> fromId = parseStringContent(strId, service);
                if (!fromId.isEmpty()) {
                    return Collections.unmodifiableList(fromId);
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Parst einen beliebigen String (z. B. URI-Liste, Dateipfad oder Song-ID) zu Dateien.
     *
     * @param raw     der Eingabestring
     * @param service der Timeline-Service
     * @return gefundene Dateien
     */
    public static List<File> parseStringContent(String raw, TimelineService service) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        List<File> list = new ArrayList<>();

        // Mehrzeilige URI-Liste (typisch für Linux GTK / Nautilus)
        String[] lines = raw.split("[\r\n]+");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // file:// URI auflösen
            if (trimmed.startsWith("file://") || trimmed.startsWith("file:/")) {
                File f = parseFileUri(trimmed);
                if (f != null && f.exists() && isSupportedAudioFile(f)) {
                    list.add(f);
                    continue;
                }
            }

            // Direkter Dateipfad
            File directFile = new File(trimmed);
            if (directFile.exists() && isSupportedAudioFile(directFile)) {
                list.add(directFile);
                continue;
            }

            // Auflösung über den TimelineService
            if (service != null) {
                Optional<File> songOpt = service.findSongFileByName(trimmed);
                if (songOpt.isPresent()) {
                    list.add(songOpt.get());
                    continue;
                }

                // ID ohne Präfix "song-" versuchen
                if (trimmed.startsWith("song-")) {
                    String cleanName = trimmed.substring(5);
                    Optional<File> cleanOpt = service.findSongFileByName(cleanName);
                    cleanOpt.ifPresent(list::add);
                }
            }
        }

        return list;
    }

    /**
     * Konvertiert einen {@code file://}-URI-String in ein {@link File}-Objekt unter Berücksichtigung von URL-Decoding.
     *
     * @param uriStr der URI-String
     * @return das aufgelöste {@link File} oder {@code null}
     */
    public static File parseFileUri(String uriStr) {
        if (uriStr == null || uriStr.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(uriStr.trim());
            return new File(uri);
        } catch (Exception ignored) {
            // Fallback: Manuelles URL-Decoding
            try {
                String clean = uriStr.trim();
                if (clean.startsWith("file://")) {
                    clean = clean.substring(7);
                } else if (clean.startsWith("file:/")) {
                    clean = clean.substring(5);
                }
                String decoded = URLDecoder.decode(clean, StandardCharsets.UTF_8);
                return new File(decoded);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
