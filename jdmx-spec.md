# Spezifikation: jdmx (Java DMX Controller)

> **Verbindliche Master-Spezifikation für die Software-Entwicklung**  
> Basierend auf den Anforderungen für den Vereins- und Bühnenbetrieb mit statischem Licht (Dimmer/PARs) und bewegtem Licht (Moving Heads, Scanner).  
> Konzeptionell angelehnt an die bewährte Trennung von QLC+ (Fader Desk, 2D Stage, Virtual Console & I/O Matrix).

---

## 1. Vision & Einsatzszenario

- **Ziel:**  
  Ein modularer, latenzarmer DMX-512 Controller in Java 21 LTS mit integriertem Web-Frontend (Javalin 6).
- **Kontext:**  
  Ausstattung für Vereinsräume, Säle und Bühnen. Typisches Rigging: **U-Traverse, die auf eine Wand gerichtet ist**, bestückt mit statischen Scheinwerfern und Moving Lights.
- **Benutzergruppen:**  
  - *Lichttechniker:* Detailkontrolle über Fader, 16-Bit Pan/Tilt, DMX-Adressierung und Fixture-Profile.
  - *Vereinsmitglieder / DJs / Theke:* Einfache Live-Bedienung über ein 3×25-Button-Raster (Cues, Farben, Intensitäten, Chaser) via Browser/Tablet.

---

## 2. Die 5 Haupt-Tabs & Permanenter Global-Header

Die Web-Anwendung (`src/main/resources/public/`) besitzt einen **permanent über allen Ansichten sichtbaren Global-Header** sowie fünf dedizierte Tabs:

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ [ jdmx ]  Grand Master: [████████░░] 80%  [● BLACKOUT]  [■ STOP ALL]  [Traffic: ▄▆█ 40Hz]               │
├─────────────────┬───────────────────┬──────────────────────┬──────────────────────┬─────────────────────┤
│ [1] FADER       │ [2] 2D STAGE      │ [3] LIVE MATRIX      │ [4] TIMELINE         │ [5] INPUT / OUTPUT  │
│     & UNIVERSE  │     & FIXTURES    │     (3x25 CONSOLE)   │     (AUDIO & SHOW)   │     (ARTNET/MIDI)   │
└─────────────────┴───────────────────┴──────────────────────┴──────────────────────┴─────────────────────┘
```

### Permanenter Global-Header (Immer verfügbar)
1. **Grand Master Fader:** Skaliert lineare Gesamthelligkeit (0–100%) ausschließlich für Dimmer/Shutter.
2. **Globaler Blackout-Button (`● BLACKOUT`):** Schaltet sofort alle Dimmer/Shutter aus (Pan/Tilt bleibt unangetastet).
3. **Globaler Haupt-Stop-Button (`■ STOP ALL / RELEASE ALL`):**
   - **Das Anti-QLC+-Feature:** Im Gegensatz zu QLC+ (wo Fader Desk, 2D-Monitor und Virtual Console jeweils separate, versteckte Override- und Stop-Buttons haben) stoppt dieser eine Button **sofort alles**:
     - Beendet alle aktiven Chaser, FX-Engines, Fades und Szenen.
     - Deaktiviert alle aktiven Tasten in der 3×25 Live-Matrix.
     - Setzt alle manuellen Fader-Overrides zurück.
     - Bringt das gesamte Universum in einen definierten Ruhezustand (keine "feststeckenden" Kanäle mehr).
4. **DMX-Traffic Live-Balkendiagramm (wie in DMXControl):**
   - Permanente Miniaturanzeige im Header und Detailanzeige in Tab 5.
   - Zeigt in Echtzeit als Balken:
     - **Frame-Rate (Hz / FPS):** Z. B. 30 Hz – 44 Hz Soll/Ist.
     - **DMX-Bus-Traffic & Aktivität:** Wieviele Kanäle ändern sich gerade, Leitungs-Durchsatz und Paket-Verkehr.
   - Farbcodiert: Grün (optimaler DMX-Fluss, 30–44 Hz), Gelb (erhöhte Latenz), Rot (Buffer-Drop/Stillstand).

---

### Tab 1: Fader Desk & Universum (Kanal-Ebene)
- **Zweck:** Diagnostik, Einleuchten, Testen und manuelles Eingreifen auf DMX-Kanal-Ebene.
- **Funktionen:**
  - **512-Kanal Fader-Raster:** Vertikale Schieberegler mit Kanalnummer, DMX-Wert (0–255) und Prozentwert (0–100%).
  - **Fixture-Gruppierung:** Kanäle können nach Geräten gruppiert ein- und ausgeklappt werden (z. B. *„Moving Head 1 [CH 1–16]“* mit Kanalbezeichnungen: *Pan, Pan Fine, Tilt, Tilt Fine, Dimmer, Color, Gobo...*).
  - **Flash-Buttons:** Für Dimmerkanäle (tastendes Setzen auf 255).
  - **High-Performance Canvas-Universe-Grid:** Live-Kompaktanzeige aller 512 Kanäle als 16x32-Raster mit Helligkeits-Visualisierung.

### Tab 2: 2D Stage & Fixture-Patch (U-Traverse & Wand)
- **Zweck:** Räumliche Anordnung, Adressierung und intuitive Ausrichtung von Moving Lights.
- **Funktionen:**
  - **2D-Bühnenplan:**
    - Schematische Abbildung des Raums: Konfigurierbare **U-Traverse** (Linker Schenkel, Querseite, Rechter Schenkel) und gegenüberliegende **Projektionswand**.
    - Platzierung von Fixtures via Drag & Drop oder Koordinaten.
    - Visuelle Lichtkegel (Beams), die in Echtzeit Farbton, Dimmer-Status und Schwenkwinkel (Pan/Tilt) an die Wand werfen.
  - **Fixture-Verwaltung:**
    - Anlegen/Bearbeiten von Geräten (Dimmer, RGB, RGBW, Moving Head 16-Bit, Custom).
    - Adressbelegungs-Balken (1–512) mit visueller Kollisionswarnung.
  - **Quick-Inspector (Klick auf ein Fixture):**
    - **2D-XY-Pad:** Intuitive Maus- oder Touch-Steuerung für Pan/Tilt.
    - **Invertierung:** Checkboxen für *Pan invertieren* und *Tilt invertieren* (essenziell für gespiegelte Köpfe an den beiden U-Schenkeln).
    - **Pan/Tilt Limits:** Min/Max-Begrenzer (Software-Limiter), damit Moving Heads den gewünschten Wandbereich nicht verlassen und nicht in Decke oder Publikum strahlen.
    - **RGBW-Farbwähler & Schnell-Dimmer / Shutter**.

### Tab 3: Live Matrix / Virtual Console (3 × 25 Button-Desk)
- **Zweck:** Das intuitive Live-Bedienfeld für den Showabend – optimiert für PC, Touch-Tablets und externe MIDI-Hardware.
- **Philosophie:** Komplett offenes, modulares System ohne starre Pflichtvorlagen. Der Lichtoperator belegt sich die Knöpfe und Fader selbst passend zum Setup (z. B. Reihe 1 für Moving Head Bewegung/Positionen, Reihe 2 für Farb-Overrides, Reihe 3 für Effekte, Strobe & Chaser).
- **Layout:** 3 Reihen mit je 25 Spalten = 75 frei konfigurierbare Slots.
- **Button-Typen:**
  1. **Full Cue (Vollszene):** Ruft eine komplette Lichtstimmung ab.
  2. **Selective / Partial Cues (Teilszenen mit Attribut-Maskierung):**
     - **Color-Only:** Ändert ausschließlich Farben (z. B. *„Rot“*, *„Blau“*, *„Warmweiß“*) – Position und Helligkeit bleiben unberührt.
     - **Intensity-Only:** Ändert nur die Helligkeit (z. B. *„100%“*, *„50%“*, *„Blinder Flash“*).
     - **Position-Only:** Fährt nur Pan/Tilt an – Farben und Dimmer bleiben unverändert.
  3. **Chaser & FX:** Startet zyklische Abläufe (z. B. *„Sinus-Welle über die Wand“*, *„Farbkreis“*, *„Stroboskop“*).
- **Button-Verhalten (Trigger-Modi):**
  - `TOGGLE`: Klick schaltet an / nochmal Klick schaltet aus.
  - `FLASH` (Momentary): Aktiv solange Maustaste / Touch / MIDI-Taste gehalten wird.
  - `EXCLUSIVE_GROUP` (Radio): Z. B. für Farbreihen: Die Aktivierung von *„Grün“* deaktiviert automatisch *„Rot“* oder *„Blau“*.
- **Fading:** Konfigurierbare Einblendzeit (`fadeTimeMs`) für weiche Übergänge.
- **Hardware-Anbindung:** Jede Taste kann per 1-Klick MIDI-Learn direkt auf physische Controller-Pads (z. B. Akai APC Mini, Novation Launchpad) gelegt werden.

### Tab 4: Timeline (Show & Music Sync)
- **Zweck:** Vorprogrammierte, mikrosekundengenaue Shows synchron zur Musik (z. B. für Showtänze oder Einmarsch-Hymnen).
- **Integrierte Audio-Wiedergabe (Ersetzt VLC / externe Zuspieler):**
  - Audio-Dateien (MP3, WAV, OGG) werden direkt in die Show geladen.
  - Das Audio-Signal wird **direkt über den Audio-Ausgang / die Soundkarte des Computers an das Mischpult / die Hallen-PA** ausgegeben.
  - **100% Synchronität:** Ton und Licht starten zeitgleich mit einem einzigen Druck auf *Play* – keine Timing-Verschiebungen und kein Herunterzählen mehr zwischen Ton- und Lichtmann.
- **Wellenform-Darstellung (Waveform-Display):**
  - Visuelle Wellenform der Audiodatei für millimetergenaues Arbeiten an Drops, Beats und Tempowechseln.
  - Playhead mit Zeitanzeige (Minuten:Sekunden:Millisekunden).
  - **Scrubbing & Spulen:** Beim Klick in die Zeitleiste springt das Lied sofort exakt an diese Stelle mit (ideal zum Einstudieren einzelner Tanz-Abschnitte).
  - Transport-Steuerung: Play, Pause, Stop, Loop-Marker, Lautstärkeregler.
- **Mehrspur-DMX-Zeitleiste (Light Tracks):**
  - Mehrere parallele Lichtspuren unter der Audiospur (z. B. für *Moving Heads Bewegung*, *Farben*, *Dimmer/Strobe*, *Frontlicht*).
  - Platzieren von Szenen- und Chaser-Blöcken per Drag & Drop.
  - Ziehbare Blocklänge (Dauer) und stufenlos einstellbare Fade-In / Fade-Out Kurven.

### Tab 5: Input / Output (I/O) & Diagnostik
- **Zweck:** Anbindung physischer Schnittstellen für DMX-Ausgabe, externe Controller und Bus-Überwachung.
- **DMX Outputs:**
  1. **Art-Net Output (Ethernet/UDP):**
     - Sendet DMX-Frames an Art-Net Nodes im Netzwerk (Port 6454).
     - Einstellbar: Ziel-IP (Broadcast `255.255.255.255` oder Unicast wie `192.168.1.100`), Subnetz und Universe.
  2. **DMX2USB Output (Serielle Schnittstelle / FTDI):**
     - Anbindung gängiger USB-DMX-Interfaces (z. B. Enttec DMX USB Pro, FTDI OpenDMX).
     - Plattformunabhängig implementiert über `jSerialComm`.
     - Port-Auswahl (z. B. `COM3` unter Windows, `/dev/ttyUSB0` unter Linux).
  3. **Virtual Simulator Driver:**
     - Headless- und Testbetrieb ohne physische Hardware (Ausgabe via Logging / Web-Visualisierung).
- **Hardware-Inputs (MIDI):**
  - **MIDI Input Subsystem:**
    - Basiert auf der nativen Java Sound API (`javax.sound.midi`) – 100% plattformunabhängig, keine C-Libraries erforderlich.
    - Automatisches Scannen angeschlossener USB-MIDI-Controller (z. B. Akai APC Mini, Novation Launchpad, Behringer BCF2000, Korg nanoKONTROL).
  - **MIDI-Learn & Mapping-Matrix:**
    - Live-MIDI-Learn: Button oder Fader in der Software anklicken, Regler/Taste auf dem MIDI-Pult betätigen -> automatische Verknüpfung.
    - Zuweisung von MIDI Note On/Off und Control Change (CC) auf:
      - Grand Master Fader
      - Blackout-Toggle
      - Einzel- oder Submaster-Fader im Fader Desk
      - Beliebige Slots in der 3×25 Live Matrix
  - **MIDI Feedback (Option):**
    - Sendet Note-On/Off-Befehle zurück an Controller mit LED-Tasten (z. B. Launchpad / APC Mini), um aktive Cues leuchten zu lassen.
- **DMX Traffic Monitor (Diagnose wie in DMXControl):**
  - Großes Echtzeit-Balkendiagramm zur Bus-Diagnose:
    - **Framerate-Verlauf (Hz / FPS):** Soll-Wert (z. B. 40 Hz) vs. Ist-Wert.
    - **Kanalaktivitäts-Balken:** Balkendiagramm der 512 Kanäle, das anzeigt, auf welchen Adressen aktuell Aktivität/Änderung stattfindet.
    - **Paket-Zähler:** Ausgegebene Pakete pro Sekunde (PPS) und Gesamtzähler.
    - **Frame-Time / DMX-Timing:** Messung der Frame-Generierungszeit in Millisekunden (Soll: < 25 ms bei 40 Hz).

---

## 3. DMX Core Engine: Technische Leitregeln für Moving Lights

### 3.1 HTP vs. LTP Merging
- **HTP (Highest Takes Precedence):**
  - Gilt ausnahmslos für **Dimmer- und Intensitätskanäle**.
  - Wenn mehrere Szenen oder Flash-Buttons denselben Dimmer ansprechen, gewinnt der höchste Wert.
- **LTP (Latest Takes Precedence):**
  - Gilt für **Pan, Tilt, Gobo, Farbe, Prisma und Shutter**.
  - Der zuletzt gesendete Befehl überschreibt den Wert – unabhängig davon, ob die Zahl größer oder kleiner ist.

### 3.2 Grand Master & Blackout-Regel
- **Grand Master:** Skaliert **ausschließlich** Kanäle vom Typ `DIMMER` und `SHUTTER` (`effektiv = (wert * master) / 255`).
- **Blackout:** Setzt **ausschließlich** `DIMMER`- und `SHUTTER`-Kanäle auf 0.
- **Verbotene Aktion:** Pan-, Tilt- oder Farbkanäle dürfen durch Grand Master oder Blackout **niemals verändert oder genullt werden**! Moving Heads müssen beim Blackout exakt auf ihrer aktuellen Position stehen bleiben.

### 3.3 16-Bit Pan/Tilt Behandlung
- Moving Heads nutzen 2 DMX-Kanäle pro Achse (Coarse = High Byte 0–255, Fine = Low Byte 0–255), entsprechend 65.536 Stufen.
- Die Engine berechnet Positionen intern als 16-Bit Integer (`0 .. 65535`) und teilt sie beim Frame-Export auf:
  - `Coarse = (value >> 8) & 0xFF`
  - `Fine   = value & 0xFF`

### 3.4 Globaler Release & Stop-All Engine Mechanismus
- Die DMX-Engine besitzt eine zentrale, thread-sichere Methode `stopAll()`:
  - **Sofortiger Abbruch:** Alle laufenden Chaser-Threads, Fading-Tasks und FX-Generatoren werden unverzüglich beendet (`cancel(true)`).
  - **Matrix-Deaktivierung:** Alle Buttons in der 3×25 Live-Matrix werden auf `active = false` gesetzt.
  - **Release aller Overrides:** Manuelle Fader-Haltungen (Manual Overrides) im Fader Desk und in der 2D-Ansicht werden freigegeben.
  - **Reset des Ausgabepuffers:** Dimmer-Kanäle werden auf 0 gesetzt. Pan/Tilt-Werte fahren sanft oder verharren auf definierter Park-/Home-Position.
  - **Verhindert Zustandskonflikte:** Löst das bekannte QLC+-Problem, bei dem man getrennte Stop-Buttons für Simple Desk, Chaser und Virtual Console suchen musste.

### 3.5 DMX-Traffic Messung & Frame-Timing
- Die `DmxEngine` erfasst kontinuierlich Diagnose-Metriken im Sendeloop:
  - **FPS-Counter:** Exakte Messung der tatsächlich erreichten Bildwiederholrate (Frames pro Sekunde).
  - **Frame-Time Tracker:** Zeitspanne in Nanosekunden/Millisekunden pro DMX-Zyklus (Renderzeit + I/O-Übertragungszeit).
  - **Delta-Analyzer:** Vergleicht Frame $N$ mit Frame $N-1$ und zählt veränderte DMX-Kanäle (`changedChannelsCount`).
  - **Bus-Auslastungsfaktor:** Berechnet das Verhältnis von aktiven Daten zu Leerlaufzeiten für das Balkendiagramm.

---

## 4. Tech-Stack & Framework-Leitplanken

- **Java-Version:** Java 21 LTS (Records, Virtual Threads, Pattern Matching).
- **Build-Tool:** **Apache Maven** (`pom.xml`) – Strikte Richtlinie, kein Gradle.
- **Web-Framework:** **Javalin 6.x** (embedded Jetty).
- **JSON:** **Jackson 2.18+** (`jackson-databind`, `jackson-datatype-jsr310`).
- **Persistenz:** **H2 Database** (`./data/jdmx_db.mv.db`) via JDBC und HikariCP. Sauberes ANSI-SQL für spätere Postgres/MySQL-Portierbarkeit.
- **Serielle Schnittstelle (USB-DMX):** `com.fazecast:jSerialComm:2.11.0`.
- **MIDI-Schnittstelle:** Java SE Standard `javax.sound.midi`.
- **Logging:** SLF4J mit `slf4j-simple`.
- **Frontend:**
  - Modernes HTML5, CSS3 Studio Dark Theme (ohne Node/NPM-Zwang).
  - **Ausschließliche Verwendung von Lucide-Icons.**
  - **Absolutes Emoji-Verbot** in der gesamten Benutzeroberfläche.
  - HTML5 Canvas für 2D-Stage und 512-Kanal-Grid.
  - Server-Sent Events (SSE) für bidirektionale Echtzeit-Synchronisation.
- **Testing:** **JUnit 5 (Jupiter)** + Mockito / AssertJ.

---

## 5. Datenmodell & Schemas

### 5.1 Datamodels (Records)

#### `ChannelType.java`
```java
package de.exit.jdmx.model;

public enum ChannelType {
    DIMMER, PAN, PAN_FINE, TILT, TILT_FINE, RED, GREEN, BLUE, WHITE, SHUTTER, GOBO, SPEED, OTHER;

    public boolean isIntensity() {
        return this == DIMMER || this == SHUTTER;
    }

    public boolean isPosition() {
        return this == PAN || this == PAN_FINE || this == TILT || this == TILT_FINE;
    }

    public boolean isColor() {
        return this == RED || this == GREEN || this == BLUE || this == WHITE;
    }
}
```

#### `Fixture.java`
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Fixture(
    String id,
    String name,
    int universe,
    int startAddress,
    FixtureType type,
    List<ChannelType> channelDefinitions,
    boolean invertPan,
    boolean invertTilt,
    int minPanPercent,   // 0 - 100
    int maxPanPercent,   // 0 - 100
    int minTiltPercent,  // 0 - 100
    int maxTiltPercent,  // 0 - 100
    double stageX,       // 0.0 - 100.0 (Position auf 2D Stage)
    double stageY        // 0.0 - 100.0
) {
    public int channelCount() {
        return channelDefinitions != null ? channelDefinitions.size() : 1;
    }

    public boolean overlapsWith(Fixture other) {
        if (this.universe != other.universe) return false;
        int thisEnd = this.startAddress + this.channelCount() - 1;
        int otherEnd = other.startAddress + other.channelCount() - 1;
        return this.startAddress <= otherEnd && other.startAddress <= thisEnd;
    }
}
```

#### `SceneScope.java` & `Scene.java`
```java
package de.exit.jdmx.model;

public enum SceneScope {
    FULL,            // Schreibt alle konfigurierten Kanäle
    COLOR_ONLY,      // Schreibt ausschließlich RGB/RGBW/Farbrad-Kanäle
    INTENSITY_ONLY,  // Schreibt ausschließlich Dimmer-Kanäle
    POSITION_ONLY,   // Schreibt ausschließlich Pan/Tilt-Kanäle
    CUSTOM           // Benutzerdefinierte Kanalliste
}
```
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Scene(
    String id,
    String name,
    int universe,
    SceneScope scope,
    Map<Integer, Integer> channelValues, // DMX-Kanal (1-512) -> Wert (0-255)
    int fadeTimeMs
) {}
```

#### `MatrixButton.java` (3 × 25 Grid Slot)
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatrixButton(
    String id,
    int row,               // 1 - 3
    int col,               // 1 - 25
    String label,          // z. B. "Wand Rot", "Cue 1", "Welle"
    String colorHex,       // Farbe der Schaltfläche im UI (z. B. "#E11D48")
    ButtonType type,       // SCENE, CHASER, FLASH, OVERRIDE
    String targetId,       // Referenzierte Scene-ID oder Chaser-ID
    ButtonMode mode,       // TOGGLE, FLASH, EXCLUSIVE
    String exclusiveGroup, // z. B. "COLORS", "POSITIONS"
    boolean active         // Aktueller Live-Zustand
) {}
```

#### `MidiMapping.java`
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MidiMapping(
    String id,
    String deviceName,     // Name des MIDI-Controllers
    int midiChannel,       // 1 - 16
    MidiMessageType messageType, // NOTE_ON, CONTROL_CHANGE
    int controlNumber,     // Note-Nummer oder CC-Nummer (0-127)
    MidiTargetType targetType,   // GRAND_MASTER, BLACKOUT, MATRIX_BUTTON, FADER
    String targetId        // z. B. Button-ID oder Kanalnummer
) {}
```

#### `DmxTrafficStats.java`
```java
package de.exit.jdmx.model;

public record DmxTrafficStats(
    int fps,                 // Aktuelle Ausgaberate in Hz / FPS
    int targetFps,           // Soll-Frequenz (Standard: 40 Hz)
    long packetsSentTotal,   // Gesamtzahl gesendeter Frames
    int activeChannelsCount, // Kanäle mit DMX-Wert > 0
    int changedChannelsCount,// Im aktuellen Zyklus veränderte Kanäle
    double busLoadPercent,   // DMX-Busauslastung (0.0 - 100.0%)
    double frameTimeMs       // Berechnungs- und Übertragungszeit in ms
) {}
```

#### `TimelineShow.java`, `TimelineTrack.java`, `TimelineBlock.java`
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TimelineShow(
    String id,
    String name,
    String audioFileName,
    long durationMs,
    List<TimelineTrack> tracks
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record TimelineTrack(
    String id,
    String name,
    String colorHex,
    List<TimelineBlock> blocks
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record TimelineBlock(
    String id,
    long startMs,
    long durationMs,
    long fadeInMs,
    long fadeOutMs,
    String sceneId
) {}
```

---

## 6. REST-API & SSE Schnittstellen

| Methode | Pfad | Beschreibung | Payload / Rückgabe |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/universes/{id}` | Zustand des Universums | `UniverseState` (512 Werte, Master, Blackout) |
| `POST`| `/api/universes/{id}/channel` | Setzt einzelnen Kanal | `{ "channel": 1, "value": 255 }` |
| `POST`| `/api/universes/{id}/channels`| Setzt mehrere Kanäle | `{ "values": { "1": 255, "2": 128 } }` |
| `POST`| `/api/universes/{id}/master`  | Setzt Grand Master | `{ "value": 255 }` |
| `POST`| `/api/universes/{id}/blackout`| Schaltet Blackout ein/aus | `{ "blackout": true/false }` |
| `POST`| `/api/engine/stop-all`        | **Globaler Release & Stop:** Stoppt alle Chaser, Fades, löst Overrides & Matrix | 200 OK |
| `GET` | `/api/engine/traffic`         | DMX-Traffic-Statistiken (FPS, Bus-Last, Packets/s) | `DmxTrafficStats` |
| `GET` | `/api/fixtures` | Liste aller Fixtures | `List<Fixture>` |
| `POST`| `/api/fixtures` | Legt Fixture an (mit Kollisionsprüfung) | `Fixture` |
| `DELETE`| `/api/fixtures/{id}` | Löscht Fixture | 204 No Content |
| `GET` | `/api/scenes` | Liste aller Szenen | `List<Scene>` |
| `POST`| `/api/scenes` | Speichert aktuelle Werte als Szene | `Scene` (inkl. `scope`) |
| `POST`| `/api/scenes/{id}/recall` | Aktiviert Szene mit Überblendung | 200 OK |
| `GET` | `/api/matrix/buttons` | Alle 75 Tasten der 3×25 Matrix | `List<MatrixButton>` |
| `PUT` | `/api/matrix/buttons/{row}/{col}` | Konfiguriert einen Button | `MatrixButton` |
| `POST`| `/api/matrix/buttons/{row}/{col}/trigger` | Triggert einen Button (Klick/Flash) | `{ "action": "press" / "release" }` |
| `GET` | `/api/timeline/shows` | Liste gespeicherter Shows | `List<TimelineShow>` |
| `POST`| `/api/timeline/shows` | Erstellt/aktualisiert eine Show | `TimelineShow` |
| `POST`| `/api/timeline/shows/{id}/audio` | Lädt Audiodatei (MP3/WAV) hoch | Multipart Audio |
| `GET` | `/api/timeline/shows/{id}/audio` | Audio-Stream für PA / Browser-Ausgabe | Audio Stream (`audio/mpeg`) |
| `POST`| `/api/timeline/control` | Transport-Steuerung (Play/Pause/Stop/Seek) | `{ "action": "play", "positionMs": 1500 }` |
| `GET` | `/api/io/config` | Aktuelle Treiber- und MIDI-Konfiguration | `IOConfig` |
| `POST`| `/api/io/driver` | Ändert DMX-Treiber (Art-Net, USB, Sim) | `DriverConfig` |
| `GET` | `/api/io/midi/devices` | Liste gefundener MIDI-Hardware-Ports | `List<String>` |
| `GET` | `/api/io/midi/mappings` | Aktuelle MIDI-Zuweisungen | `List<MidiMapping>` |
| `POST`| `/api/io/midi/mappings` | Speichert MIDI-Zuweisung | `MidiMapping` |
| `POST`| `/api/io/midi/learn` | Startet MIDI-Learn-Modus | `{ "targetType": "MATRIX_BUTTON", ... }` |
| `GET` | `/api/live/stream` | **SSE-Stream:** Sendet `universe-update`, `master-update`, `matrix-update`, `traffic-update`, `timeline-tick`, `midi-event` | EventStream (`text/event-stream`) |

---

## 7. Phasenweise Implementierungs-Roadmap

### Phase 1: Maven Setup & Projektgerüst
- [ ] Saubere `pom.xml` mit Java 21, Javalin 6, Jackson, HikariCP, H2, SLF4J, `jSerialComm`, JUnit 5.
- [ ] Maven-Plugins: Compiler (Java 21), Exec-Plugin, Shade-Plugin (Fat-JAR mit `de.exit.jdmx.Main`).
- [ ] Einstiegsklasse `Main.java` und Konfiguration `AppConfig.java`.
- [ ] **Verifikation:** `mvn clean compile` und `mvn exec:java` startet Server auf Port 7070.

### Phase 2: DMX Domain Core mit HTP/LTP, Master & 16-Bit
- [ ] `DmxUniverse.java`: 512-Kanal-Puffer, Trennung von HTP (Dimmer) und LTP (Pan/Tilt/Farbe).
- [ ] Grand Master- und Blackout-Logik (beeinflusst ausschließlich Dimmer/Shutter, niemals Pan/Tilt).
- [ ] 16-Bit Pan/Tilt Hilfsfunktionen (Coarse/Fine Splitting).
- [ ] Treiber-Interface `DmxOutputDriver` und `VirtualSimulationDriver`.
- [ ] DMX-Ticker / Loop `DmxEngine.java` (30–44 Hz).
- [ ] **Verifikation:** JUnit-5 Tests (`DmxUniverseTest`) decken HTP, LTP, 16-Bit und Master/Blackout-Isolation zu 100% ab.

### Phase 3: Relationale Persistenz (H2 / ANSI-SQL)
- [ ] `DatabaseConfig.java` mit HikariCP und H2 File-DB (`./data/jdmx_db`).
- [ ] `DatabaseInitializer.java` mit idempotentem Schema (`CREATE TABLE IF NOT EXISTS` für `fixtures`, `scenes`, `matrix_buttons`, `timeline_shows`, `timeline_tracks`, `timeline_blocks`, `midi_mappings`).
- [ ] Repositories und JDBC-Implementierungen:
  - `FixtureRepository` & `JdbcFixtureRepository`
  - `SceneRepository` & `JdbcSceneRepository`
  - `MatrixButtonRepository` & `JdbcMatrixButtonRepository`
  - `TimelineRepository` & `JdbcTimelineRepository`
  - `MidiMappingRepository` & `JdbcMidiMappingRepository`
- [ ] **Verifikation:** Integrationstests prüfen CRUD-Operationen aller Repositories gegen H2.

### Phase 4: DMX Output-Treiber (Art-Net & DMX2USB)
- [ ] `ArtNetDriver.java`: Erzeugung standardkonformer UDP-Art-DMX Pakete (OpCode `0x5000`, Port 6454).
- [ ] `DmxUsbDriver.java`: Serielle Kommunikation über `jSerialComm` (Enttec Pro / FTDI Protokoll mit Break/MAB Timing).
- [ ] Treiber-Factory und Umschaltung zur Laufzeit ohne Server-Neustart.
- [ ] **Verifikation:** `ArtNetDriverTest` prüft Header, Sequenznummer und DMX-Payload im Byte-Array.

### Phase 5: MIDI-Subsystem (Native Java MIDI)
- [ ] `MidiService.java` basierend auf `javax.sound.midi`:
  - Automatische Erkennung verfügbarer MIDI-Transmitter (`MidiSystem.getMidiDeviceInfo()`).
  - Empfang von Note-On, Note-Off und Control Change (CC) Nachrichten.
  - MIDI-Learn Manager (hört auf nächste eingehende Nachricht und verknüpft sie).
  - Event-Dispatcher leitet MIDI-Befehle an Master, Blackout, Fader oder Matrix-Buttons weiter.
- [ ] **Verifikation:** Unit-Tests mit gemocktem MIDI-Receiver simulieren CC- und Note-Befehle.

### Phase 6: REST API, Audio-Streaming & SSE Realtime Engine
- [ ] Controller:
  - `DmxApiController`: Kanäle, Master, Blackout, Stop-All, DMX-Traffic.
  - `FixtureApiController`: Fixture-CRUD mit Adress-Kollisionsprüfung.
  - `SceneApiController`: Szenen speichern und recallen (mit Scope-Filter und Fading).
  - `MatrixApiController`: 3×25 Grid konfigurieren und triggern.
  - `TimelineApiController`: Shows anlegen, Audio-Upload & Streaming, Transport-Steuerung (Play/Pause/Seek).
  - `IoApiController`: Treiberwahl, Port-Auswahl, MIDI-Geräte, MIDI-Mappings & Learn.
- [ ] `DmxSseHandler.java`: Broadcast von `universe-update`, `matrix-update`, `master-update`, `traffic-update`, `timeline-tick`.
- [ ] **Verifikation:** HTTP Integrationstests für alle REST-Endpunkte und SSE-Verbindung.

### Phase 7: Web-Frontend (Die 5 Tabs mit Lucide-Icons)
- [ ] **Basis-Layout & Theme:** Studio Dark Theme, responsive Navigation, permanenter Global-Header mit Master, Blackout, STOP ALL und Traffic-Balken, Lucide-Icons (keine Emojis!).
- [ ] **Tab 1 (Fader & Universum):** 512-Kanal Fader Desk mit Canvas Universe Grid und Gruppierung nach Fixtures.
- [ ] **Tab 2 (2D Stage & Fixtures):** Visuelle U-Traverse und Wand, Scheinwerfer-Platzierung, Quick-Inspector mit 2D-XY-Pad für Pan/Tilt, Invertierung und Limiter.
- [ ] **Tab 3 (Live Matrix):** 3×25 Button-Grid mit freier Belegbarkeit, Farbcodierung, Label, Status-Feedback, Toggle/Flash/Radio-Modi und Partial Cues.
- [ ] **Tab 4 (Timeline):** Show-Designer mit Wellenform (Waveform), synchroner Audio-Ausgabe für Hallen-PA, Playhead, Scrubbing und mehrspurigen DMX-Szenenblöcken.
- [ ] **Tab 5 (Input / Output):** Art-Net Setup, DMX2USB Port-Auswahl, MIDI-Geräteliste, MIDI-Learn Dialog, Mapping-Tabelle und großes DMX-Traffic Diagramm.
- [ ] **Verifikation:** Manueller End-to-End-Test im Browser: Fader, 2D Stage, 3x25 Matrix, Timeline mit Audio und I/O reagieren synchron und latenzfrei.

### Phase 8: Polish, Dokumentation & Standalone Fat-JAR
- [ ] Erstellung der Pflicht-Dokumente:
  - `docs/ONBOARDING.md`: Leitfaden für neue Entwickler (Setup, Architektur, Treiber-Erweiterung).
  - `docs/ARCHITECTURE.md`: Tiefendokumentation (HTP/LTP, Threading, Timing, Audio-DMX-Sync, Paketformate).
  - `README.md`: Vollständige Produkt- und Installationsanleitung.
- [ ] JavaDoc-Vollständigkeit auf allen Klassen, Records und Methoden.
- [ ] Maven Fat-JAR Build (`mvn clean package`).
- [ ] **Verifikation:** Standalone-Start via `java -jar target/jdmx-1.0.0-SNAPSHOT.jar`.
