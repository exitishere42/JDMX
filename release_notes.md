## What's Changed in JDMX v1.10.0

### Full Undo & Redo System (Memento Snapshot Engine)
- Dedicated `UndoRedoService` supporting full state snapshots of fixtures, groups, and universe patches.
- Visual **Undo** and **Redo** buttons in the unified header bar with live enabled/disabled state bindings and tooltips.
- Keyboard shortcuts: `Ctrl+Z` (Undo) and `Ctrl+Y` / `Ctrl+Shift+Z` (Redo).
- Undo/redo actions integrated with Brand Action Menu, fixture placement, dragging, deleting, renaming, and group assignments.

### Deselection Synchronization (Bidirectional)
- Clicking on empty space on the 2D stage canvas clears active selection on both the stage and in Project Explorer.
- Clicking on blank areas of the Project Explorer tree view clears active selection in both Project Explorer and on the 2D stage canvas.

### Hierarchical Device Groups (Member Fixtures Display)
- Device groups in Project Explorer now display their member fixtures as expandable tree children.
- Clicking an individual fixture inside a group selects that specific fixture on the stage canvas.
- Clicking the group item selects all group members simultaneously on the stage.
- Dragging fixtures onto a group (or onto items within a group) instantly assigns them to the group and updates the tree hierarchy.
- Pressing `Delete` / `Backspace` on a fixture inside a group removes the fixture from that group without deleting it from the project.

### Packaging & Cross-Platform Updates
- New standalone executable fat JAR: `jdmx-1.10.0-all.jar`.
- Updated Linux AppImage: `jdmx-x86_64.AppImage`.
- Updated Windows & Linux installer scripts (`install.bat`, `install.sh`).
