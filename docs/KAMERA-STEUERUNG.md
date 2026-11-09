# Kamerasteuerung α7R III – Recherchestand (Nov. 2026)

**Kurz:** Sonys offizielle Wege decken die α7R III für Picture Profiles nicht ab. Ein Versuch über USB/PTP ist möglich, der Ausgang ist offen. Nichts hiervon ist an der echten Kamera geprüft.

## Was bekannt ist
- **Imaging Edge Mobile** (Sony-App, WLAN): auslösen, einige Belichtungswerte – keine Picture Profiles.
- **PlayMemories Camera Apps:** von der α7R III nicht unterstützt.
- **Camera Remote SDK** (Sony, C++, USB/LAN/WLAN): Picture-Profile-Steuerung gibt es, aber nur für neuere Modelle (ZV-E1, α7R V, α7 IV u. a.). Die α7R III steht nicht auf der Liste. Läuft außerdem nur auf Windows/macOS/Linux-PCs, nicht auf Android.
  Quelle: https://support.d-imaging.sony.co.jp/app/sdk/en/index.html
- **Camera Remote Command** (Sony, Erweiterung des Standards PTP, betriebssystemunabhängig): Liste der unterstützten Geräte enthält **ILCE-7RM3A** und ILCE-7M3 – nicht ausdrücklich die ursprüngliche ILCE-7RM3. Ob Picture Profiles darüber erreichbar sind, ist offen.
  Quelle: https://pro.sony/en_RO/solutions/digital-imaging/sdk
- **gphoto2/libgphoto2** spricht Sonys PTP-Erweiterung im Modus „PC-Fernbedienung“; unbekannte Eigenschaften erscheinen dort als „PTP Property 0xD…“.

## Realistische Erwartung
Höchstens: zwischen PP1–PP10 / Aus umschalten und ggf. Weißabgleich (Kelvin, Verschiebung) setzen. Einzelwerte (Gamma, Schwarzwert, Knie …) per Fernsteuerung gibt es bei Sony erst bei neueren Modellen – bei der α7R III nicht erwarten.

## Weg auf Android
Android-Handy als USB-Host (USB-C-OTG-Kabel direkt an die Kamera), Kamera im Modus **PC-Fernbedienung**. PTP selbst über `android.hardware.usb` implementieren (`UsbManager`, `UsbDeviceConnection.bulkTransfer`), weil `android.mtp.MtpDevice` keine herstellerspezifischen Befehle zulässt. Sonys Befehlsablauf (Authentifizierung/Verbindungsaufbau, Abfrage aller Eigenschaften, Setzen einer Eigenschaft) steht in der Camera-Remote-Command-Referenz bzw. lässt sich aus libgphoto2 (`camlibs/ptp2`, Sony-Teil) ablesen.

## Machbarkeitstest (Phase 3)
1. Kamera auf neueste Firmware, USB-Verbindung „PC-Fernbedienung“.
2. **Am PC zuerst:** `gphoto2 --summary` und `gphoto2 --list-all-config` ausführen, Ausgaben in `docs/test-ausgaben/` ablegen.
3. Während der Aufnahme an der Kamera das Bildprofil wechseln (PP1 → PP2 → Aus) und `--list-all-config` erneut ausführen: Welche Eigenschaft ändert sich? Ist sie beschreibbar?
4. Dasselbe für Weißabgleich (Kelvin, A-B, G-M).
5. Nur wenn eine beschreibbare Eigenschaft gefunden wurde: kleines Android-Testmodul, das die Kamera verbindet, diese Eigenschaft liest und setzt.
6. Ergebnis hier dokumentieren (gefundene Property-Codes, Wertekodierung, was nicht geht).

Wenn der Test negativ ausfällt, bleibt die App bei Bibliothek + Eingabehilfe – das ist ihr Kern und funktioniert unabhängig davon.
