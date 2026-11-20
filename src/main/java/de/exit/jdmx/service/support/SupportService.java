package de.exit.jdmx.service.support;

import de.exit.jdmx.core.log.AppLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service für das Support-, Feedback- und Bug-Reporting-Subsystem.
 * <p>
 * Übermittelt Benutzernachrichten asynchron als formatierte Embeds an einen
 * vorkonfigurierten Discord-Webhook. Verwaltet zudem die lokale Chat-Historie.
 * </p>
 */
public class SupportService {

    private static final Logger LOGGER = Logger.getLogger(SupportService.class.getName());

    /** Fallback- bzw. Standard-Webhook URL für das JDMX Entwicklerteam. */
    public static final String DEFAULT_WEBHOOK_URL = System.getProperty(
            "jdmx.discord.webhook",
            System.getenv().getOrDefault("JDMX_DISCORD_WEBHOOK", "")
    );

    /** Farbe für Feedback-Embeds: Sky-Blue (#38BDF8 -> 3719160). */
    public static final int COLOR_FEEDBACK = 3719160;

    /** Farbe für Bug-Embeds: Vivid Red (#EF4244 -> 15680068). */
    public static final int COLOR_BUG = 15680068;

    private final HttpClient httpClient;
    private final Map<SupportCategory, List<SupportMessage>> historyByCategory = new ConcurrentHashMap<>();
    private volatile String webhookUrl;
    private volatile String botToken = System.getProperty("jdmx.discord.bot_token", System.getenv().getOrDefault("JDMX_DISCORD_BOT_TOKEN", ""));
    private volatile String botRecipientUserId = System.getProperty("jdmx.discord.user_id", System.getenv().getOrDefault("JDMX_DISCORD_USER_ID", ""));

    /**
     * Erstellt den Service mit Standard-HTTP-Client und Standard-Webhook-URL.
     */
    public SupportService() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    /**
     * Erstellt den Service mit übergebenem {@link HttpClient} für Dependency-Injection und Unit-Tests.
     *
     * @param httpClient Zu verwendender HTTP-Client
     */
    public SupportService(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient darf nicht null sein");
        this.webhookUrl = DEFAULT_WEBHOOK_URL;
        historyByCategory.put(SupportCategory.FEEDBACK, new CopyOnWriteArrayList<>());
        historyByCategory.put(SupportCategory.BUG, new CopyOnWriteArrayList<>());
    }

    /**
     * Erfasst aktuelle Systemdaten der laufenden Instanz.
     *
     * @param dmxRate    Aktuelle DMX-Ausgaberate in Paketen/Sekunde
     * @param recentLogs Letzte Zeilen des Logs
     * @return Gesammelte Systemdaten
     */
    public SupportSystemInfo collectSystemInfo(int dmxRate, String recentLogs) {
        return SupportSystemInfo.collectCurrent(dmxRate, recentLogs);
    }

    /**
     * Setzt die aktive Discord-Webhook-Zieladresse.
     *
     * @param webhookUrl Ziel-URL des Discord-Webhooks
     */
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl != null ? webhookUrl.trim() : "";
    }

    /**
     * Liefert die aktuell gesetzte Webhook-URL zurück.
     *
     * @return Aktuelle Webhook-URL
     */
    public String getWebhookUrl() {
        return webhookUrl;
    }

    /**
     * Konfiguriert den Bot-Token und die Empfänger-Benutzer-ID für direkte DMs.
     *
     * @param botToken        Discord-Bot-Token
     * @param recipientUserId Discord-User-ID des Empfängers
     */
    public void setBotConfig(String botToken, String recipientUserId) {
        this.botToken = botToken != null ? botToken.trim() : "";
        this.botRecipientUserId = recipientUserId != null ? recipientUserId.trim() : "";
    }

    /**
     * Prüft, ob ein Discord-Bot für den Direktnachrichten-Versand konfiguriert ist.
     *
     * @return true, falls Bot-Token und User-ID gesetzt sind
     */
    public boolean isBotConfigured() {
        return botToken != null && !botToken.isBlank() && botRecipientUserId != null && !botRecipientUserId.isBlank();
    }

    /**
     * Prüft, ob ein Bot-Token hinterlegt ist.
     *
     * @return true, falls Bot-Token gesetzt ist
     */
    public boolean isBotTokenSet() {
        return botToken != null && !botToken.isBlank();
    }

    /**
     * Ermittelt die Ziel-Benutzer-ID aus Konfiguration oder Kontaktfeld.
     *
     * @param message Die zu sendende Nachricht
     * @return Benutzer-ID oder null
     */
    public String resolveRecipientUserId(SupportMessage message) {
        if (botRecipientUserId != null && !botRecipientUserId.isBlank()) {
            return botRecipientUserId.trim();
        }
        if (message != null && message.senderContact() != null) {
            String contact = message.senderContact().trim();
            if (contact.matches("\\d{17,20}")) {
                return contact;
            }
        }
        return null;
    }

    public String getBotToken() {
        return botToken != null ? botToken : "";
    }

    public String getBotRecipientUserId() {
        return botRecipientUserId != null ? botRecipientUserId : "";
    }

    /**
     * Sendet eine Support-Nachricht asynchron an Discord (entweder als Bot-DM oder Webhook).
     *
     * @param message Die zu übermittelnde Nachricht
     * @return {@link CompletableFuture} mit {@code true} bei Erfolg, sonst {@code false}
     */
    public CompletableFuture<Boolean> sendMessage(SupportMessage message) {
        Objects.requireNonNull(message, "Nachricht darf nicht null sein");

        // Wenn Bot konfiguriert ist, bevorzugt echte Direktnachricht per Bot an den Nutzer senden
        if (isBotConfigured() || (isBotTokenSet() && resolveRecipientUserId(message) != null)) {
            return sendBotDirectMessage(message);
        }

        if (webhookUrl == null || webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
            AppLogger.warn("[SUPPORT] Weder Discord Bot noch gültiger Webhook konfiguriert. Nachricht kann nicht gesendet werden.");
            message.setStatus(SupportMessage.MessageStatus.FAILED);
            return CompletableFuture.completedFuture(false);
        }

        message.setStatus(SupportMessage.MessageStatus.SENDING);
        String jsonPayload = buildDiscordJson(message);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "JDMX-Desktop-Support/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Erstellen des HTTP-Requests", e);
            message.setStatus(SupportMessage.MessageStatus.FAILED);
            return CompletableFuture.completedFuture(false);
        }

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int code = response.statusCode();
                    // Discord Webhook gibt 204 No Content (oder 200 OK) bei erfolgreichem Versand zurück
                    if (code >= 200 && code < 300) {
                        message.setStatus(SupportMessage.MessageStatus.SENT);
                        addMessageToHistory(message);
                        AppLogger.info("[SUPPORT] Nachricht '" + message.id() + "' (" + message.category() + ") erfolgreich übermittelt.");
                        return true;
                    } else {
                        AppLogger.warn("[SUPPORT] Webhook antwortete mit HTTP-Status " + code + ": " + response.body());
                        message.setStatus(SupportMessage.MessageStatus.FAILED);
                        return false;
                    }
                })
                .exceptionally(ex -> {
                    AppLogger.warn("[SUPPORT] Verbindungsfehler beim Senden der Nachricht: " + ex.getMessage());
                    message.setStatus(SupportMessage.MessageStatus.FAILED);
                    return false;
                });
    }

    private CompletableFuture<Boolean> sendBotDirectMessage(SupportMessage message) {
        String recipientId = resolveRecipientUserId(message);
        if (recipientId == null || recipientId.isBlank()) {
            AppLogger.warn("[SUPPORT-BOT] Kein Discord-Empfänger konfiguriert (weder in Einstellungen noch als gültige User-ID im Kontaktfeld).");
            message.setStatus(SupportMessage.MessageStatus.FAILED);
            return CompletableFuture.completedFuture(false);
        }

        message.setStatus(SupportMessage.MessageStatus.SENDING);

        // 1. DM-Kanal öffnen: POST https://discord.com/api/v10/users/@me/channels
        HttpRequest openDmReq;
        try {
            openDmReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://discord.com/api/v10/users/@me/channels"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bot " + botToken)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "DiscordBot (https://github.com/exitishere42/jdmx, 1.0.0)")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"recipient_id\":\"" + recipientId + "\"}"))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Erstellen der DM-Kanal-Anfrage", e);
            message.setStatus(SupportMessage.MessageStatus.FAILED);
            return CompletableFuture.completedFuture(false);
        }

        return httpClient.sendAsync(openDmReq, HttpResponse.BodyHandlers.ofString())
                .thenCompose(openDmRes -> {
                    if (openDmRes.statusCode() < 200 || openDmRes.statusCode() >= 300) {
                        AppLogger.warn("[SUPPORT-BOT] Konnte DM-Kanal nicht öffnen (Status " + openDmRes.statusCode() + "): " + openDmRes.body());
                        message.setStatus(SupportMessage.MessageStatus.FAILED);
                        return CompletableFuture.completedFuture(false);
                    }

                    String channelId = extractIdFromJson(openDmRes.body());
                    if (channelId == null || channelId.isBlank()) {
                        AppLogger.warn("[SUPPORT-BOT] DM-Kanal-ID konnte nicht aus Antwort geparst werden: " + openDmRes.body());
                        message.setStatus(SupportMessage.MessageStatus.FAILED);
                        return CompletableFuture.completedFuture(false);
                    }

                    // 2. Nachricht in DM-Kanal senden: POST https://discord.com/api/v10/channels/{channel_id}/messages
                    String jsonPayload = buildDiscordJson(message);
                    HttpRequest sendMsgReq;
                    try {
                        sendMsgReq = HttpRequest.newBuilder()
                                .uri(URI.create("https://discord.com/api/v10/channels/" + channelId + "/messages"))
                                .timeout(Duration.ofSeconds(15))
                                .header("Authorization", "Bot " + botToken)
                                .header("Content-Type", "application/json")
                                .header("User-Agent", "DiscordBot (https://github.com/exitishere42/jdmx, 1.0.0)")
                                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                                .build();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Fehler beim Erstellen der Sende-Anfrage", e);
                        message.setStatus(SupportMessage.MessageStatus.FAILED);
                        return CompletableFuture.completedFuture(false);
                    }

                    return httpClient.sendAsync(sendMsgReq, HttpResponse.BodyHandlers.ofString())
                            .thenApply(msgRes -> {
                                if (msgRes.statusCode() >= 200 && msgRes.statusCode() < 300) {
                                    message.setStatus(SupportMessage.MessageStatus.SENT);
                                    addMessageToHistory(message);
                                    AppLogger.info("[SUPPORT-BOT] DM erfolgreich an Benutzer '" + recipientId + "' gesendet.");
                                    return true;
                                } else {
                                    AppLogger.warn("[SUPPORT-BOT] Nachricht senden fehlgeschlagen (Status " + msgRes.statusCode() + "): " + msgRes.body());
                                    message.setStatus(SupportMessage.MessageStatus.FAILED);
                                    return false;
                                }
                            });
                })
                .exceptionally(ex -> {
                    AppLogger.warn("[SUPPORT-BOT] Fehler beim Senden der Direktnachricht: " + ex.getMessage());
                    message.setStatus(SupportMessage.MessageStatus.FAILED);
                    return false;
                });
    }

    private String extractIdFromJson(String json) {
        if (json == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * Baut ein nach Discord Webhook API Spezifikation formatiertes JSON-Objekt mit Embeds.
     *
     * @param message Zu formatierende Nachricht
     * @return Gültiger JSON-Payload-String
     */
    public String buildDiscordJson(SupportMessage message) {
        int color = (message.category() == SupportCategory.BUG) ? COLOR_BUG : COLOR_FEEDBACK;
        String titlePrefix = (message.category() == SupportCategory.BUG) ? "Bug-Report" : "Feedback";
        String title = titlePrefix + " von " + message.senderName();

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"username\":\"JDMX Support Bot\",");
        sb.append("\"avatar_url\":\"https://raw.githubusercontent.com/exitishere42/jdmx/main/jdmx.png\",");
        sb.append("\"embeds\":[{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(message.messageText())).append("\",");
        sb.append("\"color\":").append(color).append(",");

        // Fields
        sb.append("\"fields\":[");
        sb.append("{\"name\":\"Absender\",\"value\":\"").append(escapeJson(message.senderName())).append("\",\"inline\":true},");
        sb.append("{\"name\":\"Kontakt\",\"value\":\"").append(escapeJson(message.senderContact())).append("\",\"inline\":true}");

        if (message.systemInfo() != null) {
            SupportSystemInfo info = message.systemInfo();
            sb.append(",{\"name\":\"System\",\"value\":\"")
                    .append(escapeJson(info.osName() + " (" + info.osArch() + ") • Java " + info.javaVersion()))
                    .append("\",\"inline\":false}");
            sb.append(",{\"name\":\"Version & DMX\",\"value\":\"")
                    .append(escapeJson("JDMX v" + info.appVersion() + " • " + info.dmxRate() + " DMX/s"))
                    .append("\",\"inline\":false}");

            if (info.recentLogs() != null && !info.recentLogs().isBlank()) {
                String truncatedLog = info.recentLogs();
                if (truncatedLog.length() > 900) {
                    truncatedLog = truncatedLog.substring(truncatedLog.length() - 900);
                }
                sb.append(",{\"name\":\"Diagnose-Log\",\"value\":\"```\\n")
                        .append(escapeJson(truncatedLog))
                        .append("\\n```\",\"inline\":false}");
            }
        }

        sb.append("],"); // Ende fields

        // Footer
        String timestampStr = DateTimeFormatter.ISO_INSTANT.format(message.timestamp());
        sb.append("\"footer\":{\"text\":\"JDMX Support • ").append(escapeJson(message.id())).append("\"},");
        sb.append("\"timestamp\":\"").append(timestampStr).append("\"");

        sb.append("}]}"); // Ende embeds und root
        return sb.toString();
    }

    /**
     * Fügt eine übermittelte Nachricht zur Chat-Historie der jeweiligen Kategorie hinzu.
     *
     * @param message Hinzuzufügende Nachricht
     */
    public void addMessageToHistory(SupportMessage message) {
        List<SupportMessage> list = historyByCategory.get(message.category());
        if (list != null && !list.contains(message)) {
            list.add(message);
        }
    }

    /**
     * Liefert die bisherige Nachrichten-Historie für eine bestimmte Kategorie.
     *
     * @param category Gewünschte Kategorie
     * @return Liste aller Nachrichten (Kopie)
     */
    public List<SupportMessage> getHistory(SupportCategory category) {
        List<SupportMessage> list = historyByCategory.get(category);
        return list != null ? new ArrayList<>(list) : List.of();
    }

    /**
     * Maskiert JSON-Steuerzeichen wie Anführungszeichen, Backslashes und Newlines sicher.
     *
     * @param text Rohtext
     * @return JSON-sicher maskierter Text
     */
    public static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < ' ') {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
