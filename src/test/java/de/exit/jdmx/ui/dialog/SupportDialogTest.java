package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.service.support.SupportCategory;
import de.exit.jdmx.service.support.SupportMessage;
import de.exit.jdmx.service.support.SupportService;
import de.exit.jdmx.ui.MainWindow;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für den {@link SupportDialog}.
 */
class SupportDialogTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit läuft bereits
        }
    }

    @Test
    @DisplayName("Sollte Dialog mit Underline-Tabs und Standardansicht (Feedback) initialisieren")
    void shouldInitializeDialogWithUnderlineTabs() {
        SupportService mockService = mock(SupportService.class);
        SupportDialog dialog = new SupportDialog(null, mockService);

        VBox view = (VBox) dialog.getView();
        assertThat(view).isNotNull();
        assertThat(view.getStyleClass()).contains("support-dialog-card");

        // Tabs prüfen (Underline-Stil)
        Button tabFeedback = dialog.getBtnTabFeedback();
        Button tabBugs = dialog.getBtnTabBugs();
        assertThat(tabFeedback).isNotNull();
        assertThat(tabBugs).isNotNull();
        assertThat(tabFeedback.getStyleClass()).contains("dialog-underline-tab", "active");
        assertThat(tabBugs.getStyleClass()).contains("dialog-underline-tab");
        assertThat(tabBugs.getStyleClass()).doesNotContain("active");

        // Standard-Kategorie ist FEEDBACK
        assertThat(dialog.getActiveCategory()).isEqualTo(SupportCategory.FEEDBACK);
        // Diagnostik-Checkbox im Feedback-Tab unsichtbar
        assertThat(dialog.getCbIncludeDiagnostics().isVisible()).isFalse();
    }

    @Test
    @DisplayName("Sollte zwischen Feedback und Bugs per Underline-Tabs umschalten")
    void shouldSwitchTabsCorrectly() {
        SupportService mockService = mock(SupportService.class);
        SupportDialog dialog = new SupportDialog(null, mockService);

        // Umschalten auf Bugs
        dialog.getBtnTabBugs().fire();

        assertThat(dialog.getActiveCategory()).isEqualTo(SupportCategory.BUG);
        assertThat(dialog.getBtnTabBugs().getStyleClass()).contains("active");
        assertThat(dialog.getBtnTabFeedback().getStyleClass()).doesNotContain("active");
        assertThat(dialog.getCbIncludeDiagnostics().isVisible()).isTrue();

        // Zurückschalten auf Feedback
        dialog.getBtnTabFeedback().fire();

        assertThat(dialog.getActiveCategory()).isEqualTo(SupportCategory.FEEDBACK);
        assertThat(dialog.getBtnTabFeedback().getStyleClass()).contains("active");
        assertThat(dialog.getBtnTabBugs().getStyleClass()).doesNotContain("active");
        assertThat(dialog.getCbIncludeDiagnostics().isVisible()).isFalse();
    }

    @Test
    @DisplayName("Sollte leere Nachricht ablehnen und Statushinweis anzeigen")
    void shouldRejectEmptyMessage() {
        SupportService mockService = mock(SupportService.class);
        SupportDialog dialog = new SupportDialog(null, mockService);

        dialog.getTxtMessage().setText("   ");
        dialog.getBtnSend().fire();

        assertThat(dialog.getLblStatus().getText()).isEqualTo(I18n.get("support.status.empty"));
        verify(mockService, never()).sendMessage(any());
    }

    @Test
    @DisplayName("Sollte Nachricht absenden und SupportService aufrufen")
    void shouldSendMessageViaService() {
        SupportService mockService = mock(SupportService.class);
        when(mockService.sendMessage(any(SupportMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        SupportDialog dialog = new SupportDialog(null, mockService);
        dialog.getTxtName().setText("Lars");
        dialog.getTxtContact().setText("lars@example.com");
        dialog.getTxtMessage().setText("Tolles neues Feature!");

        dialog.getBtnSend().fire();

        verify(mockService, times(1)).sendMessage(any(SupportMessage.class));
    }
}
