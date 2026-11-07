package de.exit.jdmx.service;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den {@link ProjectExplorerService}.
 * <p>
 * Prüft die strikte Trennung von Core/Service-Geschäftslogik:
 * Initialisierung der 5 Standard-Ordner, freie Auflistung neuer Items,
 * Drag/Drop-Verschieben zwischen Ordnern und Umsortierung.
 * </p>
 */
class ProjectExplorerServiceTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte 5 feste Standard-Ordner mit Ordner-Icons und Farben initialisieren")
    void shouldInitializeStandardFolders() {
        ProjectExplorerService service = new ProjectExplorerService();

        List<ExplorerItem> folders = service.getStandardFolders();
        assertThat(folders).hasSize(5);

        assertThat(folders.get(0).getName()).isEqualTo("Geräte");
        assertThat(folders.get(0).getType()).isEqualTo(ExplorerItemType.FOLDER);
        assertThat(folders.get(0).isStandardFolder()).isTrue();
        assertThat(folders.get(0).getIconName()).isEqualTo("folder");

        assertThat(folders.get(1).getName()).isEqualTo("Gerätegruppen");
        assertThat(folders.get(2).getName()).isEqualTo("Presets");
        assertThat(folders.get(3).getName()).isEqualTo("Szenenlisten");
        assertThat(folders.get(4).getName()).isEqualTo("Effekte");
    }

    @Test
    @DisplayName("Sollte neue Items standardmäßig in die freie Liste (nicht automatisch einsortiert) aufnehmen")
    void shouldAddItemsToUnassignedListByDefault() {
        ProjectExplorerService service = new ProjectExplorerService();

        ExplorerItem spot = new ExplorerItem("spot-1", "Spot Links 1", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#60A5FA"));
        service.addItem(spot);

        assertThat(service.getUnassignedItems()).containsExactly(spot);
        assertThat(service.findParentFolderId("spot-1")).isNull();
        assertThat(service.getItemsInFolder(service.getStandardFolders().get(0).getId())).isEmpty();
    }

    @Test
    @DisplayName("Sollte Items per Drag/Drop in Standard-Ordner verschieben können")
    void shouldMoveItemToFolder() {
        ProjectExplorerService service = new ProjectExplorerService();

        ExplorerItem spot = new ExplorerItem("spot-1", "Spot Links 1", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#60A5FA"));
        service.addItem(spot);

        String devicesFolderId = service.getStandardFolders().get(0).getId();
        boolean moved = service.moveItemToFolder("spot-1", devicesFolderId);

        assertThat(moved).isTrue();
        assertThat(service.getUnassignedItems()).isEmpty();
        assertThat(service.getItemsInFolder(devicesFolderId)).containsExactly(spot);
        assertThat(service.findParentFolderId("spot-1")).isEqualTo(devicesFolderId);
    }

    @Test
    @DisplayName("Sollte Items zwischen Ordnern und zurück in die freie Liste verschieben können")
    void shouldMoveBetweenFoldersAndBackToUnassigned() {
        ProjectExplorerService service = new ProjectExplorerService();

        ExplorerItem spot = new ExplorerItem("spot-1", "Spot Links 1", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#60A5FA"));
        service.addItem(spot);

        String devicesId = service.getStandardFolders().get(0).getId();
        String groupsId = service.getStandardFolders().get(1).getId();

        // 1. In Geräte-Ordner
        service.moveItemToFolder("spot-1", devicesId);
        assertThat(service.getItemsInFolder(devicesId)).containsExactly(spot);

        // 2. In Gruppen-Ordner verschieben
        service.moveItemToFolder("spot-1", groupsId);
        assertThat(service.getItemsInFolder(devicesId)).isEmpty();
        assertThat(service.getItemsInFolder(groupsId)).containsExactly(spot);
        assertThat(service.findParentFolderId("spot-1")).isEqualTo(groupsId);

        // 3. Zurück in freie Liste
        service.moveItemToUnassigned("spot-1");
        assertThat(service.getItemsInFolder(groupsId)).isEmpty();
        assertThat(service.getUnassignedItems()).containsExactly(spot);
        assertThat(service.findParentFolderId("spot-1")).isNull();
    }

    @Test
    @DisplayName("Sollte Items innerhalb eines Ordners umsortieren können")
    void shouldReorderItemsInFolder() {
        ProjectExplorerService service = new ProjectExplorerService();
        String devicesId = service.getStandardFolders().get(0).getId();

        ExplorerItem spot1 = new ExplorerItem("s1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE);
        ExplorerItem spot2 = new ExplorerItem("s2", "Spot 2", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE);
        ExplorerItem spot3 = new ExplorerItem("s3", "Spot 3", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE);

        service.addItemToFolder(devicesId, spot1);
        service.addItemToFolder(devicesId, spot2);
        service.addItemToFolder(devicesId, spot3);

        assertThat(service.getItemsInFolder(devicesId)).containsExactly(spot1, spot2, spot3);

        // Verschiebe Spot 3 an Index 0
        service.reorderInFolder(devicesId, "s3", 0);
        assertThat(service.getItemsInFolder(devicesId)).containsExactly(spot3, spot1, spot2);
    }

    @Test
    @DisplayName("Sollte Standard-Ordner vor dem Löschen schützen")
    void shouldProtectStandardFoldersFromDeletion() {
        ProjectExplorerService service = new ProjectExplorerService();
        String devicesId = service.getStandardFolders().get(0).getId();

        boolean removed = service.removeItem(devicesId);
        assertThat(removed).isFalse();
        assertThat(service.getStandardFolders()).hasSize(5);
    }

    @Test
    @DisplayName("Sollte Listener bei Änderungen zuverlässig benachrichtigen")
    void shouldNotifyListenersOnChange() {
        ProjectExplorerService service = new ProjectExplorerService();
        int[] count = new int[1];
        service.addChangeListener(() -> count[0]++);

        ExplorerItem spot = new ExplorerItem("s1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE);
        service.addItem(spot);
        assertThat(count[0]).isEqualTo(1);

        service.moveItemToFolder("s1", service.getStandardFolders().get(0).getId());
        assertThat(count[0]).isEqualTo(2);
    }

    @Test
    @DisplayName("Sollte eindeutige Fixture-Namen generieren (Basisname, dann (1), (2) usw.)")
    void shouldGenerateUniqueFixtureNamesWithIncrementingNumbers() {
        ProjectExplorerService service = new ProjectExplorerService();

        // 1. Wenn noch kein Gerät existiert -> Basisname bleibt unverändert
        String name1 = service.generateUniqueFixtureName("ZQ02042");
        assertThat(name1).isEqualTo("ZQ02042");

        // Gerät hinzufügen
        service.addItem(new ExplorerItem("f1", name1, ExplorerItemType.FIXTURE, false, "box", Color.BLUE));

        // 2. Zweites Gerät mit demselben Namen -> erhält Suffix " (1)"
        String name2 = service.generateUniqueFixtureName("ZQ02042");
        assertThat(name2).isEqualTo("ZQ02042 (1)");
        service.addItem(new ExplorerItem("f2", name2, ExplorerItemType.FIXTURE, false, "box", Color.BLUE));

        // 3. Drittes Gerät -> erhält Suffix " (2)"
        String name3 = service.generateUniqueFixtureName("ZQ02042");
        assertThat(name3).isEqualTo("ZQ02042 (2)");
        service.addItem(new ExplorerItem("f3", name3, ExplorerItemType.FIXTURE, false, "box", Color.BLUE));

        // 4. Ein anderes Modell ("MiniSpot") -> startet wieder ohne Suffix
        String other1 = service.generateUniqueFixtureName("MiniSpot");
        assertThat(other1).isEqualTo("MiniSpot");
        service.addItem(new ExplorerItem("f4", other1, ExplorerItemType.FIXTURE, false, "box", Color.BLUE));

        String other2 = service.generateUniqueFixtureName("MiniSpot");
        assertThat(other2).isEqualTo("MiniSpot (1)");

        // 5. Wenn ZQ02042 (1) gelöscht wird, wird die Lücke (1) wiederverwendet
        service.removeItem("f2");
        String reused = service.generateUniqueFixtureName("ZQ02042");
        assertThat(reused).isEqualTo("ZQ02042 (1)");
    }

    @Test
    @DisplayName("Sollte Items umbenennen und FixtureInstance sowie Listener synchronisieren")
    void shouldRenameItemAndSyncFixtureInstance() {
        ProjectExplorerService service = new ProjectExplorerService();

        de.exit.jdmx.core.model.ChannelDefinition ch = new de.exit.jdmx.core.model.ChannelDefinition(1, "Dimmer", de.exit.jdmx.core.model.ChannelType.DIMMER, 0, false);
        de.exit.jdmx.core.model.DmxMode mode = new de.exit.jdmx.core.model.DmxMode("1CH", List.of(ch));
        de.exit.jdmx.core.model.FixtureDefinition def = new de.exit.jdmx.core.model.FixtureDefinition("dim", "Gen", "Dimmer", ExplorerItemType.FIXTURE, List.of(mode));
        de.exit.jdmx.core.model.FixtureInstance inst = new de.exit.jdmx.core.model.FixtureInstance("inst-1", "Altes Fixture", def, mode, 1, 1, 0, 0);

        ExplorerItem item = new ExplorerItem("f1", "Altes Fixture", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE, inst);
        service.addItem(item);

        int[] changeCount = new int[1];
        service.addChangeListener(() -> changeCount[0]++);

        // Umbenennen
        boolean renamed = service.renameItem("f1", "Neuer Scheinwerfer");
        assertThat(renamed).isTrue();
        assertThat(item.getName()).isEqualTo("Neuer Scheinwerfer");
        assertThat(inst.getName()).isEqualTo("Neuer Scheinwerfer");
        assertThat(changeCount[0]).isEqualTo(1);

        // Ungültige Umbenennungen
        assertThat(service.renameItem("f1", "")).isFalse();
        assertThat(service.renameItem("f1", "   ")).isFalse();
        assertThat(service.renameItem("f1", null)).isFalse();
        assertThat(service.renameItem("unknown-id", "Neu")).isFalse();

        // Standard-Ordner dürfen nicht umbenannt werden
        String devicesFolderId = service.getStandardFolders().get(0).getId();
        assertThat(service.renameItem(devicesFolderId, "Anderer Name")).isFalse();
    }
}
