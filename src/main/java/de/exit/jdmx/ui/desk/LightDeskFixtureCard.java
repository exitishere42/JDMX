package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Kompakte Fixture-Karte für die untere Schnellzugriffs-Leiste im LightDesk (Tab 2).
 * <p>
 * Visualisiert Name, Adressbereich und Farbe eines gepatchten Scheinwerfers.
 * Ein Klick auf die Karte fokussiert und scrollt die 512-Kanal-Faderbank direkt zum Startkanal.
 * </p>
 */
public class LightDeskFixtureCard extends HBox {

    private final ExplorerItem item;
    private final FixtureInstance fixtureInstance;
    private final Label lblTitle;
    private final Label lblChannels;
    private final Region colorDot;

    /**
     * Erstellt eine neue Fixture-Karte für den LightDesk.
     *
     * @param item            Explorer-Item des Scheinwerfers
     * @param onCardClicked   Aktion beim Anklicken der Karte (erhält die 1-basierte Startadresse)
     */
    public LightDeskFixtureCard(ExplorerItem item, Consumer<Integer> onCardClicked) {
        super(8);
        this.item = Objects.requireNonNull(item, "item darf nicht null sein");
        this.fixtureInstance = item.getFixtureInstance();

        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("desk-fixture-card");
        setPrefHeight(38);
        setMinHeight(38);
        setMaxHeight(38);

        // Farbpunkt / Akzent
        this.colorDot = new Region();
        colorDot.setMinSize(8, 8);
        colorDot.setPrefSize(8, 8);
        colorDot.setMaxSize(8, 8);
        colorDot.getStyleClass().add("desk-fixture-card-dot");

        Color accentColor = item.getColor() != null ? item.getColor() : Color.web(StudioTheme.HEX_PRIMARY_GRAY);
        String hex = String.format("#%02X%02X%02X",
                (int) (accentColor.getRed() * 255),
                (int) (accentColor.getGreen() * 255),
                (int) (accentColor.getBlue() * 255));
        colorDot.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 4px;");

        // Icon
        String iconName = item.getIconName() != null ? item.getIconName() : "lamp";
        LucideIcon icon = new LucideIcon(iconName, 14, accentColor);

        // Textbereich (Titel oben, Kanäle unten)
        VBox textContainer = new VBox(1);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        this.lblTitle = new Label(item.getName());
        lblTitle.getStyleClass().add("desk-fixture-card-title");

        int startAddr = fixtureInstance != null ? fixtureInstance.getStartAddress() : 1;
        int channelCount = (fixtureInstance != null && fixtureInstance.getActiveMode() != null)
                ? fixtureInstance.getActiveMode().getChannelCount()
                : 1;
        int endAddr = startAddr + channelCount - 1;

        this.lblChannels = new Label(startAddr == endAddr
                ? "Kanal " + startAddr
                : "Kanal " + startAddr + " - " + endAddr);
        lblChannels.getStyleClass().add("desk-fixture-card-channels");

        textContainer.getChildren().addAll(lblTitle, lblChannels);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        getChildren().addAll(colorDot, icon, textContainer);

        // Klick-Handler
        setOnMouseClicked(e -> {
            if (onCardClicked != null) {
                onCardClicked.accept(startAddr);
            }
        });
    }

    /**
     * Liefert das Label für den Scheinwerfer-Titel.
     *
     * @return Titel-Label
     */
    public Label getTitleLabel() {
        return lblTitle;
    }

    /**
     * Liefert das Label für den Kanalbereich.
     *
     * @return Kanal-Label
     */
    public Label getChannelsLabel() {
        return lblChannels;
    }

    /**
     * Liefert das zugrunde liegende Explorer-Item.
     *
     * @return ExplorerItem
     */
    public ExplorerItem getItem() {
        return item;
    }

    /**
     * Liefert die Scheinwerfer-Instanz.
     *
     * @return FixtureInstance
     */
    public FixtureInstance getFixtureInstance() {
        return fixtureInstance;
    }

    /**
     * Liefert die 1-basierte Startadresse des Scheinwerfers.
     *
     * @return Startadresse (1 bis 512)
     */
    public int getStartAddress() {
        return fixtureInstance != null ? fixtureInstance.getStartAddress() : 1;
    }
}
