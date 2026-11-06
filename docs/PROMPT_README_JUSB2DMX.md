# Prompt für den Dokumentations-Agenten: Einbindung der `jusb2dmx` Hardware-Bibliothek in `README.md`

Kopiere den folgenden Text und übergib ihn dem Agenten, der das `README.md` von `jdmx` aktualisieren soll:

```markdown
Bitte aktualisiere die Dokumentation in `README.md` des Projekts `jdmx`.

### Hintergrund:
In `jdmx` wurde die universelle, plattformübergreifende Pure-Java USB-to-DMX Hardware-Treiber-Bibliothek `jusb2dmx` (`de.exit.jusb2dmx:jusb2dmx:1.0.0`) als native Hardware-Schnittstelle integriert. 

### Aufgaben für das README.md:
1. **DMX Hardware Output Sektion ergänzen:**
   - Dokumentiere, dass `jdmx` neben Art-Net UDP (Port 6454) und virtuellem Loopback nun auch die native Java-Bibliothek `jusb2dmx` nutzt.
   - Unterstützte Hardware: FTDI FT232R-basierte DMX-Interfaces (insbesondere **JMS USB to DMX PRO**, OpenDMX-kompatible Adapter) über direkte Low-Level-Ansteuerung (Break, Mark-After-Break, 512 DMX-Kanäle).
   - Hervorheben, dass dies vollständig ohne Python-Subprozesse, ohne C-Compiler und ohne COM-Port-Latenzen über reine Java/JNA-Treiber erfolgt.

2. **Dynamische Hardware-Erkennung im I/O Patch Tab (QLC+ Stil):**
   - Beschreibe den neuen **I/O Patch Workspace Tab** (Tab 5):
     - 4 Universen mit individueller Input- und Output-Zuweisung.
     - **Dynamische Erkennung**: Angeschlossene JMS USB2DMX PRO Adapter und MIDI-Controller werden live per Hardware-Discovery erkannt und erscheinen nur im Dropdown, wenn sie tatsächlich physisch angeschlossen sind.
     - Visueller Statusindikator (Aktiv / Inaktiv) und Detail-Konfigurations-Dialog pro Universum.

3. **Maven Dependency & Lokale Installation:**
   - Erwähne kurz die Maven-Abhängigkeit:
     ```xml
     <dependency>
         <groupId>de.exit.jusb2dmx</groupId>
         <artifactId>jusb2dmx</artifactId>
         <version>1.0.0</version>
     </dependency>
     <dependency>
         <groupId>net.java.dev.jna</groupId>
         <artifactId>jna</artifactId>
         <version>5.14.0</version>
     </dependency>
     ```
   - Falls die Bibliothek nicht im lokalen Maven-Repository (`~/.m2`) vorliegt, kann sie aus `d:/programming/tools/jusb2dmx` mit `mvn clean install` gebaut werden.

4. **Sprache und Stil:**
   - Gemäß den Projekt-Richtlinien muss die `README.md` auf **Englisch** verfasst sein.
   - Halte die Dokumentation präzise, übersichtlich und entwicklerfreundlich.
```
