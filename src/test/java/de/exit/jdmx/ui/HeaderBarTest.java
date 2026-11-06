package de.exit.jdmx.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link HeaderBar}.
 */
class HeaderBarTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @Test
    @DisplayName("Sollte Brand-Button mit Logo-Graphic links und Text jdmx ohne Border erzeugen")
    void shouldCreateBrandButtonInHeader() {
        HeaderBar headerBar = new HeaderBar();
        assertThat(headerBar.getChildren()).isNotEmpty();

        Node firstNode = headerBar.getChildren().getFirst();
        assertThat(firstNode).isInstanceOf(Button.class);

        Button brandBtn = (Button) firstNode;
        assertThat(brandBtn.getText()).isEqualTo("jdmx");
        assertThat(brandBtn.getGraphic()).isNotNull();
        assertThat(brandBtn.getStyleClass()).contains("brand-button");
    }

    @Test
    @DisplayName("Sollte Tab 2 als LightDesk und Tab 5 als I/O Patch benennen")
    void shouldNameSecondTabLightDeskAndFifthTabIoPatch() {
        HeaderBar headerBar = new HeaderBar();
        assertThat(headerBar.getTabButtons()).hasSize(5);
        assertThat(headerBar.getTabButtons().get(1).getText()).isEqualTo("LightDesk");
        assertThat(headerBar.getTabButtons().get(4).getText()).isEqualTo("I/O Patch");
    }
}
