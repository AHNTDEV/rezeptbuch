# Rezeptbuch α7R III – Android-App

## Worum es geht
Eine Android-App, mit der Martin eine eigene **Rezept-Galerie für Bildprofile seiner Sony α7R III** (ILCE-7RM3) pflegt – nach dem Vorbild der Film-Rezepte bei Fujifilm. Ein „Rezept“ ist ein vollständiger Satz Picture-Profile-Werte plus Weißabgleich, der einen bestimmten Film-Look auf JPEGs erzeugt.

Anders als bei Fuji kann man der α7R III kein Rezept „schicken“. Die Kamera hat zehn Speicherplätze (PP1–PP10), die man von Hand mit Werten belegt. Die App ist deshalb vor allem **Bibliothek, Eingabehilfe und Platz-Verwaltung**. Kamerasteuerung per USB ist ein späteres, optionales Modul (siehe `docs/KAMERA-STEUERUNG.md`).

## Was hier liegt
- `docs/SPEC.md` – Funktionen, Screens, Datenmodell, Wertebereiche, Menü-Reihenfolge. **Maßgeblich.**
- `docs/KAMERA-STEUERUNG.md` – Recherchestand zur Fernsteuerung und Plan für einen Machbarkeitstest.
- `data/recipes.json` – Startbestand: 8 Rezepte. Beim ersten Start in die lokale Datenbank importieren.
- `data/fuji-adaptions.json` – aus Fujifilm-Rezepten (filmsimrecipes.com) umgerechnete Rezepte, **ungeprüfte Näherung**, Regeln in `docs/FUJI-ADAPTION.md`, erzeugt mit `tools/`.
- `prototype/index.html` – funktionierender Web-Prototyp (läuft als claude.ai-Artifact). Referenz für Abläufe, Texte und Look – **nicht** portieren, sondern nachbauen. Der Prototyp nutzt eine Plattform-API (`window.claude.use("db")`), die es in Android nicht gibt.

## Technische Leitplanken
- Kotlin, Jetpack Compose, Material 3 (eigenes dunkles Farbschema, siehe SPEC), minSdk 26.
- Architektur: einfache Schichten – UI (Compose) → ViewModel → Repository → Room. Keine überdimensionierten Frameworks.
- Persistenz: Room. Fotos als Dateien im App-Speicher (`filesDir/photos/`), in der DB nur Dateinamen.
- Offline-first, kein Login, kein Backend. Export/Import der Bibliothek als JSON (gleiches Format wie `data/recipes.json`) zur Sicherung.
- Dependency Injection: Hilt oder manuell – Hauptsache schlank.
- Tests: Unit-Tests für Formatierung (Vorzeichen, A/B-, G/M-Notation), JSON-Import/-Export und Platz-Logik.

## Konventionen
- UI-Sprache **Deutsch**. Kamerabegriffe so, wie sie im Sony-Menü stehen (Gamma, Knie, Farbmodus …); Sony-Werte wie „Still“, „Cine1“, „S-Gamut3.Cine“, „Type3“ nicht übersetzen.
- Zahlen mit Vorzeichen anzeigen (+6, −7, 0). Weißabgleich-Verschiebung als „A2 / M1“.
- Code, Bezeichner und Commit-Messages auf Englisch.

## Arbeitsweise
- In Phasen arbeiten (siehe SPEC, Abschnitt „Phasen“). Phase 1 zuerst vollständig fertig und lauffähig machen.
- Alles, was die Kamera betrifft, ist **unbestätigt**, bis es an einer echten α7R III getestet wurde. Nichts als funktionierend markieren, was nur aus der Dokumentation abgeleitet ist.
- Unklarheiten bei Kamerawerten nicht raten, sondern im Rezept als Notiz kennzeichnen (Beispiele in `recipes.json`).
