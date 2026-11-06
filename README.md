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

**Modular Desktop DMX512 Lighting Controller & Real-Time Stage Visualizer**  
*Built with Java 21 LTS & JavaFX 21*

[Features](#features) • [Hardware & I/O Patch](#dmx-hardware-output--io-patching) • [Installation](#installation) • [Build from Source](#build-from-source) • [Releases](https://github.com/exitishere42/JDMX/releases/latest)

</div>

---

## Features

- **2D Stage Visualizer**: Real-time 2D canvas with projection wall, U-truss rigging, and dynamic light beam projection for moving heads and spots with collision avoidance.
- **Ergonomic Device Controls**: Dedicated inspector dock featuring:
  - **Position Module**: Interactive 2D XY-Pad with 16-bit Pan/Tilt precision, fine sliders, invert toggles, and quick presets (`Center`, `Wall`, `Audience`, `Left`, `Right`).
  - **Intensity Module**: Grand Master fader, individual fixture dimmer, shutter controls, and strobe rate sliders.
  - **Color Module**: RGB/RGBW color mixer, color wheel presets, and live color preview.
- **Unified Single Top-Bar**:
  - **Brand Action Menu (`[ jdmx ]`)**: Project management, settings, undo/redo, and fullscreen toggle.
  - **5 Primary Workspaces**: One-click switching between `Stage & Fixtures`, `LightDesk`, `Button Matrix`, `Show-Timeline`, and `Input/Output`.
  - **Quick Blackout (`[● BLACKOUT]`)**: Instantly kills dimmer and shutter channels while locking moving head positions and colors in place.
  - **Emergency Release (`[■ STOP]`)**: Global kill switch to instantly stop all active faders, chasers, and cues.
  - **Live Telemetry**: Real-time DMX framerate counter (`40 DMX/s`) and bus monitor.
- **Native USB-to-DMX & Multi-Protocol Engine**:
  - Pure-Java low-level USB-to-DMX output via integrated **`jusb2dmx`** driver library.
  - Standard Art-Net 4 network streaming (UDP port `6454`).
  - Jitter-free, hardware-timed 40 Hz output loop preventing fixture flickering.
  - Native MIDI controller support with interactive MIDI Learn (`javax.sound.midi`).
- **In-App Modal Overlays**: All dialogs, settings, and wizards render within the application window over a dark backdrop — no displaced OS popups or focus loss during live shows.
- **Lighting Suite Companion**: Seamless ecosystem partner to [**artnet2dmx**](https://github.com/exitishere42/artnet2dmx) (USB-DMX Bridge) and [**sound2artnet**](https://github.com/exitishere42/sound2artnet) (Sound-to-Light Engine).

---

## DMX Hardware Output & I/O Patching

### 1. Pure-Java Hardware Interface (`jusb2dmx`)
**jdmx** integrates the standalone, cross-platform hardware driver library **`jusb2dmx`** (`de.exit.jusb2dmx:jusb2dmx:1.0.0`) for direct USB-to-DMX communication:
- **Direct Low-Level Control**: Full timing control over Break, Mark-After-Break (MAB), and streaming 512 DMX channels per universe.
- **Supported Interfaces**: FTDI FT232R-based USB-to-DMX adapters, specifically **JMS USB to DMX PRO** and OpenDMX-compatible interfaces.
- **Zero Overhead**: **100% pure Java / JNA** — no Python subprocesses, no external C compilers, and zero virtual COM port latency.
- **Alternative Backends**: Supports Art-Net 4 UDP streaming (broadcast & unicast) and in-memory virtual loopback.

```text
+-----------------------+-------------------------+-------------------------+--------------------+
| DRIVER / INTERFACE    | BACKEND TECHNOLOGY      | HARDWARE TARGET         | TIMING / LATENCY   |
+-----------------------+-------------------------+-------------------------+--------------------+
| jusb2dmx (Native)     | Pure Java + JNA         | JMS USB to DMX PRO /    | Ultra-Low Latency  |
|                       | (Direct USB Control)    | FTDI FT232R OpenDMX     | (Hardware Timed)   |
| Art-Net 4 Sender      | UDP Socket (Port 6454)  | Network Nodes, QLC+,    | Network Latency    |
|                       |                         | artnet2dmx, Resolume    | (< 2ms Local)      |
| Virtual Loopback      | In-Memory DMX Buffer    | Internal Simulation     | Instant (Testing)  |
+-----------------------+-------------------------+-------------------------+--------------------+
```

### 2. Dynamic Hardware Discovery & I/O Patch Workspace (QLC+ Style)
The **Input/Output Workspace (Tab 5)** provides a comprehensive, 4-universe patching matrix inspired by industry-standard consoles:
- **4 Configurable Universes**: Each universe features independent input and output routing.
- **Live Hardware Discovery**: Connected **JMS USB2DMX PRO** interfaces and USB MIDI controllers are automatically detected on hot-plug. Output dropdowns populate dynamically and only show physical adapters when they are genuinely plugged into the system.
- **Visual Status Indicators**: Real-time status dots highlight active routing and signal flow per universe.
- **Per-Universe Configuration**: Dedicated setup modals (`⚙`) allow universe-level adjustments (IP destination, subnet, port address, universe offsets).
- **Expansion-Ready Layout**: Provides ample headroom and space below the universe table for future visual telemetry, real-time monitors, and live fixtures.

---

## Architecture

```text
+--------------------+      +-------------------------+      +--------------------------+
|  SHOW & WORKSPACE  |      |     JDMX CORE ENGINE    |      |    DMX OUTPUT & STAGE    |
|                    |      |                         |      |                          |
|  * Stage & Canvas  | ===> |  * 512-Channel Buffers  | ===> |  * jusb2dmx (JMS USB)    |
|  * Device Controls |      |  * HTP / LTP Merging    |      |  * Art-Net 4 (UDP 6454)  |
|  * LightDesk       |      |  * 40 Hz Output Loop    |      |  * Virtual Loopback      |
|  * Button Matrix   |      |  * Grand Master & Stop  |      |  * 2D Stage Visualizer   |
|  * I/O 4-Universe  |      |  * Hardware Discovery   |      |  * I/O Expansion Space   |
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

## Installation

### Linux One-Liner
```bash
curl -fsSL https://github.com/exitishere42/JDMX/releases/latest/download/install.sh | bash
```

### Windows
Download and run [`install.bat`](https://github.com/exitishere42/JDMX/releases/latest/download/install.bat) from the [latest release](https://github.com/exitishere42/JDMX/releases/latest).

### Standalone AppImage (Linux)
```bash
chmod +x jdmx-x86_64.AppImage && ./jdmx-x86_64.AppImage
```

### Uninstall
- **Linux**: `curl -fsSL https://github.com/exitishere42/JDMX/releases/latest/download/uninstall.sh | bash`
- **Windows**: Run `uninstall.bat` in `%USERPROFILE%\jdmx`

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

The standalone executable fat JAR will be generated at `target/jdmx-1.5.0-all.jar`.

---

## Author

```text
+-------------------------------------------------------------------+
|  Programmed by:     exitishere42                                  |
|  GitHub:            https://github.com/exitishere42               |
|  Repository:        https://github.com/exitishere42/JDMX          |
+-------------------------------------------------------------------+
```
