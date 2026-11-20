package de.exit.jdmx.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für {@link SupportMessage} und {@link SupportCategory}.
 */
class SupportMessageTest {

    @Test
    @DisplayName("SupportMessage kann vollständig instanziiert und gelesen werden")
    void shouldCreateSupportMessage() {
        SupportSystemInfo sysInfo = new SupportSystemInfo(
                "Windows 11",
                "amd64",
                "21.0.10",
                "Eclipse Adoptium",
                "1.20.10",
                40,
                "Log output sample"
        );

        Instant now = Instant.now();
        SupportMessage message = new SupportMessage(
                "msg-123",
                SupportCategory.BUG,
                "Lars",
                "lars@example.com",
                "Die Fader reagieren manchmal verzögert.",
                now,
                sysInfo,
                SupportMessage.MessageStatus.SENT
        );

        assertThat(message.id()).isEqualTo("msg-123");
        assertThat(message.category()).isEqualTo(SupportCategory.BUG);
        assertThat(message.senderName()).isEqualTo("Lars");
        assertThat(message.senderContact()).isEqualTo("lars@example.com");
        assertThat(message.messageText()).isEqualTo("Die Fader reagieren manchmal verzögert.");
        assertThat(message.timestamp()).isEqualTo(now);
        assertThat(message.systemInfo()).isNotNull();
        assertThat(message.systemInfo().osName()).isEqualTo("Windows 11");
        assertThat(message.systemInfo().dmxRate()).isEqualTo(40);
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.SENT);
    }

    @Test
    @DisplayName("SupportMessage mit leeren optionalen Feldern verwendet saubere Fallbacks")
    void shouldHandleEmptyOptionalFields() {
        SupportMessage message = SupportMessage.create(
                SupportCategory.FEEDBACK,
                "",
                null,
                "Tolle App, vielen Dank!",
                null
        );

        assertThat(message.id()).isNotBlank();
        assertThat(message.category()).isEqualTo(SupportCategory.FEEDBACK);
        assertThat(message.senderName()).isEqualTo("Anonym");
        assertThat(message.senderContact()).isEqualTo("-");
        assertThat(message.messageText()).isEqualTo("Tolle App, vielen Dank!");
        assertThat(message.timestamp()).isNotNull();
        assertThat(message.systemInfo()).isNull();
        assertThat(message.status()).isEqualTo(SupportMessage.MessageStatus.PENDING);
    }

    @Test
    @DisplayName("SupportMessage validiert leeren oder null Text strikt")
    void shouldRejectBlankMessageText() {
        assertThatThrownBy(() -> SupportMessage.create(SupportCategory.FEEDBACK, "Tester", null, "   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nachricht darf nicht leer sein");

        assertThatThrownBy(() -> SupportMessage.create(SupportCategory.BUG, "Tester", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
