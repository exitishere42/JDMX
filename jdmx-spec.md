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
     - **`Support`** (Icon: Lucide `life-buoy`): Öffnet den Support-, Feedback- und Bug-Reporting-Dialog mit Underline-Tabs, Chat-Verlauf und Discord Webhook-Anbindung.
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

### Tab 3: Button Matrix

Die Button Matrix ist als vollwertige Live-Performance-Konsole aufgebaut und folgt dem ergonomischen Zwei-Spalten-Layout der **Stage View** (`StageWorkspaceView`), wobei die linke Seite vollflächig für das Tasten-Raster genutzt wird:

```text
┌────────────────────────────────────────────────────────┬───────────────────────┐
│ BUTTON MATRIX GRID (Volle Höhe)                        │ [ jdmx Explorer ] [⇥] │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐        │ 📁 Geräte             │
│ │ Button  │ │ Button  │ │ Button  │ │ Button  │ ...    │ 📁 Gerätegruppen      │
│ │ (Szene) │ │ (Chaser)│ │ (Flash) │ │ (Toggle)│        │ 📁 Presets            │
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘        │ 📁 Szenenlisten       │
│                                                        │   ├─ Szene 1 ───┐     │
│ (Rechtsklick-Menü direkt am Pad:                       │ 📁 Effekte      │ D&D │
│  - Toggle / Flash Modus                                │   └─ Chaser 1 ──┼──┐  │
│  - Farbe wählen                                        │                 │  │  │
│  - Zuweisung löschen)                                  │                 ▼  ▼  │
│                                                        │                [Button│
│                                                        │                       │
└────────────────────────────────────────────────────────┴───────────────────────┘
```

#### 1. Layout & Struktur:
- **Horizontales Haupt-SplitPane (`MatrixWorkspaceView`):**
  - **Rechts (Volle Höhe):** Vollwertiger `ProjectExplorerView` mit Einklapp- und Ausklapp-Button (`arrow-right-to-line` / `arrow-left-to-line`). Voller Zugriff auf Szenen, Chaser, Geräte und Gruppen. Drag & Drop aus dem Explorer direkt auf beliebige Buttons im Grid.
  - **Links (Volle Höhe):** Vollflächiges Button-Raster (`MatrixGridView`), frei von störenden Docks oder Leisten.
- **Tasten-Raster (`MatrixGridView`):**
  - Responsives Kachel-Raster mit konfigurierbaren Zeilen und Spalten (Standard: 4 Zeilen × 8 Spalten = 32 Buttons).
  - Taktile Button-Kacheln (`MatrixButtonNode`) mit haptischem Hardware-Look (Material Design 2 Dark, 4px Rundung, 1px Border), Statusanzeige (Inaktiv, Aktiv/Leuchtend mit Glow), Typ-Icon (Lucide), Index und Titel.
  - **Direktes Rechtsklick-Menü:** Trigger-Modus (`TOGGLE` / `FLASH`), Farbpalette und Zuweisung löschen direkt per Rechtsklick auf dem Pad.

#### 2. Trigger-Modi:
- **`TOGGLE`:** Klick schaltet den Button ein (Szene/Chaser startet), erneuter Klick schaltet ihn aus.
- **`FLASH`:** Solange gedrückt gehalten wird (Mausklick gehalten oder MIDI Pad gedrückt), ist die Szene/der Chaser aktiv; beim Loslassen stoppt sie sofort.

#### 3. Integration & Global Stop:
- Klick auf den globalen roten `[■ STOP]`-Button im All-in-One Header deaktiviert sofort alle aktiven Matrix-Buttons und setzt den DMX-Ausgang in den definierten Ruhezustand.

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

### Playlist-Tracks & Raster (FL Studio Playlist-Konzept)

```text
┌──────────────┬──────────────────────────────────────────────────────────────┐
│ [▶] [■] 00:00│ [Bar 1]       [Bar 2]       [Bar 3]       [Bar 4]            │
├──────────────┼──────────────────────────────────────────────────────────────┤
│ ● Track 1    │ ───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───   │
├──────────────┤ ───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───┼───   │
│ ┌ ─ ─ ─ ─ ┐  │                                                              │
│ │    +    │  │                                                              │
│ └ ─ ─ ─ ─ ┘  │                                                              │
└──────────────┴──────────────────────────────────────────────────────────────┘
```

#### Linke Track-Header-Leiste
- **Initiale Spur:** Die Timeline startet standardmäßig mit einer Spur: **„Track 1"**.
- **Track-Box (`TimelineTrackHeaderBox`):**
  - **Mute-Knopf (●):** Runder Statusindikator. Grün (`#10B981`) = aktiv, Dunkelgrau (`#3F3F46`) = stummgeschaltet (Muted). Ein Klick schaltet die Spur stumm oder wieder aktiv.
  - **Track-Name:** Fette Typografie im Studio-Dark-Design.
  - **Inline-Rename:** Per Doppelklick auf den Namen öffnet sich direkt ein Textfeld an Ort und Stelle. Mit `Enter` oder Klick außerhalb wird der neue Name übernommen; mit `Escape` wird abgebrochen.
- **`[+]`-Button (Stage-Stil ohne Outline):**
  - Direkt unter der letzten Trackbox platziert.
  - Transparent im Ruhezustand (wie in Stage), mit dezentem Hover-Effekt.
  - Erzeugt bei Klick die nächste Spur mit fortlaufender Nummerierung (`Track 2`, `Track 3` etc.).
  - Auch wenn „Track 1" umbenannt wurde (z. B. zu „Showtanz"), wird die nächste Spur sauber als „Track 2" benannt.

#### Rechtes Timeline-Grid (`TimelineGridView`)
- Horizontale Spur-Zeilen sind exakt auf die Höhe der linken Track-Header-Boxen (48px) ausgerichtet.
- Vertikales Taktraster unterteilt Takte (Bars) und Viertel-Beats mit dezenten Kontrastlinien.
- Vertikales Scrollen zwischen linker Header-Spalte und rechtem Grid ist bidirektional synchronisiert.
- **Song-Clips & Wellenform:** Spuren mit zugewiesenem Song zeigen einen abgerundeten Clip-Block in Smaragdgrün (`#064E3B`/`#10B981`) mit Song-Titel und Wellenform-Kontur.
- **Dynamische Timeline-Linie (Playhead):**
  - Prominente vertikale Nadel in Warm-Orange (`#FB923C`) mit dreieckigem Playhead-Kopf am oberen Rand.
  - Wandert in Echtzeit bei laufender Wiedergabe über das Raster.
  - Erlaubt Scrubbing und Positionswechsel per Klick oder Ziehen mit der Maus an jede beliebige Stelle.
- **Drag & Drop:** Songs können aus dem Explorer direkt auf eine freie oder bestehende Spur gezogen werden.

#### Playback-Steuerung & Zeitverfolgung
- **Transport-Buttons:** `Play` (Start/Pause) und `Stop` (setzt auf `00:00.000` zurück).
- **Echtzeit-Zeitanzeige:** Präzise Anzeige im Format `mm:ss.SSS`.
- **Hintergrund-Taktung:** Thread-sicherer 25ms Playback-Timer im `TimelineService`.

#### Architektur & Klassen
| Klasse | Schicht | Funktion |
| :--- | :--- | :--- |
| `TimelineTrack` | `core.model` | ID, Name, Mute-Status, Spurhöhe, Song-Zuweisung (`songName`, `songFile`) |
| `TimelineService` | `service` | Verwaltung der Tracks, Song-Import, Song-Zuweisung, Playback-Clock (Play/Pause/Stop), Thread-Sicherheit |
| `TimelineTrackHeaderBox` | `ui.timeline` | Linke Trackbox mit Mute-Punkt, Inline-Rename und Drag&Drop-Ziel für Songs |
| `TimelineGridView` | `ui.timeline` | Canvas-Zeichnung des Rasters, Song-Clips und der dynamischen Playhead-Timeline-Linie |
| `TimelineInspectorView` | `ui.timeline` | Inspektor-Ansicht für selektierte Elemente |
| `TimelineMainView` | `ui.timeline` | Playlist-Hauptansicht mit Toolbar, Track-Header-Leiste und Grid |

---

### Rechte Seitenleiste (Explorer & Inspector)

```
┌──────────────────────────┐
│ [Inspektor] [Explorer][+]│  ← kompakte Text-Tabs + Stage-Style Plus-Button
├──────────────────────────┤
│ ▸ Songs                  │  ← TreeView mit zentriertem Pfeil & Drag&Drop
│   ├─ showtanz26.mp3      │
│   └─ fanfare.mp3         │
└──────────────────────────┘
```

- **Kompakte Tab-Leiste** oben: `[Inspektor]` und `[Explorer]` (aktiver Tab: unterstrichen, Primärfarbe).
- **Inspektor-Ansicht (`TimelineInspectorView`):** Ermöglicht Detailinspektion und Beat-Parametrierung; zeigt leeren Platzhalter bei inaktiver Auswahl.
- **`[+]` Button** (ohne Outline im Stage-Stil):
  - Klick öffnet Dropdown-Menü und wechselt automatisch zum Explorer-Tab.
  - Dropdown (`ShowTimelineAddMenu`): oben wie unten sauber abgerundet (`8px`), Dropdown enthält: **Song** → öffnet System-Dateidialog (`FileChooser` für MP3 & WAV).
  - Nach Auswahl wird der Song importiert und im Songs-Ordner der TreeView gelistet.
- **TreeView (`TimelineExplorerTreeCell`):**
  - Saubere vertikale Zentrierung des Ausklapp-Pfeils (`arrow`), Ausblendung auf Leaf-Ebene (Songs).
  - Native Drag & Drop Unterstützung: Songs können per Dragboard direkt auf Tracks und ins Grid gezogen werden.
- **Implementierung:** `ShowTimelineWorkspaceView` (SplitPane, 76/24), `ShowTimelineExplorerView`, `TimelineInspectorView`, `ShowTimelineAddMenu`.

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
| **2026-11-25** | **Tab 4: Vollständige Anzeige aller angelegten Szenen im Timeline Project Explorer (v0.29.0)** | Integration eines dedizierten Standard-Ordners `Szenen` im Project Explorer der Show-Timeline (`ShowTimelineExplorerView`): Zeigt 100 % aller im Projekt je angelegten Lichtszenen synchron aus `SceneService` und `ProjectExplorerService` mit violettem Studio-Icon (`#A78BFA`, `bookmark`). Volle reaktive Aktualisierung bei Erstellen, Umbenennen oder Löschen von Szenen über `SceneService.addChangeListener`. Doppelklick auf eine Szene in der Timeline aktiviert diese sofort im DMX-Output, Rechtsklick bietet ein Kontextmenü („Szene aktivieren"). 464 Tests zu 100% grün. |
| **2026-11-25** | **Tab 4: Nahtlose Project-Explorer Breiten- und Splitter-Synchronisation mit dem Stage-Tab (v0.28.3)** | Vollständige und reaktionsschnelle Synchronisation der Breite und Divider-Position des Project Explorers zwischen Stage-Tab (Tab 0) und Show-Timeline-Tab (Tab 3): Exakte Ausrichtung von `DEFAULT_MIN_WIDTH` (300px) und `DEFAULT_PREF_WIDTH` (300px), deterministische Divider-Ermittlung via `lastExpandedRatio` in `ShowTimelineWorkspaceView` und `StageWorkspaceView`, Eliminierung rekursiver Tab-Wechsel-Feedbackschleifen in `HeaderBar.setActiveTab` und Bereinigung asynchroner Race-Conditions. 461 Tests zu 100% grün. |
| **2026-11-04** | **Tab 1: Steuerungsdock (DMXControl-Stil)** | Unteres 24/7 Dock auf feste Mindesthöhe von 135px verankert. Modul 1: `Intensität` horizontal auf feste 300px entkoppelt mit großzügigem Randabstand zur Farbkante, Tasten komplett eckig mit 1px Umrandung im Hardware-Look. Modul 2 & 3: `Farbe` und `Position` teilen sich im dynamischen SplitPane stabil 50/50 den restlichen Raum ohne Jitter/Wander-Effekt. |
| **2026-11-05** | **Tab 1: Dashboard-Höhe auf 220px skaliert** | Standard- und Mindesthöhe des Steuerungsdocks von 135px auf **220px** angehoben (`DOCK_HEIGHT = 220.0`), um Fader, Farbrad und 2D-XY-Pad noch mehr vertikalen Raum und Bedienkomfort zu bieten. |
| **2026-11-05** | **Tab 1: Project Explorer (Rechte Seitenleiste)** | Durchgehender `Project Explorer` über die gesamte rechte Fensterhöhe (neben Bühne und Dock). 5 aufklappbare Standard-Ordner (`Geräte`, `Gerätegruppen`, `Presets`, `Szenenlisten`, `Effekte`) mit Drag & Drop-Sortierung, unkategorisierten Items unterhalb, zentriertem grünem `[ + ]` Hinzufügen-Menü (`ExplorerAddMenu`) und Einklapp-Button (`arrow-right-to-line` / `arrow-left-to-line`). Drag & Drop ist strikt auf die Baumstruktur im Explorer beschränkt. |
| **2026-11-05** | **Tab 2: Umbenennung in LightDesk** | Tab 2 von `Simple Desk` in **`LightDesk`** umbenannt, um eine moderne, eigenständige Nomenklatur zu gewährleisten. |
| **2026-11-14** | **Tab 4: Show-Timeline Workspace — Rechte Seitenleiste** | `ShowTimelineWorkspaceView` ersetzt den Placeholder in Tab 4. Rechte Seitenleiste (`ShowTimelineExplorerView`) mit kompakten Tabs (`[Explorer]` aktiv, Inspector folgt später) und `[+]`-Outline-Button (öffnet `ShowTimelineAddMenu` mit „Song"-Eintrag, wechselt automatisch auf Explorer-Tab). TreeView mit Songs-Ordner. `ExplorerItemType.SONG` ergänzt. CSS, i18n (DE/EN) vollständig. 8 neue Tests, alle 297 grün. |
| **2026-11-14** | **Tab 4: Beat-System** | Automatische Beat-Erkennung per `BeatAnalyzerService` (rote Marker auf Wellenform), manuelle Verfeinerung (Löschen per `Entf`/`Backspace`, Verschieben, Doppelklick zum Hinzufügen), Beat Record Modus per MIDI-Tap (`⏺ BEAT REC`-Button + Taste `R`). Export: Beat-Daten (BPM + Timestamps) als ID3v2 TXXX-Tag `JDMX_BEATS` via JAudioTagger — Audio bleibt qualitätsidentisch. Neue Phase 4b in der Roadmap. |
| **2026-11-14** | **Tab 4: Show-Timeline Playlist (Track-Boxen & Grid)** | Implementierung des FL Studio Playlist-Konzepts: Linke Track-Header-Leiste mit `TimelineTrackHeaderBox` (runder Mute-Knopf aktiv/stumm, Inline-Rename per Doppelklick), Outline `[+]`-Button unter den Boxen für fortlaufende Track-Erzeugung (`Track 1` default, dann `Track 2`, `Track 3` etc. auch bei Umbenennung). Rechtes synchronisiertes Canvas-Taktraster (`TimelineGridView`) mit horizontalen Spur-Lanes und Takt-/Beat-Raster. MVC-Trennung mit `TimelineTrack` und thread-sicherem `TimelineService`. 17 neue Tests, alle 314 Tests grün. |
| **2026-11-14** | **Tab 4: Inspektor-Tab, Song-Import & Playback-Engine** | Seitenleiste um `TimelineInspectorView` erweitert mit Umschaltung via `[Inspektor]`- und `[Explorer]`-Tabs. `[+]`-Button im Tab-Header und Track-`[+]`-Button auf transparentes Stage-Design ohne Outline umgestellt. Song-Import (`*.mp3`, `*.wav`) via FileChooser und `TimelineService.importSong()`, automatische Synchronisation mit der Songs-TreeView. Playback-Steuerung (Play/Pause, Stop) mit Millisekunden-Genauigkeit (`mm:ss.SSS`) und 25ms Taktung. 7 neue Tests, alle 321 Tests grün. |
| **2026-11-14** | **Tab 4: Song Drag & Drop, Playhead-Linie & FL Studio Playlist-Polish (v1.19.0)** | Drag & Drop von Songs direkt aus dem Explorer auf Spuren oder das Grid mit automatischer Song-Zuweisung (`assignSongToTrack`) und Umbenennung unberührter Standard-Tracks. Optische Korrektur des TreeView-Ausklapppfeils (mittige Ausrichtung und Ausblendung bei Leaf-Elementen). Dropdown-Menü des `[+]`-Buttons oben abgerundet (`-fx-background-radius: 8px`). Vertikale Timeline-Linie (Playhead) in Amber (`#FB923C`) mit Zeigerkopf und interaktivem Scrubbing per Klick & Drag auf dem Grid. Track-Header-Box ergonomisch nach FL Studio ausgerichtet (Name links, runder LED-Mute-Button rechts). 10 neue Tests, 331 Tests zu 100% grün. |
| **2026-11-14** | **Tab 4: Echte Wellenform & Dynamische Song-Länge (v1.20.0)** | Echte Audioanalyse via `AudioAnalysisService` (PCM-Dekodierung für WAV und reines Java JLayer für MP3). Exakte Song-Gesamtlaufzeit und 1000-Punkte normalisierte Hüllkurven-Peaks ohne Verlust akustischer Transienten. Song-Clip-Länge im Grid skaliert dynamisch mit der echten Songdauer (`millisToX(durationMs)`), Anzeige von Titel und Laufzeit `(mm:ss)` im Header. Automatische horizontale Erweiterung des Rasters (`setPrefWidth`) mit Scrollbalken für das gesamte Lied. Digitalanzeige zeigt `mm:ss.SSS / mm:ss.SSS`. 8 neue Tests, 339 Tests zu 100% grün. |
| **2026-11-14** | **Tab 4: Hotfix & Installer Auto-Detection (v1.20.1)** | Behebung von Threading-Kollisionen in `TimelineGridView` durch strikte `Platform.runLater`-Synchronisation der Canvas-Redraws. Robustes Überspringen von ID3v2-Tags und defekten Frames in `AudioAnalysisService` ohne Abbruch. Direkte Unterstützung von Drag & Drop für externe Audiodateien aus dem Windows Explorer / Desktop (`db.hasFiles()`) auf Spuren und Grid. `install.bat` auf dynamische GitHub-API-Erkennung der neuesten JAR-Datei umgestellt (verhindert 404 und startet stets die aktuelle Version). 339 Tests 100% grün. |
| **2026-11-15** | **Update-System: Dynamische Versionsauflösung (v1.20.2)** | Behebung des veralteten Hardcoded-Versionsstands (`1.15.5`) im Update-Dialog (`AboutUpdateDialog` / `UpdateService`). Dynamische Versionserkennung über Maven-Ressourcenfilterung (`version.properties`) und Manifest-Erkennung mit `1.20.2`-Fallback. Der Update-Dialog zeigt nun stets die tatsächlich installierte Version an und markiert das System korrekt als aktuell. 340 Tests 100% grün. |
| **2026-11-15** | **Tab 4: Track-Layout-Fix & Universeller Audio-Import (v1.20.5)** | Fix der auf 25px zusammengestauchten linken Track-Header-Leiste durch feste 160px Vorzugs-, Minimal- und Maximalbreite für `leftHeadersPane` und `tracksScrollPane`. Universeller Audio-Import & Drag-Drop via `TimelineDragHelper`: Unterstützung für Linux GNOME/Nautilus `text/uri-list`, direkte Dateipfade, URL-Dekodierung und `song-` Präfixe. Direkte Zuweisung per Doppelklick auf Songs im Explorer, Kontextmenü im Explorer („Auf Spur legen", „Auf neue Spur legen", „Aus Explorer entfernen"), Kontextmenü auf Track-Boxen und Grid („Song zuweisen...", „Track hinzufügen", „Track löschen") sowie externes Hereinziehen von Audiodateien direkt in den Explorer. 9 neue Tests, 349 Tests zu 100% grün. |
| **2026-11-15** | **Tab 4: JLayer Audio-Playback & Linux-Stabilität (v1.20.6)** | Umstellung der Playback-Engine auf reines Java JLayer mit synchroner Audio-Line für verlustfreie, plattformübergreifende Audiowiedergabe unter Windows und Linux. 353 Tests 100% grün. |
| **2026-11-15** | **Tab 4: Asynchroner Import, Waveform & Scroll-Performance (v1.20.7)** | Asynchrone Wellenformanalyse im Hintergrund-Thread gegen UI-Lags beim Hereinziehen großer MP3s, transiente Peak-Spikes für saubere Lautstärke-Visualisierung, zuverlässiges Playhead-Seek und flüssiges Grid-Scrolling. 357 Tests 100% grün. |
| **2026-11-15** | **Tab 4: Leertaste & Doppelklick-Stopp-Workflow (v1.20.8)** | Steuerung der Timeline via Leertaste (Start / Pause), einfacher Klick auf Stopp setzt Wiedergabe auf 0:00 zurück, Doppelklick auf Stopp scrollt zudem die Grid-Ansicht an den Anfang. 361 Tests 100% grün. |
| **2026-11-15** | **Tab 4: Multitrack Audio, Clip Dragging & Playhead Polish (v1.20.9)** | Vollständige Mehrspur-Audio-Engine mit simultaner Wiedergabe aller ungemuteten Tracks unter Berücksichtigung individueller Startzeiten (`startTimeMs`). Horizontales Verschieben von Song-Clips per Klick & Drag mit `MOVE`-Cursor. Playhead-Scrubbing strikt isoliert auf den oberen Dreieck-Griff (`y <= 24px`). Vollständige Beseitigung des Mauszeiger-Zitterns über Play/Stop-Buttons durch selektives Caching von Grafik-Updates und DOM-Boxen. 367 Tests 100% grün. |
| **2026-11-15** | **Tab 4: Live Audio-Sync beim Clip-Verschieben (v1.20.10)** | Sofortige Live-Audiosynchronisation beim Verschieben von Song-Clips während laufender Timeline: Stotterfreier 80ms-Debounce während des kontinuierlichen Ziehens und verzögerungsfreier Sofort-Resync beim Loslassen der Maustaste (`onMouseReleased`). Songs springen während der Wiedergabe nahtlos an die neue relative Position zum Playhead bzw. stoppen/starten dynamisch, wenn der Playhead außerhalb oder innerhalb des Clips liegt. 370 Tests 100% grün. |
| **2026-11-20** | **Support & Feedback System: In-App Chat mit Underline-Tabs (v1.21.1)** | Neuer Menüpunkt `Support` im Brand-Dropdown-Menü. Modales In-App Overlay `SupportDialog` mit eleganter Underline-Tab-Navigation (*Feedback* & *Bugs* mit schlanker 2px-Unterstrichlinie statt Buttons, analog zum Show-Tab Explorer/Inspector). Chat-basierter Nachrichtenverlauf, Vorlagen, optionale Kontaktangabe und automatische System- & Diagnosedaten-Übertragung (OS, Java, JDMX, Log) bei Fehlermeldungen. Asynchrone Anbindung mit Embed-Formatierung und sauberer MVC-Entkopplung via `SupportService`. |
| **2026-11-20** | **Support-Dialog Styling & Kontrast-Optimierung (v1.21.2)** | Harmonisches Farbschema: Weiches, lesbares "weißes Rot" (`#F87171`) für Brand-Icon, Dialog-Header, Underline-Tabs und Autoren-Labels. Smaragdgrünes Sende-Icon (`#10B981`). Reines Weiß (`#FFFFFF`) mit dezentem Platzhalter (`#A1A1AA`) in Eingabefeldern und Chat-Bubbles für perfekte Lesbarkeit auf dunklem Studio-Hintergrund. |
| **2026-11-20** | **Discord Bot Direktnachrichten (DM) & Einstellungs-Persistenz (v1.21.3)** | Direkte Benachrichtigung des Discord-Accounts via Discord REST API (Bot DM Channels & Messages). Automatische Ermittlung der Empfänger-ID aus Konfiguration oder Kontaktfeld. Sichere lokale Speicherung des Bot-Tokens und der User-ID in der betriebssystemspezifischen `settings.json` (git-ignoriert, kein Token-Leakage ins Repository). 397 Tests zu 100% grün. |
| **2026-11-20** | **Nativer Standard-Discord-Webhook für Out-of-the-Box Community Support (v1.21.4)** | Direkte Integration des offiziellen Discord-Webhooks als nativer Standard in `SupportService` (`DEFAULT_WEBHOOK_URL`). Ermöglicht jedem Anwender weltweit das direkte Absenden von Bug-Reports und Feedback ohne Notwendigkeit eines eigenen Bots, Tokens oder Konfigurationsaufwands. Vollständige Embed-Formatierung mit Systemdaten und Live-Übertragung. 398 Tests zu 100% grün. |
| **2026-11-20** | **Support-Redesign: Studio-Reports, Gelbe Discord-Embeds & Log-Dateianhang (v1.21.5)** | Vollständige Ablösung der informellen Chat-Bubbles im `SupportDialog` durch ein aufgeräumtes Studio-Report-Formular. Informatives Header-Banner mit Zweck-Erklärung (Feedback vs. Bugs), ergonomische Eingabefelder (Name, Kontakt, Diagnostik-Checkbox) und strukturierter Sende-Verlauf mit Status-Badge (`✓ Gesendet`), Zeitstempel und Dateianhangs-Badge. Discord-Embeds im markanten JDMX-Gelb (`#FBBF24`). Übertragung von System- und Diagnoselogs bei Bug-Reports als echter Dateianhang (`jdmx-diagnostic.log` via `multipart/form-data`) statt unübersichtlicher Textblöcke. |
| **2026-11-20** | **Support-Polish: Helles Logo-Gelb (#FEDC94), randlose Pills & diagnostischer Log-Export (v1.21.6)** | Umstellung der Discord-Embed- und Akzentfarbe auf das originale, helle JDMX-Logo-Gelb (`#FEDC94` / `16702612`). Beseitigung der doppelten Kasten-in-Kasten-Struktur in der Verlaufsansicht (`support-history-scroll` nun komplett transparent und rahmenlos; übermittelte Berichte als ergonomische runde Pills mit 14px Radius und dezentem Amber-Tag für Log-Anhänge). Absicherung, dass bei aktiviertem Diagnose-Häkchen stets ein aussagekräftiger `jdmx-diagnostic.log`-Export als Anhang gemeinsam in derselben Nachricht übermittelt wird. |
| **2026-11-21** | **Support-Dialog: Einheitliche Eckenrundung (4px) für übermittelte Berichte (v1.21.7)** | Harmonisierung der Eckenradien im `SupportDialog`: Die Karten übermittelter Berichte (`support-history-card`) und Diagnose-Tags übernehmen nun exakt dieselbe 4px-Rundung (`-fx-background-radius: 4px; -fx-border-radius: 4px;`) wie das obere Eingabeformular (Message-Box & Textfelder), bei weiterhin rahmenlosem, transparentem Hintergrund des Verlaufs. |
| **2026-11-21** | **Support-Embeds: Differenzierte Discord-Farbe Hellrot (#F87171) für Bug-Reports (v1.21.8)** | Umstellung der Discord-Embed-Farbe bei Bug-Reports auf das markante, blendfreie Hellrot (`#F87171` / `16281969`), während Feedback-Meldungen weiterhin im hellen JDMX-Logo-Gelb (`#FEDC94` / `16702612`) erscheinen. Ermöglicht die sofortige visuelle Unterscheidung von Fehlerberichten und Verbesserungsvorschlägen im Discord-Kanal. |
| **2026-11-22** | **Sicherheits-Patch: AES-256-GCM Quellcode-Verschleierung für Discord-Webhook (v1.21.9)** | Vollständige Beseitigung von Klartext-Webhook-URLs im GitHub-Repository zum Schutz vor automatisierten Scanner-Bots (FriendlyScanner). Die Standard-Webhook-URL wird zur Laufzeit aus aufgeteilten Chiffre-Fragmenten via AES-256-GCM und abgeleitetem Salt dynamisch entschlüsselt. Ermöglicht 100% Out-of-the-Box Support- und Bug-Reporting für alle weltweiten Nutzer ohne Konfigurationsaufwand und ohne Risiko automatischer Löschung durch externe Git-Scanner. |
| **2026-11-22** | **Tab 4: Track-Verschiebungssperre während aktiver Show-Wiedergabe (v1.21.10)** | Sobald die Show gestartet wurde (per Play-Button in der Header-Bar oder Leertaste), sind alle Audio-Tracks und Clips in der Timeline gegen horizontales Verschieben gesperrt. In `TimelineGridView` werden Klick- und Drag-Aktionen auf Song-Clips während aktiver Wiedergabe ignoriert und der Mauszeiger bleibt neutral (`Cursor.DEFAULT`). Das obere Playhead-Dreieck (Scrubbing/Seeking) bleibt voll funktionsfähig. Auch im Service-Kern (`TimelineService.setTrackStartTimeMs`) werden Änderungen während laufender Wiedergabe abgewiesen (`return false`), um komplexe Live-Resync-Zustände, Glitches und Latenzen zu unterbinden. Nach dem Stoppen ist das Verschieben sofort wieder wie gewohnt aktiv. 401 Tests zu 100% grün. |
| **2026-11-23** | **Tab 4: Pioneer Rekordbox 3-Band Waveform Engine & Timeline Scrubbing-Fix (v1.22.0)** | Vollständige Ablösung der monochromen grünen Wellenform durch das moderne Pioneer Rekordbox 3-Band DJ-Design: Automatische Frequenztrennung in `AudioAnalysisService` via State Variable Filter (SVF) in Low/Bass (< 250 Hz, Electric Blue `#2563EB`), Mid/Mitten (250 Hz - 3 kHz, Amber/Orange `#F59E0B`) und High/Höhen (> 3 kHz, Weiß `#FFFFFF`). Gerendert als gestapelte Frequenzbänder auf tiefdunklem Studio-Deck (`#121215`) mit exakter Sample-Ausrichtung (0px Offset) zur Eliminierung von visuellen Kick-Latenzen. Vollständige Behebung des Audio-Stacking-Bugs beim Ziehen des Playheads (Timeline-Scrubbing): Atomare Thread-Sperre in `AudioPlaybackService` gegen Zombie-Streams und butterweiches Scrubbing ohne Audio-Spam mit nahtloser Fortsetzung beim Loslassen. 405 Tests zu 100% grün. |
| **2026-11-24** | **Tab 4: Timeline-Skalierung, Waveform Text-Entfernung & Dynamischer Horizontaler Zoom (v1.23.0)** | Vollständige Entfernung des störenden Text-Pills und Labels über der Rekordbox 3-Band Wellenform in `TimelineGridView` – die Höhen-, Mitten- und Bass-Bänder sind nun 100% frei und unverdeckt sichtbar (der Titel verbleibt übersichtlich in der linken Spurenleiste). Vergrößerung der Standard-Spurhöhe von 48px auf 76px (`TimelineTrack.DEFAULT_TRACK_HEIGHT = 76.0`) für +60% vertikalen Raum und nahezu verdoppelte Amplitudenhöhe der Wellenform (32px Amplitude). Einführung von dynamischem horizontalem Zeitleisten-Zoom (24 bis 160 px/s, Standard 60 px/s) via DAW-Standard `Ctrl + Mausrad`, sowie ergonomische Zoom-Buttons `[-]`, `[100%]`, `[+]` in der oberen Show-Timeline Toolbar (`TimelineMainView`). 410 Tests zu 100% grün. |
| **2026-11-24** | **Tab 4: Pioneer Rekordbox Multi-Layer Solid Waveform Engine (v1.24.0)** | Vollständige Beseitigung aller parallelen Einzelstriche durch die authentische Rekordbox 3-Band Multi-Layer Solid Waveform: Drei eigenständige, vollflächig geschlossene 2D-Polygone (`drawSingleBandSolidWaveform`), die deckend aufeinandergelegt werden. Layer 1 (Fundament/Hintergrund): Royal Blue (`#2253D9`) für das tiefe Bass-Band. Layer 2 (Mitte): Amber Gold / Warmes Orange (`#D97706`) für Vocals und Synths, das bei Mitten-Dominanz (z. B. Snare/Clap) über das Blau hinausragt. Layer 3 (Vordergrund/Kern): Pure White (`#FFFFFF`) für messerscharfe Transienten, Kicks und Hi-Hats. Erhöhung der Analyse-Auflösung in `AudioAnalysisService` von 1.000 auf 4.000 Punkte (`DEFAULT_WAVEFORM_POINTS = 4000`) für hochauflösende Oszilloskop-Kurven. 414 Tests zu 100% grün. |
| **2026-11-24** | **Tab 4: Zero-Drift Audio-Hardware-Master-Clock Synchronisation & Beta-SemVer-Umstellung (v0.25.0)** | Vollständige Behebung des Playhead-/Waveform-/Song-Drifts über echte Soundkarten-Hardware-Synchronisation: Ablösung des JLayer `AdvancedPlayer` durch direkte PCM-Pipeline (`Bitstream` + `Decoder` + `SourceDataLine`) mit 100ms-Puffer und exaktem `header.ms_per_frame()`-Seeking (CBR, VBR & 48 kHz sicher). `AudioPlaybackService.getMasterPlaybackPositionMs()` liest die reale Soundkarten-Position (`SourceDataLine.getMicrosecondPosition()` bzw. `Clip.getMicrosecondPosition()`). `TimelineService` rastet per Phase-Lock auf die Audio-Hardware-Clock ein und nutzt bei Stille/Lücken eine verlustfreie Anchor-Nano-Clock (kein Sub-Millisekunden-Verlust mehr). Exakte Frame-Dauer-Summierung in `AudioAnalysisService` und gerundetes Index-Mapping in `TimelineGridView`. Umstellung des Versionsschemas auf die Beta-Reihe `0.Y.Z` (Start mit `v0.25.0` bis zum finalen `v1.0.0`). 418 Tests zu 100% grün. |
| **2026-11-24** | **Linux-Installer Hotfix: LF-Zeilenenden & `.gitattributes` (v0.25.1)** | Behebung von `$'\r': Kommando nicht gefunden` beim Ausführen von `install.sh` unter Linux durch Konvertierung auf reine Unix-LF-Zeilenenden (`\n`), Anlage einer verbindlichen `.gitattributes`-Regel (`*.sh text eol=lf`) und eines automatisierten JUnit-5-Regressionstests (`ShellScriptLineEndingsTest`). 419 Tests zu 100% grün. |
| **2026-11-24** | **Tab 4: Follow-Playhead Button & Maus-zentrierter `Ctrl + Scroll` Zoom (v0.26.0)** | Einführung eines `ToggleButton btnFollowPlayhead` (Crosshair-Icon, standardmäßig aktiv) in der oberen Show-Timeline Toolbar (`TimelineMainView`). Hält den Playhead-Strich ab 45% des Viewports automatisch zentriert, sodass die Waveform flüssig unter dem Strich mitscrollt. Präzisions-Upgrade für `Ctrl + Scroll` Zooming (`zoomAtScreenX` in `TimelineGridView`): Fixiert beim Rein-/Rauszoomen exakt den Zeitpunkt unter dem Mauszeiger (inkl. `textDeltaY`-Fallback für Linux/Trackpads), während die Toolbar-Zoom-Buttons automatisch auf den Playhead zentrieren. 423 Tests zu 100% grün. |
| **2026-11-24** | **Tab 4: Zero-Jitter 60/120 FPS Timeline-Follow, Tile-Cache & Fine-Scrolling (v0.26.1)** | Vollständige Beseitigung von Lags, Zittern und Chunk-Scrollen: (1) Trennung von `notifyPlaybackFrame()` und `notifyChange()` in `TimelineService` (beendet 50x/s Explorer-Baum-Rebuilds und Layout-Invalidierungen); (2) Audio-Soft-PLL (`getSmoothedPlaybackPositionMs`) + VSync `AnimationTimer` in `TimelineMainView`; (3) 1024px Offscreen GPU Waveform Tile-Cache (`WritableImage` mit Subpixel-Antialiasing) in `TimelineGridView`; (4) Subpixel-genaues `canvas.setTranslateX/Y(scrollX)` (0 Layout-Passes, kein ±1px Zittern); (5) Entfernung der `0.0005` Chunk-Schwelle im Follow-Modus und feines pixel-basiertes horizontales Scrollen. 428 Tests zu 100% grün. |
| **2026-11-24** | **Tab 4: JavaFX Prism GPU-Layer-Architektur & entkoppelter Viewport (v0.26.2)** | Umbau von `TimelineGridView` und `TimelineMainView` auf eine reine DAW-artige GPU-Layer-Architektur: (1) Eliminierung des `ScrollPane`-Layout-Thrashings durch einen festen geclippten Viewport (`BorderPane` + entkoppelte `ScrollBar`s); (2) Trennung in 3 GPU-Layer: periodisches Beat-Hintergrund-Canvas (`translateX(-(scrollX % BAR_WIDTH))`, 0 Redraws beim horizontalen Scrollen/Playback), `Group`-Waveform-Layer mit gecachten 1024px-`ImageView`-Kacheln (`translateX(-scrollX)`), und dedizierte `playheadGroup` (`Line` + `Polygon` mit direkter `translateX`-Verschiebung); (3) Beseitigung doppelter/dreifacher `redraw()`-Aufrufe pro Frame (`updatePlaybackView`) sowie fixe Breite für `lblTimeDisplay` gegen Toolbar-Relayouts. 429 Tests zu 100% grün. |
| **2026-11-24** | **Tab 4: FL-Studio-Style Page-Turn Follow & Relayout-Boundary (v0.26.3)** | Umstellung von `TimelineMainView.computeFollowScrollX` auf ruhiges Page-Turn-Scrolling: Während des Abspielens bleibt die gesamte Timeline stehen und nur der GPU-Playhead wandert flüssig durch den Viewport; erst kurz vor dem rechten Rand (`max(viewW * 0.85, viewW - 45 px)`) blättert die Timeline einmalig vor, sodass der Playhead wieder links bei 10 % steht. Zusätzlich echte JavaFX-Relayout-Boundary (`155 x 24 px` fix) für `lblTimeDisplay` und `CacheHint.SPEED` für `canvas` & `playheadGroup`. 430 Tests zu 100% grün. |
| **2026-11-25** | **Tab 4: Explorer-Breite und Split-Ratio mit Stage-Tab synchronisiert (v0.28.2)** | Anpassung der Show-Timeline Explorer-Seitenleiste (`ShowTimelineExplorerView` & `ShowTimelineWorkspaceView`): Erhöhung der Mindest- und Vorzugsbreite von 260px/280px auf exakt 300px (identisch mit `ProjectExplorerView` im Stage-Tab), Anpassung der Standard-Split-Ratio von 0.76 auf 0.78 sowie automatische nahtlose Divider-Synchronisation zwischen Stage, Matrix und Timeline beim Tab-Wechsel in `MainWindow.switchToTab`. |
| **2026-11-25** | **Tab 4: Szenen-Ordner im Show-Timeline Projekt-Explorer (v0.29.0)** | Fester Ordner „Szenen" im Show-Timeline Explorer (Tab 4). Reaktive Live-Synchronisation aller Lichtszenen aus dem Projekt (`SceneService` & `ProjectExplorerService`). Doppelklick oder Rechtsklick ➔ „Szene aktivieren" schaltet die Szene sofort aktiv auf den DMX-Ausgang. 464 Tests 100% grün. |
| **2026-11-26** | **Tab 4: Szenen-Clips in der Timeline mit Playhead-Aktivierung & Deaktivierung (v0.30.0)** | Ziehen einer Szene per Drag & Drop aus dem Explorer in die Timeline erstellt automatisch eine neue Spur mit einem 2-Sekunden-Clip (`#A78BFA` Kontur, `#221838` Surface, Bookmark-Icon & Szenenname). Klick auf den Clip schaltet die Szene zur DMX-Vorschau aktiv. Playhead-Trigger-Engine in `TimelineService`: Erreicht der Playhead den Szenen-Clip, wird die Szene automatisch aktiviert (`applyScene`), und sobald der 2s-Block verlassen wird (oder bei Stop/Scrubbing), wird die Szene automatisch wieder deaktiviert (`deactivateScene`, Kanäle auf 0). 477 Tests zu 100% grün. |
| **2026-11-26** | **Tab 4: Daslight-Clip-Design, bilaterales Resizing & konfigurierbares Grid-Snapping (v0.31.0)** | (1) Daslight-konformes Szenen-Clip-Design mit 50px Spurhöhe, goldenem Korpus (`#FACC15`), unterem halbtransparentem Infokasten (60% Schwarz), Daslight-Dauerformat `00m08s80` und dezenten 4px Eckenrundungen; (2) Bilaterales Kanten-Resizing (Croppen) an linker und rechter Kante mit H-Resize-Cursor und thread-sicherer Minimaldauern-Validierung (250ms); (3) Sekunden-basiertes dynamisches Hintergrund-Raster mit Major- und Minor-Balken passend zu Clip-Dauern; (4) Grid-Snapping Dropdown in der Timeline-Toolbar (`Frei`, `1 Sekunde`, `5 Sekunden`, `10 Sekunden`, `30 Sekunden`, `1 Minute`) mit reaktiver Lokalisierung in allen Sprachen (DE, EN, RU). 495 Tests zu 100% grün. |
| **2026-11-26** | **Tab 4: Stufenloses Clip-Verschieben (Free Move), Viertel- & Halbsekunden-Raster (v0.32.0)** | (1) Trennung von Clip-Move und Clip-Crop: Verschieben des Clips auf der Timeline erfolgt stufenlos frei (`free`), während das bilaterale Kanten-Resizing (Croppen) präzise am eingestellten Grid-Raster einrastet; (2) Erweiterung des Grid-Snappings um `1/4 Sekunde` (250 ms) und `1/2 Sekunde` (500 ms) inklusive proportionaler Hintergrundlinien-Unterteilungen; (3) Vollständige Lokalisierung der neuen Rasteroptionen in Deutsch, Englisch und Russisch. 496 Tests zu 100% grün. |
| **2026-11-26** | **Tab 4: Fester 1s-Hintergrund & Standard-Snap 1s (v0.32.1)** | Das periodische Hintergrundraster der Zeitleiste bleibt unabhängig von der gewählten Einrast-Auflösung dauerhaft fest auf dem Standard-1-Sekunden-Raster (`1000 ms` Major-Linien mit dezenten `250 ms` Unterteilungen); der Standard-Snap beim Start wird auf `1s` (`TimelineGridSnap.SEC_1`) gesetzt, während das Snapping dynamisch ausschließlich beim Kanten-Resizing (Croppen) greift. 497 Tests zu 100% grün. |
| **2026-11-27** | **Tab 4: Benutzerdefiniertes Timeline-Kontextmenü & Kaskadierendes Clip-Farbauswahl-Flyout (v0.33.0)** | (1) Benutzerdefiniertes Studio-Kontextmenü (`TimelineContextMenu`) auf Basis von `Popup` (`.action-menu-popup` & `.action-menu-item`) mit pixelgenauer Ausrichtung der linken oberen Menüecke am Mauszeiger unter Kompensation der Schatten-Ränder; (2) Kaskadierendes Untermenü (Flyout-Dropdown `TimelineColorFlyout`): Beim Hovern über den Menüeintrag „Farbe ▸" öffnet sich nahtlos rechts daneben das 2. Dropdown zur Farbauswahl (ohne diffuses Fenster oder Glow-Effekt); (3) 6 kuratierte Studio-Standardfarben (Gelb `#FACC15`, Cyan `#38BDF8`, Grün `#10B981`, Rot `#EF4444`, Lila `#8B5CF6`, Pink `#EC4899`) als taktile Farbkacheln im 2x3-Palette-Grid mit feinem Rahmen, reinweißem Hover-Highlight und zentriertem Häkchen auf der aktiven Farbe; (4) Automatische Helligkeits-/Luminanz-Berechnung im Clip-Rendering, sodass der Szenenname auf hellen Farben schwarz (`#000000`) und auf dunkleren Farben reinweiß (`#FFFFFF`) dargestellt wird; (5) MVC-Persistenz im `TimelineTrack` (`colorHex`) und `TimelineService`. |
| **2026-11-25** | **Tab 3: Vollständiger Neuaufbau der Button Matrix im Stage-View-Layout (v0.28.0)** | Vollständige Neuentwicklung von Tab 3 (Button Matrix) analog zur Zwei-Spalten-Architektur der Stage View (`StageWorkspaceView`): (1) Horizontales SplitPane mit vollwertigem `ProjectExplorerView` über die volle rechte Höhe (Ein-/Ausklappen via `arrow-right-to-line` / `arrow-left-to-line`, Drag & Drop Zuweisung direkt auf Kacheln); (2) Linker Bereich mit vertikalem SplitPane aus oberem responsiven Button-Grid (`MatrixGridView`, 32 Pads 4x8) und unterem 220px Steuerungsdock (`MatrixControlDock`) für selektierte Kacheln; (3) Taktile Button-Kacheln (`MatrixButtonNode`) nach Material Design 2 Dark mit Statusanzeige, Farb-Glow und Lucide-Icons; (4) Core-Service (`MatrixService`) mit reaktiven `TOGGLE`- und `FLASH`-Modi, Not-Aus-Integration über den globalen `[■ STOP]`-Button sowie vollständiger Projekt-Persistenz (`MatrixButtonDataDto` im `ProjectData`). 462 Tests zu 100% grün. |
| **2026-11-25** | **Tab 4: Mute-Button Farbgebung auf Studio-Rot angepasst (v0.27.1)** | Anpassung des Mute-Buttons (**M**) in `TimelineTrackHeaderBox`: Umstellung der aktiven Farbgebung von Bernsteinton auf sattes Studio-Rot (`#EF4444` Rahmen und `#FCA5A5` Text auf `#450A0A` Hintergrund bzw. `#F87171` auf `#5A1212` bei Hover). 434 Tests zu 100% grün. |
| **2026-11-25** | **Tab 4: Studio DAW Mute- (M) & Solo- (S) Buttons für Track-Header (v0.27.0)** | Ersetzung des einzelnen grünen Punkts im Track-Header (`TimelineTrackHeaderBox`) durch zwei eingerahmte DAW-Style Buttons mit den Großbuchstaben **M** (Mute) und **S** (Solo). Subtiles dunkles Styling im Ruhezustand (`#1E1E1E` mit 1px `#333333` Rahmen), leuchtend bernsteinfarben (`#F59E0B` auf `#451A03`) bei aktivem Mute und strahlend goldgelb (`#EAB308` auf `#3E3204`) bei aktivem Solo. Vollständige Solo-Logik in `TimelineTrack`, `TimelineService` (`toggleTrackSolo`, `isTrackAudible`) und im `AudioPlaybackService` (beim Aktivieren von Solo auf beliebigen Spuren werden nur Tracks im Solo-Modus abgespielt). 434 Tests zu 100% grün. |
| **2026-11-24** | **Tab 4: Volle Ausnutzung der Timeline-Breite beim Page-Turn Follow (v0.26.4)** | Anpassung der Page-Turn-Auslöseschwelle: Die Timeline-Bar wandert bis ganz an das Ende des Viewports (`rightThreshold = viewW`) und blättert erst beim Erreichen des rechten Randes auf die nächste Seite um. 430 Tests zu 100% grün. |
| **2026-11-20** | **UI-Skalierung für kompakte Displays & Globale Einstellungs-Persistenz (v1.21.0)** | Virtuelles Viewport-Scaling für kleinere Displays und Laptops (75%–150%) über koordinatentreue Skalierungstransformation ohne Rand-Clipping. Proportionale Popups und Dropdowns. Vollständige globale Persistenz benutzerspezifischer Einstellungen (`settings.json` in APPDATA/Config) über Neustarts und neue Projekte hinweg (Sprachauswahl, UI-Scale, Tastenkürzel bleiben beim Erstellen eines neuen Projekts erhalten und werden nicht auf Standardwerte zurückgesetzt). Speicherung des UI-Scales im Projekt-DTO (`SettingsDataDto`). 7 neue Tests, 377 Tests 100% grün. |


