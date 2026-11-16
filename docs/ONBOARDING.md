# JDMX Entwickler-Onboarding & Schnelleinstieg

Willkommen im Entwickler-Leitfaden von **JDMX**!  
Dieses Dokument wurde so geschrieben, dass jede Person – auch ohne Vorwissen über dieses Projekt oder ohne KI-Unterstützung – die Software in wenigen Minuten versteht, lokal ausführen, testen und weiterentwickeln kann.

---

## 1. Voraussetzungen & Werkzeuge

Um JDMX zu bauen und auszuführen, benötigst du genau zwei Dinge auf deinem Rechner:

1. **Java Development Kit (JDK) 21 LTS**
   * *Empfohlen:* Eclipse Adoptium Temurin 21 oder OpenJDK 21.
   * *Prüfung im Terminal:*
     ```bash
     java -version
     javac -version
     ```
     (Es muss `21.x` ausgegeben werden).
2. **Apache Maven 3.9+**
   * *Prüfung im Terminal:*
     ```bash
     mvn -version
     ```
3. **Empfohlene Entwicklungsumgebung (IDE):**
   * **IntelliJ IDEA (Community oder Ultimate Edition):** Perfekte Java- und JavaFX-Unterstützung ohne manuelle Konfiguration.
   * Alternativ: VS Code mit der Erweiterung *Extension Pack for Java*.

---

## 2. Projekt öffnen & Starten in 60 Sekunden

### Option A: In IntelliJ IDEA
1. Starte IntelliJ und wähle **File $\rightarrow$ Open...**
2. Wähle das Verzeichnis `jdmx` (wo die `pom.xml` liegt) und klicke auf **Open as Project**.
3. IntelliJ erkennt das Maven-Projekt automatisch und lädt alle Bibliotheken im Hintergrund.
4. Navigiere zu `src/main/java/de/exit/jdmx/JdmxApplication.java` und klicke auf den grünen **Play-Pfeil** neben `public static void main(...)`.

### Option B: Über die Kommandozeile (Terminal)
```bash
# Projekt bauen und direkt ausführen:
mvn clean compile exec:java
```

---

## 3. Die wichtigsten Maven-Befehle (Dein Werkzeugkasten)

| Befehl | Zweck & Auswirkung |
| :--- | :--- |
| `mvn test` | Führt alle **370 automatisierten Tests** aus. Dauert ca. 15–30 Sekunden. Zeigt dir sofort, ob irgendwo etwas kaputtgegangen ist. |
| `mvn test -Dtest=TimelineServiceTest` | Führt gezielt nur die Tests einer einzelnen Klasse aus (ideal für schnelles Entwickeln). |
| `mvn package -DskipTests` | Baut die fertige, ausführbare All-in-One Fat-JAR unter `target/jdmx-<version>-all.jar` (ca. 24 MB groß, enthält alle Bibliotheken & JavaFX). |
| `mvn javadoc:javadoc` | Generiert das vollständige HTML-API-Dokumentationsportal unter `target/reports/apidocs/index.html`. |
| `mvn clean` | Löscht den `target/`-Ordner (bereinigt alte Build-Dateien). |

---

## 4. Die 3 eisernen Architektur-Regeln von JDMX

Um die Software stabil, wartbar und fehlerfrei zu halten, folgt JDMX drei strengen Grundsätzen:

### Regel 1: Reine MVC-Architektur (Model-View-Controller)
* **Core & Service (`de.exit.jdmx.core` / `de.exit.jdmx.service`):**  
  Hier schlägt das Herz der Anwendung! Alle DMX-Berechnungen, Fader-Kurven, Fade-Zeiten, HTP/LTP-Logik, Matrix-Zustände, Audio-Streams (JLayer) und Treiber-Kommunikation laufen **ausschließlich hier**. Dieser Code weiß nichts von Fenstern, Buttons oder Slidern.
* **JavaFX View (`de.exit.jdmx.ui`):**  
  Die Benutzeroberfläche ist eine **reine Anzeige- und Eingabeschicht**. Sie zeichnet den Zustand und leitet Benutzerinteraktionen (Klicks, Slider-Bewegungen, Tastatur-Events) unverzüglich an die entsprechenden Services weiter.  
  *Wichtig:* In Controllern oder Views darf **niemals** eine DMX-Berechnung, Audio-Dekodierung oder Netzwerk-Paket-Erzeugung stattfinden!

### Regel 2: Thread-Sicherheit & `Platform.runLater`
* JDMX arbeitet mit mehreren parallelen Threads:
  * **JavaFX Application Thread:** Verarbeitet Mausklicks, Tastatur und zeichnet die UI (60 FPS).
  * **DMX Engine Thread:** Sendet stur 40-mal pro Sekunde (40 Hz) DMX-Pakete ins Netzwerk.
  * **Audio Playback Threads:** Dekodieren MP3- und WAV-Streams für jede Spur unabhängig.
* **Merksatz:** Wenn ein Hintergrund-Thread (z. B. der DMX-Loop oder Audio-Tick) die Benutzeroberfläche aktualisieren will, muss dies **immer** über `Platform.runLater(() -> { ... })` geschehen!

### Regel 3: Test-Driven Development (TDD)
* Wenn du eine neue Funktion baust oder einen Fehler behebst:
  1. Schreibe **zuerst** einen Test in `src/test/java/...`, der die neue Funktion aufruft (der Test schlägt fehl = *Rot*).
  2. Implementiere die Funktion im Service oder Core, bis der Test besteht (*Grün*).
  3. Führe `mvn test` aus, um sicherzustellen, dass keine alte Funktion beschädigt wurde (*Regressionstest*).

---

## 5. Projektstruktur auf einen Blick

```text
jdmx/
├── pom.xml                  # Maven Konfiguration & Abhängigkeiten
├── jdmx-spec.md             # Master-Spezifikation (Living Requirements)
├── docs/                    # Entwickler-Dokumentation
│   ├── ONBOARDING.md        # Dieses Dokument
│   ├── ARCHITECTURE.md      # Technische Tiefen-Dokumentation & Signalflüsse
│   ├── CODE_MAP.md          # Dateifinder: Was liegt wo?
│   └── HOWTO.md             # Schritt-für-Schritt Kochbuch für Änderungen
├── src/
│   ├── main/
│   │   ├── java/de/exit/jdmx/
│   │   │   ├── JdmxApplication.java # Haupteinstiegspunkt (main-Methode)
│   │   │   ├── core/        # DMX-Engine, Art-Net, USB, Modelle, Logger
│   │   │   ├── service/     # Business-Logik (Timeline, Audio, DMX, Update)
│   │   │   └── ui/          # JavaFX-Views, Canvas, Dialoge, Icons
│   │   └── resources/       # CSS-Stylesheets, Icons, version.properties
│   └── test/
│       └── java/de/exit/jdmx/ # 370 JUnit 5 Tests
└── target/                  # Generierte Builds, JARs und JavaDocs
```

---

## 6. Wo finde ich Hilfe bei Fragen?

1. **Welche Datei ist für was zuständig?** $\rightarrow$ Lies [`docs/CODE_MAP.md`](./CODE_MAP.md).
2. **Wie fließt das Signal (DMX / Audio)?** $\rightarrow$ Lies [`docs/ARCHITECTURE.md`](./ARCHITECTURE.md).
3. **Wie baue ich ein konkretes Feature ein?** $\rightarrow$ Lies [`docs/HOWTO.md`](./HOWTO.md).
4. **Was waren die ursprünglichen Anforderungen?** $\rightarrow$ Lies [`jdmx-spec.md`](../jdmx-spec.md).
