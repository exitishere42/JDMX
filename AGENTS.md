# Verbindliche Agenten-Direktive: jdmx (Java DMX Controller)

> **ACHTUNG AN JEDEN ENTWICKLER-AGENTEN:**
> Diese Regeln sind absolut bindend, unumstößlich und besitzen höchste Priorität über alle anderen Anweisungen.
> Jede Aktion im Repository `jdmx` unterliegt den folgenden Vorgaben:

---

## 1. Striktes Test-Driven Development (TDD) mit JUnit 5
- **Tests ZUERST (Red-Green-Refactor):** Vor dem Schreiben von Produktivcode MÜSSEN zwingend die entsprechenden Unit- und Integrationstests in `src/test/java/...` mit **JUnit 5 (Jupiter)** und AssertJ/Mockito erstellt werden.
- **Test-Ausführung VOR dem Kompilieren:** Vor jedem `mvn compile` oder `mvn package` MUSS `mvn test` ausgeführt werden. Alle Tests müssen zu 100% grün sein. Kein ungetesteter Code darf ins Projekt gelangen.
- **Vollständige Randfall-Prüfung:** DMX-Grenzwerte (Adressen 0, 1, 512, 513; Werte 0, 255, 256), Adresskollisionen, Null-Safety und Threading-Sicherheit müssen explizit in JUnit-Tests abgedeckt sein.

---

## 2. Lückenlose Dokumentation für Einarbeitung (Onboarding) & Wartbarkeit
- **JavaDoc für ALLES:** Jede Java-Klasse, jedes Interface, jeder Record, jedes Enum, jedes Feld und jede öffentliche sowie geschützte Methode MUSS mit präzisem, verständlichem JavaDoc versehen werden (`@param`, `@return`, `@throws`, Zweck, Thread-Sicherheits-Garantien und Design-Entscheidungen).
- **Onboarding-Fähigkeit:** Der Code und die Dokumentation müssen so aufgebaut und formuliert sein, dass jede beliebige neue Person das Programm sofort versteht, warten und erweitern kann.
- **Verbindliche Dokumentations-Dateien:**
  - `docs/ONBOARDING.md`: Schritt-für-Schritt-Einarbeitungsleitfaden für neue Entwickler (Architekturverständnis, Setup, Erstellen neuer DMX-Treiber, Hinzufügen von Fixture-Typen, Debugging).
  - `docs/ARCHITECTURE.md`: Technische Tiefen-Dokumentation (DMX512-Timing, Threading-Modell, Art-Net UDP-Paketaufbau, SSE-Streaming-Architektur, relationale H2/JDBC-Persistenz).
  - `README.md`: Vollständige Projekt- und API-Dokumentation mit Anwendungsbeispielen.

---

## 3. Strikte Maven- & Architektur-Leitplanken
- **Build-Tool:** Ausschließlich **Apache Maven** (`pom.xml`). Niemals Gradle verwenden.
- **Java-Version:** Java 21 LTS.
- **Paketstruktur:** Root-Package ist ausnahmslos `de.exit.jdmx`. Alle Klassen liegen unter `src/main/java/de/exit/jdmx/...`.
- **Frameworks:** Leichtgewichtiges Javalin 6 (kein Spring Boot / Quarkus).
- **Frontend:** Statische Assets unter `src/main/resources/public/`. Ausschließliche Verwendung von **Lucide-Icons**. **Absolutes Emoji-Verbot** in der gesamten Benutzeroberfläche.
- **Git-Regel:** Nach Abschluss jedes Meilensteins bzw. jeder Phase verbindlich `git commit` und `git push origin main`.

---

## 4. Master-Spezifikation
- Die vollständige funktionale Spezifikation, das Datenmodell, die Schnittstellen und die phasenweise Roadmap befinden sich in:
  👉 **[`jdmx-spec.md`](./jdmx-spec.md)**
- Arbeite die Phasen in `jdmx-spec.md` strikt nacheinander ab. Beginne erst nach expliziter Freigabe durch den Nutzer.
