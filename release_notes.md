## What's Changed in JDMX v1.9.0

### Keyboard Deletion
- Direct deletion of fixtures on the 2D stage canvas and in Project Explorer using `Delete` and `Backspace` keys.
- Deletion removes fixtures from the stage, synchronizes the explorer tree, updates project state, and triggers auto-save.

### Bidirectional Selection Synchronization
- Selecting a fixture or group in Project Explorer automatically highlights and selects the corresponding fixture nodes on the 2D stage canvas (and vice versa).
- Tree nodes in Project Explorer automatically expand and scroll into view when fixtures are selected on the canvas.

### Pan/Tilt Inspector Dock Synchronization
- Selecting moving heads immediately reflects their live pan and tilt values on the 2D XY-pad and fine faders in the Position Module.
- Fixture nodes preserve their pan/tilt positions.
- Added a `Home` button action to reset pan/tilt to 50%/50% center position and transmit standard DMX center coordinates.

### Fixture Groups Logic & Persistence
- Added device group logic: Creating a new device group automatically includes currently selected fixtures.
- Drag & Drop fixtures directly into group folders in Project Explorer.
- Selecting a group in Project Explorer synchronizes selection to all group members on the stage canvas.
- Full project persistence for fixture groups via `GroupDataDto` in `.jdmx` project files.

### Cross-Platform Packaging & Updates
- Updated executable fat JAR `jdmx-1.9.0-all.jar`.
- Updated Linux AppImage `jdmx-x86_64.AppImage`.
- Updated in-app updater and installation scripts (`install.bat`, `install.sh`).
