# Fuji-Rezepte für die α7R III adaptieren

**Stand:** heuristische Übersetzung, an keiner Kamera geprüft. Alle so erzeugten Rezepte tragen das Stichwort „Fuji-Adaption“ und den Hinweis „nicht an der α7R III geprüft“.

## Quelle
- Rezepte von https://filmsimrecipes.com (öffentliche Rezeptseiten, die Einstellungen der jeweiligen Ersteller). Jede Adaption nennt den Ersteller (Instagram-Name) und verlinkt die Rezeptseite.
- Rezepte hinter der Pro-Sperre der Seite wurden nicht übernommen.
- Look-Texte sind eigene Formulierungen, aus den umgerechneten Werten abgeleitet.
- Der Import läuft über `tools/scrape_filmsim.py` (1 Anfrage pro Sekunde) und `tools/fuji_to_sony.py`. Ergebnis: `data/fuji-adaptions.json`. Erneut erzeugen mit:

```
python tools/scrape_filmsim.py <ausgabeordner>
python tools/fuji_to_sony.py <ausgabeordner>/recipes_raw.json data/fuji-adaptions.json
```

## Warum nur eine Näherung
Fuji und Sony rechnen anders. Fuji kennt fertige Filmsimulationen mit festen Kurven und Farbtönen, Sony nur einzelne Regler (Gamma, Schwarzwert, Knie, Farbmodus, Farbtiefe …). Die Regeln unten sind aus der Bedeutung der Parameter abgeleitet, nicht gemessen. Wer ein Rezept an der Kamera ausprobiert, sollte die Werte danach per Testfoto nachjustieren und das Ergebnis als Notiz im Rezept festhalten.

## Regeln

### Filmsimulation → Grundprofil
| Fuji | Gamma | Farbmodus | Sättigung | Farbphase | Farbtiefe R G B C M Y |
|---|---|---|---|---|---|
| Provia/Standard | Movie | Movie | 0 | 0 | 0 0 0 0 0 0 |
| Velvia/Vivid | Still | Still | +12 | 0 | +2 +3 +2 +2 +1 +2 |
| Astia/Soft | Cine2 | Movie | −4 | +1 | 0 −1 −1 −1 −1 −1 |
| Classic Chrome | Cine1 | Pro | −6 | 0 | 0 −3 −2 −2 −1 −1 |
| Classic Negative | Still | Cinema | +4 | −2 | +1 −3 +2 +1 0 −1 |
| Nostalgic Neg. | Cine2 | Cinema | +2 | +2 | +1 −2 −2 −1 +1 +2 |
| Eterna | Cine2 | Cinema | −8 | −1 | 0 −2 −1 −1 −1 −1 |
| Reala Ace | Still | Movie | 0 | 0 | +1 0 0 0 0 0 |
| Pro Neg. Std | Movie | Pro | −3 | 0 | 0 |
| Pro Neg. Hi | Still | Pro | +1 | 0 | 0 |
| Acros (alle) | Still | Black & White | – | – | – |

### Einzelregler
| Fuji | Sony | Regel |
|---|---|---|
| Shadow (−2 … +4) | Schwarzwert, Schwarzgamma-Stufe | Schwarzwert = −3 × Shadow; Stufe = −Shadow; Bereich Middle |
| DR + Highlight | Knie | DR100 ohne Highlight-Korrektur: Auto. Sonst Manuell: Punkt = 100 / 92,5 / 85 (DR100 / 200 / 400) + 2,5 × Highlight, Steigung 0 / +1 / +2 |
| Color (−4 … +4) | Sättigung | Basis + 4 × Color |
| Color Chrome Effect | Farbtiefe R G C M Y | Strong +2, Weak +1 |
| Color Chrome FX Blue | Farbtiefe B | Strong +3, Weak +1 |
| Sharpness (−4 … +4) | Detail-Stufe, Modus Auto | gerundet 1,5 × Sharpness |
| Weißabgleich Kelvin / Preset | Farbtemperatur | Kelvin übernommen; Daylight 5500, Shade 7000, Cloudy 6000, Incandescent 3000, Fluorescent 4200; bei Auto 5500 als Startwert (mit Hinweis) |
| Red / Blue Shift (±9) | A-B, G-M | A-B = (Red − Blue) / 2 × 7/9; G-M = −(Red + Blue) / 2 × 7/9; auf 0,25 gerundet |

### Nicht übertragbar (steht nur in den Notizen)
Grain Effect, Clarity, Noise Reduction (bei Sony getrennt vom Bildprofil), ISO- und Belichtungsempfehlung, Filter der Acros-Varianten.

## Prüfen
`DataFilesTest` stellt sicher, dass jede erzeugte Zahl in den Bereichen aus SPEC Abschnitt 3 liegt und dass jede Adaption als ungeprüft gekennzeichnet ist und ihre Quelle nennt.
