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

[Features](#features) • [Installation](#installation) • [Quick Start](#quick-start) • [Releases](https://github.com/exitishere42/JDMX/releases/latest)

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
  - **5 Primary Workspaces**: One-click switching between `Stage & Fixtures`, `Simple Desk`, `Button Matrix`, `Show-Timeline`, and `Input/Output`.
  - **Quick Blackout (`[● BLACKOUT]`)**: Instantly kills dimmer and shutter channels while locking moving head positions and colors in place.
  - **Emergency Release (`[■ STOP]`)**: Global kill switch to instantly stop all active faders, chasers, and cues.
  - **Live Telemetry**: Real-time DMX framerate counter (`40 DMX/s`) and bus monitor.
- **Multi-Protocol DMX Engine**:
  - Hardware-timed 40 Hz output loop preventing fixture flickering.
  - Standard Art-Net 4 network streaming (UDP `6454`).
  - Native USB-DMX serial support for FTDI OpenDMX and Enttec DMX USB Pro (`jSerialComm`).
  - Native MIDI controller support with interactive MIDI Learn (`javax.sound.midi`).
- **In-App Modal Overlays**: All dialogs, settings, and wizards render within the application window over a dark backdrop — no displaced OS popups or focus loss during live shows.
- **Lighting Suite Companion**: Seamless ecosystem partner to [**artnet2dmx**](https://github.com/exitishere42/artnet2dmx) (USB-DMX Bridge) and [**sound2artnet**](https://github.com/exitishere42/sound2artnet) (Sound-to-Light Engine).

---

## Architecture

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

## Quick Start (Development)

```bash
# Run with Maven
mvn exec:java

# Run tests
mvn clean test

# Build executable Fat JAR
mvn clean package
```

The standalone executable fat JAR will be generated at `target/jdmx-1.2.1-all.jar`.

---

## Author

```text
+-------------------------------------------------------------------+
|  Programmed by:     exitishere42                                  |
|  GitHub:            https://github.com/exitishere42               |
|  Repository:        https://github.com/exitishere42/JDMX          |
+-------------------------------------------------------------------+
```
