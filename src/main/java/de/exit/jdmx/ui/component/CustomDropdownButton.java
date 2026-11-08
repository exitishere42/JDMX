package de.exit.jdmx.ui.component;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Modernes, kompaktes Custom-Dropdown-Steuerelement im Studio-Dark-Design.
 * <p>
 * Basiert auf dem gleichen Popup-Muster wie das Aktionsmenü des Brand-Buttons
 * und der Explorer-Hinzufügen-Button.
 * Verhindert übermäßig breite Standard-ComboBoxen und hält die Benutzeroberfläche
 * maßvoll und ergonomisch.
 * </p>
 */
public class CustomDropdownButton extends HBox {

    private final StringProperty selectedItem = new SimpleStringProperty();
    private final ObservableList<String> items = FXCollections.observableArrayList();

    private final Label textLabel;
    private final LucideIcon chevronIcon;
    private final Popup menuPopup;
    private final VBox menuContainer;
    private Consumer<String> onItemSelected;

    /**
     * Erstellt einen neuen {@link CustomDropdownButton} mit initialem Wert und Elementen.
     *
     * @param initialSelection Initial ausgewählter Text
     * @param availableItems   Liste aller zur Auswahl stehenden Optionen
     */
    public CustomDropdownButton(String initialSelection, List<String> availableItems) {
        this(initialSelection, availableItems, 190, 210, 230);
    }

    /**
     * Erstellt einen neuen {@link CustomDropdownButton} mit benutzerdefinierten Breitenbeschränkungen.
     *
     * @param initialSelection Initial ausgewählter Text
     * @param availableItems   Liste aller zur Auswahl stehenden Optionen
     * @param minWidth         Minimale Breite in Pixeln
     * @param prefWidth        Bevorzugte Breite in Pixeln
     * @param maxWidth         Maximale Breite in Pixeln
     */
    public CustomDropdownButton(String initialSelection, List<String> availableItems, double minWidth, double prefWidth, double maxWidth) {
        super(6);
        setAlignment(Pos.CENTER_LEFT);
        setCursor(Cursor.HAND);
        getStyleClass().add("custom-dropdown-btn");

        // Feste maßvolle Breite (verhindert unnatürliches Aufblähen)
        setMinWidth(minWidth);
        setPrefWidth(prefWidth);
        setMaxWidth(maxWidth);

        // Text-Anzeige
        textLabel = new Label();
        textLabel.getStyleClass().add("custom-dropdown-label");
        textLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        textLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textLabel, Priority.ALWAYS);

        // Chevron-Down Icon
        chevronIcon = new LucideIcon("chevron-down", 12, StudioTheme.COLOR_TEXT_MED);

        getChildren().addAll(textLabel, chevronIcon);

        // Popup-Menü initialisieren
        menuPopup = new Popup();
        menuPopup.setAutoHide(true);
        menuPopup.setHideOnEscape(true);

        menuContainer = new VBox(2);
        menuContainer.getStyleClass().add("action-menu-popup");
        menuContainer.setMinWidth(minWidth);
        menuContainer.setPrefWidth(prefWidth);
        menuPopup.getContent().add(menuContainer);

        // Event-Handler für Klick
        setOnMouseClicked(e -> togglePopup());

        // Daten belegen
        if (availableItems != null) {
            setItems(availableItems);
        }
        if (initialSelection != null) {
            selectItem(initialSelection);
        } else if (!items.isEmpty()) {
            selectItem(items.getFirst());
        }

        selectedItem.addListener((obs, oldVal, newVal) -> textLabel.setText(newVal != null ? newVal : ""));
    }

    /**
     * Toggelt die Sichtbarkeit des Dropdown-Popups direkt unterhalb des Steuerelements.
     */
    private void togglePopup() {
        if (menuPopup.isShowing()) {
            menuPopup.hide();
        } else {
            Bounds bounds = localToScreen(getBoundsInLocal());
            if (bounds != null && getScene() != null && getScene().getWindow() != null) {
                // Breite des Popups an Steuerelement anpassen
                menuContainer.setMinWidth(getWidth());
                menuPopup.show(getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 4);
            }
        }
    }

    /**
     * Setzt die Liste der wählbaren Optionen und aktualisiert das Popup.
     *
     * @param newItems Neue Elementliste
     */
    public void setItems(List<String> newItems) {
        items.setAll(Objects.requireNonNullElseGet(newItems, List::of));
        rebuildMenuItems();
    }

    /**
     * Wählt ein bestimmtes Element aus und benachrichtigt registrierte Listener.
     *
     * @param item Auszuwählendes Element
     */
    public void selectItem(String item) {
        if (item != null && !items.contains(item)) {
            items.add(item);
            rebuildMenuItems();
        }
        selectedItem.set(item);
        textLabel.setText(item != null ? item : "");
        if (onItemSelected != null) {
            onItemSelected.accept(item);
        }
    }

    /**
     * Baut die Menü-Einträge im Popup neu auf.
     */
    private void rebuildMenuItems() {
        menuContainer.getChildren().clear();
        for (String item : items) {
            Button itemBtn = new Button(item);
            itemBtn.getStyleClass().add("action-menu-item");
            itemBtn.setMaxWidth(Double.MAX_VALUE);
            itemBtn.setAlignment(Pos.CENTER_LEFT);
            itemBtn.setCursor(Cursor.HAND);

            itemBtn.setOnAction(e -> {
                selectItem(item);
                menuPopup.hide();
            });

            menuContainer.getChildren().add(itemBtn);
        }
    }

    /**
     * Setzt den Callback-Handler für Auswahleinbrüche.
     *
     * @param onItemSelected Consumer für das ausgewählte Element
     */
    public void setOnItemSelected(Consumer<String> onItemSelected) {
        this.onItemSelected = onItemSelected;
    }

    public String getSelectedItem() {
        return selectedItem.get();
    }

    public StringProperty selectedItemProperty() {
        return selectedItem;
    }

    public ObservableList<String> getItems() {
        return items;
    }
}
