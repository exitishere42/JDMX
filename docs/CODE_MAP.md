# JDMX Code-Landkarte & Datei-Schnellfinder

Dieser Leitfaden ist dein praktischer Reiseführer durch alle 95 Java-Klassen von JDMX.  
Wenn du etwas suchst oder ändern möchtest, schlage einfach hier nach!

---

## 1. Der 10-Sekunden Schnell-Finder („Wo finde ich was?“)

| Was möchtest du tun / ändern? | Zu bearbeitende Datei(en) |
| :--- | :--- |
| **Show-Timeline:** Leertaste, Play/Pause, Stopp-Buttons | [`de.exit.jdmx.ui.timeline.TimelineMainView`](../src/main/java/de/exit/jdmx/ui/timeline/TimelineMainView.java) |
| **Show-Timeline:** Wellenform zeichnen, Taktraster, Playhead-Linie, Clips verschieben | [`de.exit.jdmx.ui.timeline.TimelineGridView`](../src/main/java/de/exit/jdmx/ui/timeline/TimelineGridView.java) |
| **Show-Timeline:** Spuren (Mute, Name, Spuren hinzufügen) | [`de.exit.jdmx.ui.timeline.TimelineTrackHeaderBox`](../src/main/java/de/exit/jdmx/ui/timeline/TimelineTrackHeaderBox.java) & [`TimelineService`](../src/main/java/de/exit/jdmx/service/TimelineService.java) |
| **Show-Timeline:** Song-Dateien importieren & Explorer-Baum | [`de.exit.jdmx.ui.timeline.ShowTimelineExplorerView`](../src/main/java/de/exit/jdmx/ui/timeline/ShowTimelineExplorerView.java) & [`TimelineDragHelper`](../src/main/java/de/exit/jdmx/ui/timeline/TimelineDragHelper.java) |
| **Audio-Engine:** MP3/WAV-Wiedergabe, Lautstärke, Multitrack-Playback, Seeking | [`de.exit.jdmx.service.AudioPlaybackService`](../src/main/java/de/exit/jdmx/service/AudioPlaybackService.java) |
| **Audio-Analyse:** MP3-Wellenform berechnen, Transienten, Peaks | [`de.exit.jdmx.service.AudioAnalysisService`](../src/main/java/de/exit/jdmx/service/AudioAnalysisService.java) |
| **DMX-Ausgabe:** Art-Net UDP-Pakete ins Netzwerk senden (Port 6454) | [`de.exit.jdmx.core.artnet.ArtNetSender`](../src/main/java/de/exit/jdmx/core/artnet/ArtNetSender.java) & [`ArtNetPacketBuilder`](../src/main/java/de/exit/jdmx/core/artnet/ArtNetPacketBuilder.java) |
| **DMX-Universum:** 512-Kanal Puffer, Blackout, HTP/LTP-Merging, 16-Bit Pan/Tilt | [`de.exit.jdmx.core.dmx.DmxUniverse`](../src/main/java/de/exit/jdmx/core/dmx/DmxUniverse.java) |
| **Bühnen-Ansicht (Tab 1):** 2D U-Traverse, Beams, Moving-Head-Lichtkegel | [`de.exit.jdmx.ui.stage.StageCanvasView`](../src/main/java/de/exit/jdmx/ui/stage/StageCanvasView.java) & [`StageFixtureNode`](../src/main/java/de/exit/jdmx/ui/stage/StageFixtureNode.java) |
| **Steuerungsdock (Tab 1):** Farbrad, 2D-XY-Pad für Pan/Tilt, Intensität | [`de.exit.jdmx.ui.stage.DeviceControlDock`](../src/main/java/de/exit/jdmx/ui/stage/DeviceControlDock.java) |
| **LightDesk (Tab 2):** 512-Kanal Fader-Pult, Schieberegler, Flash-Buttons | [`de.exit.jdmx.ui.desk.LightDeskWorkspaceView`](../src/main/java/de/exit/jdmx/ui/desk/LightDeskWorkspaceView.java) & [`CanvasDeskView`](../src/main/java/de/exit/jdmx/ui/desk/CanvasDeskView.java) |
| **Kopfleiste & Workspace-Tabs:** Grand Master, Blackout, Tab-Umschaltung | [`de.exit.jdmx.ui.HeaderBar`](../src/main/java/de/exit/jdmx/ui/HeaderBar.java) & [`MainWindow`](../src/main/java/de/exit/jdmx/ui/MainWindow.java) |
| **Geräte-Bibliothek & Patchen:** QLC+ (.qxf) / GDTF Scheinwerfer-Profile | [`de.exit.jdmx.service.FixtureLibraryService`](../src/main/java/de/exit/jdmx/service/FixtureLibraryService.java) & [`FixturePatchDialog`](../src/main/java/de/exit/jdmx/ui/dialog/FixturePatchDialog.java) |
| **Projekt speichern & laden:** H2-Datenbank, Show-Dateien | [`de.exit.jdmx.service.ProjectStorageService`](../src/main/java/de/exit/jdmx/service/ProjectStorageService.java) |
| **Update-System:** Versionsprüfung auf GitHub & Auto-Update | [`de.exit.jdmx.service.UpdateService`](../src/main/java/de/exit/jdmx/service/UpdateService.java) & [`AboutUpdateDialog`](../src/main/java/de/exit/jdmx/ui/dialog/AboutUpdateDialog.java) |
| **Icons & Vektoren:** Lucide Vektor-Pfade (farbige Icons, SVG) | [`de.exit.jdmx.ui.icon.LucideIcon`](../src/main/java/de/exit/jdmx/ui/icon/LucideIcon.java) |

---

## 2. Vollständiger Klassen-Index nach Paketen

### 🔹 `de.exit.jdmx` (Haupteinstieg)
* **`JdmxApplication.java`**: Startet das JavaFX-Toolkit, initialisiert Singletons/Services, lädt das Hauptfenster und fängt ungefangene Exceptions ab.
* **`Main.java`**: Robuster Launcher-Wrapper, um JavaFX ohne Modul-Konflikte aus dem Fat-JAR starten zu können.

### 🔹 `de.exit.jdmx.core.*` (DMX-Engine, Art-Net, Logging & Datenmodelle)
* **`core.artnet.ArtNetSender`**: Öffnet einen UDP-Socket und sendet 512-Byte-Frames an IP-Adressen (Broadcast/Unicast).
* **`core.artnet.ArtNetPacketBuilder`**: Baut binäre Art-Net-DMX-Pakete nach der Spezifikation (Header, OpCode `0x5000`, Version 14).
* **`core.dmx.DmxUniverse`**: Das DMX-Universum im Speicher. Speichert die Werte 0–255 für 512 Kanäle, wendet HTP/LTP-Merging an und berechnet 16-Bit Pan/Tilt.
* **`core.fixture.parser.QxfParser`**: Liest QLC+ Fixture-XML-Dateien (`.qxf`) ein und erzeugt Java-Objekte daraus.
* **`core.fixture.parser.GdtfParser`**: Liest moderne GDTF-Archive (`.gdtf` / Zip mit XML) ein.
* **`core.log.AppLogger`**: Plattformunabhängiges Logging in die Konsole und in `logs/jdmx.log`.
* **`core.model.*`**:
  * `TimelineTrack`: Datenklasse für eine Timeline-Spur (Name, Song-Datei, Startzeit `startTimeMs`, Mute).
  * `AudioAnalysisResult`: Speichert Gesamtlaufzeit und 1000 Hüllkurven-Peaks einer MP3/WAV.
  * `FixtureDefinition` & `FixtureInstance`: Beschreibt einen Scheinwerfertyp und seine gepatchte Instanz (Adresse, Universum).
  * `ChannelDefinition` & `ChannelType`: Enums für Kanaltypen (Dimmer, Pan, Tilt, Red, Green, Blue, Strobe, Gobo).

### 🔹 `de.exit.jdmx.service` (Die Logik-Zentrale)
* **`TimelineService`**: Taktgeber der Timeline (25ms Intervall), Spuren-Verwaltung, Playback-Status, Seeking und Song-Import.
* **`AudioPlaybackService`**: Multi-Voice Audio-Engine. Verwaltet parallele Audiokanäle für Spuren, JLayer MP3-Seeking und Live-Resync.
* **`AudioAnalysisService`**: Asynchrone Hintergrundberechnung der Wellenform-Peaks (0 ms UI-Lags).
* **`DmxService`**: Zentrale Schnittstelle für alle DMX-Änderungen aus der UI zum DmxUniverse.
* **`FixtureLibraryService`**: Durchsucht den Systemordner nach Scheinwerferprofilen und verwaltet die Geräteliste.
* **`ProjectStorageService`**: Persistiert Shows in H2-Dateien und lädt sie wiederherstellend ein.
* **`UpdateService`**: Fragt GitHub Releases ab, vergleicht SemVer und stößt In-App-Updates an.

### 🔹 `de.exit.jdmx.ui.timeline` (Show-Timeline & FL Studio Playlist)
* **`TimelineMainView`**: Oberer Transport-Header (Play, Pause, Stop, Leertaste-Handling, Zeitanzeige `mm:ss.SSS`).
* **`TimelineGridView`**: Das interaktive Canvas-Raster (Zeittakt-Lineal, Playhead-Dreieck, Song-Clips mit Wellenform, horizontales Verschieben per Drag & Drop).
* **`TimelineTrackHeaderBox`**: Linke Spurenleiste (Spur-Boxen mit Mute-LED, Inline-Umbenennung per Doppelklick, `[+]`-Track-Button).
* **`ShowTimelineExplorerView`**: Rechte Seitenleiste mit Song-Explorer, Drag & Drop-Quelle und Datei-Import.
* **`TimelineDragHelper`**: Erkennt plattformübergreifend externe Audiodateien (Windows Explorer, GNOME Nautilus `text/uri-list`).

### 🔹 `de.exit.jdmx.ui.stage` (Tab 1: 2D Bühne & Steuerungsdock)
* **`StageWorkspaceView`**: Hauptcontainer für Tab 1.
* **`StageCanvasView`**: 2D-Bühnen-Canvas mit U-Traverse, Projektionsleinwand und farbigen Lichtkegeln.
* **`DeviceControlDock`**: Unteres 220px-Dock mit `IntensityModuleView` (Dimmer), `ColorModuleView` (Farbrad) und `PositionModuleView` (2D-XY-Pad).
* **`ProjectExplorerView`**: Rechte Seitenleiste für Geräte, Gruppen, Presets und Szenen.

### 🔹 `de.exit.jdmx.ui.desk` (Tab 2: LightDesk)
* **`LightDeskWorkspaceView`**: Virtuelles Lichtmischpult.
* **`CanvasDeskView`**: Ultra-schnelles 512-Kanal Fader-Raster auf einem einzigen Canvas (Zero-Lag).
* **`DeskScrollBar`**: Eigener taktiler Schieberegler für geschmeidiges horizontales Scrollen durch alle 512 Kanäle.

### 🔹 `de.exit.jdmx.ui.icon` (Vektorgrafiken)
* **`LucideIcon`**: Rendert saubere SVG-Vektorpfade (Play, Stop, Mute, Volume, DMX, Settings etc.) in beliebigen Farben ohne Emojis.
