# JDMX Entwickler-Kochbuch (How-To Rezepte)

Dieses Dokument bietet dir praktische, schrittweise Anleitungen für die häufigsten Erweiterungen und Fehlerbehebungen in JDMX.

---

## Rezept 1: Einen neuen Button in die Benutzeroberfläche einbauen

Angenommen, du möchtest in der Kopfleiste oder Timeline einen neuen Button „Schnell-Mute“ einbauen:

### Schritt 1: Button erzeugen & stylen
Öffne die entsprechende View (z. B. `TimelineMainView.java`):
```java
// Button erstellen mit Lucide-Icon
Button btnQuickMute = new Button();
btnQuickMute.getStyleClass().add("timeline-btn"); // Nutzt das Studio-Dark CSS
btnQuickMute.setGraphic(new LucideIcon(LucideIcon.VOLUME_X, 16, Color.web("#EF4444")));
btnQuickMute.setTooltip(new Tooltip("Alle Spuren stummschalten"));
```

### Schritt 2: Event mit dem Service verbinden
```java
btnQuickMute.setOnAction(e -> {
    // Rufe IMMER eine Methode im Service auf, schreibe keine Logik in die View!
    timelineService.muteAllTracks();
});
```

### Schritt 3: Die Methode im Service definieren
In `TimelineService.java`:
```java
public void muteAllTracks() {
    for (TimelineTrack track : tracks) {
        track.setMuted(true);
    }
    notifyChange(); // Benachrichtigt UI und Audio-Engine über die Änderung
}
```

---

## Rezept 2: Ein neues globales Tastaturkürzel registrieren

Angenommen, du möchtest, dass die Taste `B` jederzeit einen Blackout auslöst:

### Schritt 1: `MainWindow.java` oder die Ziel-View öffnen
Im Key-Event-Handler (`setOnKeyPressed`):
```java
scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
    // 1. Prüfen, ob der Benutzer gerade in einem Textfeld tippt:
    Node focusOwner = scene.getFocusOwner();
    if (focusOwner instanceof TextInputControl) {
        return; // Normal weitertippen lassen!
    }

    // 2. Taste abfragen:
    if (e.getCode() == KeyCode.B) {
        dmxService.toggleBlackout();
        e.consume(); // Verhindert weitere Weiterleitung des Events
    }
});
```

---

## Rezept 3: Einen Unit-Test für eine neue Funktion schreiben (TDD)

Angenommen, du hast eine neue Methode `TimelineService.muteAllTracks()` geplant:

1. Öffne `src/test/java/de/exit/jdmx/service/TimelineServiceTest.java`.
2. Füge den Test am Ende ein:
```java
@Test
@DisplayName("Sollte alle Spuren auf einmal stummschalten")
void shouldMuteAllTracks() {
    TimelineService service = new TimelineService();
    service.addTrack("Track 2");
    
    // Aktion ausführen
    service.muteAllTracks();

    // Zustand mit AssertJ prüfen
    assertThat(service.getTracks())
        .allMatch(TimelineTrack::isMuted);
}
```
3. Führe im Terminal `mvn test -Dtest=TimelineServiceTest` aus.  
   *(Der Test schlägt fehl, solange die Methode nicht existiert).*
4. Implementiere die Methode in `TimelineService.java`.
5. Führe den Test erneut aus $\rightarrow$ **Erfolgreich grün!**

---

## Rezept 4: Fehlerdiagnose „Kein DMX-Signal am Gerät“

Wenn Scheinwerfer nicht reagieren:

1. **Log-Datei prüfen:**
   * Öffne `logs/jdmx.log`.
   * Siehst du `[DMX-CHANGE]`-Einträge? Wenn ja, verarbeitet JDMX deine Fader-Bewegungen intern korrekt.
2. **Art-Net Netzwerkeinstellungen prüfen:**
   * Klicke im Menü auf **I/O Patch** (Tab 5).
   * Steht Art-Net auf der richtigen Netzwerkkarte / IP (z. B. `192.168.x.x` oder `2.x.x.x`)?
   * Ist das Universum richtig gemappt (Universum 1 in JDMX $\rightarrow$ Universum 0 oder 1 auf der Node)? Viele Nodes beginnen bei 0 zu zählen!
3. **Firewall (Port 6454 UDP):**
   * Art-Net benötigt eingehend und ausgehend freigegebenen **UDP-Port 6454**.
   * Windows Defender / Linux `ufw` prüfen:
     ```bash
     # Linux:
     sudo ufw allow 6454/udp
     ```
4. **ArtNet-Monitor einschalten:**
   * Starte das Schwestertool `artnet2view` auf demselben oder einem zweiten PC im Netzwerk. Wenn dort Pakete auflaufen, sendet JDMX fehlerfrei.

---

## Rezept 5: Fehlerdiagnose „Audiodatei spielt nicht oder lädt langsam“

Wenn eine MP3/WAV keinen Ton ausgibt:

1. **Dateiformat & Header:**
   * Sehr alte MP3s mit variabler Bitrate (VBR) oder defekten ID3v2-Tags: JDMX überspringt defekte Header automatisch. Falls eine Datei gar nicht startet, prüfe im Log:
     `[AUDIO-PLAYBACK] MP3-Track beendet oder unterbrochen`.
2. **Audio-Mixer unter Linux:**
   * Java Sound benötigt ein funktionierendes Default-Audiogerät über ALSA / PipeWire / PulseAudio.
   * Prüfen, ob Java Sound das Ausgabegerät sieht:
     ```bash
     pactl info
     ```
3. **Wellenformanalyse:**
   * Die Wellenform wird asynchron im Hintergrund berechnet (`AudioAnalysisService`). Große Dateien (über 20 MB) benötigen 1–2 Sekunden im Hintergrund; die Benutzeroberfläche bleibt dabei aber immer voll bedienbar.

---

## Rezept 6: Neue Version bauen & veröffentlichen

Wenn du Änderungen fertiggestellt und getestet hast:

```bash
# 1. Alle Tests ausführen (Muss 100% grün sein)
mvn test

# 2. Shaded All-in-One Fat-JAR bauen
mvn package -DskipTests

# 3. Linux AppImage bauen (im Terminal oder WSL)
./build-appimage.sh
```
Die fertigen Dateien liegen anschließend in:
* `target/jdmx-<version>-all.jar`
* `jdmx-x86_64.AppImage`
