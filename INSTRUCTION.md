# Projektanweisung: jdmx (Java DMX Controller)

> **An den ausführenden Entwickler-Agenten:**
> Dieses Dokument ist deine verbindliche Spezifikation und Arbeitsanweisung.
> Arbeite die Roadmap strikt Phase für Phase ab.
> Führe nach jedem Schritt die definierten Verifikations-Befehle aus. Weiche nicht eigenmächtig von Tech-Stack, Paketstruktur oder Architektur ab.
> **Git-Regel:** Nach jedem großen Meilenstein / Phasenabschluss ist verbindlich ein aussagekräftiger Git-Commit und `git push` auf `origin main` durchzuführen.

---

## 1. Projektübersicht & Scope

- **Problem & Ziel:**  
  Ein hochperformantes, modulares DMX-512 Lichtsteuerungs-System in Java mit einer **direkt integrierten, modernen JavaFX Desktop-Oberfläche** (kein Browser, kein Javalin, keine Web-Server-Latenz). Das System verbindet eine reaktive DMX-Engine (512 Kanäle pro Universum, Art-Net / USB-DMX / Simulation) direkt über JavaFX-Properties und Event-Bindings mit einer professionellen Studio-Bedienoberfläche.

- **UI/UX Design-Philosophie (KaSo 2.0 Clean Dark Mode Ästhetik):**
  - **Flache Volltonfarben:** Belegte Kacheln und aktive Elemente besitzen satte, kontrastreiche Volltonfarben (Gelb, Grün, Rot, Amber, Cyan) ohne künstliche Verläufe, Neonglows oder unscharfe Schlagschatten.
  - **Gestrichelte leere Kacheln:** Unbelegte Slots in Matrizen sind dezent gestrichelt (`1.5px dashed`) mit abgerundeten Ecken (`border-radius: 8px`).
  - **Pill-Badges:** Elegante, abgerundete Kapsel-Badges für Statuswerte (BPM, DMX-Adresse, Universe-Status).
  - **Flash-Funktion:** Flash-Pads sind oben links mit einem klaren Lucide-Stern (`star`) gekennzeichnet.
  - **Spatial Grid:** Konsistentes 8pt Raster (`4px`, `8px`, `10px`, `12px`, `16px`).
  - **100% Lucide-Icons:** Ausschließliche Vektordarstellung via JavaFX `SVGPath`. **Absolutes Emoji-Verbot** in der gesamten Benutzeroberfläche.

- **Struktur der 5 Arbeitsbereiche (Workspaces / Tabs):**
  1. **2D Stage & Fixtures:** CAD-Rigging-Blaupause der Traverse und Bühne mit Echtzeit-Vektor-Beams, Fixture-Inspektor (Color-Picker, Dimmer, Pan/Tilt XY-Pad mit Invertierung/Begrenzer).
  2. **Fader & Universum:** 512-Kanal Universe-Heatmap (High-Performance JavaFX Canvas) und klassische Kanalzüge mit Fader, Wertanzeige (0–255 / %) und Flash-Buttons.
  3. **Live Matrix (3×16 Pad Desk):** 48 Pads pro Bank (Seiten 1–4, insgesamt 192 Trigger), scrollfrei optimiert, mit linker Edit-Sidebar (Flash-Tool, Toggle-Tool, Farbpalette, Momentary/Latching).
  4. **Timeline & Music Sync:** Audio-Waveform Visualisierung, BPM-Tapper / Timecode-Zähler und parallele DMX-Lichtspuren zur musiksynchronen Ablaufsteuerung.
  5. **Input / Output & Diagnostics:** Art-Net Broadcast / Unicast Konfiguration, USB-DMX Treiber (OpenDMX/FTDI), MIDI-Controller Mapping und DMX-Bus-Traffic Oszilloskop/Monitor.

- **DMX Engine Core:**
  - 512-Kanal-Frame-Buffer pro Universum (Standard: Universum 1).
  - Grand Master Fader (0–100%) mit linearer Skalierung (`effektiv = (raw * master) / 255`).
  - Sofortiger Blackout-Toggle (setzt Ausgabewerte auf 0, behält interne Sollwerte im Speicher).
  - HTP (Highest Takes Precedence) und LTP (Latest Takes Precedence) Merging für Szenenüberblendungen.
  - Modulares Treiber-Interface (`DmxOutputDriver`) mit:
    - `VirtualSimulationDriver` (Standard für Tests und Headless-Betrieb).
    - `ArtNetDriver` (Standard UDP Broadcast/Unicast auf Port 6454).
    - Optional: Erweiterbar für FTDI / USB-DMX.

- **Persistenz (H2 & SQL-Ready):**
  - Lokale H2-Datenbank (`./data/jdmx_db.mv.db`) über HikariCP Connection Pool und ANSI-SQL Repositories.
  - Speichert Fixtures, 3×16 Pad-Belegungen, Szenen, Cues und globale Einstellungen.

- **In-Scope (MVP v1.0):**
  - Vollständiges JavaFX 21 UI mit allen 5 Tabs im KaSo 2.0 Dark-Theme.
  - DMX Universe Engine mit Grand Master, Blackout und Fader-Bedienung.
  - 3×16 Live Matrix mit Pad-Triggerung, Flash/Toggle-Modus und Farbcodierung.
  - 512-Kanal Canvas-Heatmap mit 60 FPS flüssiger Darstellung.
  - Art-Net & Simulations-Treiber.
  - H2-Datenbankpersistenz.
  - Standalone Fat-JAR (per Doppelklick ausführbar).

- **Out-of-Scope:**
  - Keine Webbrowser-Anbindung, kein Javalin, keine REST-API, kein SSE.
  - Keine komplexen 3D-Meshes/CAD-Imports (reine 2D-Vektor-Bühne genügt für v1.0).

---

## 2. Tech-Stack & Feste Leitplanken

- **Runtime & OS:** Java 21 LTS, Windows (plattformunabhängig lauffähig).
- **GUI-Framework:** **OpenJFX 21 LTS** (`javafx-controls`, `javafx-graphics`, `javafx-base`, `javafx-media`).
- **Build-Tool:** **Apache Maven** (`pom.xml`) – Strikte Pflicht laut Arbeitsrichtlinie.
- **Serialization & JSON:** **Jackson 2.18+** (`jackson-databind`, `jackson-datatype-jsr310`).
- **Datenbank & Persistenz:** **H2 Database 2.3+** mit **HikariCP 5.1+** Connection Pool und sauberem JDBC-Repository-Layer.
- **Logging:** **SLF4J 2.0+** mit `slf4j-simple`.
- **Testing:** **JUnit 5 (Jupiter)** + Mockito.

### Java-Spezifikation (Strikter Standard)
- **GroupId:** `de.exit`
- **ArtifactId:** `jdmx`
- **Base Package:** `de.exit.jdmx`
- **Verzeichnisstruktur:** Alle Java-Klassen liegen zwingend unter `src/main/java/de/exit/jdmx/...`.
- **Maven-Plugins:**
  - `maven-compiler-plugin` (Release 21)
  - `javafx-maven-plugin` (Start via `mvn javafx:run` mit Main-Klasse `de.exit.jdmx.App`)
  - `exec-maven-plugin` (Start via `mvn exec:java` mit Launcher `de.exit.jdmx.Main`)
  - `maven-jar-plugin` & `maven-shade-plugin` (Fat-JAR für Doppelklick-Ausführung)

### Verbotene Muster & Anti-Halluzinations-Regeln
- ❌ **Kein Web-Stack / Javalin:** Kein Webserver, kein REST, kein SSE, kein HTTP. Reines Desktop-JavaFX!
- ❌ **Absolutes Emoji-Verbot:** Keine Emojis im gesamten UI (Buttons, Labels, Tabs, Logs). Ausschließlich Lucide-Icons!
- ❌ **Kein Gradle:** Ausschließlich Apache Maven mit sauberer `pom.xml`.
- ❌ **Keine blockierenden I/O-Aufrufe im JavaFX Application Thread:** DMX-Ausgabe-Loop und Datenbankzugriffe laufen in separaten Worker-Threads / Virtuellen Threads. UI-Updates erfolgen strikt über `Platform.runLater()`.
- ❌ **Keine UI-Glows / Unschärfen:** KaSo 2.0 Design erfordert scharfe Kanten, flache Volltonfarben und dezent gestrichelte leere Kacheln.

---

## 3. Architektur & Dateistruktur

```text
jdmx/
├── pom.xml
├── .gitignore
├── README.md
├── INSTRUCTION.md
├── data/                                 # Lokales Verzeichnis für H2-Datenbank (*.mv.db)
└── src/
    ├── main/
    │   ├── java/
    │   │   └── de/
    │   │       └── exit/
    │   │           └── jdmx/
    │   │               ├── Main.java                         # Fat-JAR Launcher (ohne extends Application)
    │   │               ├── App.java                          # JavaFX Application Einstiegspunkt
    │   │               ├── config/
    │   │               │   ├── AppConfig.java                # Pfade, Fenstermaße, DMX-Frequenz, Treiberwahl
    │   │               │   └── DatabaseConfig.java           # HikariCP DataSource & Connection Pooling
    │   │               ├── core/
    │   │               │   ├── DmxUniverse.java              # 512-Kanal Frame Buffer, Master, Blackout
    │   │               │   ├── DmxEngine.java                # Ticker/Scheduler (30–44 Hz DMX Frame Loop)
    │   │               │   └── driver/
    │   │               │       ├── DmxOutputDriver.java      # Interface: sendFrame(int universe, byte[] data)
    │   │               │       ├── VirtualSimulationDriver.java # Logging & In-Memory Visualisierung
    │   │               │       └── ArtNetDriver.java         # UDP Art-Net DMX Paket-Sender
    │   │               ├── model/
    │   │               │   ├── Fixture.java                  # Record: id, name, type, universe, startAddress, channels
    │   │               │   ├── FixtureType.java              # Enum: DIMMER, RGB, RGBW, MOVING_HEAD
    │   │               │   ├── MatrixPad.java                # Record: padIndex, page, label, color, mode (FLASH/TOGGLE), sceneId
    │   │               │   ├── Scene.java                    # Record: id, name, universe, channelValues, fadeTimeMs
    │   │               │   └── PadMode.java                  # Enum: FLASH (Momentary), TOGGLE (Latching)
    │   │               ├── repository/
    │   │               │   ├── FixtureRepository.java        # Interface für Fixture-CRUD
    │   │               │   ├── MatrixPadRepository.java      # Interface für Pad-Desk-Persistenz
    │   │               │   ├── SceneRepository.java          # Interface für Szenen-CRUD
    │   │               │   └── jdbc/
    │   │               │       ├── JdbcFixtureRepository.java# ANSI-SQL H2 Implementierung
    │   │               │       ├── JdbcMatrixPadRepository.java
    │   │               │       ├── JdbcSceneRepository.java
    │   │               │       └── DatabaseInitializer.java  # Automatische Tabellen-Erstellung (DDL)
    │   │               ├── service/
    │   │               │   ├── DmxService.java               # DMX Geschäftslogik: Fader, Master, Blackout
    │   │               │   ├── FixtureService.java           # Adresskollisionen & Profilverwaltung
    │   │               │   └── MatrixService.java            # Pad-Triggering & Umschalten von Szenen
    │   │               └── ui/
    │   │                   ├── MainView.java                 # Root BorderPane mit Top-Header & Workspace-Tabs
    │   │                   ├── HeaderBar.java                # Grand Master, Blackout, Universe Status & BPM
    │   │                   ├── icons/
    │   │                   │   └── LucideIcon.java           # Vektorbasiertes Lucide SVG-Renderer Modul
    │   │                   └── tabs/
    │   │                       ├── StageTab.java             # Tab 1: 2D Stage Rigging & Fixture Inspector
    │   │                       ├── FaderUniverseTab.java     # Tab 2: 512 Canvas Heatmap & Fader-Desk
    │   │                       ├── LiveMatrixTab.java        # Tab 3: 3×16 Pad Desk mit Edit-Sidebar
    │   │                       ├── TimelineTab.java          # Tab 4: Audio Waveform & DMX Timeline
    │   │                       └── DiagnosticsTab.java       # Tab 5: Art-Net Setup & Traffic Oszilloskop
    │   └── resources/
    │       └── de/
    │           └── exit/
    │               └── jdmx/
    │                   └── styles/
    │                       ├── dark-theme.css                # KaSo 2.0 Clean Dark Mode Tokens
    │                       └── components.css                # Fader, Matrix-Pads, Badges, Sliders
    └── test/
        └── java/
            └── de/
                └── exit/
                    └── jdmx/
                        ├── core/
                        │   ├── DmxUniverseTest.java
                        │   └── ArtNetDriverTest.java
                        ├── service/
                        │   ├── DmxServiceTest.java
                        │   └── FixtureServiceTest.java
                        └── repository/
                            └── JdbcFixtureRepositoryTest.java
```

---

## 4. Datenmodell & Schemas

### 4.1 Java Records

#### `Fixture.java`
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Fixture(
    String id,
    String name,
    int universe,
    int startAddress,       // 1 bis 512
    FixtureType type,
    int channelCount
) {
    public boolean overlapsWith(Fixture other) {
        if (this.universe != other.universe) return false;
        int thisEnd = this.startAddress + this.channelCount - 1;
        int otherEnd = other.startAddress + other.channelCount - 1;
        return this.startAddress <= otherEnd && other.startAddress <= thisEnd;
    }
}
```

#### `MatrixPad.java`
```java
package de.exit.jdmx.model;

public record MatrixPad(
    int page,           // 1 bis 4
    int padIndex,       // 0 bis 47 (3 Reihen x 16 Spalten)
    String label,       // z. B. "Strobe White" oder leer
    String colorHex,    // Volltonfarbe z. B. "#eab308" (Amber), null wenn leer
    PadMode mode,       // FLASH (tastend) oder TOGGLE (rastend)
    String sceneId      // Verknüpfte Szene oder null
) {
    public boolean isEmpty() {
        return colorHex == null || colorHex.isBlank();
    }
}
```

---

## 5. Detaillierte Arbeitsbereiche mit "Definition of Done" (DoD)

### Workspace 1: 2D Stage & Fixtures
- **Beschreibung:**  
  Visuelle 2D-Rigging-Ansicht der Traverse und Scheinwerfer mit Vektor-Lichtkegeln (Beams), die Live-Helligkeit und Farbe widerspiegeln.
  Rechter Quick-Inspector: Color-Picker (RGB), Dimmer-Slider, Pan/Tilt XY-Pad mit Invertierungs-Schaltern.
- **Definition of Done:**  
  - Scheinwerfer anwählen ➔ Pan/Tilt XY-Pad bewegen ➔ Scheinwerfer-Beam dreht sich im 2D-View; DMX-Kanäle für Pan/Tilt aktualisieren sich sofort.

### Workspace 2: Fader & Universum
- **Beschreibung:**  
  - Oben: 512-Kanal Universe-Heatmap auf einem JavaFX Canvas (32×16 Raster). Jeder Kanal zeigt Helligkeitswert (0–255) über Farbintensität. Klick auf eine Kachel wählt den Kanal aus.
  - Unten: Fader-Bank für konfigurierte Scheinwerfer oder freie DMX-Kanäle mit Schiebereglern und taktilen Flash-Buttons.
- **Definition of Done:**  
  - Fader ziehen ➔ Canvas-Kachel ändert Helligkeit in Echtzeit (60 FPS); DMX-Engine sendet neuen Wert.

### Workspace 3: Live Matrix (3×16 Pad Desk)
- **Beschreibung:**  
  - 48 Pads (3 Zeilen à 16 Spalten) ohne Scrollbalken.
  - Bank-Umschaltung: Seiten 1, 2, 3, 4 oben im Workspace.
  - Gestrichelte leere Kacheln (`1.5px dashed`), belegte Kacheln mit satten Volltonfarben.
  - Flash-Pads tragen oben links das Lucide-Icon `STAR`.
  - Linke Sidebar: Edit-Werkzeuge (Toggle-Modus, Flash-Modus, Farbwähler, Szene zuweisen).
- **Definition of Done:**  
  - Mausklick auf Flash-Pad ➔ Pad leuchtet auf, Szene wird getriggert; Loslassen ➔ Szene schaltet sofort ab.
  - Klick auf Toggle-Pad ➔ Szene rastet ein und bleibt aktiv, bis erneut geklickt wird.

### Workspace 4: Timeline & Music Sync
- **Beschreibung:**  
  Audio-Player mit Waveform-Visualisierung, BPM-Tapper, Timecode-Anzeige (MM:SS.mmm) und parallelen DMX-Event-Spuren für musiksynchrone Lichtshows.
- **Definition of Done:**  
  - Audio laden ➔ Play drücken ➔ Abspiel-Cursor fährt über die Waveform; Marker triggern definierte DMX-Szenen.

### Workspace 5: Input / Output & Diagnostics
- **Beschreibung:**  
  Art-Net Konfiguration (IP-Adresse, Subnetz, Universum), Umschalter auf Virtual Simulation, Bus-Traffic-Anzeige (Pakete/Sekunde, FPS-Zähler).
- **Definition of Done:**  
  - Art-Net aktivieren mit Ziel `127.0.0.1` ➔ Diagnostics-Tab zeigt aktiven UDP-Traffic mit 30–44 Hz.

---

## 6. Phasenweise Implementierungs-Roadmap

### Phase 1: Maven-Setup & Basis JavaFX App
- [ ] Saubere `pom.xml` mit JavaFX 21 (`controls`, `graphics`, `media`), Jackson, HikariCP, H2, SLF4J, JUnit 5.
- [ ] Plugins konfigurieren: `javafx-maven-plugin`, `exec-maven-plugin`, `maven-jar-plugin`, `maven-shade-plugin`.
- [ ] `Main.java` (Fat-JAR Launcher) und `App.java` (JavaFX Application).
- [ ] Modernes KaSo 2.0 Dark-Theme Basis-Stylesheet (`dark-theme.css`).
- [ ] `LucideIcon.java`: Vektorbasiertes Lucide SVG-Path System für JavaFX.
- [ ] **Verifikation:** `mvn clean compile` und `mvn javafx:run` öffnet das Dark-Theme Fenster mit Lucide-Icons.

### Phase 2: DMX Core Engine & Treiber
- [ ] `DmxUniverse.java`: 512-Kanal Buffer mit Master & Blackout Logik.
- [ ] `DmxOutputDriver.java`, `VirtualSimulationDriver.java`, `ArtNetDriver.java`.
- [ ] `DmxEngine.java`: Thread-sicherer Frame-Loop (30–44 Hz) entkoppelt vom JavaFX-Thread.
- [ ] **Verifikation:** Unit-Tests `DmxUniverseTest` und `ArtNetDriverTest` erfolgreich via `mvn test`.

### Phase 3: H2 Persistenz-Schicht
- [ ] `DatabaseConfig.java` & `DatabaseInitializer.java`.
- [ ] ANSI-SQL Repositories für `Fixture`, `MatrixPad` (3×16) und `Scene`.
- [ ] **Verifikation:** Integrationstests speichern und laden Pads/Fixtures aus H2 via `mvn test`.

### Phase 4: Hauptlayout & Header-Controls
- [ ] `MainView.java`: Root-Layout mit KaSo Tab-Navigation für die 5 Workspaces.
- [ ] `HeaderBar.java`: Grand Master Fader, roter Blackout-Knopf, Universe-Status-Pills, BPM-Tapper.
- [ ] Verknüpfung Header ➔ `DmxService`.
- [ ] **Verifikation:** Grand Master & Blackout beeinflussen die DMX-Ausgabe direkt; optisches Feedback im Header.

### Phase 5: 3×16 Live Matrix & Fader/Universe-Tabs
- [ ] `LiveMatrixTab.java`: 3 Reihen × 16 Spalten (48 Pads), Seiten 1–4, KaSo gestrichelte Kacheln / Volltonfarben, Flash-Stern-Icon.
- [ ] `FaderUniverseTab.java`: 512-Kanal Canvas-Heatmap mit 60 FPS und Fader-Züge mit Flash-Buttons.
- [ ] `StageTab.java`: 2D Stage Blaupause und Quick-Inspector mit Pan/Tilt XY-Pad.
- [ ] **Verifikation:** Klick- und Flash-Interaktionen auf Pads und Fader steuern DMX-Werte fehlerfrei.

### Phase 6: Polish, Diagnostics & Fat-JAR Build
- [ ] `DiagnosticsTab.java`: Art-Net Network-Monitor & Oszilloskop.
- [ ] `TimelineTab.java`: Basis Audio-Waveform & Timecode.
- [ ] Ausführbare Standalone Fat-JAR bauen und testen.
- [ ] `README.md` aktualisieren.
- [ ] **Verifikation:** `mvn clean package` erzeugt lauffähiges JAR; Start via `java -jar target/jdmx-1.0.0-SNAPSHOT.jar`.

---

## 7. Start- und Testbefehle

- **Projekt kompilieren:**  
  `mvn clean compile`
- **Tests ausführen:**  
  `mvn test`
- **JavaFX Anwendung im Dev-Modus starten:**  
  `mvn javafx:run`
- **Alternativer Start via Exec-Plugin:**  
  `mvn exec:java`
- **Ausführbares Standalone Fat-JAR bauen:**  
  `mvn clean package`
- **Fat-JAR ausführen:**  
  `java -jar target/jdmx-1.0.0-SNAPSHOT.jar`
