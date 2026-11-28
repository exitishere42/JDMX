package de.exit.jdmx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.exit.jdmx.Main;
import de.exit.jdmx.core.model.UpdateChannel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Automatischer Update-Dienst für JDMX.
 * <p>
 * Prüft asynchron die neuesten Releases auf GitHub und führt bei Bedarf
 * ein plattformübergreifendes In-App-Update (Windows & Linux) durch.
 * </p>
 */
public class UpdateService {

    private static final Logger LOGGER = Logger.getLogger(UpdateService.class.getName());

    /** Feste Fallback-Version für Entwicklungsumgebungen. */
    public static final String FALLBACK_VERSION = "0.37.0-SNAPSHOT";

    /** Aktuelle Anwendungsversion (dynamisch ermittelt aus version.properties). */
    public static final String CURRENT_VERSION = resolveCurrentVersion();

    /** Build-Zeitstempel (dynamisch ermittelt aus version.properties). */
    public static final String BUILD_TIMESTAMP = resolveBuildTimestamp();

    /**
     * Ermittelt den Build-Zeitstempel aus der gefilterten version.properties-Ressource.
     *
     * @return ISO-8601 Zeitstempel oder leerer String
     */
    private static String resolveBuildTimestamp() {
        try (InputStream is = UpdateService.class.getResourceAsStream("/de/exit/jdmx/version.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                String ts = props.getProperty("build.timestamp");
                if (ts != null && !ts.isBlank() && !ts.startsWith("${")) {
                    return ts.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /**
     * Prüft, ob die aktuell laufende Anwendung ein Entwicklungs-Snapshot ist.
     *
     * @return True, wenn die Version "-SNAPSHOT" enthält
     */
    public static boolean isSnapshot() {
        return CURRENT_VERSION != null && CURRENT_VERSION.toUpperCase().contains("SNAPSHOT");
    }

    /**
     * Ermittelt die aktuelle Versionsnummer der Anwendung aus der gefilterten
     * version.properties-Ressource, dem Package-Manifest oder der Fallback-Konstante.
     *
     * @return Versionsnummer im SemVer-Format (z. B. "1.20.2")
     */
    private static String resolveCurrentVersion() {
        try (InputStream is = UpdateService.class.getResourceAsStream("/de/exit/jdmx/version.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                String ver = props.getProperty("version");
                if (ver != null && !ver.isBlank() && !ver.startsWith("${")) {
                    return ver.trim();
                }
            }
        } catch (Exception ignored) {
        }

        Package pkg = UpdateService.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null && !pkg.getImplementationVersion().isBlank()) {
            return pkg.getImplementationVersion().trim();
        }

        return FALLBACK_VERSION;
    }

    /** GitHub Repository-Bezeichnung. */
    public static final String GITHUB_REPO = "exitishere42/JDMX";

    /** GitHub Releases API Endpunkt für stabile Releases. */
    public static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    /** GitHub Release Tag für kontinuierliche Snapshots. */
    public static final String SNAPSHOT_TAG = "snapshot";

    /** GitHub Releases API Endpunkt für kontinuierliche Snapshots. */
    public static final String SNAPSHOT_API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/tags/" + SNAPSHOT_TAG;

    /** Web-URL des Repositories. */
    public static final String REPO_URL = "https://github.com/" + GITHUB_REPO;

    /**
     * Datenmodell für Versions- und Release-Informationen von GitHub.
     */
    public record ReleaseInfo(
            String tagName,
            String name,
            String body,
            String htmlUrl,
            boolean isNewer,
            String jarDownloadUrl,
            String jarFileName,
            long jarSize,
            String appImageDownloadUrl,
            String appImageFileName,
            long appImageSize,
            String publishedAt,
            UpdateChannel channel
    ) {
        public ReleaseInfo(String tagName, String name, String body, String htmlUrl, boolean isNewer,
                           String jarDownloadUrl, String jarFileName, long jarSize,
                           String appImageDownloadUrl, String appImageFileName, long appImageSize) {
            this(tagName, name, body, htmlUrl, isNewer, jarDownloadUrl, jarFileName, jarSize,
                 appImageDownloadUrl, appImageFileName, appImageSize, "", UpdateChannel.RELEASE);
        }
    }

    /**
     * Prüft asynchron das neueste Release auf GitHub anhand des Standard-Kanals.
     *
     * @return CompletableFuture mit den Release-Informationen
     */
    public static CompletableFuture<ReleaseInfo> checkLatestRelease() {
        return checkLatestRelease(isSnapshot() ? UpdateChannel.SNAPSHOT : UpdateChannel.RELEASE);
    }

    /**
     * Prüft asynchron ein Release oder Snapshot auf GitHub für den angegebenen Kanal.
     *
     * @param channel Der abzufragende Update-Kanal (RELEASE oder SNAPSHOT)
     * @return CompletableFuture mit den Release-Informationen
     */
    public static CompletableFuture<ReleaseInfo> checkLatestRelease(UpdateChannel channel) {
        final UpdateChannel targetChannel = channel != null ? channel : UpdateChannel.RELEASE;
        final String endpoint = targetChannel == UpdateChannel.SNAPSHOT ? SNAPSHOT_API_URL : API_URL;

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(6))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "jdmx-updater/" + CURRENT_VERSION)
                        .header("Accept", "application/vnd.github.v3+json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("GitHub API Fehler (" + targetChannel + "): HTTP " + response.statusCode());
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());

                String tagName = root.path("tag_name").asText("");
                String name = root.path("name").asText(tagName);
                String body = root.path("body").asText("");
                String htmlUrl = root.path("html_url").asText(REPO_URL);
                String publishedAt = root.path("published_at").asText("");

                String jarUrl = null;
                String jarName = null;
                long jarSize = 0;

                String appImageUrl = null;
                String appImageName = null;
                long appImageSize = 0;

                JsonNode assets = root.path("assets");
                if (assets.isArray()) {
                    for (JsonNode asset : assets) {
                        String aName = asset.path("name").asText("");
                        String aUrl = asset.path("browser_download_url").asText("");
                        long aSize = asset.path("size").asLong(0);

                        if (aName.endsWith("-all.jar") || (aName.endsWith(".jar") && !aName.contains("javadoc") && !aName.contains("sources"))) {
                            jarUrl = aUrl;
                            jarName = aName;
                            jarSize = aSize;
                        } else if (aName.endsWith(".AppImage")) {
                            appImageUrl = aUrl;
                            appImageName = aName;
                            appImageSize = aSize;
                        }
                    }
                }

                boolean isNewer;
                if (targetChannel == UpdateChannel.SNAPSHOT) {
                    isNewer = isSnapshotNewer(publishedAt, BUILD_TIMESTAMP, CURRENT_VERSION);
                } else {
                    isNewer = isReleaseNewer(tagName, CURRENT_VERSION);
                }

                return new ReleaseInfo(
                        tagName,
                        name,
                        body,
                        htmlUrl,
                        isNewer,
                        jarUrl,
                        jarName,
                        jarSize,
                        appImageUrl,
                        appImageName,
                        appImageSize,
                        publishedAt,
                        targetChannel
                );
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Konnte GitHub Releases für Kanal " + targetChannel + " nicht abrufen: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Prüft, ob ein Remote-Snapshot neuer ist als die installierte Version.
     *
     * @param publishedAt    Veröffentlichungszeitpunkt auf GitHub (ISO-8601)
     * @param localTimestamp Lokaler Build-Zeitstempel (ISO-8601)
     * @param currentVersion Aktuell installierte Version
     * @return True, wenn ein Wechsel auf den Snapshot empfohlen wird
     */
    public static boolean isSnapshotNewer(String publishedAt, String localTimestamp, String currentVersion) {
        if (currentVersion != null && !currentVersion.toUpperCase().contains("SNAPSHOT")) {
            // Wer ein stabiles Release installiert hat und den Snapshot-Kanal prüft,
            // soll den Snapshot immer als Update/Wechsel installieren können.
            return true;
        }

        if (publishedAt == null || publishedAt.isBlank() || localTimestamp == null || localTimestamp.isBlank()) {
            return false;
        }

        try {
            Instant remoteTime = Instant.parse(publishedAt);
            Instant localTime = Instant.parse(localTimestamp);
            long secondsDiff = Duration.between(localTime, remoteTime).toSeconds();
            return secondsDiff > 300;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Prüft, ob ein stabiles Release für die installierte Version ein Update oder ein sinnvoller Wechsel ist.
     *
     * @param latestTag      Neuestes Tag auf GitHub (z. B. "v0.35.0")
     * @param currentVersion Aktuell installierte Version
     * @return True, wenn das Release neuer ist oder ein Wechsel vom Snapshot auf ein stabiles Release möglich ist
     */
    public static boolean isReleaseNewer(String latestTag, String currentVersion) {
        if (latestTag == null || latestTag.isBlank()) {
            return false;
        }
        if (currentVersion != null && currentVersion.toUpperCase().contains("SNAPSHOT")) {
            // Wenn man einen Snapshot nutzt und im Release-Kanal nachsieht,
            // ist die stabile Version immer als Rollback-Wechsel verfügbar!
            return true;
        }
        return isVersionNewer(latestTag, currentVersion);
    }

    /**
     * Vergleicht zwei Versionsnummern nach SemVer (z. B. "v1.2.1" vs "1.2.0").
     *
     * @param latestTag      Neuestes Tag auf GitHub
     * @param currentVersion Aktuell installierte Version
     * @return True, wenn das neueste Tag neuer ist als die installierte Version
     */
    public static boolean isVersionNewer(String latestTag, String currentVersion) {
        if (latestTag == null || currentVersion == null) {
            return false;
        }
        String cleanLatest = latestTag.trim().replaceAll("^[^0-9]+", "");
        String cleanCurrent = currentVersion.trim().replaceAll("^[^0-9]+", "");
        if (cleanLatest.isEmpty() || cleanCurrent.isEmpty()) {
            return false;
        }

        String[] latParts = cleanLatest.split("[.\\-_]");
        String[] curParts = cleanCurrent.split("[.\\-_]");
        int len = Math.max(latParts.length, curParts.length);
        for (int i = 0; i < len; i++) {
            int l = i < latParts.length ? parsePart(latParts[i]) : 0;
            int c = i < curParts.length ? parsePart(curParts[i]) : 0;
            if (l > c) {
                return true;
            }
            if (l < c) {
                return false;
            }
        }
        return false;
    }

    private static int parsePart(String s) {
        try {
            String digits = s.replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Lädt das Update herunter und startet die Anwendung nahtlos neu.
     *
     * @param release          Release-Informationen des neuesten Updates
     * @param progressCallback Fortschrittsanzeige (0.0 bis 1.0)
     * @param statusCallback   Statustext-Meldungen
     * @return CompletableFuture des Update-Vorgangs
     */
    public static CompletableFuture<Void> performUpdate(
            ReleaseInfo release,
            Consumer<Double> progressCallback,
            Consumer<String> statusCallback
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                File codeSource;
                try {
                    codeSource = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                } catch (Exception e) {
                    codeSource = new File(".");
                }

                File installDir = codeSource.isDirectory() ? codeSource : codeSource.getParentFile();
                if (installDir == null) {
                    installDir = new File(".");
                }

                String appImagePath = System.getenv("APPIMAGE");
                boolean isAppImage = appImagePath != null && new File(appImagePath).exists() && release.appImageDownloadUrl() != null;
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

                if (isAppImage) {
                    // Update als Linux AppImage
                    statusCallback.accept("Lade neues AppImage herunter...");
                    File targetAppImage = new File(appImagePath);
                    File tempFile = new File(targetAppImage.getParentFile(), targetAppImage.getName() + ".download");

                    downloadFile(release.appImageDownloadUrl(), tempFile, release.appImageSize(), progressCallback, statusCallback);

                    tempFile.setExecutable(true, false);
                    statusCallback.accept("Ersetze AppImage...");
                    Files.move(tempFile.toPath(), targetAppImage.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    statusCallback.accept("Starte neue Version...");
                    new ProcessBuilder(targetAppImage.getAbsolutePath()).start();
                    System.exit(0);
                    return;
                }

                // Update als Fat-JAR
                if (release.jarDownloadUrl() == null) {
                    throw new IllegalStateException("Kein passendes Release-JAR in den GitHub Assets gefunden.");
                }

                statusCallback.accept("Lade Programmdatei herunter...");
                File tempJar = new File(installDir, "jdmx-update.jar");
                downloadFile(release.jarDownloadUrl(), tempJar, release.jarSize(), progressCallback, statusCallback);

                String newJarName = release.jarFileName() != null ? release.jarFileName() : "jdmx-" + release.tagName() + "-all.jar";
                File targetJar = new File(installDir, newJarName);

                if (!isWindows) {
                    // Linux / macOS: Direktes Ersetzen möglich
                    statusCallback.accept("Wende Update an...");
                    Files.copy(tempJar.toPath(), new File(installDir, "jdmx.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(tempJar.toPath(), new File(installDir, "jdmx-snapshot-all.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tempJar.toPath(), targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    File runSh = new File(installDir, "run.sh");
                    if (runSh.exists()) {
                        String runScriptContent = "#!/usr/bin/env bash\n" +
                                "SCRIPT_DIR=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")\" && pwd)\"\n" +
                                "cd \"$SCRIPT_DIR\"\n" +
                                "exec java -jar \"$SCRIPT_DIR/jdmx.jar\" \"$@\"\n";
                        Files.writeString(runSh.toPath(), runScriptContent);
                        runSh.setExecutable(true);
                        statusCallback.accept("Starte jdmx neu...");
                        new ProcessBuilder("bash", runSh.getAbsolutePath()).directory(installDir).start();
                    } else {
                        statusCallback.accept("Starte jdmx neu...");
                        new ProcessBuilder("java", "-jar", targetJar.getAbsolutePath()).directory(installDir).start();
                    }
                    System.exit(0);
                } else {
                    // Windows: Bypassen der Dateisperre über update-runner.bat
                    statusCallback.accept("Bereite Neustart vor...");
                    String runningJar = (codeSource != null && codeSource.isFile()) ? codeSource.getName() : "jdmx.jar";
                    File updateBat = new File(installDir, "update-runner.bat");
                    String batContent = "@echo off\r\n" +
                            "chcp 65001 >nul 2>&1\r\n" +
                            "timeout /t 1 /nobreak >nul\r\n" +
                            ":wait\r\n" +
                            "del /f /q \"%~dp0" + runningJar + "\" >nul 2>&1\r\n" +
                            "if exist \"%~dp0" + runningJar + "\" (\r\n" +
                            "    timeout /t 1 /nobreak >nul\r\n" +
                            "    goto wait\r\n" +
                            ")\r\n" +
                            "del /f /q \"%~dp0jdmx.jar\" >nul 2>&1\r\n" +
                            "del /f /q \"%~dp0jdmx-*-all.jar\" >nul 2>&1\r\n" +
                            "copy /y \"%~dp0jdmx-update.jar\" \"%~dp0jdmx.jar\" >nul 2>&1\r\n" +
                            "copy /y \"%~dp0jdmx-update.jar\" \"%~dp0jdmx-snapshot-all.jar\" >nul 2>&1\r\n" +
                            "move /y \"%~dp0jdmx-update.jar\" \"%~dp0" + targetJar.getName() + "\" >nul 2>&1\r\n" +
                            "if exist \"%~dp0run.bat\" (\r\n" +
                            "    start \"\" \"%~dp0run.bat\"\r\n" +
                            ") else (\r\n" +
                            "    start \"\" javaw -jar \"%~dp0jdmx.jar\"\r\n" +
                            ")\r\n" +
                            "del \"%~f0\"\r\n";

                    Files.writeString(updateBat.toPath(), batContent);
                    statusCallback.accept("Starte jdmx neu...");
                    new ProcessBuilder("cmd.exe", "/c", "start", "/b", updateBat.getAbsolutePath())
                            .directory(installDir)
                            .start();
                    System.exit(0);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fehler beim Ausführen des Updates: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private static void downloadFile(
            String downloadUrl,
            File destination,
            long expectedSize,
            Consumer<Double> progressCallback,
            Consumer<String> statusCallback
    ) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "jdmx-updater/" + CURRENT_VERSION)
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Download fehlgeschlagen: HTTP " + response.statusCode());
        }

        long totalBytes = expectedSize;
        if (totalBytes <= 0) {
            totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(0);
        }

        try (InputStream in = response.body();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            long bytesReadTotal = 0;
            int bytesRead;

            long lastProgressUpdate = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesReadTotal += bytesRead;

                long now = System.currentTimeMillis();
                if (now - lastProgressUpdate > 100 || bytesReadTotal == totalBytes) {
                    lastProgressUpdate = now;
                    if (totalBytes > 0) {
                        double progress = (double) bytesReadTotal / totalBytes;
                        progressCallback.accept(progress);
                        double mbRead = bytesReadTotal / (1024.0 * 1024.0);
                        double mbTotal = totalBytes / (1024.0 * 1024.0);
                        statusCallback.accept(String.format("Lade Update herunter... (%.1f / %.1f MB - %.0f%%)", mbRead, mbTotal, progress * 100));
                    } else {
                        progressCallback.accept(-1.0);
                        double mbRead = bytesReadTotal / (1024.0 * 1024.0);
                        statusCallback.accept(String.format("Lade Update herunter... (%.1f MB)", mbRead));
                    }
                }
            }
        }
    }
}
