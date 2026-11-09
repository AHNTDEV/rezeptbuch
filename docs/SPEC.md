# Spezifikation – Rezeptbuch α7R III (Android)

## 1. Ziel
Film-Rezepte für die α7R III sammeln, ansehen, an der Kamera eingeben und den zehn Bildprofil-Plätzen (PP1–PP10) zuordnen. Ein Rezept = Bildprofil-Werte + Weißabgleich. Rezepte wirken nur auf JPEGs.

## 2. Screens

### 2.1 Galerie (Start)
- Raster aus Karten (2 Spalten Handy, mehr auf Tablet). Karte: Bild, Name, Look-Kurztext, Gamma, Weißabgleich (`8000 K · B3 / M1.75`), Platz-Badge (z. B. „PP3“), falls belegt.
- Bild: erstes eigenes Beispielfoto. Ohne Foto: **berechnete Farbkarte** (siehe 5.) mit dem Hinweis „Farbstimmung · grobe Annäherung“.
- Suche (Name, Look, Stichworte, Notizen) und Filter-Chips nach Stichworten („Alle“ + jedes vorkommende Stichwort).
- FAB „+ Rezept“.
- Leerzustand: „Noch keine Rezepte. Leg über „+ Rezept“ das erste an.“

### 2.2 Rezept-Detail
- Großes Bild bzw. Farbkarte, darunter Vorschaubilder der Fotos, „+ Beispielfoto“ (Fotoauswahl über den Android Photo Picker), „Foto entfernen“ (mit Rückfrage).
- Look-Text, Stichworte, Notizen (hervorgehoben, z. B. Hinweise zu unklaren Quellwerten).
- Aktionen: **An Kamera einstellen**, Platz wählen (– / PP1…PP10), Bearbeiten, Duplizieren (Name + „ (Variante)“, ohne Fotos), Löschen (mit Rückfrage, gibt den Platz frei).
- Wertegruppen in Menü-Reihenfolge (siehe 4.): Tonwerte · Farbe · Detail · Weißabgleich. Knie-Punkt/-Steigung nur bei Knie-Modus „Manuell“, Detail-Unterwerte nur bei Detail-Modus „Manuell“.
- Quelle mit Link.

### 2.3 Eingabe-Modus („An Kamera einstellen“)
- Für den Einsatz an der Kamera: Bildschirm bleibt an (`FLAG_KEEP_SCREEN_ON`), große Schrift, große Tippflächen.
- Kopf: Menüpfad „MENU → Kameraeinstellungen 1 → Bildprofil → PPx → ►“ und für den Weißabgleich „MENU → Kameraeinstellungen 1 → Weißabgleich → Farbtemp./Filter, dann ► für A-B / G-M“, mit Hinweis, dass Bezeichnungen je nach Sprache/Firmware abweichen können. PPx = zugewiesener Platz oder „PPx“.
- Abhak-Liste, gruppiert (Bildprofil · Farbtiefe · Detail · Weißabgleich), Fortschrittsbalken „n von m erledigt“, „Zurücksetzen“. Häkchen nur im Speicher, nicht persistent.
- Wenn alles abgehakt: „Fertig. Mach ein Testfoto und lade es als Beispielfoto hoch.“

### 2.4 PP-Plätze
- Zehn Zeilen PP1–PP10: Platz, Rezeptname, Gamma + Weißabgleich, Auswahl (– frei – / Rezepte alphabetisch).
- Ein Rezept liegt höchstens auf einem Platz: Zuweisung an einen neuen Platz entfernt es vom alten.
- Hinweistext: Die Kamera speichert nur die Bildprofil-Werte; der Weißabgleich wird separat eingestellt. Wer PP-Plätze für Video nutzt, lässt einige frei.
- „Belegung teilen“ (Android-Share-Sheet, Text): `PP1: Kodak Gold — WB 8000 K · B3 / M1.75` bzw. `PP2: frei`.

### 2.5 Editor (neu / bearbeiten)
Gruppen: Allgemein (Name*, Look, Stichworte kommagetrennt, Notizen, Quelle, Quell-Link) · Tonwerte · Farbe · Detail · Weißabgleich. Eingaben per Stepper/Dropdown mit den Bereichen aus 4.; Gamma und Farbmodus zusätzlich frei eingebbar (die Quellen schreiben z. B. „S-Gamut3/S-Gamut“).

### 2.6 Einstellungen
- Bibliothek exportieren (JSON, inkl. Platzbelegung; Fotos optional als ZIP).
- Bibliothek importieren (zusammenführen oder ersetzen, mit Rückfrage).
- Startbestand wiederherstellen (die 8 mitgelieferten Rezepte).
- Fuji-Adaptionen hinzufügen (Rezepte aus `data/fuji-adaptions.json`, siehe `docs/FUJI-ADAPTION.md`).

## 3. Datenmodell

```
Recipe
  id: String (Slug, z. B. "kodak-gold")
  order: Int
  name: String, look: String, tags: List<String>, notes: String
  source: String, sourceUrl: String
  photos: List<String>          // Dateinamen in filesDir/photos
  pp: PictureProfile
  wb: WhiteBalance
  updatedAt: Instant

PictureProfile
  blackLevel: Int?              // -15..+15
  gamma: String                 // siehe 4.
  blackGamma: { range: Wide|Middle|Narrow, level: Int? (-7..+7) }
  knee: { mode: Auto|Manuell, point: Double? (75..105, Schritt 2.5), slope: Int? (-5..+5) }
  colorMode: String
  saturation: Int?              // -32..+32
  colorPhase: Int?              // -7..+7
  depth: { R,G,B,C,M,Y: Int? }  // je -7..+7
  detail: { level: Int? (-7..+7), mode: Auto|Manuell, vh: Int? (-2..+2),
            bw: Type1..Type5, limit: Int? (0..7), crisp: Int? (0..7), hl: Int? (0..4) }

WhiteBalance
  kelvin: Int                   // 2500..9900, Schritt 100
  ab: Double                    // + = A (Amber), - = B (Blau); Schritt 0.25, -7..+7
  gm: Double                    // + = G (Grün), - = M (Magenta); Schritt 0.25, -7..+7

Slots: Map<"PP1".."PP10", recipeId?>
```
Leere Werte („“ in JSON) = nicht gesetzt → Anzeige „–“. Notation „A2-M1“ aus Quellen = ab +2, gm −1.

## 4. Menü-Reihenfolge und Werte (α7R III, Bildprofil)
1. Schwarzwert
2. Gamma: Movie, Still, Cine1–Cine4, ITU709, ITU709(800%), S-Log2, S-Log3, HLG, HLG1–HLG3
3. Schwarzgamma: Bereich, Stufe
4. Knie: Modus; bei Manuell Punkt, Steigung
5. Farbmodus: Movie, Still, Cinema, Pro, ITU709 Matrix, Black & White, S-Gamut, S-Gamut3.Cine, S-Gamut3
6. Sättigung
7. Farbphase
8. Farbtiefe R, G, B, C, M, Y
9. Detail: Stufe, Modus; bei Manuell V/H-Balance, B/W-Balance, Begrenzung, Crispening, Lichter-Detail

Die Wertelisten stammen aus dem Prototyp und Sonys Hilfe-Guide; an der Kamera gegenprüfen.

## 5. Farbkarte (Platzhalter ohne Foto)
Wie im Prototyp (`swatchHTML` / `castColor`): Verlauf Schatten → Mitte → Lichter, getönt aus Weißabgleich (höherer Kelvin-Wert = wärmeres Bild), A-B/G-M-Verschiebung und Sättigung; Schattenhelligkeit aus dem Schwarzwert; darunter sechs Farbfelder R G B C M Y, moduliert durch die Farbtiefe. Ist ausdrücklich nur eine Annäherung – immer mit Hinweis.

## 6. Gestaltung
Dunkelkammer-Look mit Kontaktbogen-Motiv. Das Dunkle ist bewusst gewählt (Fotografie, Bedienung bei wenig Licht), der Akzent ist das Bernstein des Dunkelkammer-Lichts und wird sparsam eingesetzt.

- **Farben:** Hintergrund #12100E, Filmgrund #0C0A09, Flächen #1A1714 / #231F1B, Linien #332D27, Text #EDE4D6, gedämpft #9C9183, Akzent Bernstein #E8A33D, Fehler #E5735F.
- **Schriften** (als OFL-Dateien in `res/font`): **Young Serif** für Namen und Titel, **IBM Plex Sans** für Text, **IBM Plex Mono** für alle Kamerawerte (gepunktete Null, damit „0“ und „O“ am Kameramenü nicht verwechselt werden).
- **Schriftgrößen:** Text 12 / 14 / 16, Titel 17 / 22 / 30, Werte und Randbeschriftung 11 / 14.
- **Formen:** fast eckige Ecken (2–8 dp), wie Filmbilder. Kasten, Rand und Füllung nur dort, wo ein Element sich abheben muss (Kartenbild, Hinweiskasten, Eingabefelder). Wertegruppen sind Abschnitte mit Haarlinie, keine Karten.
- **Motive:** Perforation unter der Kopfzeile; Karten als Kontaktbogen-Bild mit Randbeschriftung („▸ 05“ = Rezeptnummer, daneben der PP-Platz); Werte mit gepunkteter Führungslinie wie auf einem Laborblatt; Farbtiefe als sechs Spalten mit Balken von der Mitte (−7 … +7); Weißabgleich als Kelvin-Skala 2500–9900 K mit Marke und A-B / G-M-Raster mit Punkt.
- **Eingabe-Modus:** Liste mit Haarlinien statt Kästen, 68 dp hohe Zeilen, Werte 24 sp in Bernstein; Erledigtes wird gedämpft und durchgestrichen.

## 7. Phasen
1. **Bibliothek (MVP):** Datenmodell, Room, Import von `recipes.json`, Galerie, Detail, Editor, Eingabe-Modus, PP-Plätze, Fotos. Lauffähig auf Martins Handy.
2. **Sichern & Teilen:** JSON-Export/-Import, ZIP mit Fotos, Rezept als Text teilen.
3. **Machbarkeitstest Kamera (USB):** siehe `docs/KAMERA-STEUERUNG.md`. Ergebnis dokumentieren, bevor irgendetwas davon in die UI kommt.
4. **Kamerasteuerung (nur wenn Phase 3 positiv):** aktuellen PP-Platz der Kamera anzeigen und umschalten; ggf. Weißabgleich setzen.
