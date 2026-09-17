# Projektanweisung: jdmx (Java DMX Controller)

> **An den ausführenden Entwickler-Agenten:**
> Dieses Dokument ist deine verbindliche Spezifikation und Arbeitsanweisung.
> Arbeite die Roadmap strikt Phase für Phase ab.
> Führe nach jedem Schritt die definierten Verifikations-Befehle aus. Weiche nicht eigenmächtig von Tech-Stack, Paketstruktur oder Architektur ab.

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
  - **Fixture- & Profilverwaltung:** Adressierung von Geräten (Dimmer, RGB, RGBW, Generic Pan/Tilt) mit Startadresse und Kanalbelegung.
  - **Szenen & Presets:** Speichern von Kanalzuständen als Szenen mit Fader/Fade-Time (ms) und Abrufen per API/Web.
  - **Persistenz (H2 / SQL-Ready):** Speicherung von Fixtures, Universum-Einstellungen und Szenen via H2 Database. Saubere Trennung über Repository-Interfaces und ANSI-SQL/JDBC, sodass die Datenbank jederzeit über Konfiguration auf PostgreSQL oder MySQL umgestellt werden kann.
  - **Web-Dashboard (Embedded):**
    - Komplett aus dem Backend ausgeliefert (`src/main/resources/public/`).
    - 512-Kanal Universe Grid (High-Performance Canvas- oder Virtualized-DOM-Rendering für flüssige 30–60 FPS ohne UI-Lags).
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
- **Logging:** **SLF4J** mit `slf4j-simple` oder Logback.
- **Frontend:**
  - Modernes HTML5, CSS3 (Studio Dark Theme mit CSS-Variablen) und JavaScript (ES Modules).
  - Native **Lucide-Icons** (SVG / lucide).
  - Performance: HTML5 Canvas / Virtual Grid für 512 DMX-Kanäle, EventSource API für SSE.
- **Testing:** **JUnit 5 (Jupiter)** + Mockito für Unit- & Integrationstests.

### Java-Spezifikation (Strikter Standard)
- **GroupId:** `de.exit`
- **ArtifactId:** `jdmx`
- **Base Package:** `de.exit.jdmx`
- **Verzeichnisstruktur:** Alle Java-Klassen liegen zwingend unter `src/main/java/de/exit/jdmx/...`.
- **Maven-Plugins:**
  - `maven-compiler-plugin` (Java 21)
  - `exec-maven-plugin` (Ermöglicht `mvn exec:java`)
  - `maven-jar-plugin` & `maven-shade-plugin` (Für ausführbare Standalone Fat-JARs mit `Main-Class: de.exit.jdmx.Main`)

### Verbotene Muster & Fallstricke (Anti-Halluzinations- & Qualitätsregeln)
- ❌ **Kein Gradle:** Bei Java gilt strikte und ausnahmslose Maven-Pflicht (`pom.xml`).
- ❌ **Kein Spring Boot / Quarkus:** Das Projekt basiert leichtgewichtig auf Javalin 6. Keine schweren Enterprise-Container.
- ❌ **Absolutes Emoji-Verbot im Web-UI:** Keine Emojis in Buttons, Headers, Statusanzeigen oder Tooltips. Ausschließlich Lucide-Icons!
- ❌ **Kein Node/NPM-Zwang für das Basis-Frontend:** Das Web-Frontend wird direkt über statische Ressourcen im Classpath ausgeliefert (`src/main/resources/public`), damit das Maven-Build ohne externe Node-Installationen sauber baut und sofort lauffähig ist.
- ❌ **Keine blockierenden I/O-Aufrufe im DMX- oder SSE-Loop:** Der DMX-Sendeloop und SSE-Broadcasts dürfen den Request-Thread nicht blockieren; Entkopplung über Ringbuffer / atomare Byte-Arrays (`AtomicIntegerArray` oder thread-sichere Puffer).
- ❌ **Keine hardcodierten H2-Spezifika in der Geschäftslogik:** Repositories müssen saubere ANSI-SQL DDLs/Queries nutzen, um jederzeit per Konfiguration auf PostgreSQL oder MySQL umgestellt werden zu können.

---

## 3. Architektur & Dateistruktur

```text
jdmx/
├── pom.xml
├── .gitignore
├── README.md
├── INSTRUCTION.md
├── data/                                 # Lokales Verzeichnis für H2-Datenbank (*.db)
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
    │   │               │   ├── DmxUniverse.java              # 512-Kanal Frame Buffer, Master, Blackout
    │   │               │   ├── DmxEngine.java                # Ticker/Scheduler (z. B. 30–44 Hz DMX Frame Loop)
    │   │               │   └── driver/
    │   │               │       ├── DmxOutputDriver.java      # Interface: sendFrame(int universe, byte[] data)
    │   │               │       ├── VirtualSimulationDriver.java # Logging & In-Memory Visualisierung
    │   │               │       └── ArtNetDriver.java         # UDP Art-Net DMX Paket-Sender
    │   │               ├── model/
    │   │               │   ├── Fixture.java                  # Record/POJO: id, name, type, universe, startAddress, channels
    │   │               │   ├── FixtureType.java              # Enum/Record: DIMMER_1CH, RGB_3CH, RGBW_4CH, CUSTOM
    │   │               │   ├── Scene.java                    # Record/POJO: id, name, channelMap, fadeTimeMs
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
    │   │               │   ├── FixtureService.java           # Fixture-Zuweisung & Adresskollisionsprüfung
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
    │       └── public/                                       # Web-Frontend (Statische Assets)
    │           ├── index.html                                # Dashboard-Layout
    │           ├── css/
    │           │   ├── theme.css                             # Studio Dark Theme, Farbvariablen
    │           │   └── components.css                        # Fader, Channel-Grid, Modals, Badges
    │           └── js/
    │               ├── app.js                                # Haupt-App-Initialisierung & Navigation
    │               ├── sse.js                                # SSE Client mit Auto-Reconnect & Event-Dispatcher
    │               ├── universe-grid.js                      # High-Performance 512-Kanal Canvas-Grid
    │               ├── fader-desk.js                         # Touch/Mouse-Fader mit Latenz-Optimierung
    │               ├── fixtures.js                           # Fixture-Liste, Color-Picker & Adress-Matrix
    │               └── scenes.js                             # Szenen-Speichern & Szenen-Trigger
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

## 4. Datenmodell & Schnittstellen

### 4.1 Java Datamodels (Records)

#### `Fixture.java`
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Fixture(
    String id,              // UUID als String
    String name,            // z. B. "Front Truss Spot 1"
    int universe,           // Standard: 1
    int startAddress,       // 1 bis 512
    FixtureType type,       // DIMMER_1CH, RGB_3CH, RGBW_4CH, CUSTOM
    int channelCount        // Anzahl belegter DMX-Kanäle
) {
    public boolean overlapsWith(Fixture other) {
        if (this.universe != other.universe) return false;
        int thisEnd = this.startAddress + this.channelCount - 1;
        int otherEnd = other.startAddress + other.channelCount - 1;
        return this.startAddress <= otherEnd && other.startAddress <= thisEnd;
    }
}
```

#### `Scene.java`
```java
package de.exit.jdmx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Scene(
    String id,
    String name,
    int universe,
    Map<Integer, Integer> channelValues, // Kanal (1-512) -> Wert (0-255)
    int fadeTimeMs                       // Übergangszeit in Millisekunden
) {}
```

#### `UniverseState.java` (DTO für SSE / REST)
```java
package de.exit.jdmx.model;

public record UniverseState(
    int universeId,
    int[] channels,      // Array der Länge 512 (Werte 0-255)
    int masterValue,     // Grand Master (0-255 bzw. 0-100%)
    boolean blackout     // True falls Blackout aktiv ist
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
- **Definition of Done:**
  - *Eingabe:* Kanal 1 auf 200 setzen, Grand Master auf 50% (128).
  - *Erwartetes Ergebnis:* Interner Rohwert Kanal 1 = 200, effektiver Ausgabewert = 100.
  - *Zustandsänderung:* Aufruf von Blackout setzt effektiven Ausgabewert auf 0; Deaktivieren von Blackout stellt Wert 100 sofort wieder her.

### Feature 2: SSE Live-Streaming (Server-Sent Events)
- **Beschreibung:**  
  Clients verbinden sich über `/api/live/stream`. Javalin verwaltet die verbundenen Clients im `DmxSseHandler`. Bei Wertänderungen oder in einem adaptiven Intervall (z. B. max. 25–30 Hz bei Bewegung) werden Deltas oder Frame-Snapshots im JSON-Format gepusht.
- **Definition of Done:**
  - *Eingabe:* Browser öffnet Dashboard; REST-Call ändert Kanal 5 auf 255.
  - *Erwartetes Ergebnis:* Browser empfängt innerhalb von < 50ms das Event `universe-update` ohne HTTP-Polling.

### Feature 3: Modulare Treiber (Simulation & Art-Net)
- **Beschreibung:**  
  Treiber implementieren `DmxOutputDriver`. `ArtNetDriver` versendet standardkonforme Art-Net DMX UDP-Pakete (Port 6454) an das konfigurierte Subnetz/Broadcast. `VirtualSimulationDriver` dient für Unit-Tests und Entwicklungsbetrieb.
- **Definition of Done:**
  - *Eingabe:* Start mit `jdmx.driver=artnet`, `jdmx.artnet.target=127.0.0.1`.
  - *Erwartetes Ergebnis:* UDP-Paket mit gültigem Art-Net Header (`Art-Net\0`, OpCode `0x5000`) wird auf Port 6454 abgesetzt.

### Feature 4: Austauschbare Persistenz (H2 & SQL-Ready)
- **Beschreibung:**  
  `JdbcFixtureRepository` und `JdbcSceneRepository` verwalten Fixtures und Szenen in einer relationalen H2-Datenbank (`./data/jdmx_db.mv.db`).
  Tabellen: `fixtures`, `scenes`, `scene_channels`. Alle Queries verwenden sauberes Standard-SQL.
- **Definition of Done:**
  - *Eingabe:* Anlegen eines RGB-Fixtures auf Universum 1, Startadresse 10; Neustart der Anwendung.
  - *Erwartetes Ergebnis:* Nach Neustart liefert `GET /api/fixtures` das angelegte Fixture unverändert zurück.

### Feature 5: Web Dashboard mit Lucide-Icons & High-Performance Matrix
- **Beschreibung:**  
  Ein ansprechendes Studio Dark Theme UI:
  - Header: Verbindungsstatus (SSE Indikator), Grand Master Fader, prominenter Blackout-Button (Lucide `Power` / `AlertCircle`), Universum-Wahl.
  - Kanal-Matrix: Visuelle Darstellung aller 512 Kanäle als High-Performance HTML5 Canvas oder virtualisiertes Raster mit Helligkeits-Visualisierung.
  - Fader-Bank: Dynamische Fader für ausgewählte Kanäle oder Fixtures mit Touch-/Maus-Support.
  - Fixture-Manager: Modal zum Hinzufügen mit visueller Belegungsanzeige der DMX-Adressen (1–512) zur Vermeidung von Kanal-Überschneidungen.
  - 100% Lucide-Icons (z. B. `Sliders`, `Sun`, `Zap`, `Power`, `Plus`, `Trash2`, `Play`, `Save`). Keine Emojis!
- **Definition of Done:**
  - *Eingabe:* Fader im UI bewegen.
  - *Erwartetes Ergebnis:* Slider sendet Update; Canvas-Matrix visualisiert sofort die neue Helligkeit; andere geöffnete Browser-Tabs aktualisieren sich live via SSE.

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

## 7. Phasenweise Implementierungs-Roadmap

### Phase 1: Maven Setup & Projektgerüst
- [ ] Saubere `pom.xml` mit Java 21, Javalin 6, Jackson, HikariCP, H2, SLF4J, JUnit 5.
- [ ] Plugins einrichten: `maven-compiler-plugin`, `exec-maven-plugin`, `maven-jar-plugin`, `maven-shade-plugin`.
- [ ] Einstiegsklasse `Main.java` und Konfiguration `AppConfig.java`.
- [ ] **Verifikation:** `mvn clean compile` und `mvn exec:java` startet den Javalin-Server auf Port 7070.

### Phase 2: DMX Domain Core & Output-Treiber
- [ ] `DmxUniverse.java`: 512-Kanal-Puffer, Master-Berechnung, Blackout-Flag.
- [ ] `DmxOutputDriver.java` Interface.
- [ ] `VirtualSimulationDriver.java` und `ArtNetDriver.java` (UDP Socket).
- [ ] `DmxEngine.java`: Periodischer DMX-Tick.
- [ ] **Verifikation:** Unit-Tests `DmxUniverseTest` und `ArtNetDriverTest` laufen fehlerfrei via `mvn test`.

### Phase 3: Relationale Persistenz (H2 & SQL-Ready)
- [ ] `DatabaseConfig.java` mit Connection-Pool (HikariCP) und H2-Konfiguration.
- [ ] `DatabaseInitializer.java` mit idempotentem DDL-Setup (`CREATE TABLE IF NOT EXISTS`).
- [ ] `FixtureRepository` & `SceneRepository` Interfaces und JDBC-Implementierungen.
- [ ] `FixtureService` mit Adress-Kollisionsprüfung.
- [ ] **Verifikation:** Integrationstest speichert Fixtures und Szenen in H2 und liest sie aus (`mvn test`).

### Phase 4: Javalin REST API & SSE Realtime Stream
- [ ] Controller: `DmxApiController`, `FixtureApiController`, `SceneApiController`.
- [ ] `DmxSseHandler.java`: Client-Registrierung, Ping/Keepalive und Broadcast bei Frame-/Kanaländerungen.
- [ ] Fehler-Handling & HTTP-Statuscodes (400, 404, 409).
- [ ] **Verifikation:** curl/HTTP-Tests für Kanal-Änderungen und SSE-Verbindung.

### Phase 5: Web-Dashboard Frontend (HTML5, CSS, JS, Lucide)
- [ ] Dark Theme Layout (`index.html`, `theme.css`, `components.css`) mit Studio-Ästhetik.
- [ ] Lucide-Icons einbinden (keine Emojis!).
- [ ] `universe-grid.js`: 512-Kanal Matrix mit Canvas-Rendering für flüssige Performance.
- [ ] `fader-desk.js`: Interaktive Schieberegler mit 0–255 / Prozentanzeige und Master/Blackout.
- [ ] `sse.js`: Reaktive Anbindung für verzögerungsfreie Live-Synchronisation.
- [ ] Fixture- & Szenen-Verwaltung im UI.
- [ ] **Verifikation:** Manueller Test im Browser: Fader-Bewegungen aktualisieren DMX-Werte in Echtzeit; SSE synchronisiert Multi-Tab-Instanzen.

### Phase 6: Polish, Optimierung & Dokumentation
- [ ] Optimierung der Frame-Rate und Latenz im Frontend & Backend.
- [ ] Erstellung der ausführlichen `README.md` mit Build- und Anwendungsdokumentation.
- [ ] Build des Standalone Fat-JARs (`mvn clean package`).
- [ ] **Verifikation:** Eigenständiges Ausführen via `java -jar target/jdmx-1.0.0-SNAPSHOT.jar`.

---

## 8. Start- und Testbefehle

- **Projekt kompilieren:**  
  `mvn clean compile`
- **Automatisierte Tests ausführen:**  
  `mvn test`
- **Server im Entwicklungsmodus starten:**  
  `mvn exec:java`
- **Ausführbares Standalone-JAR bauen:**  
  `mvn clean package`
- **Standalone Fat-JAR ausführen:**  
  `java -jar target/jdmx-1.0.0-SNAPSHOT.jar`
- **Standard-URL im Browser:**  
  `http://localhost:7070`
