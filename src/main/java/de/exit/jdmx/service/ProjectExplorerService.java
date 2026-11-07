package de.exit.jdmx.service;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service für die Geschäftslogik des Project Explorers (100% Core/Service-Schicht).
 * <p>
 * Verwaltet:
 * <ul>
 *     <li>Die 5 festen Standard-Ordner (Geräte, Gerätegruppen, Presets, Szenenlisten, Effekte)</li>
 *     <li>Die freie Auflistung unzugeordneter neuer Items auf Root-Ebene</li>
 *     <li>Drag & Drop Verschieben zwischen Ordnern, in Ordner hinein und zurück in die freie Liste</li>
 *     <li>Umsortierung innerhalb von Ordnern oder im Root-Bereich</li>
 * </ul>
 * Thread-sicher und rein logikbasiert ohne JavaFX-UI-Abhängigkeiten.
 * </p>
 */
public class ProjectExplorerService {

    public static final String FOLDER_DEVICES_ID = "folder-devices";
    public static final String FOLDER_GROUPS_ID = "folder-groups";
    public static final String FOLDER_PRESETS_ID = "folder-presets";
    public static final String FOLDER_CUELISTS_ID = "folder-cuelists";
    public static final String FOLDER_EFFECTS_ID = "folder-effects";

    private final List<ExplorerItem> standardFolders = new ArrayList<>();
    private final Map<String, List<ExplorerItem>> folderContents = new LinkedHashMap<>();
    private final List<ExplorerItem> unassignedItems = new ArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public ProjectExplorerService() {
        initStandardFolders();
    }

    private void initStandardFolders() {
        standardFolders.add(new ExplorerItem(FOLDER_DEVICES_ID, "Geräte", ExplorerItemType.FOLDER, true, "folder", Color.web("#60A5FA")));
        standardFolders.add(new ExplorerItem(FOLDER_GROUPS_ID, "Gerätegruppen", ExplorerItemType.FOLDER, true, "folder", Color.web("#34D399")));
        standardFolders.add(new ExplorerItem(FOLDER_PRESETS_ID, "Presets", ExplorerItemType.FOLDER, true, "folder", Color.web("#FBBF24")));
        standardFolders.add(new ExplorerItem(FOLDER_CUELISTS_ID, "Szenenlisten", ExplorerItemType.FOLDER, true, "folder", Color.web("#A78BFA")));
        standardFolders.add(new ExplorerItem(FOLDER_EFFECTS_ID, "Effekte", ExplorerItemType.FOLDER, true, "folder", Color.web("#F472B6")));

        for (ExplorerItem folder : standardFolders) {
            folderContents.put(folder.getId(), new ArrayList<>());
        }
    }

    /**
     * Liefert die 5 Standard-Ordner in unveränderlicher Liste.
     */
    public List<ExplorerItem> getStandardFolders() {
        return Collections.unmodifiableList(standardFolders);
    }

    /**
     * Liefert die Items innerhalb eines bestimmten Ordners.
     *
     * @param folderId ID des Ordners
     * @return Liste der enthaltenen Items
     */
    public List<ExplorerItem> getItemsInFolder(String folderId) {
        List<ExplorerItem> list = folderContents.get(folderId);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Liefert die unzugeordneten Items auf Root-Ebene.
     */
    public List<ExplorerItem> getUnassignedItems() {
        return Collections.unmodifiableList(unassignedItems);
    }

    /**
     * Fügt ein neues Item zur unzugeordneten Root-Liste hinzu (Nutzeranforderung:
     * neue Items werden nicht automatisch einsortiert, sondern frei aufgelistet).
     *
     * @param item Das hinzuzufügende Item
     */
    public void addItem(ExplorerItem item) {
        Objects.requireNonNull(item, "item must not be null");
        unassignedItems.add(item);
        fireChange();
    }

    /**
     * Fügt ein Item direkt in einen bestimmten Ordner ein.
     */
    public void addItemToFolder(String folderId, ExplorerItem item) {
        Objects.requireNonNull(folderId, "folderId must not be null");
        Objects.requireNonNull(item, "item must not be null");
        List<ExplorerItem> list = folderContents.get(folderId);
        if (list != null) {
            list.add(item);
            fireChange();
        }
    }

    /**
     * Verschiebt ein Item per Drag & Drop in einen Zielordner.
     *
     * @param itemId         ID des zu verschiebenden Items
     * @param targetFolderId ID des Zielordners
     * @return true, wenn das Item erfolgreich verschoben wurde
     */
    public boolean moveItemToFolder(String itemId, String targetFolderId) {
        List<ExplorerItem> targetList = folderContents.get(targetFolderId);
        if (targetList == null) {
            return false;
        }

        ExplorerItem item = extractItem(itemId);
        if (item == null) {
            return false;
        }

        targetList.add(item);
        fireChange();
        return true;
    }

    /**
     * Verschiebt ein Item per Drag & Drop aus einem Ordner zurück in die unzugeordnete Root-Liste.
     *
     * @param itemId ID des Items
     * @return true bei Erfolg
     */
    public boolean moveItemToUnassigned(String itemId) {
        ExplorerItem item = extractItem(itemId);
        if (item == null) {
            return false;
        }

        unassignedItems.add(item);
        fireChange();
        return true;
    }

    /**
     * Sortiert ein Item innerhalb eines Ordners an eine neue Index-Position.
     */
    public boolean reorderInFolder(String folderId, String itemId, int targetIndex) {
        List<ExplorerItem> list = folderContents.get(folderId);
        if (list == null) {
            return false;
        }

        int currentIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(itemId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0) {
            return false;
        }

        ExplorerItem item = list.remove(currentIndex);
        int clampedIndex = Math.max(0, Math.min(targetIndex, list.size()));
        list.add(clampedIndex, item);
        fireChange();
        return true;
    }

    /**
     * Sortiert ein unzugeordnetes Item in der Root-Liste an eine neue Index-Position.
     */
    public boolean reorderUnassigned(String itemId, int targetIndex) {
        int currentIndex = -1;
        for (int i = 0; i < unassignedItems.size(); i++) {
            if (unassignedItems.get(i).getId().equals(itemId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0) {
            return false;
        }

        ExplorerItem item = unassignedItems.remove(currentIndex);
        int clampedIndex = Math.max(0, Math.min(targetIndex, unassignedItems.size()));
        unassignedItems.add(clampedIndex, item);
        fireChange();
        return true;
    }

    /**
     * Entfernt ein Item aus dem System (Standard-Ordner sind geschützt und können nicht entfernt werden).
     */
    public boolean removeItem(String itemId) {
        for (ExplorerItem folder : standardFolders) {
            if (folder.getId().equals(itemId)) {
                return false; // Standard-Ordner können nicht gelöscht werden
            }
        }

        ExplorerItem removed = extractItem(itemId);
        if (removed != null) {
            for (ExplorerItem group : getItemsInFolder(FOLDER_GROUPS_ID)) {
                group.removeMemberFixtureId(itemId);
            }
            fireChange();
            return true;
        }
        return false;
    }

    /**
     * Benennt ein Item um und synchronisiert die verknüpfte FixtureInstance (falls vorhanden).
     * Standard-Ordner sind geschützt und können nicht umbenannt werden.
     *
     * @param itemId  Die ID des umzubenennenden Items
     * @param newName Der neue Anzeigename (darf nicht leer oder null sein)
     * @return true bei erfolgreicher Umbenennung, false bei ungültigem Namen oder geschütztem Ordner
     */
    public boolean renameItem(String itemId, String newName) {
        if (newName == null || newName.isBlank()) {
            return false;
        }
        for (ExplorerItem folder : standardFolders) {
            if (folder.getId().equals(itemId)) {
                return false;
            }
        }
        ExplorerItem item = findItemById(itemId);
        if (item == null) {
            return false;
        }
        String trimmedName = newName.trim();
        item.setName(trimmedName);
        if (item.getFixtureInstance() != null) {
            item.getFixtureInstance().setName(trimmedName);
        }
        fireChange();
        return true;
    }

    /**
     * Generiert einen eindeutigen Namen für ein neues Scheinwerfer-Item.
     * <p>
     * Ist der Basisname noch nicht vergeben, wird er unverändert zurückgegeben (z. B. "ZQ02042").
     * Ist er bereits vergeben, wird ein fortlaufendes Suffix der Form " (1)", " (2)" usw. angehängt.
     * </p>
     *
     * @param baseName Der gewünschte Basisname
     * @return Ein im Projekt noch ungenutzter, eindeutiger Name
     */
    public String generateUniqueFixtureName(String baseName) {
        if (baseName == null || baseName.isBlank()) {
            return "Fixture";
        }
        String cleanBase = baseName.trim();
        Set<String> existingNames = getAllItemNames();
        if (!existingNames.contains(cleanBase)) {
            return cleanBase;
        }
        int index = 1;
        while (existingNames.contains(cleanBase + " (" + index + ")")) {
            index++;
        }
        return cleanBase + " (" + index + ")";
    }

    private Set<String> getAllItemNames() {
        Set<String> names = new HashSet<>();
        for (List<ExplorerItem> list : folderContents.values()) {
            for (ExplorerItem item : list) {
                names.add(item.getName());
            }
        }
        for (ExplorerItem item : unassignedItems) {
            names.add(item.getName());
        }
        return names;
    }

    /**
     * Sucht ein Item anhand seiner ID (egal ob in einem Ordner oder unzugeordnet).
     */
    public ExplorerItem findItemById(String itemId) {
        for (ExplorerItem folder : standardFolders) {
            if (folder.getId().equals(itemId)) {
                return folder;
            }
        }
        for (List<ExplorerItem> items : folderContents.values()) {
            for (ExplorerItem item : items) {
                if (item.getId().equals(itemId)) {
                    return item;
                }
            }
        }
        for (ExplorerItem item : unassignedItems) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Liefert die ID des Elternordners eines Items oder null, wenn es unzugeordnet ist.
     */
    public String findParentFolderId(String itemId) {
        for (Map.Entry<String, List<ExplorerItem>> entry : folderContents.entrySet()) {
            for (ExplorerItem item : entry.getValue()) {
                if (item.getId().equals(itemId)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Entnimmt ein Item aus seinem aktuellen Kontext (egal ob Ordner oder Root).
     */
    private ExplorerItem extractItem(String itemId) {
        for (List<ExplorerItem> items : folderContents.values()) {
            Iterator<ExplorerItem> it = items.iterator();
            while (it.hasNext()) {
                ExplorerItem item = it.next();
                if (item.getId().equals(itemId)) {
                    it.remove();
                    return item;
                }
            }
        }

        Iterator<ExplorerItem> it = unassignedItems.iterator();
        while (it.hasNext()) {
            ExplorerItem item = it.next();
            if (item.getId().equals(itemId)) {
                it.remove();
                return item;
            }
        }

        return null;
    }

    /**
     * Entfernt alle nicht-festen Items aus Ordnern und dem unzugeordneten Bereich.
     */
    public void clearItems() {
        for (List<ExplorerItem> items : folderContents.values()) {
            items.clear();
        }
        unassignedItems.clear();
        fireChange();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    /**
     * Löst eine Aktualisierung aller registrierten Change-Listener aus.
     */
    public void notifyChange() {
        fireChange();
    }

    private void fireChange() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }
}
