package de.exit.jdmx.service.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für {@link SupportService}.
 */
class SupportServiceTest {

    private HttpClient mockHttpClient;
    private SupportService supportService;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        supportService = new SupportService(mockHttpClient);
    }

    @Test
    @DisplayName("Sollte Systeminformationen korrekt sammeln")
    void shouldCollectSystemInfo() {
        SupportSystemInfo info = supportService.collectSystemInfo(40, "Letzte Log-Zeilen...");

        assertThat(info.osName()).isNotBlank();
        assertThat(info.osArch()).isNotBlank();
        assertThat(info.javaVersion()).isNotBlank();
        assertThat(info.appVersion()).isNotBlank();
        assertThat(info.dmxRate()).isEqualTo(40);
        assertThat(info.recentLogs()).isEqualTo("Letzte Log-Zeilen...");
    }

    @Test
    @DisplayName("Sollte valides Discord Webhook JSON für Feedback generieren")
    void shouldBuildDiscordPayloadForFeedback() {
        SupportMessage message = SupportMessage.create(
                SupportCategory.FEEDBACK,
                "Lars",
                "lars@example.com",
                "Eine tolle Ergänzung wäre ein Farbfilter!",
                null
        );

        String json = supportService.buildDiscordJson(message);

        assertThat(json).contains("\"username\":\"JDMX Support Bot\"");
        assertThat(json).contains("Feedback von Lars");
        assertThat(json).contains("Eine tolle Ergänzung wäre ein Farbfilter!");
        assertThat(json).contains("lars@example.com");
        // Farbe für Feedback sollte das helle JDMX Logo-Gelb sein (Dezimalwert 16702612 bzw. #FEDC94)
        assertThat(json).contains("\"color\":16702612");
    }

    @Test
    @DisplayName("Sollte valides Discord Webhook JSON für Bug mit Diagnose-Feldern generieren")
    void shouldBuildDiscordPayloadForBugWithSystemInfo() {
        SupportSystemInfo sysInfo = new SupportSystemInfo(
                "Linux",
                "x86_64",
                "21.0.2",
                "Debian",
                "1.20.10",
                44,
                "ERROR: Serial port not found"
        );

        SupportMessage message = SupportMessage.create(
                SupportCategory.BUG,
                "Bühnentechniker",
                "tech@rcv.de",
                "USB-Treiber verbindet nach Standby nicht mehr.",
                sysInfo
        );

        String json = supportService.buildDiscordJson(message);

        assertThat(json).contains("Bug-Report von Bühnentechniker");
        assertThat(json).contains("USB-Treiber verbindet nach Standby nicht mehr.");
        assertThat(json).contains("Linux (x86_64)");
        assertThat(json).contains("Java 21.0.2");
        assertThat(json).contains("44 DMX/s");
        assertThat(json).contains("jdmx-diagnostic.log");
        // Farbe für Bug sollte Hellrot sein (Dezimalwert 16281969 bzw. #F87171)
        assertThat(json).contains("\"color\":16281969");
    }

    @Test
    @DisplayName("Sollte Sonderzeichen und Zeilenumbrüche im JSON sicher maskieren")
    void shouldEscapeJsonSpecialCharacters() {
        SupportMessage message = SupportMessage.create(
                SupportCategory.FEEDBACK,
                "User \"Pro\"",
                "user@test.org",
                "Zeile 1\nZeile 2 mit \"Quotes\" und Backslash: \\",
                null
        );

        String json = supportService.buildDiscordJson(message);

        assertThat(json).contains("User \\\"Pro\\\"");
        assertThat(json).contains("Zeile 1\\nZeile 2 mit \\\"Quotes\\\" und Backslash: \\\\");
    }

    @Test
    @DisplayName("Sollte Nachricht erfolgreich über HttpClient an Webhook senden")
    @SuppressWarnings("unchecked")
    void shouldSendMessageSuccessfully() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(204); // Discord gibt 204 No Content bei Erfolg zurück
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        supportService.setWebhookUrl("https://discord.com/api/webhooks/123/fake-test-webhook");

        SupportMessage message = SupportMessage.create(
                SupportCategory.FEEDBACK,
                "Tester",
                null,
                "Testnachricht",
                null
        );

        CompletableFuture<Boolean> future = supportService.sendMessage(message);
        Boolean success = future.get();

        assertThat(success).isTrue();
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.SENT);
        assertThat(supportService.getHistory(SupportCategory.FEEDBACK)).contains(message);
    }

    @Test
    @DisplayName("Sollte bei HTTP-Fehlercode oder Netzwerkfehler Status FAILED setzen")
    @SuppressWarnings("unchecked")
    void shouldHandleFailedSendGracefully() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(400); // Bad Request
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        supportService.setWebhookUrl("https://discord.com/api/webhooks/123/fake-test-webhook");

        SupportMessage message = SupportMessage.create(
                SupportCategory.BUG,
                "Tester",
                null,
                "Test bug",
                null
        );

        CompletableFuture<Boolean> future = supportService.sendMessage(message);
        Boolean success = future.get();

        assertThat(success).isFalse();
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.FAILED);
    }

    @Test
    @DisplayName("Sollte Direktnachricht über Discord Bot REST API an Benutzer-ID senden")
    @SuppressWarnings("unchecked")
    void shouldSendDirectMessageViaBotSuccessfully() throws Exception {
        HttpResponse<String> channelResponse = mock(HttpResponse.class);
        when(channelResponse.statusCode()).thenReturn(200);
        when(channelResponse.body()).thenReturn("{\"id\":\"dm-chan-999\",\"type\":1}");

        HttpResponse<String> messageResponse = mock(HttpResponse.class);
        when(messageResponse.statusCode()).thenReturn(200);
        when(messageResponse.body()).thenReturn("{\"id\":\"msg-888\"}");

        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(channelResponse))
                .thenReturn(CompletableFuture.completedFuture(messageResponse));

        supportService.setWebhookUrl("");
        supportService.setBotConfig("fake-bot-token", "123456789012345678");

        SupportMessage message = SupportMessage.create(
                SupportCategory.BUG,
                "Lars",
                null,
                "Der Bot sendet jetzt direkte DMs!",
                null
        );

        CompletableFuture<Boolean> future = supportService.sendMessage(message);
        Boolean success = future.get();

        assertThat(success).isTrue();
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.SENT);
        assertThat(supportService.getHistory(SupportCategory.BUG)).contains(message);
    }

    @Test
    @DisplayName("Sollte Fehler behandeln wenn Bot keinen DM-Kanal zum Nutzer öffnen kann")
    @SuppressWarnings("unchecked")
    void shouldFailWhenBotCannotOpenDmChannel() throws Exception {
        HttpResponse<String> channelResponse = mock(HttpResponse.class);
        when(channelResponse.statusCode()).thenReturn(403); // Forbidden (z.B. User hat DMs deaktiviert)
        when(channelResponse.body()).thenReturn("{\"message\":\"Cannot send messages to this user\",\"code\":50007}");

        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(channelResponse));

        supportService.setWebhookUrl("");
        supportService.setBotConfig("fake-bot-token", "123456789012345678");

        SupportMessage message = SupportMessage.create(
                SupportCategory.BUG,
                "Tester",
                null,
                "Bug ohne DM-Rechte",
                null
        );

        CompletableFuture<Boolean> future = supportService.sendMessage(message);
        Boolean success = future.get();

        assertThat(success).isFalse();
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.FAILED);
    }

    @Test
    @DisplayName("Sollte Nachricht ablehnen wenn Webhook-URL leer oder ungültig ist")
    void shouldRejectInvalidWebhookUrl() throws Exception {
        supportService.setWebhookUrl("");

        SupportMessage message = SupportMessage.create(
                SupportCategory.FEEDBACK,
                "Tester",
                null,
                "Feedback ohne URL",
                null
        );

        CompletableFuture<Boolean> future = supportService.sendMessage(message);
        Boolean success = future.get();

        assertThat(success).isFalse();
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.FAILED);
    }

    @Test
    @DisplayName("Sollte Bot-DM mit User-ID aus Kontaktfeld senden wenn keine globale ID konfiguriert ist")
    @SuppressWarnings("unchecked")
    void shouldSendBotDmUsingRecipientUserIdFromContactField() throws Exception {
        HttpResponse<String> channelResponse = mock(HttpResponse.class);
        when(channelResponse.statusCode()).thenReturn(200);
        when(channelResponse.body()).thenReturn("{\"id\":\"dm-contact-chan\",\"type\":1}");

        HttpResponse<String> messageResponse = mock(HttpResponse.class);
        when(messageResponse.statusCode()).thenReturn(200);
        when(messageResponse.body()).thenReturn("{\"id\":\"msg-contact-123\"}");

        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(channelResponse))
                .thenReturn(CompletableFuture.completedFuture(messageResponse));

        supportService.setWebhookUrl("");
        supportService.setBotConfig("fake-bot-token", "");

        SupportMessage message = SupportMessage.create(
                SupportCategory.BUG,
                "Lars",
                "987654321012345678",
                "Bug gemeldet mit Discord ID im Kontaktfeld",
                null
        );

        CompletableFuture<Boolean> future = supportService.sendMessage(message);
        Boolean success = future.get();

        assertThat(success).isTrue();
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.SENT);
    }

    @Test
    @DisplayName("Sollte fehlschlagen wenn Bot-Token gesetzt aber keine User-ID ermittelt werden kann")
    void shouldFailWhenBotTokenIsSetWithoutRecipientId() throws Exception {
        supportService.setWebhookUrl("");
        supportService.setBotConfig("fake-bot-token", "");

        SupportMessage message = SupportMessage.create(
                SupportCategory.BUG,
                "Lars",
                "keine-id-sondern-text",
                "Bug ohne ID",
                null
        );

        CompletableFuture<Boolean> future = supportService.sendMessage(message);
        Boolean success = future.get();

        assertThat(success).isFalse();
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.FAILED);
    }

    @Test
    @DisplayName("Sollte standardmäßig vorkonfigurierte DEFAULT_WEBHOOK_URL verwenden")
    @SuppressWarnings("unchecked")
    void shouldUseDefaultWebhookUrlOutOfTheBox() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(204);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        SupportService serviceWithDefaultWebhook = new SupportService(mockHttpClient);
        assertThat(serviceWithDefaultWebhook.getWebhookUrl()).isNotEmpty().startsWith("https://discord.com/api/webhooks/");

        SupportMessage message = SupportMessage.create(
                SupportCategory.FEEDBACK,
                "Community Member",
                null,
                "Tolle App, danke!",
                null
        );

        CompletableFuture<Boolean> future = serviceWithDefaultWebhook.sendMessage(message);
        Boolean success = future.get();

        assertThat(success).isTrue();
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.SENT);
    }

    @Test
    @DisplayName("Sollte korrekten Multipart-Payload mit JSON und Log-Datei erstellen")
    void shouldBuildValidMultipartPayload() {
        String boundary = "test-boundary-123";
        String json = "{\"content\":\"test\"}";
        String logContent = "Log line 1\nLog line 2";

        byte[] payload = SupportService.buildMultipartPayload(boundary, json, "test.log", logContent);
        String payloadStr = new String(payload, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(payloadStr).contains("--test-boundary-123");
        assertThat(payloadStr).contains("name=\"payload_json\"");
        assertThat(payloadStr).contains("{\"content\":\"test\"}");
        assertThat(payloadStr).contains("name=\"files[0]\"; filename=\"test.log\"");
        assertThat(payloadStr).contains("Log line 1\nLog line 2");
        assertThat(payloadStr).contains("--test-boundary-123--");
    }

    @Test
    @DisplayName("Sollte Nachrichtenhistorie pro Kategorie getrennt verwalten")
    void shouldSeparateHistoryByCategory() {
        SupportMessage fb = SupportMessage.create(SupportCategory.FEEDBACK, "A", null, "Idee 1", null);
        SupportMessage bug = SupportMessage.create(SupportCategory.BUG, "B", null, "Fehler 1", null);

        supportService.addMessageToHistory(fb);
        supportService.addMessageToHistory(bug);

        List<SupportMessage> feedbackList = supportService.getHistory(SupportCategory.FEEDBACK);
        List<SupportMessage> bugList = supportService.getHistory(SupportCategory.BUG);

        assertThat(feedbackList).containsExactly(fb);
        assertThat(bugList).containsExactly(bug);
    }
}
