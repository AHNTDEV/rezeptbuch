# Rezeptbuch α7R III

Android-App für eigene Film-Rezepte (Bildprofile + Weißabgleich) der Sony α7R III.

Start mit Claude Code: Ordner öffnen, `claude` starten und z. B. sagen:
> Lies CLAUDE.md und docs/SPEC.md und setz Phase 1 um.

- `CLAUDE.md` – Projektkontext und Leitplanken
- `docs/SPEC.md` – Spezifikation
- `docs/KAMERA-STEUERUNG.md` – Recherche und Testplan USB-Steuerung
- `data/recipes.json` – 8 Startrezepte
- `prototype/index.html` – Web-Prototyp als Referenz
- `app/` – Android-Projekt (Phase 1)

## Bauen und testen

JDK von Android Studio verwenden (`JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`):

```
gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. `data/recipes.json` wird beim Bauen als Asset eingebunden (einzige Quelle des Startbestands).
