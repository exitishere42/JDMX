# Spezifikation: jdmx (Java DMX Controller)

> **Verbindliche Master-Spezifikation für die Software-Entwicklung**  
> Zielplattform: **Native JavaFX 21 LTS Desktop-Anwendung** (Apache Maven, Java 21 LTS).  
> Konzipiert für den Vereins- und Showbetrieb (Reihener Carnevals Verein) mit statischem Licht und Moving Lights an einer **U-Traverse gegen eine Projektionswand**.  
> Vereint die besten Ergonomie-Konzepte aus **QLC+ (Simple Desk & Fixture Library)**, **DMXControl 3 (Device Control Dock)** und **FL Studio (Timeline Playlist & Browser)**.

---

## 1. Vision & System-Architektur

- **Ziel:** Ein hochperformantes, modulares DMX-512 Lichtsteuerungs-System als native Desktop-Anwendung (OpenJFX 21 LTS).
- **Architektur-Grundsatz (Reines MVC):**
  - **Core & Service-Schicht (`de.exit.jdmx.core` / `service`):** Sämtliche DMX-Berechnungen, Universe-Puffer (512 Kanäle), HTP/LTP-Merging, Grand Master, Blackout/Stop-All-Logik, 16-Bit Pan/Tilt-Interpolation, 40Hz Frame-Loop, Audio-Synchronisation und Treiber-Kommunikation laufen **vollständig entkoppelt von der UI**.
  - **JavaFX UI-Schicht (`de.exit.jdmx.ui`):** Visualisiert ausschließlich den Systemzustand über Observable Properties / Events und leitet Bediener-Interaktionen an die Services weiter.
- **Hardware-Anbindung:**
  - **DMX-Output:** Art-Net UDP (Port 6454), DMX2USB (`jSerialComm` für Enttec DMX USB Pro / FTDI), Virtueller Simulationstreiber.
  - **Inputs:** Native Java MIDI (`javax.sound.midi`) für Controller (Akai APC Mini, Novation Launchpad etc.) mit Live-MIDI-Learn.
  - **Audio-Output:** Integrierte Audio-Engine (JavaFX Media) gibt Showtanz-Musik direkt über die Laptop-Soundkarte an das Mischpult / die Hallen-PA aus (100% synchron zum Licht).

---

## 2. Die All-in-One Kopfzeile (Single Top-Bar)

Um maximale vertikale Arbeitsfläche für Bühne, Fader und Timeline zu garantieren, sind die Tab-Navigation, der Brand und die wichtigsten globalen Steuer- und Diagnose-Elemente in einer **einzigen, ultra-kompakten Kopfzeile** vereint:

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ [ jdmx ]  │  [Stage & Fixtures]  [Simple Desk]  [Button Matrix]  [Show-Timeline]  [Input/Output]  │  [● BLACKOUT]  40 DMX/s  [■ STOP] │
└────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Elemente von links nach rechts:
1. **Brand & Action-Menü:** `[ jdmx ]`
   - Klickbarer, randloser Button mit Warm-Amber-Glühbirnen-Logo links und dem Markennamen `jdmx` in Hellgrau.
   - Öffnet beim Klick ein kompaktes vertikales Aktions-Dropdown (inspiriert vom QLC+ „Actions“-Menü, aber schlank und fokussiert):
     - **`New Project`** (Icon: Lucide `file-plus`): Erstellt ein neues Projekt (mit Sicherheitsabfrage bei ungespeicherten Änderungen).
     - **`Open File`** (Icon: Lucide `folder-open`): Öffnet einen `.jdmx`-Showstand.
     - **`Save`** (Icon: Lucide `save`): Speichert das aktuelle Projekt direkt.
     - **`Save as...`** (Icon: Lucide `file-pen`): Speichert unter neuem Namen/Pfad.
     - **`Undo / Redo`** (Icons: Lucide `undo-2` / `redo-2`): Schnelles Rückgängigmachen / Wiederherstellen von Bearbeitungsschritten.
     - **`Settings`** (Icon: Lucide `settings`): Öffnet die allgemeinen Einstellungen im In-App Overlay.
     - **`Fullscreen`** (Icon: Lucide `maximize`): Schaltet den randlosen Vollbildmodus um.
     - *(Hinweis: Die Liste ist modular aufgebaut und kann jederzeit um weitere Einträge wie z. B. About erweitert werden).*
2. **Tab-Umschalter (Die 5 Haupt-Arbeitsbereiche):**
   - `[Stage & Fixtures]` (Tab 1)
   - `[Simple Desk]` (Tab 2)
   - `[Button Matrix]` (Tab 3)
   - `[Show-Timeline]` (Tab 4)
   - `[Input/Output]` (Tab 5)
3. **Rechte Global-Aktionen & Status:**
   - **`[● BLACKOUT]` (Toggle-Button):** Schaltet sofort alle Dimmer/Shutter-Werte auf 0. **Wichtig:** Pan-, Tilt- und Farbkanäle bleiben stur auf ihrer Position stehen (Moving Heads drehen sich niemals weg!). Leuchtet rot, wenn aktiv.
   - **`40 DMX/s`:** Live-Anzeige der DMX-Paketrate / Framerate und des Bus-Traffics.
   - **`[■ STOP]` (Globaler Release / Not-Aus):** Großer, roter Haupt-Stop-Button:
     - Stoppt sofort alle aktiven Chaser, Fades und Effekt-Engines.
     - Deaktiviert alle aktiven Tasten in der Button Matrix.
     - Gibt alle manuellen Fader-Overrides im Simple Desk frei.
     - Bringt das gesamte Universum mit 1 Klick in den definierten Ruhezustand.

---

## 2.1 In-App Modal Overlay Architektur ("Fenster im Fenster")

Alle Dialoge, Assistenten, Einstellungen und Bestätigungs-Popups werden **ausnahmslos als In-App Modal Overlays („Fenster im Fenster“)** realisiert – exakt nach dem Vorbild des Update- und Info-Systems von `sound2artnet` und `artnet2dmx`:

- **Keine separaten OS-Fenster (Stages):** Dialoge öffnen sich niemals in einem eigenen Betriebssystem-Fenster, um Fokusverlust, Multi-Monitor-Desync oder Verstecken hinter der Hauptbühne im Live-Einsatz zu verhindern.
- **Aufbau des In-App Modals:**
  - **Dunkler Schleier:** Ein halbtransparenter Scrim (`rgba(0, 0, 0, 0.65)`) deckt das gesamte Hauptfenster ab und blockiert Klicks auf die dahinterliegende Arbeitsfläche.
  - **Zentrierte Dialog-Karte:** Material 2 Surface (`#1E1E1E`) mit subtilem 1px Rand (`#2E2E2E`), abgerundeten Ecken (`8px`) und weichem Drop-Shadow.
  - **Header:** Titel mit thematischem Lucide-Icon links und einem Schließen-Button `[✕]` oben rechts.
  - **Tastatur- & Klick-Verhalten:** Schließen bei Klick auf `[✕]`, Drücken der `Escape`-Taste oder Klick auf den abgedunkelten Hintergrund.
  - **Betroffene Komponenten:**
    - Settings-Dialog
    - 3-Schritte Fixture Patch Dialog (`+` in Stage & Fixtures)
    - New / Open / Save As Projekt-Dialoge
    - Sicherheits- und Bestätigungsabfragen (z. B. ungespeicherte Änderungen)
    - About- & Versions-Dialog

---

## 3. Die 5 Workspace-Tabs im Detail

### Tab 1: Stage & Fixtures (Bühne, U-Traverse & Fixture-Verwaltung)

```text
┌───┬────────────────────────────────────────────────────────────────────────────────────────────┬─────────────────┐
│   │                                                                                            │ [ 📁 EXPLORER ] │
│ + │   ══════════════════════════════ PROJEKTIONSWAND ══════════════════════════════            │                 │
│   │             ▲                           ▲                           ▲                      │ 📁 Presets      │
│ F │            / \                         / \                         / \                     │   ├─ Wand_Rot   │
│ I │           /   \  (Lichtkegel)         /   \                       /   \                    │   └─ Wand_Mitte │
│ X │          /     \                     /     \                     /     \                   │                 │
│ T │      [ MH 1 ]                    [ MH 2 ]                    [ MH 3 ]                      │ 📁 Groups       │
│ U │         │                           │                           │                          │   ├─ All_MH     │
│ R │      ┌──┴───────────────────────────┴───────────────────────────┴──┐                       │   └─ U_Truss    │
│ E │      │                 U - T R A V E R S E                         │                       │                 │
│ S │      │                                                             │                       │ (nach rechts    │
│   │      │  (Neue Fixtures spawnen hier mittig nebeneinander mit       │                       │  aufklappbar)   │
│   │      │   Kollisionslogik: [MH 1] [MH 2] [MH 3] [MH 4])             │                       │                 │
└───┴──────┴─────────────────────────────────────────────────────────────┴───────────────────────┴─────────────────┘
▼ 24/7 STEUERUNGS-DOCK (Permanente untere Leiste mit 3 horizontal verschiebbaren Modulen, Stage behält >60% Platz)
┌───────────────────────────────┬───────────────────────────────┬──────────────────────────────────────────────────┐
│ INTENSITÄT                    │ FARBE (HSV)                   │ POSITION                                         │
│                               │                               │                                                  │
│ ┌────┐  │     │   ┌────┐┌────┐│ ┌───┐                         │         ┌───────────┬───────────┐     │          │
│ │100%│  │     │   │Lumo││Nox ││ │HSV│                         │         │           │           │     │          │
│ ├────┤  │     │   │s   ││    ││ └───┘        ╭───────╮        │         │           │           │     │          │
│ │ 70%│  │     │   └────┘└────┘│             ╭─╯   ●   ╰─╮     │   ──────┼───────────●───────────┼────── [█] Tilt   │
│ ├────┤ [█]   [█]              │             │ Farb-   │       │         │           │           │     │          │
│ │ 50%│  │     │               │             ╰─╮ kreis ╭─╯     │         │           │           │     │          │
│ ├────┤  │     │               │               ╰───────╯       │         └───────────┴───────────┘     │          │
│ │ 30%│  │     │               │                               │                                                  │
│ ├────┤  │     │               │   (purer Farbkreis            │                    ───[█]───                     │
│ │  0%│  │     │               │    mit Farbpaletten)          │                    Pan-Slider                    │
│ └────┘  │     │               │                               │                                                  │
│       Dimmer Strobo           │                               │   [✓] Pan             [✓] Tilt            [Home] │
└───────────────────────────────┴───────────────────────────────┴──────────────────────────────────────────────────┘
```

1. **Linke Leiste (`+` Button):**
   - Öffnet den **3-Schritte Fixture-Patch-Dialog** (QLC+ Style):
     1. **Marke auswählen:** Durchsuchbare Herstellerliste (`[ 🔍 Suchen... ]`: U'King, Showtec, Eurolite, Cameo, Stairville, Generic etc.) mit `[ + ]` für Custom Fixtures.
     2. **Modell / Profil auswählen:** DMX-Modi des Herstellers (z. B. `11CH`, `14CH`).
     3. **Properties konfigurieren:** Name, Universe (Standard: 1), Startadresse (1–512), Quantity (Anzahl), Channel Mode, Address Gap $\rightarrow$ Button **`[ Fixtures hinzufügen ]`**.
   - **Intelligente Spawning- & Kollisionslogik:** Bei z. B. `Quantity: 4` spawnen die 4 Fixtures **sauber nebeneinander in der Mitte der Traverse**, statt aufeinander zu liegen.
2. **Hauptbereich (Mitte):**
   - 2D-Fläche mit der **U-Traverse gegen die Projektionswand**.
   - Scheinwerfer frei per Drag & Drop auf den U-Schenkeln und der Mitte platzierbar.
   - Vektor-Beams simulieren Abstrahlrichtung, Pan/Tilt-Winkel, Farbe und Helligkeit in Echtzeit an die Wand.
3. **Rechte Leiste (ausklappbar):**
   - File Explorer für Schnellzugriff auf gespeicherte Positions-Presets, Farb-Paletten und Gruppen.
4. **Ausklappdock unten (DMXControl 3 Style Device Control):**
   - **Intensität:** Prozent-Schnelltasten (`VL`=100%, `70`, `50`, `30`, `0`), vertikale Fader für `Dimmer` und `Strobo`, Taster `Auf`, `Zu`, `Lumos` (100%), `Nox` (0%).
   - **Farbe:** Purer, aufgeräumter **HSV-Farbkreis** mit Picker-Punkt (ohne störende X/Y-Slider).
   - **Position:** Großes **2D-XY-Fadenkreuz** mit Pan-Slider darunter und Tilt-Slider rechts, Checkboxen für **`Pan invertieren`** und **`Tilt invertieren`** (essenziell für die gespiegelten U-Schenkel) sowie Min/Max-Begrenzern.

---

### Tab 2: Simple Desk (QLC+ Fader-Pult)

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Universe: [ Universe 1 v ]                                                                              │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│  │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │  │
│  │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │  │
│ [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] [█] │
│  0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0  │
│  1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23  24  25  26 │
│ [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] [X] │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ [██████ Custom Scrollbar                                                                              ] │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ FIXTURE LIST                                                                                            │
│ • Moving Head Links 1      Kanäle:  1 - 16   (16 Kanäle, Typ: Spot Moving Head)                         │
│ • Moving Head Links 2      Kanäle: 17 - 32   (16 Kanäle, Typ: Spot Moving Head)                         │
│ • LED Bar Wand 1           Kanäle: 33 - 39   ( 7 Kanäle, Typ: RGBW Bar)                                 │
│ • LED Bar Wand 2           Kanäle: 40 - 46   ( 7 Kanäle, Typ: RGBW Bar)                                 │
│ • Frontlicht Bühne         Kanäle: 47 - 50   ( 4 Kanäle, Typ: Dimmer Pack)                              │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

- **Kopfzeile:** Ausschließlich das saubere Dropdown `Universe: [ Universe 1 v ]` (kein DMX/%-Button, kein Löschknopf daneben).
- **Faderbank:** Schlanke vertikale Fader mit silbergrauem Fader-Cap.
  - Unter jedem Fader: DMX-Wert (`0–255`), Kanalnummer fett (`1`, `2`...), kleiner Reset-Button `[ X ]` pro Kanal.
- **Custom Scrollbar:** Schmaler, feiner Scrollbalken direkt unter den Fader-Füßen zum flüssigen Durchscrollen aller 512 Kanäle.
- **Fixture List (unten):** Liste aller Scheinwerfer mit Name, belegten Kanälen (z. B. `1 - 16`) und Typ.
  - **Auto-Scroll:** Ein Klick auf ein Fixture scrollt oben automatisch zu dessen Kanälen!
- **Weggelassen:** Keine Commands History, kein Keypad.

---

### Tab 3: Button Matrix (Live Show Konsole)

- **3 Reihen frei konfigurierbare Pads:** Für Live-Busking am Showabend (Reihe 1: Bewegungen/Positionen, Reihe 2: Farben, Reihe 3: Effekte & Strobes).
- **Attribut-Filter (Selective Cues):**
  - *Color-Only:* Ändert nur Farben – Position und Helligkeit laufen ungestört weiter.
  - *Intensity-Only:* Ändert nur Helligkeit (z. B. 100%, 50%, Blinder).
  - *Position-Only:* Fährt nur Pan/Tilt an.
- **Trigger-Modi:** `TOGGLE`, `FLASH` (Taster für Tusch/Strobe) und `EXCLUSIVE_GROUP` (Radio-Gruppe für Farben).
- **1-Klick MIDI-Learn:** Zuweisung auf physische Controller-Pads (Akai APC Mini, Launchpad etc.).

---

### Tab 4: Show-Timeline (FL Studio Style Show & Music Designer)

```text
┌───────────────────────┬─────────────────────────────────────────────────────────────────────────────┐
│ [ jdmx File Explorer] │ [▶ PLAY] [■ STOP] [🔁 LOOP] [🧲 SNAP: Beat] 01:23.450 / 03:45.000   Vol:[███] │
├───────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
│ 📁 Audio & Songs      │ [Bar 1]       [Bar 2]       [Bar 3]       [Bar 4]       [Bar 5]             │
│   ├─ showtanz26.mp3 ──┼─► [Line 1 (Audio):  ▄█▆▄▂  ▄█▆▄▂  ▄█▆▄▂  ▄█▆▄▂  ▄█▆▄▂  ▄█▆▄▂  ▄█▆▄▂  ]    │
│   └─ fanfare.mp3      │                                                                           │
│ 📁 Scenes & Cues      │                                                                           │
│   ├─ Wand_Blau ───────┼─► [Line 2: Wand_Blau ─────────►]  [Line 2: Wand_Rot ────────────────────►]│
│   ├─ Wand_Rot         │                                                                           │
│   └─ Warmweiss_Garde  │                                                                           │
│ 📁 Chasers & FX       │                                                                           │
│   ├─ Welle_Schnell ───┼─►                 [Line 3: Welle_Schnell ───────────────►]                │
│   └─ Strobe_Gewitter ─┼─►                                [Line 4: ⚡ STROBE 3s]                    │
│ 📁 Fixture Groups     │                                                                           │
│   └─ Moving_Heads_U   │ [Line 5: (Freie Spur für beliebige Clips...)                            ] │
└───────────────────────┴─────────────────────────────────────────────────────────────────────────────┘
```

- **Integrierte Audio-Wiedergabe:** MP3/WAV wird direkt geladen und über die Soundkarte an die Hallen-PA ausgegeben (kein VLC mehr nötig, 100% synchron!).
- **Linker File Explorer (Browser):**
  - 📁 Audio & Songs
  - 📁 Scenes & Cues
  - 📁 Chasers & FX
  - 📁 Fixture Groups
- **Multi-Line Playlist (Tracks):** Reines Drag & Drop aus dem Explorer auf beliebige Spuren, Kanten zum Verlängern/Kürzen ziehen, Fade-In/Out Griffe, Audio-Wellenform, Scrubbing (Klick in Timeline springt Musik + Licht synchron an dieselbe Stelle).

---

### Tab 5: Input/Output & Diagnostik

- **DMX-Ausgänge:** Art-Net UDP (Port 6454), DMX2USB (`jSerialComm`), Virtuelle Simulation.
- **MIDI-Eingänge:** Native Java MIDI (`javax.sound.midi`), Device-Scanning, Mapping-Tabelle.
- **Diagnostik:** DMX-Traffic Balkendiagramm (FPS, Paketrate, Bus-Last).

---

## 4. DMX Core Regeln für Moving Lights

1. **HTP (Highest Takes Precedence):** Gilt ausschließlich für Dimmer/Intensität.
2. **LTP (Latest Takes Precedence):** Gilt für Pan, Tilt, Farbe, Gobo, Shutter.
3. **Schutz vor Wegdrehen:** Grand Master und Blackout dürfen **niemals** Pan/Tilt verändern. Moving Heads bleiben exakt auf ihrer Position stehen!
4. **16-Bit Pan/Tilt:** Saubere Aufteilung in Coarse & Fine Byte für ruckelfreie Bewegungen an der Wand.

---

## 5. Implementierungs-Roadmap

- [ ] **Phase 1: Maven Setup & Projektgerüst** (Java 21, OpenJFX 21, H2, JUnit 5, `pom.xml`, `Main.java`).
- [ ] **Phase 2: DMX Core Engine & Output-Treiber** (`DmxUniverse` mit HTP/LTP & 16-Bit, 40Hz `DmxEngine`, `ArtNetDriver`, `DmxUsbDriver`, TDD 100%).
- [ ] **Phase 3: Relationale Persistenz & Fixture-Verwaltung** (H2 Schema, JDBC Repositories, Fixture-Library, Kollisionsprüfung).
- [ ] **Phase 4: Hardware-Inputs & Audio-Sync** (Natives MIDI mit Live-Learn, Audio-Playback Engine via JavaFX Media).
- [ ] **Phase 5: JavaFX Desktop UI & All-in-One Header** (`MainWindow`, Studio Dark Theme, `LucideIcon.java`, Single Top-Bar).
- [ ] **Phase 6: Die 5 Workspace-Tabs** (Stage & Fixtures, Simple Desk, Button Matrix, Show-Timeline, Input/Output).
- [ ] **Phase 7: Dokumentation & Packaging** (`ONBOARDING.md`, `ARCHITECTURE.md`, `README.md`, Fat-JAR).
