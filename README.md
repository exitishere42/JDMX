<div align="center">

<img src="jdmx.png" alt="jdmx Logo" width="128" height="128" />

```text
          _     _                 
         (_)   | |                
          _  __| |_ __ _____  __  
         | |/ _` | '_ ` _ \ \/ /  
         | | (_| | | | | | |>  <   
        _/ |\__,_|_| |_| |_/_/\_\ 
       |__/                       
```

**Modular Desktop DMX512 Lighting Controller & Real-Time Stage Visualizer built with Java 21 LTS & JavaFX 21**

</div>

---

## About the Project

**jdmx** is a high-performance, modular DMX512 lighting control workstation designed for live shows, stage performances, and venue lighting. Built from the ground up for reliable live operation, it unites fixture management, real-time stage visualization, ergonomic channel controls, and cue triggering in an intuitive single-window studio interface.

Visually and structurally, **jdmx** is crafted with an ultra-clean Dark Studio interface and serves as the central control console alongside its lighting suite twin applications, [**sound2artnet**](https://github.com/exitishere42/sound2artnet) (real-time sound-to-light audio engine) and [**artnet2dmx**](https://github.com/exitishere42/artnet2dmx) (hardware USB-to-DMX output bridge).

```text
+--------------------+      +-------------------------+      +--------------------------+
|  SHOW & WORKSPACE  |      |     JDMX CORE ENGINE    |      |    DMX OUTPUT & STAGE    |
|                    |      |                         |      |                          |
|  * Stage & Canvas  | ===> |  * 512-Channel Buffers  | ===> |  * Art-Net 4 (UDP 6454)  |
|  * Device Controls |      |  * HTP / LTP Merging    |      |  * USB-DMX Interfaces   |
|  * Simple Desk     |      |  * 40 Hz Output Loop    |      |  * 2D Stage Visualizer   |
|  * Button Matrix   |      |  * Grand Master & Stop  |      |  * Fixture Beams & Walls |
+--------------------+      +-------------------------+      +------------+-------------+
                                                                          |
                                                                          | DMX512 / Art-Net
                                                                          v
                            +-----------------------------------------------------------+
                            |                 DMX512 LIGHTING FIXTURES                  |
                            |                                                           |
                            |  [ Moving Heads ]   [ RGBW PARs ]   [ Dimmer & Strobes ]  |
                            +-----------------------------------------------------------+
```

---

## Features

### 1. Unified Single Top-Bar & 5 Workspace Tabs
- **All-in-One Compact Header Bar**: Combines workspace navigation, branding, global actions, and live telemetry into a single space-saving top bar:
  - **Brand Action Menu (`[ jdmx ]`)**: Quick dropdown menu for `New Project`, `Open File`, `Save`, `Save as...`, `Undo / Redo`, `Settings`, and `Fullscreen`.
  - **5 Primary Workspaces**: One-click switching between `Stage & Fixtures`, `Simple Desk`, `Button Matrix`, `Show-Timeline`, and `Input/Output`.
  - **Quick Blackout Button (`[● BLACKOUT]`)**: Instantly forces all dimmer and shutter channels to zero while locking moving head pan/tilt angles and color positions in place.
  - **Live Telemetry (`40 DMX/s`)**: Constant framerate counter and bus activity indicator.
  - **Global Emergency Stop / Release (`[■ STOP]`)**: One-click master kill switch to stop all active chasers, faders, and running cues, restoring the entire universe to its default state.

### 2. In-App Modal Overlay Architecture ("Window in Window")
- **Zero OS-Stage Clutter**: All configuration panels, wizards, settings, and confirmation dialogs open within the application window over a semi-transparent scrim (`rgba(0, 0, 0, 0.65)`).
- **Live Safety**: Prevents window-focus desync, background hiding, or dialog displacement across multi-monitor production setups.
- **Instant Keyboard Control**: Dismiss modals at any time with `Escape` or by clicking the shaded backdrop.

### 3. Stage Canvas & 2D Live Stage Visualizer
- **Rigging & Stage Geometry**: Interactive 2D visualization featuring projection walls, U-truss rigging, and real-time beam projections for moving heads and static spots.
- **Fixture Placement & Collision Handling**: Drag and drop fixtures onto trusses and stage positions with automatic spacing and collision avoidance.
- **Collapsible Project Explorer**: Left/right collapsible browser tree with 4px rounded cells, smooth toggle actions, and structured organization for Fixtures, Presets, Groups, and Cues.

### 4. Device Control Dock
- **Position Module**: Interactive 2D XY-Pad with 16-bit Pan/Tilt precision, fine faders, invert toggles, and instant alignment presets (`Center`, `Wall`, `Audience`, `Left`, `Right`).
- **Intensity Module**: Grand Master fader, individual fixture dimmer, shutter controls, and hardware/software strobe rate sliders.
- **Color Module**: RGB / RGBW color mixer, color wheel presets, and live color preview swatch.
- **Exclusive Single-Open Cards**: Accordion-style ergonomics to keep the workspace clean and focused during live performance.

### 5. DMX512 Core Engine & Multi-Protocol Output
- **Continuous 40 Hz Output Loop**: Decoupled frame generation guaranteeing steady, jitter-free DMX signal timing without fixture flickering.
- **Art-Net 4 Network Sender**: Standard-compliant `ArtDMX` broadcast/unicast over UDP port `6454` for direct network integration with visualizers or hardware nodes.
- **USB-DMX Interfaces**: Plug-and-play support for FTDI OpenDMX and Enttec DMX USB Pro adapters via native serial communication (`jSerialComm`).
- **MIDI Controller Support**: Seamless integration for MIDI controllers and pad surfaces via native Java MIDI (`javax.sound.midi`) with interactive MIDI Learn.
- **Audio Timeline Sync**: Integrated JavaFX Media audio pipeline for music playback synchronized sample-accurately to light cues.

---

## Installation & Quick Start (GitHub Release)

### 1. Automatic Installer (Recommended)
Download [`install.sh`](https://github.com/exitishere42/JDMX/releases/latest/download/install.sh) (Linux) or [`install.bat`](https://github.com/exitishere42/JDMX/releases/latest/download/install.bat) (Windows) from the [**Releases**](https://github.com/exitishere42/JDMX/releases/latest) page:
- Automatically installs the application into `~/jdmx` (e.g., `/home/regie/jdmx` on Linux or `%USERPROFILE%\jdmx` on Windows).
- Checks if **Java 21+** is already installed — skips installation if present, or automatically installs the latest OpenJDK 21 LTS if missing.
- Creates a **Desktop Entry / Start Menu App** with the **jdmx** logo so you can launch it directly from your application dash.

**Linux One-Line Install:**
```bash
curl -fsSL https://github.com/exitishere42/JDMX/releases/latest/download/install.sh | bash
```

**Windows Install:**
Download and run `install.bat` from the [latest release](https://github.com/exitishere42/JDMX/releases/latest).

### 2. Standalone Linux AppImage
Download [`jdmx-x86_64.AppImage`](https://github.com/exitishere42/JDMX/releases/latest/download/jdmx-x86_64.AppImage) from the [latest release](https://github.com/exitishere42/JDMX/releases/latest):
```bash
chmod +x jdmx-x86_64.AppImage
./jdmx-x86_64.AppImage
```

### 3. Uninstallation (`uninstall.sh` / `uninstall.bat`)
To completely remove **jdmx** (including `~/jdmx`, icons, and the Desktop/Start Menu entry):

**Linux One-Line Uninstall:**
```bash
curl -fsSL https://github.com/exitishere42/JDMX/releases/latest/download/uninstall.sh | bash
```

**Windows Uninstall:**
Download and run [`uninstall.bat`](https://github.com/exitishere42/JDMX/releases/latest/download/uninstall.bat) from the [latest release](https://github.com/exitishere42/JDMX/releases/latest) (or run `%USERPROFILE%\jdmx\uninstall.bat`).

---

## Build from Source

### Requirements
- **Java 21 LTS** or newer
- **Apache Maven 3.8+**

### Run Application (Development)
On Windows (batch launcher):
```cmd
run.bat
```

Or directly via Maven:
```bash
mvn exec:java
```

### Rebuild AppImage (Linux / WSL)
```bash
mvn clean package -DskipTests
./build-appimage.sh
```

### Compile & Run Tests
```bash
mvn clean test
```

### Build Executable Fat JAR
```bash
mvn clean package
```
The standalone fat JAR will be created at `target/jdmx-1.0.0-all.jar`.

---

## Author

```text
+-------------------------------------------------------------------+
|  Programmed by:     exitishere42                                  |
|  GitHub:            https://github.com/exitishere42               |
|  Repository:        https://github.com/exitishere42/JDMX          |
+-------------------------------------------------------------------+
```
