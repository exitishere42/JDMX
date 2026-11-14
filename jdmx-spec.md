# Spezifikation: jdmx (Java DMX Controller)

> **Verbindliche Master-Spezifikation für die Software-Entwicklung**  
> Zielplattform: **Native JavaFX 21 LTS Desktop-Anwendung** (Apache Maven, Java 21 LTS).  
> Konzipiert für den Vereins- und Showbetrieb (Reihener Carnevals Verein) mit statischem Licht und Moving Lights an einer **U-Traverse gegen eine Projektionswand**.  
> Vereint die besten Ergonomie-Konzepte aus **QLC+ (LightDesk Fader-Pult & Fixture Library)**, **DMXControl 3 (Device Control Dock)** und **FL Studio (Timeline Playlist & Browser)**.

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
│ [ jdmx ]  │  [Stage & Fixtures]  [LightDesk]  [Button Matrix]  [Show-Timeline]  [Input/Output]  │  [● BLACKOUT]  40 DMX/s  [■ STOP] │
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
   - `[LightDesk]` (Tab 2)
   - `[Button Matrix]` (Tab 3)
   - `[Show-Timeline]` (Tab 4)
   - `[Input/Output]` (Tab 5)
3. **Rechte Global-Aktionen & Status:**
   - **`[● BLACKOUT]` (Toggle-Button):** Schaltet sofort alle Dimmer/Shutter-Werte auf 0. **Wichtig:** Pan-, Tilt- und Farbkanäle bleiben stur auf ihrer Position stehen (Moving Heads drehen sich niemals weg!). Leuchtet rot, wenn aktiv.
   - **`40 DMX/s`:** Live-Anzeige der DMX-Paketrate / Framerate und des Bus-Traffics.
   - **`[■ STOP]` (Globaler Release / Not-Aus):** Großer, roter Haupt-Stop-Button:
     - Stoppt sofort alle aktiven Chaser, Fades und Effekt-Engines.
     - Deaktiviert alle aktiven Tasten in der Button Matrix.
     - Gibt alle manuellen Fader-Overrides im LightDesk frei.
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

1. **Zentrales Hinzufügen-Menü im Project Explorer:**
   - Direkt neben dem Titel `Project Explorer` befindet sich ein dezentes, grünes **`[ + ]`** Icon (`btn-add` mit Lucide `plus` in Smaragdgrün `#10B981`).
   - Ein Klick darauf öffnet ein kompaktes Action-Dropdown-Menü (`ExplorerAddMenu`) im Stil des `BrandActionMenu`:
     - **`Gerät`** (Icon: `box`, Cyan): Öffnet den universellen **In-App Fixture-Patch-Dialog** zum Hinzufügen/Importieren von Scheinwerfern.
     - **`Gerätegruppe`** (Icon: `layers`, Grün): Erstellt eine neue Gruppe im Ordner `Gerätegruppen`.
     - **`Chaser`** (Icon: `list-music`, Amber): Erstellt eine neue Szenenliste / Chaser im Ordner `Szenenlisten`.
     - **`Effekt`** (Icon: `sparkles`, Violett): Erstellt einen neuen Effekt im Ordner `Effekte`.
     - **`Preset`** (Icon: `star`, Orange): Erstellt ein neues Preset im Ordner `Presets`.
   - **Universeller Fixture-Import & Bibliotheks-System:**
     - Nativer XML-Parser für **QLC+ Fixtures (`.qxf`)**.
     - Nativer ZIP/XML-Parser für **GDTF Fixtures (`.gdtf`)** (`description.xml`).
     - Vorinstallierte Standard-Profile (`Generic Dimmer`, `Generic RGB Par`, `Generic RGBW Par`, `Generic Moving Head Spot`).
     - Datei-Import (`FileChooser`) für beliebige externe `.qxf`- und `.gdtf`-Dateien.
   - **In-App Fixture-Patch-Dialog:**
     - 1-Klick-Auswahl aus Hersteller, Modell und DMX-Modus.
     - Konfiguration von Startadresse (1–512), Universum (1–4) und Anzahl (Quantity).
     - **Intelligente Spawning- & Kollisionslogik:** Bei z. B. `Quantity: 4` spawnen die 4 Fixtures sauber nebeneinander in der Mitte der Traverse und werden automatisch im Project Explorer unter `Geräte` registriert.
2. **Hauptbereich (Mitte):**
   - 2D-Fläche mit der **U-Traverse gegen die Projektionswand** – vollständig frei von störenden Overlays oder schwebenden Toolbars für maximale Sicht.
   - **Visuelle Scheinwerfer-Repräsentation (`StageFixtureNode`):**
     - **Gehäuse:** 36x36px Quadrat mit dezenten **`2px` Eckenrundungen** (`-fx-background-radius: 2px; -fx-border-radius: 2px;`), Hardware-Dark `#1E1E1E`, dezenter Rahmen `#383838`.
     - **Linse:** 34px Durchmesser Innenkreis, bündig an allen 4 Innenkanten zentriert (ohne Randlücken, ohne Herausragen).
     - **Dimmer & Farbe:** Zeigt dynamisch die Farbe und Helligkeit (0–100%). Bei klassischen Scheinwerfern/Dimmern ohne RGB-Kanäle wird schönes 3200K Halogen-Warmweiß (`#FFE4B5`) statt Digitalweiß verwendet.
     - **Strobe-Pulsieren:** Linse blitzt im Strobe-Takt zwischen Lichtfarbe und Dunkel (`#141414`), wenn Strobe aktiv ist.
     - **Positions-Punkt (Pan/Tilt Tracker Dot):**
       - Nur bei Moving Lights / Scheinwerfern mit Pan/Tilt sichtbar.
       - Wandert im Linsenkreis synchron zum 2D-XY-Pad des Positions-Docks.
       - **Automatischer Kontrast:** Schaltet nach relativer WCAG-Luminanz dynamisch zwischen Schwarz (auf hellen Farben) und Weiß (auf dunklen Farben) um, um immer gestochen scharf erkennbar zu sein.
     - **Klick-basierter Namens-Badge:**
       - Im Normalzustand ausgeblendet für eine 100% aufgeräumte, blendfreie Bühne.
       - Bei Klick (Selektion) blendet sich direkt unter dem 36x36px Viereck ein eleganter Badge mit Border, Name und Adresse ein (`#18181B`, `border: 1px solid #38BDF8`).
       - Gehäuse erhält bei Selektion einen leuchtenden Cyan-Rahmen (`#38BDF8`).
   - Scheinwerfer frei per Drag & Drop aus dem Project Explorer auf der Bühne platzierbar und mit der Maus verschiebbar.
   - **Tastatur-Löschung (`Entf` / `Backspace`):** Markierte Scheinwerfer (Einzel- oder Mehrfachauswahl via Marquee) können direkt per `Entf` (Delete) oder `Backspace` von der Bühne und aus dem Explorer entfernt werden.
   - **Bidirektionale Selektions- & Deselektions-Synchronisation:**
     - Klick auf der Bühne selektiert das Element im Project Explorer und klappt geschlossene Ordner automatisch auf.
     - Klick im Project Explorer auf ein Gerät selektiert den Scheinwerfer auf der Bühne (inkl. Gizmo und Dock-Sync).
     - Klick im Project Explorer auf eine Gerätegruppe selektiert alle zur Gruppe gehörenden Scheinwerfer auf der Bühne gleichzeitig.
     - Klick auf eine freie Fläche der 2D-Bühne hebt die Selektion auf der Bühne und im Project Explorer synchron auf.
     - Klick auf eine freie Fläche des Project Explorers hebt die Selektion im Explorer und auf der Bühne synchron auf.
   - Vektor-Beams simulieren Abstrahlrichtung, Pan/Tilt-Winkel, Farbe und Helligkeit in Echtzeit an die Wand.
3. **Rechte Leiste (Project Explorer):**
   - Durchgehende Seitenleiste über die volle Höhe rechts neben Bühne und Dock.
   - Baumansicht (`TreeView`) mit 5 aufklappbaren Standard-Ordnern (`Geräte`, `Gerätegruppen`, `Presets`, `Szenenlisten`, `Effekte`).
   - **Gerätegruppen (`GROUP`):**
     - Erstellen über `+` -> `Gerätegruppe` übernimmt automatisch alle aktuell auf der Bühne ausgewählten Scheinwerfer.
     - Drag & Drop eines Scheinwerfers auf eine Gruppe fügt diesen zur Gruppe hinzu.
     - Vollständig persistent im JSON-Projektmodell (`GroupDataDto`) gespeichert und wiederhergestellt.
   - Neue/unkategorisierte Items erscheinen frei unterhalb der Standard-Ordner.
   - **Umfassendes Drag & Drop:**
     - Items können per Drag & Drop in Standard-Ordner einsortiert, zwischen Ordnern verschoben oder im Root-Bereich frei umsortiert werden.
     - Scheinwerfer (`FIXTURE`) können per Drag & Drop direkt auf die 2D-Bühne (`StageCanvasView`) gezogen und an den Zielkoordinaten platziert sowie anschließend frei verschoben werden.
   - Flexibel dünner/dicker ziehbar per SplitPane-Divider.
   - Einklapp- & Ausklapp-Button im Explorer-Header mit Lucide-Icons (`arrow-right-to-line` zum Einklappen, `arrow-left-to-line` zum Ausklappen), merkt sich die vorherige Benutzerbreite.
4. **24/7 Steuerungs-Dock unten (Kompaktes DMXControl 3 Style Device Control):**
   - **Kompakte Leiste:** Bauform mit 220px Höhe (`DOCK_HEIGHT = 220.0`), scharfkantiges Moduldesign (0px Radius nur auf den Modulkarten), garantiert >60% vertikalen Raum für die obere Arbeitsfläche.
   - **Intensität:** 5er-Schnelltasten (`VL`=100%, `70`, `50`, `30`, `0`) vollflächig gestapelt, 2 vertikale Fader (`Dimmer`, `Strobo` – ohne Shutter), 2x2 Tasterblock (`Auf`, `Zu`, `Lumos` [100%], `Nox` [0%]).
   - **Farbe:** Großer **HSV-Farbkreis (98x98px)** bis ca. 0.5cm an den Rand (ohne HSV-Schriftzug) sowie je 4 anpassbare Preset-Buttons links und rechts vom Rad (Linksklick: anwenden, Rechtsklick: speichern).
   - **Position:** Großes **2D-XY-Fadenkreuz (96x96px)** bis ca. 0.5cm an den Rand mit Pan-Slider darunter und Tilt-Slider rechts sowie einem extra **`Home`**-Button im Header zur schnellen Zentrierung auf 50%/50%.

---

### Tab 2: LightDesk (QLC+ Fader-Pult)

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

### Beat-System (Show-Timeline)

#### 1. Automatische Beat-Erkennung

Sobald ein Lied in der Show-Timeline geladen ist, kann per Klick auf den Button **„🔍 Beats erkennen"** (Toolbar der Timeline) eine automatische Analyse gestartet werden.

- Der `BeatAnalyzerService` analysiert die MP3/WAV-Datei offline (im Hintergrund-Thread, UI bleibt reaktionsfähig).
- Ergebnis: Liste von Beat-Timestamps (ms-genau) + ermittelter BPM-Wert.
- Jeder erkannte Beat wird als **roter senkrechter Strich** auf der Wellenform in der Timeline angezeigt.
- Es gibt nur einen einzigen Beat-Typ: **„Beat"** — unabhängig davon, ob er automatisch erkannt oder manuell hinzugefügt wurde. Kein visueller Unterschied.

#### 2. Manuelle Verfeinerung

Da die automatische Erkennung nicht immer perfekt ist (z. B. falsche Beats bei reinen Vokal-Passagen), können Beats manuell angepasst werden:

| Aktion | Bedienung |
| :--- | :--- |
| **Beat löschen** | Klick auf roten Marker (wird ausgewählt) → `Entf` oder `Backspace` |
| **Beat verschieben** | Marker anfassen und horizontal ziehen |
| **Beat manuell hinzufügen** | Doppelklick auf die Wellenform an der gewünschten Stelle |
| **Alle Beats löschen** | „Beats löschen"-Button in der Toolbar |

#### 3. Beat Record (MIDI-Tap)

Für das präzise Hinzufügen von Beats während der Wiedergabe gibt es den **Beat Record Modus**:

- **Aktivierung:** Button **„⏺ BEAT REC"** in der Timeline-Toolbar drücken (alternativ Taste `R`).
- Der Button leuchtet rot / blinkt, solange der Modus aktiv ist.
- Das Lied läuft per `▶ PLAY` weiter.
- Jeder **MIDI-Pad-Treffer** (per MIDI-Learn zuweisbar) oder Tastendruck (`B` als Fallback ohne MIDI) setzt exakt zum aktuellen Playback-Zeitstempel einen neuen Beat-Marker.
- Der Marker erscheint **sofort live** auf der Wellenform.
- **Deaktivierung:** „⏺ BEAT REC" nochmal drücken.

#### 4. Beat-Export (ID3 TXXX-Tag)

Beim Rechtsklick auf ein Lied in der Timeline → **„Exportieren mit Beats"** wird die Originaldatei exportiert:

- Das Audio wird **vollständig unverändert** übernommen (identische Qualität, keine Neukodierung).
- Die Beat-Daten (BPM + alle Timestamps in ms) werden als **ID3v2 TXXX-Tag** mit dem Key `JDMX_BEATS` in die MP3-Datei eingebettet:

```json
{
  "bpm": 128,
  "beats": [0, 468, 936, 1404, 1872]
}
```

- Die exportierte Datei bleibt eine **normale `.mp3`** — in jedem Player abspielbar.
- JDMX kann diese Datei beim erneuten Laden erkennen und die Beats direkt wiederherstellen (kein erneutes Analysieren nötig).
- **Bibliothek:** `JAudioTagger` (Maven-Dependency).

#### Neue Komponenten

| Komponente | Package | Beschreibung |
| :--- | :--- | :--- |
| `BeatAnalyzerService` | `service` | Offline-Analyse von MP3/WAV, liefert BPM + `List<BeatMarker>` |
| `BeatTapService` | `service` | Wertet MIDI/Tastatur-Taps während Playback aus, erzeugt `BeatMarker` |
| `BeatMarker` | `core.model` | Timestamp (ms) — kein weiteres Unterscheidungsmerkmal |
| `BeatExportService` | `service` | Schreibt/liest Beat-JSON via ID3 TXXX-Tag mit JAudioTagger |
| Beat-Marker Rendering | UI / Timeline-Canvas | Rote senkrechte Linien auf der Wellenform |

#### Roadmap-Eintrag

- [ ] **Phase 4b: Beat-System** — `BeatAnalyzerService`, `BeatTapService` (MIDI/Tastatur), Beat-Marker Rendering in Timeline-Canvas, Beat Record Modus, `BeatExportService` (JAudioTagger / ID3 TXXX), Beats beim Laden automatisch wiederherstellen. TDD 100%.

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
- [ ] **Phase 6: Die 5 Workspace-Tabs** (Stage & Fixtures, LightDesk, Button Matrix, Show-Timeline, Input/Output).
- [ ] **Phase 7: Dokumentation & Packaging** (`ONBOARDING.md`, `ARCHITECTURE.md`, `README.md`, Fat-JAR).

---

## 6. Änderungshistorie (Living Requirements)

| Datum | Komponente / Anforderung | Änderung / Spezifikation |
| :--- | :--- | :--- |
| **2026-11-04** | **Tab 1: Steuerungsdock (DMXControl-Stil)** | Unteres 24/7 Dock auf feste Mindesthöhe von 135px verankert. Modul 1: `Intensität` horizontal auf feste 300px entkoppelt mit großzügigem Randabstand zur Farbkante, Tasten komplett eckig mit 1px Umrandung im Hardware-Look. Modul 2 & 3: `Farbe` und `Position` teilen sich im dynamischen SplitPane stabil 50/50 den restlichen Raum ohne Jitter/Wander-Effekt. |
| **2026-11-05** | **Tab 1: Dashboard-Höhe auf 220px skaliert** | Standard- und Mindesthöhe des Steuerungsdocks von 135px auf **220px** angehoben (`DOCK_HEIGHT = 220.0`), um Fader, Farbrad und 2D-XY-Pad noch mehr vertikalen Raum und Bedienkomfort zu bieten. |
| **2026-11-05** | **Tab 1: Project Explorer (Rechte Seitenleiste)** | Durchgehender `Project Explorer` über die gesamte rechte Fensterhöhe (neben Bühne und Dock). 5 aufklappbare Standard-Ordner (`Geräte`, `Gerätegruppen`, `Presets`, `Szenenlisten`, `Effekte`) mit Drag & Drop-Sortierung, unkategorisierten Items unterhalb, zentriertem grünem `[ + ]` Hinzufügen-Menü (`ExplorerAddMenu`) und Einklapp-Button (`arrow-right-to-line` / `arrow-left-to-line`). Drag & Drop ist strikt auf die Baumstruktur im Explorer beschränkt. |
| **2026-11-05** | **Tab 2: Umbenennung in LightDesk** | Tab 2 von `Simple Desk` in **`LightDesk`** umbenannt, um eine moderne, eigenständige Nomenklatur zu gewährleisten. |
| **2026-11-14** | **Tab 4: Beat-System** | Automatische Beat-Erkennung per `BeatAnalyzerService` (rote Marker auf Wellenform), manuelle Verfeinerung (Löschen per `Entf`/`Backspace`, Verschieben, Doppelklick zum Hinzufügen), Beat Record Modus per MIDI-Tap (`⏺ BEAT REC`-Button + Taste `R`). Export: Beat-Daten (BPM + Timestamps) als ID3v2 TXXX-Tag `JDMX_BEATS` via JAudioTagger — Audio bleibt qualitätsidentisch. Neue Phase 4b in der Roadmap. |

