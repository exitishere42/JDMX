package de.exit.jdmx.ui.component;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link MarkdownView}.
 * Prüft das Parsen und Rendern von Markdown-Inhalten (Überschriften, Listen, Codeblöcke, TextFlows).
 */
class MarkdownViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit bereits initialisiert
        }
    }

    @Test
    @DisplayName("Sollte leeren Text und Null-Werte ohne Fehler behandeln")
    void shouldHandleNullAndEmptyGracefully() {
        MarkdownView view = new MarkdownView();
        view.setMarkdown(null);
        assertThat(view.getContentBox().getChildren()).isEmpty();

        view.setMarkdown("");
        assertThat(view.getContentBox().getChildren()).isEmpty();
    }

    @Test
    @DisplayName("Sollte Überschriften (#, ##, ###) als hervorgehobene Labels rendern")
    void shouldRenderHeadingsAsLabels() {
        MarkdownView view = new MarkdownView();
        view.setMarkdown("# Haupttitel\n## Untertitel\n### Sektion");

        VBox content = view.getContentBox();
        assertThat(content.getChildren()).hasSize(3);

        Label h1 = (Label) content.getChildren().get(0);
        assertThat(h1.getText()).isEqualTo("Haupttitel");

        Label h2 = (Label) content.getChildren().get(1);
        assertThat(h2.getText()).isEqualTo("Untertitel");

        Label h3 = (Label) content.getChildren().get(2);
        assertThat(h3.getText()).isEqualTo("Sektion");
    }

    @Test
    @DisplayName("Sollte Listenpunkte (- und *) mit Bullet-Symbolen erzeugen")
    void shouldRenderListItems() {
        MarkdownView view = new MarkdownView();
        view.setMarkdown("- Erster Punkt\n* Zweiter Punkt");

        VBox content = view.getContentBox();
        assertThat(content.getChildren()).hasSize(2);
    }

    @Test
    @DisplayName("Sollte mehrzeilige Codeblöcke (```) in Code-Containern zusammenfassen")
    void shouldRenderCodeBlocks() {
        MarkdownView view = new MarkdownView();
        String md = "Normaler Text\n```bash\ncurl -fsSL https://example.com | bash\n```\nWeiterer Text";
        view.setMarkdown(md);

        VBox content = view.getContentBox();
        assertThat(content.getChildren()).hasSize(3);

        Node codeBlock = content.getChildren().get(1);
        assertThat(codeBlock.getStyleClass()).contains("markdown-code-block");
    }

    @Test
    @DisplayName("Sollte Trennlinien (---) rendern")
    void shouldRenderDividers() {
        MarkdownView view = new MarkdownView();
        view.setMarkdown("Absatz 1\n---\nAbsatz 2");

        VBox content = view.getContentBox();
        assertThat(content.getChildren()).hasSize(3);
        assertThat(content.getChildren().get(1).getStyleClass()).contains("markdown-divider");
    }
}
