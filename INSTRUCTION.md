# Projektanweisung: jdmx (Java DMX Controller)

> **Verbindliche Arbeitsanweisung an jeden ausführenden Entwickler-Agenten:**
> Dieses Dokument ist deine absolute Spezifikation und Arbeitsanweisung.
> Jede Abweichung von den folgenden Regeln, dem Tech-Stack, der Paketstruktur oder der Roadmap ist strengstens untersagt!
>
> ### 🛑 Eiserne Grundregeln für JEDEN Entwicklungsschritt:
> 1. **STRIKTES TEST-DRIVEN DEVELOPMENT (TDD) MIT JUNIT 5:**
>    - Bei JEDER Funktionalität und JEDER Klasse werden **AUSNAHMSLOS ZUERST** die Unit-/Integrationstests in `src/test/java/...` geschrieben (Red-Phase).
>    - Erst danach wird die eigentliche Implementierung in `src/main/java/...` umgesetzt (Green-Phase).
>    - **VOR DEM KOMPILIEREN / BUILDEN** (`mvn compile`, `mvn package`) MUSS zwingend `mvn test` ausgeführt werden! Alle Tests müssen zu 100% grün sein. Ungetesteter Code darf unter keinen Umständen kompiliert oder committet werden.
> 2. **LÜCKENLOSE DOKUMENTATION FÜR ONBOARDING & WARTBARKEIT:**
>    - **ALLES muss verständlich dokumentiert sein:** Jede Klasse, jedes Interface, jedes Enum, jeder Record und jede Methode MUSS präzises, verständliches **JavaDoc** besitzen (`@param`, `@return`, `@throws`, Zweck, Thread-Sicherheits-Garantien und architektonische Begründungen).
>    - Die Codebasis und Dokumentation müssen so gestaltet sein, dass jede beliebige neue Person das Programm sofort versteht, neue Entwickler problemlos eingearbeitet werden können (**Onboarding-Fähigkeit**) und das System maximal wartbar bleibt.
>    - Verbindliche Dokumentations-Dateien im Projekt:
>      - `docs/ONBOARDING.md`: Schritt-für-Schritt Einarbeitungs-Leitfaden für neue Entwickler (Projekt-Setup, Systemarchitektur, wie man neue DMX-Treiber implementiert, wie man neue Fixture-Typen anlegt, Test- & Debugging-Workflows).
>      - `docs/ARCHITECTURE.md`: Tiefgehende technische Architekturbeschreibung (DMX512 Frame-Timing, Thread-Sicherheit bei atomaren Puffern, Art-Net UDP-Paketaufbau, SSE-Streaming, H2/JDBC-Persistenz).
>      - `README.md`: Vollständige Benutzer-, Installations-, Konfigurations- und API-Dokumentation mit Anwendungsbeispielen.
> 3. **STRIKTE MAVEN-PFLICHT & JAVA 21 LTS:**
>    - Ausnahmslose Verwendung von **Apache Maven** (`pom.xml`). Niemals Gradle verwenden.
>    - Standard-Verzeichnisstruktur (`src/main/java`, `src/main/resources`, `src/test/java`).
>    - Root-Package ist ausnahmslos `de.exit.jdmx`.
> 4. **GIT-MEILENSTEINE:**
>    - Nach jedem großen Meilenstein / Phasenabschluss ist verbindlich ein aussagekräftiger Git-Commit und `git push` auf `origin main` durchzuführen.
> 5. **PLANUNGS-MODUS (WARTE AUF NUTZER-FREIGABE):**
>    - Beginne erst mit der Implementierung, wenn der Nutzer die Phase explizit freigibt!

---

## 1. Projektübersicht & Scope

- **Problem & Ziel:**  
  Ein hochperformantes, modulares DMX-512 Lichtsteuerungs-System in Java mit einer integrierten, reaktiven Weboberfläche. Das Backend verarbeitet DMX-Universen, Fixtures (Scheinwerfer/Moving Heads/Dimmer), Presets und Szenen und stellt eine latenzarme DMX-Output-Engine bereit (Art-Net / virtueller Simulator / erweiterbar für USB/FTDI). Das Frontend bietet eine moderne Dark-Theme Studio-Weboberfläche zur Live-Steuerung, Fader-Bedienung und 512-Kanal-Visualisierung.
  
- **Kommunikationsarchitektur:**
  - **REST-API:** Für Konfiguration, Fixture-Management, Szenen-CRUD und statische Abfragen.
  - **SSE (Server-Sent Events):** Für kontinuierliches Echtzeit-Streaming von Kanalwerten, Universe-Frames, Master-Status und Event-Feedback mit hoher Bildwiederholrate und minimalem Overhead direkt über Javalin HTTP.

- **In-Scope (MVP v1.0):**
  - **DMX Universe Engine:** 512-Kanal-Frame-Buffer pro Universum (Standard: Universum 1), Grand Master (0–100%), Flash-Funktionen und sofortiger Blackout-Toggle.
  - **DMX Output Treiber-Architektur:** Modulares Treiber-Interface (`DmxOutputDriver`) mit:
    1. `VirtualSimulationDriver` (Standard für Tests und Headless-Betrieb).
    2. `ArtNetDriver` (Standard-UDP-Broadcast für professionelle DMX-Ethernet-Knoten).
  - **Fixture- & Profilverwaltung:** Adressierung von Geräten (Dimmer, RGB, RGBW, Generic Pan/Tilt) mit Startadresse und Kanalbelegung sowie automatischer Adress-Kollisionsprüfung.
  - **Szenen & Presets:** Speichern von Kanalzuständen als Szenen mit Fader/Fade-Time (ms) und sanftem, nicht-blockierendem Überblenden per API/Web.
  - **Persistenz (H2 / SQL-Ready):** Speicherung von Fixtures, Universum-Einstellungen und Szenen via H2 Database. Saubere Trennung über Repository-Interfaces und ANSI-SQL/JDBC, sodass die Datenbank jederzeit über Konfiguration auf PostgreSQL oder MySQL umgestellt werden kann.
  - **Web-Dashboard (Embedded):**
    - Komplett aus dem Backend ausgeliefert (`src/main/resources/public/`).
    - 512-Kanal Universe Grid (High-Performance Canvas-Rendering für flüssige 30–60 FPS ohne UI-Lags).
    - Multi-Channel Fader Desk (Grand Master, Submaster, Einzelfader mit Prozent- & DMX-Wert 0–255).
    - Fixture-Inspektor mit RGB/Color-Picker und Schnell-Dimmer.
    - Blackout-Button mit rotem Warnzustand.
    - 100% Lucide-Icons, konsequentes Studio Dark-Theme, absolutes Emoji-Verbot.
  - **Fat-JAR:** Vollständig lauffähiges, eigenständiges JAR mit eingebettetem Jetty/Javalin via Maven-Plugins.

- **Out-of-Scope (Ausdrücklich für spätere Versionen reserviert):**
  - Keine komplexen 3D-Bühnenvisualisierer (z. B. Three.js / WebGL-3D-Rigging).
  - Keine Benutzer- und Rechteverwaltung / Mehrbenutzer-Authentifizierung im MVP.
  - Keine direkte proprietäre Hardware-Treiberbindung im Kern (ausschließlich sauberes Treiber-Interface; Art-Net und Simulation genügen für v1.0).

---

## 2. Tech-Stack & Feste Leitplanken

- **Runtime:** Java 21 LTS (Virtuelle Threads / Records / Pattern Matching).
- **Betriebssystem:** Windows (vollständig plattformunabhängig lauffähig).
- **Build-Tool:** **Apache Maven** (`pom.xml`) – Strikte Vorgabe nach Arbeitsrichtlinie.
- **Web- & API-Framework:** **Javalin 6.x** (auf embedded Eclipse Jetty).
- **Serialization & JSON:** **Jackson 2.18+** (`jackson-databind`, `jackson-datatype-jsr310`).
- **Persistenz & DB:** **H2 Database** (eingebetteter Modus / File-basiert in `./data/jdmx_db`), angebunden über saubere JDBC-Repositories / HikariCP. Vollständig SQL-kompatibel vorbereitet für Postgres/MySQL.
- **Logging:** **SLF4J** mit `slf4j-simple`.
- **Frontend:**
  - Modernes HTML5, CSS3 (Studio Dark Theme mit CSS-Variablen) und JavaScript (ES Modules).
  - Native **Lucide-Icons** (SVG / lucide).
  - Performance: HTML5 Canvas / Virtual Grid für 512 DMX-Kanäle, EventSource API für SSE.
- **Testing & TDD:** **JUnit 5 (Jupiter)** + **AssertJ** + **Mockito** für isolierte Unit- & Integrationstests.

### Java-Spezifikation (Strikter Standard)
- **GroupId:** `de.exit`
- **ArtifactId:** `jdmx`
- **Base Package:** `de.exit.jdmx`
- **Verzeichnisstruktur:** Alle Java-Klassen liegen zwingend unter `src/main/java/de/exit/jdmx/...`. Alle Tests unter `src/test/java/de/exit/jdmx/...`.
- **Maven-Plugins:**
  - `maven-compiler-plugin` (Java 21)
  - `maven-surefire-plugin` (JUnit 5 Test-Runner vor dem Compile/Package)
  - `exec-maven-plugin` (Ermöglicht `mvn exec:java`)
  - `maven-jar-plugin` & `maven-shade-plugin` (Für ausführbare Standalone Fat-JARs mit `Main-Class: de.exit.jdmx.Main`)

### Verbotene Muster & Fallstricke (Anti-Halluzinations- & Qualitätsregeln)
- ❌ **Kein ungetesteter Code:** Niemals Code schreiben, bevor nicht der entsprechende Unit-Test existiert (TDD-Pflicht).
- ❌ **Kein Compile ohne vorherigen grünen Test:** `mvn test` muss immer erfolgreich durchlaufen.
- ❌ **Kein undokumentierter Code:** Klassen und Methoden ohne JavaDoc sind nicht zulässig.
- ❌ **Kein Gradle:** Bei Java gilt strikte und ausnahmslose Maven-Pflicht (`pom.xml`).
- ❌ **Kein Spring Boot / Quarkus:** Das Projekt basiert leichtgewichtig auf Javalin 6. Keine schweren Enterprise-Container.
- ❌ **Absolutes Emoji-Verbot im Web-UI:** Keine Emojis in Buttons, Headers, Statusanzeigen oder Tooltips. Ausschließlich Lucide-Icons!
- ❌ **Kein Node/NPM-Zwang für das Basis-Frontend:** Das Web-Frontend wird direkt über statische Ressourcen im Classpath ausgeliefert (`src/main/resources/public`), damit der Maven-Build ohne externe Node-Installationen sauber baut und sofort lauffähig ist.
- ❌ **Keine blockierenden I/O-Aufrufe im DMX- oder SSE-Loop:** Der DMX-Sendeloop und SSE-Broadcasts dürfen den Request-Thread nicht blockieren; Entkopplung über Ringbuffer / atomare Datenstrukturen (`AtomicIntegerArray` oder thread-sichere Puffer).
- ❌ **Keine hardcodierten H2-Spezifika in der Geschäftslogik:** Repositories müssen saubere ANSI-SQL DDLs/Queries nutzen, um jederzeit per Konfiguration auf PostgreSQL oder MySQL umgestellt werden zu können.

---

## 3. Architektur & Dateistruktur

```text
jdmx/
├── pom.xml                                   # Strikte Maven-Konfiguration mit Java 21 & Plugins
├── .gitignore                                # Git-Ignore für target/, data/, .idea/ etc.
├── AGENTS.md                                 # Verbindliche Entwickler-Direktive für alle Agenten
├── INSTRUCTION.md                            # Master-Spezifikation & verbindliche Roadmap
├── README.md                                 # Umfassende Benutzer-, Setup- & API-Dokumentation
├── docs/                                     # Lückenlose Dokumentation für Einarbeitung & Wartbarkeit
│   ├── ARCHITECTURE.md                       # DMX512-Buffer, Art-Net UDP, Threading, SSE-Design
│   └── ONBOARDING.md                         # Schritt-für-Schritt Entwickler-Einführung & Erweiterungs-Leitfaden
├── data/                                     # Lokales Verzeichnis für H2-Datenbank (*.db)
└── src/
    ├── main/
    │   ├── java/
    │   │   └── de/
    │   │       └── exit/
    │   │           └── jdmx/
    │   │               ├── Main.java                         # Anwendungs-Einstiegspunkt & Serverstart
    │   │               ├── config/
    │   │               │   ├── AppConfig.java                # Port, DMX-Frequenz, DB-URL, Treiberwahl
    │   │               │   └── DatabaseConfig.java           # DataSource & Connection Pooling (HikariCP / H2)
    │   │               ├── core/
    │   │               │   ├── DmxUniverse.java              # 512-Kanal Frame Buffer, Master, Blackout (Thread-safe)
    │   │               │   ├── DmxEngine.java                # Scheduler für periodischen DMX-Output (z. B. 40 Hz)
    │   │               │   └── driver/
    │   │               │       ├── DmxOutputDriver.java      # Interface: sendFrame(int universe, byte[] data)
    │   │               │       ├── VirtualSimulationDriver.java # Logging & In-Memory Visualisierung/Prüfung
    │   │               │       └── ArtNetDriver.java         # UDP Art-Net DMX Paket-Sender (Port 6454)
    │   │               ├── model/
    │   │               │   ├── Fixture.java                  # Record: id, name, universe, startAddress, type, channels
    │   │               │   ├── FixtureType.java              # Enum: DIMMER_1CH, RGB_3CH, RGBW_4CH, CUSTOM
    │   │               │   ├── Scene.java                    # Record: id, name, universe, channelValues, fadeTimeMs
    │   │               │   └── UniverseState.java            # DTO für API/SSE: universeId, channels[], master, blackout
    │   │               ├── repository/
    │   │               │   ├── FixtureRepository.java        # Interface für Fixture-CRUD
    │   │               │   ├── SceneRepository.java          # Interface für Szenen-CRUD
    │   │               │   └── jdbc/
    │   │               │       ├── JdbcFixtureRepository.java# ANSI-SQL Implementierung (H2 / Postgres kompatibel)
    │   │               │       ├── JdbcSceneRepository.java  # ANSI-SQL Implementierung
    │   │               │       └── DatabaseInitializer.java  # Automatische Schema-Migration / Tabellen-Setup
    │   │               ├── service/
    │   │               │   ├── DmxService.java               # Geschäftslogik: Fader setzen, Szenen abrufen, Master
    │   │               │   ├── FixtureService.java           # Fixture-Zuweisung & Adresskollisionsprüfung (1-512)
    │   │               │   └── SceneService.java             # Szenen verwalten und sanft einblenden (Fading)
    │   │               └── web/
    │   │                   ├── JavalinApp.java               # Konfiguration von Javalin & Routen-Registrierung
    │   │                   ├── controller/
    │   │                   │   ├── DmxApiController.java     # REST Endpoints für Universen & Kanäle
    │   │                   │   ├── FixtureApiController.java # REST Endpoints für Fixtures
    │   │                   │   └── SceneApiController.java   # REST Endpoints für Szenen
    │   │                   └── sse/
    │   │                       └── DmxSseHandler.java        # SSE-Client-Verwaltung & Broadcast-Streamer
    │   └── resources/
    │       ├── application.properties                        # Server-Port, DB-Pfad, Default-Treiber
    │       └── public/                                       # Web-Frontend (Statische Assets, Lucide Icons)
    │           ├── index.html                                # Dashboard-Layout (Studio Dark Theme)
    │           ├── css/
    │           │   ├── theme.css                             # Studio Dark Theme, Farbvariablen
    │           │   └── components.css                        # Fader, Channel-Grid, Modals, Badges
    │           └── js/
    │               ├── app.js                                # Haupt-App-Initialisierung & Navigation
    │               ├── sse.js                                # SSE Client mit Auto-Reconnect & Event-Dispatcher
    │               ├── universe-grid.js                      # High-Performance 512-Kanal Canvas-Grid
    │               ├── fader-desk.js                         # Touch/Mouse-Fader mit Latenz-Optimierung
    │               ├── fixtures.js                           # Fixture-Liste, Color-Picker & Adress-Matrix
    │               └── scenes.js                             # Szenen-Speichern & Szenen-Trigger mit Fading
    └── test/                                                 # TDD-TESTS (WERDEN ZUERST GESCHRIEBEN!)
        └── java/
            └── de/
                └── exit/
                    └── jdmx/
                        ├── config/
                        │   └── AppConfigTest.java            # Prüfung Property-Loading und Defaults
                        ├── core/
                        │   ├── DmxUniverseTest.java          # Grenzwerte (1-512), Master-Skalierung, Blackout
                        │   ├── ArtNetDriverTest.java         # UDP-Paketstruktur, OpCode 0x5000, Header
                        │   └── VirtualSimulationDriverTest.java # In-Memory Frame-Aufzeichnung
                        ├── model/
                        │   └── FixtureTest.java              # Adress-Überlappung & Validierung
                        ├── service/
                        │   ├── DmxServiceTest.java           # DmxService Geschäftslogik
                        │   ├── FixtureServiceTest.java       # Kollisionsprüfung & Range-Validierung
                        │   └── SceneServiceTest.java         # Szenen-Recall & Fade-Interpolation
                        ├── repository/
                        │   ├── JdbcFixtureRepositoryTest.java# H2 Integrationstest Fixtures
                        │   └── JdbcSceneRepositoryTest.java  # H2 Integrationstest Szenen
                        └── web/
                            ├── DmxApiControllerTest.java     # REST API Validierung & Statuscodes
                            └── FixtureApiControllerTest.java # Fixture Endpoints & HTTP 409 Kollision
```

---

## 4. Datenmodell & Schnittstellen

### 4.1 Java Datamodels (Records)

#### `Fixture.java`
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Repräsentiert ein DMX-Fixture (Scheinwerfer, Moving Head, Dimmer etc.).
 * 
 * @param id           Eindeutige UUID des Fixtures
 * @param name         Benutzerdefinierter Anzeigename (z. B. "Front Truss Spot 1")
 * @param universe     DMX-Universum (Standard: 1)
 * @param startAddress DMX-Startadresse (1 bis 512)
 * @param type         Typ des Fixtures (definiert Kanalbelegung und Eigenschaften)
 * @param channelCount Anzahl belegter aufeinanderfolgender DMX-Kanäle
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Fixture(
    String id,
    String name,
    int universe,
    int startAddress,
    FixtureType type,
    int channelCount
) {
    /**
     * Prüft, ob sich dieses Fixture mit einem anderen Fixture im Adressbereich überschneidet.
     * 
     * @param other Das zu prüfende andere Fixture
     * @return true, falls eine Adresskollision im selben Universum vorliegt, sonst false
     */
    public boolean overlapsWith(Fixture other) {
        if (this.universe != other.universe) return false;
        int thisEnd = this.startAddress + this.channelCount - 1;
        int otherEnd = other.startAddress + other.channelCount - 1;
        return this.startAddress <= otherEnd && other.startAddress <= thisEnd;
    }
}
```

#### `FixtureType.java`
```java
package de.exit.jdmx.model;

/**
 * Unterstützte Standard-Fixture-Profile mit hinterlegter Kanalanzahl.
 */
public enum FixtureType {
    DIMMER_1CH(1, "Einfacher 1-Kanal Dimmer"),
    RGB_3CH(3, "3-Kanal RGB Scheinwerfer (Rot, Grün, Blau)"),
    RGBW_4CH(4, "4-Kanal RGBW Scheinwerfer (Rot, Grün, Blau, Weiß)"),
    CUSTOM(1, "Benutzerdefiniertes Fixture");

    private final int defaultChannels;
    private final String description;

    FixtureType(int defaultChannels, String description) {
        this.defaultChannels = defaultChannels;
        this.description = description;
    }

    public int getDefaultChannels() { return defaultChannels; }
    public String getDescription() { return description; }
}
```

#### `Scene.java`
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Repräsentiert eine Lichtszene / Preset mit Kanal-Sollwerten.
 * 
 * @param id            Eindeutige ID der Szene
 * @param name          Anzeigename der Szene (z. B. "Warm White Relax")
 * @param universe      Betroffenes Universum (z. B. 1)
 * @param channelValues Zuordnung von DMX-Kanal (1-512) zu Wert (0-255)
 * @param fadeTimeMs    Überblendzeit in Millisekunden (0 = sofortiger Schnitt)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Scene(
    String id,
    String name,
    int universe,
    Map<Integer, Integer> channelValues,
    int fadeTimeMs
) {}
```

#### `UniverseState.java` (DTO für SSE / REST)
```java
package de.exit.jdmx.model;

/**
 * Snapshot des aktuellen Universum-Zustands für REST-Antworten und SSE-Streaming.
 * 
 * @param universeId  ID des Universums (1..n)
 * @param channels    Array aller 512 DMX-Kanalwerte (0 bis 255)
 * @param masterValue Grand Master Wert (0 bis 255 entsprechend 0-100%)
 * @param blackout    Gibt an, ob der globale Blackout aktiv ist
 */
public record UniverseState(
    int universeId,
    int[] channels,
    int masterValue,
    boolean blackout
) {}
```

### 4.2 REST API Routen

| Methode | Pfad | Beschreibung | Payload / Rückgabe |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/universes/{id}` | Aktueller Zustand des Universums | `UniverseState` (JSON) |
| `POST`| `/api/universes/{id}/channel` | Setzt einzelnen Kanalwert | `{ "channel": 1, "value": 255 }` |
| `POST`| `/api/universes/{id}/channels`| Setzt mehrere Kanalwerte | `{ "values": { "1": 255, "2": 128 } }` |
| `POST`| `/api/universes/{id}/master`  | Setzt Grand Master | `{ "value": 255 }` |
| `POST`| `/api/universes/{id}/blackout`| Schaltet Blackout ein/aus | `{ "blackout": true/false }` |
| `GET` | `/api/fixtures` | Liste aller konfigurierten Fixtures | `List<Fixture>` |
| `POST`| `/api/fixtures` | Neues Fixture anlegen (mit Kollisionsprüfung) | `Fixture` |
| `DELETE`| `/api/fixtures/{id}` | Fixture löschen | 204 No Content |
| `GET` | `/api/scenes` | Liste aller Szenen | `List<Scene>` |
| `POST`| `/api/scenes` | Aktuelle Werte als Szene speichern | `{ "name": "Warm White", ... }` |
| `POST`| `/api/scenes/{id}/recall` | Szene aktivieren (mit optionalem Fading) | 200 OK |
| `GET` | `/api/live/stream` | **SSE-Stream:** Sendet `universe-update`, `master-update`, `blackout-update` | EventStream (`text/event-stream`) |

---

## 5. Detaillierte Features mit "Definition of Done" (DoD)

### Feature 1: DMX Core Engine mit Master & Blackout
- **Beschreibung:**  
  Ein thread-sicherer Universe-Puffer mit 512 DMX-Kanälen (0–255).  
  Der Grand-Master skaliert die effektiven Ausgabewerte linear (`effektiv = (kanalWert * masterWert) / 255`).  
  Bei aktivem Blackout werden an den Ausgabetreiber ausnahmslos Nullen (0) übertragen, während die Sollwerte im Speicher erhalten bleiben.
- **Definition of Done (TDD):**
  - *Automatisierter Test:* `DmxUniverseTest` prüft:
    - Kanal 1 auf 200 setzen, Grand Master auf 50% (128) ➔ Effektiver Ausgabewert = 100.
    - Blackout aktivieren ➔ Effektiver Ausgabewert = 0; Deaktivieren stellt 100 wieder her.
    - Kanäle außerhalb 1–512 werfen eine `IllegalArgumentException`.
    - Werte außerhalb 0–255 werden sauber geclamped oder validiert.
  - *Dokumentation:* 100% JavaDoc für `DmxUniverse` und Methoden.

### Feature 2: SSE Live-Streaming (Server-Sent Events)
- **Beschreibung:**  
  Clients verbinden sich über `/api/live/stream`. Javalin verwaltet die verbundenen Clients im `DmxSseHandler`. Bei Wertänderungen werden Frame-Updates im JSON-Format verzögerungsfrei gepusht.
- **Definition of Done (TDD):**
  - *Automatisierter Test:* Unit-Test für SSE-Client-Registrierung und Broadcast-Payload.
  - *Latenz:* Browser empfängt innerhalb von < 50ms das Event `universe-update` ohne HTTP-Polling.
  - *Dokumentation:* 100% JavaDoc für `DmxSseHandler` inklusive Threading-Erklärung.

### Feature 3: Modulare Treiber (Simulation & Art-Net)
- **Beschreibung:**  
  Treiber implementieren `DmxOutputDriver`. `ArtNetDriver` versendet standardkonforme Art-Net DMX UDP-Pakete (Port 6454) an das konfigurierte Subnetz/Broadcast. `VirtualSimulationDriver` dient für Unit-Tests und Entwicklungsbetrieb.
- **Definition of Done (TDD):**
  - *Automatisierter Test:* `ArtNetDriverTest` prüft Header (`Art-Net\0`, OpCode `0x5000`, Version 14, Universe-ID, 512 Bytes Data) via lokalem UDP-Socket.
  - *Dokumentation:* 100% JavaDoc für alle Treiber und das Interface.

### Feature 4: Austauschbare Persistenz (H2 & SQL-Ready)
- **Beschreibung:**  
  `JdbcFixtureRepository` und `JdbcSceneRepository` verwalten Fixtures und Szenen in einer relationalen H2-Datenbank (`./data/jdmx_db`).
  Tabellen: `fixtures`, `scenes`, `scene_channels`. Alle Queries verwenden sauberes Standard-SQL.
- **Definition of Done (TDD):**
  - *Automatisierter Test:* `JdbcFixtureRepositoryTest` und `JdbcSceneRepositoryTest` prüfen Speichern, Lesen, Löschen in einer In-Memory H2 DB.
  - *Dokumentation:* 100% JavaDoc für Repositories und DDL-Migration.

### Feature 5: Web Dashboard mit Lucide-Icons & High-Performance Matrix
- **Beschreibung:**  
  Ein ansprechendes Studio Dark Theme UI:
  - Header: Verbindungsstatus (SSE Indikator), Grand Master Fader, prominenter Blackout-Button (Lucide `Power` / `AlertCircle`), Universum-Wahl.
  - Kanal-Matrix: Visuelle Darstellung aller 512 Kanäle als High-Performance HTML5 Canvas mit Helligkeits-Visualisierung.
  - Fader-Bank: Dynamische Fader für ausgewählte Kanäle oder Fixtures mit Touch-/Maus-Support.
  - Fixture-Manager: Modal zum Hinzufügen mit visueller Belegungsanzeige der DMX-Adressen (1–512) zur Vermeidung von Kanal-Überschneidungen.
  - 100% Lucide-Icons (z. B. `Sliders`, `Sun`, `Zap`, `Power`, `Plus`, `Trash2`, `Play`, `Save`). Keine Emojis!
- **Definition of Done:**
  - Fader-Bewegungen aktualisieren DMX-Werte in Echtzeit; SSE synchronisiert Multi-Tab-Instanzen sofort.
  - Vollständige Inline-Dokumentation in allen JavaScript-Dateien.

---

## 6. Fehlermatrix & Edge Cases

| Situation / Ausnahme | Systemverhalten | Feedback an Nutzer / API |
| :--- | :--- | :--- |
| Ungültige DMX-Adresse (< 1 oder > 512) | Ablehnung im Domain-Model / Controller | HTTP 400 Bad Request: `{"error": "DMX address must be between 1 and 512"}` |
| Adresskollision bei Fixture-Erstellung | FixtureService prüft Adressbereich vor dem Speichern | HTTP 409 Conflict: `{"error": "Address overlaps with existing fixture: [Name]"}` |
| Ungültiger DMX-Wert (< 0 oder > 255) | Clamping auf 0 bzw. 255 oder Validierungsfehler | HTTP 400 Bad Request bei API-Aufruf; automatische Begrenzung im Web-Fader |
| Verbindungsabbruch des SSE-Streams | Browser EventSource verbindet sich automatisch neu (`retry: 2000`) | Status-Indikator im UI wechselt auf Gelb/Rot (Lucide `WifiOff`); bei Reconnect wieder Grün (`Wifi`) |
| Fehlende oder korrupte H2-Datenbank | `DatabaseInitializer` repariert / erzeugt Tabellenschema idempotent (`CREATE TABLE IF NOT EXISTS`) | Info-Log im Backend; reibungsloser Start mit leeren Tabellen |
| Art-Net Zieladresse nicht erreichbar | UDP ist verbindungslos; Fehler werden geloggt, blockieren jedoch nicht die Engine | Warn-Log im Backend; kein Absturz der DMX Engine |

---

## 7. Phasenweise Implementierungs-Roadmap (Strikt TDD & Dokumentation)

### Phase 1: Maven Setup, Plugins & Basiskonfiguration
- [ ] Saubere `pom.xml` mit Java 21, Javalin 6, Jackson, HikariCP, H2, SLF4J, JUnit 5, AssertJ, Mockito.
- [ ] Plugins einrichten: `maven-compiler-plugin`, `maven-surefire-plugin`, `exec-maven-plugin`, `maven-jar-plugin`, `maven-shade-plugin`.
- [ ] **TDD Schritt 1:** `src/test/java/de/exit/jdmx/config/AppConfigTest.java` schreiben.
- [ ] **TDD Schritt 2:** `AppConfig.java` implementieren und mit vollständigem JavaDoc versehen.
- [ ] Einstiegsklasse `Main.java` anlegen mit JavaDoc.
- [ ] **Verifikation VOR Compile:** `mvn test` läuft zu 100% grün durch.
- [ ] **Verifikation Compile:** `mvn clean compile` und `mvn exec:java` startet den Server.
- [ ] **Git-Commit & Push:** Meilenstein 1 abschließen.

### Phase 2: DMX Domain Core & Output-Treiber (TDD-First)
- [ ] **TDD Schritt 1:** Tests schreiben:
  - `DmxUniverseTest.java`: 512-Kanal-Buffer, Master-Berechnung, Blackout-Verhalten, Boundary-Tests (Kanal 1, 512, ungültig 0, 513).
  - `ArtNetDriverTest.java`: UDP-Paketstruktur, OpCode 0x5000, 512 Byte Payload.
  - `VirtualSimulationDriverTest.java`: Speichern und Auslesen letzter gesendeter Frames.
- [ ] **TDD Schritt 2:** Implementierung:
  - `DmxUniverse.java` (Thread-sicher mit `AtomicIntegerArray` oder synchronisiertem Puffer).
  - `DmxOutputDriver.java` Interface.
  - `VirtualSimulationDriver.java` & `ArtNetDriver.java`.
  - `DmxEngine.java`: Periodischer DMX-Tick.
- [ ] **Dokumentation:** Jede Klasse und Methode mit vollständigen JavaDocs dokumentieren.
- [ ] **Verifikation VOR Compile:** `mvn test` ausführen – alle Tests müssen grün sein!
- [ ] **Git-Commit & Push:** Meilenstein 2 abschließen.

### Phase 3: Relationale Persistenz & Fixture-Domain (TDD-First)
- [ ] **TDD Schritt 1:** Tests schreiben:
  - `FixtureTest.java`: Adress-Kollisionstest (`overlapsWith`).
  - `FixtureServiceTest.java`: Validierung vor dem Speichern.
  - `JdbcFixtureRepositoryTest.java`: In-Memory H2 CRUD für Fixtures.
  - `JdbcSceneRepositoryTest.java`: In-Memory H2 CRUD für Szenen mit Mappings.
  - `SceneServiceTest.java`: Szenen abrufen und Fading-Interpolation.
- [ ] **TDD Schritt 2:** Implementierung:
  - Records: `Fixture.java`, `FixtureType.java`, `Scene.java`, `UniverseState.java`.
  - `DatabaseConfig.java` (HikariCP) & `DatabaseInitializer.java` (ANSI DDL).
  - `FixtureRepository.java`, `SceneRepository.java` & JDBC-Implementierungen.
  - `FixtureService.java`, `SceneService.java`, `DmxService.java`.
- [ ] **Dokumentation:** Vollständige JavaDocs für alle Entitäten, Repositories und Services.
- [ ] **Verifikation VOR Compile:** `mvn test` muss 100% grün durchlaufen.
- [ ] **Git-Commit & Push:** Meilenstein 3 abschließen.

### Phase 4: Javalin REST API & SSE Realtime Stream (TDD-First)
- [ ] **TDD Schritt 1:** API-Tests schreiben:
  - `DmxApiControllerTest.java`: REST-Aufrufe, Statuscodes (400, 404), Master, Blackout.
  - `FixtureApiControllerTest.java`: CRUD und Kollision (HTTP 409).
- [ ] **TDD Schritt 2:** Implementierung:
  - `DmxApiController.java`, `FixtureApiController.java`, `SceneApiController.java`.
  - `DmxSseHandler.java`: Connection-Handling und Live-Broadcasts.
  - `JavalinApp.java`: Routing und Error-Handling.
- [ ] **Dokumentation:** Ausführliche JavaDocs und API-Beschreibung.
- [ ] **Verifikation VOR Compile:** `mvn test` muss fehlerfrei laufen.
- [ ] **Verifikation Integration:** API-Endpunkte via HTTP testen.
- [ ] **Git-Commit & Push:** Meilenstein 4 abschließen.

### Phase 5: Web-Dashboard Frontend (Canvas, Lucide, Studio Dark)
- [ ] Dark Theme Layout (`index.html`, `theme.css`, `components.css`) mit Studio-Ästhetik.
- [ ] Lucide-Icons sauber einbinden (keine Emojis!).
- [ ] `universe-grid.js`: 512-Kanal Matrix mit Canvas-Rendering für flüssige Performance.
- [ ] `fader-desk.js`: Interaktive Schieberegler mit 0–255 / Prozentanzeige und Master/Blackout.
- [ ] `sse.js`: Reaktive Anbindung für verzögerungsfreie Live-Synchronisation.
- [ ] Fixture- & Szenen-Verwaltung im UI.
- [ ] **Dokumentation:** Ausführliche Kommentare im Frontend-Code zur Funktionsweise.
- [ ] **Verifikation:** Manueller Test im Browser: Fader-Bewegungen aktualisieren DMX-Werte in Echtzeit; SSE synchronisiert Multi-Tab-Instanzen.
- [ ] **Git-Commit & Push:** Meilenstein 5 abschließen.

### Phase 6: Lückenlose Dokumentation, Wartbarkeit & Release
- [ ] Erstellung von `docs/ONBOARDING.md`:
  - Voraussetzungen & Lokales Setup
  - Architektur-Überblick & Paketstruktur
  - Wie man neue DMX-Treiber hinzufügt (Tutorial)
  - Wie man neue Fixture-Profile anlegt (Tutorial)
  - Test-Ausführung und Debugging-Leitfaden
- [ ] Erstellung von `docs/ARCHITECTURE.md`:
  - DMX-512 Timing & Frame-Buffer
  - Grand Master Skalierungs-Mathematik
  - Threading- & Synchronisationsmodell
  - Art-Net Protokollspezifikation & UDP-Frame-Aufbau
  - Server-Sent Events (SSE) Architektur
  - Relationale H2/JDBC-Datenbankarchitektur
- [ ] Erstellung von `README.md`:
  - Projektvorstellung, Features, Schnellstart
  - Vollständige REST-API & SSE-Referenz mit Beispielen
  - Konfigurationsmöglichkeiten (`application.properties`)
- [ ] **Verifikation VOR Build:** `mvn test` läuft grün durch.
- [ ] **Fat-JAR Build:** `mvn clean package` erzeugt lauffähiges JAR.
- [ ] **Laufzeit-Verifikation:** Ausführen via `java -jar target/jdmx-1.0.0-SNAPSHOT.jar`.
- [ ] **Git-Commit & Push:** Finaler Meilenstein-Commit und Push auf `origin main`.

---

## 8. Start- und Testbefehle

- **Automatisierte Tests ausführen (TDD - VOR JEDEM BUILD):**  
  `mvn test`
- **Projekt kompilieren:**  
  `mvn clean compile`
- **Server im Entwicklungsmodus starten:**  
  `mvn exec:java`
- **Ausführbares Standalone-JAR bauen:**  
  `mvn clean package`
- **Standalone Fat-JAR ausführen:**  
  `java -jar target/jdmx-1.0.0-SNAPSHOT.jar`
- **Standard-URL im Browser:**  
  `http://localhost:7070`
