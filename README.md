# Fotofinder

**Fotofinder** ist eine Android-App, die das Wiederfinden von Fotos in großen Sammlungen einfach macht – komplett **auf dem Gerät** und **ohne Cloud**. Sie durchsucht die Galerie nach Stichwörtern/Tags, nach **Text im Bild** (OCR), nach **Aufnahmedatum** und nach **Aufnahmeort** (GPS) und lässt sich zugleich als vollwertige Galerie nutzen.

> Privatsphäre zuerst: Die gesamte Indexierung (OCR, GPS) passiert lokal. Es werden keine Bilder oder Daten an einen Server gesendet. Internet wird nur für Kartenkacheln und die Ortsauflösung verwendet.

---

## Funktionen

- **Galerie-Start** – beim Öffnen werden alle Fotos angezeigt, mit Ladeanzeige bei großen Beständen.
- **Suche**
  - Stichwörter / **Tags** (manuell, Mehrfachauswahl)
  - **Text im Bild (OCR)** mit Optionen „Ganzes Wort" und „Groß-/Kleinschreibung"
  - **Datumsbereich**
  - **Standort**: mit/ohne GPS, Umkreis um aktuellen Standort, einen Ort oder einen Kartenpunkt
- **Sortierung** nach Datum, Name oder Entfernung (auf-/absteigend)
- **Gruppierung passend zur Sortierung** mit Abschnitts-Überschriften und angehefteter Kopfzeile
  - Datum → Monat · Name → Herkunftsordner · Entfernung → Abstandsbänder
- **Raster-Dichte** 2–5 Spalten per Schieberegler
- **Schnell-Scrollleiste** mit Sprungmarke; beim Ziehen Platzhalter-Raster, Sprung erst beim Loslassen
- **Vollbild-Viewer**: Wischen, Doppeltipp und **Pinch-Zoom**, Teilen, Tag bearbeiten, Info (mit gerenderter Mini-Karte), Löschen
- **Kartenansicht** aller verorteten Fotos; Tippen auf einen Marker öffnet das Foto
- **Markieren-Modus**: Mehrfachauswahl, Stapel-Tagging, Teilen und Löschen mehrerer Fotos
- **Hintergrund-Indexierung** (OCR + GPS) als Vordergrunddienst mit Fortschrittsanzeige
- **Feedback & Unterstützen**: In-App-Feedback-Formular (per E-Mail) und Spenden-Link
- **Material You**: dynamische Farben, Hell-/Dunkelmodus
- Schnelle Thumbnails über vom System vorgenerierte MediaStore-Vorschaubilder

---

## Screenshots

> Platzhalter – hier können Screenshots ergänzt werden.

| Galerie | Suche & Filter | Viewer | Karte |
|--------|----------------|--------|-------|
| _tbd_  | _tbd_          | _tbd_  | _tbd_ |

---

## Voraussetzungen

- **Android 10 (API 29)** oder neuer
- Android Studio (aktuelle stabile Version)
- JDK 17

---

## Bauen & Ausführen

```bash
git clone <REPO-URL> PhotoFinder
cd PhotoFinder

# Debug-Build auf ein verbundenes Gerät
./gradlew installDebug

# Signiertes Release-Bundle für den Play Store (siehe RELEASE.md)
./gradlew bundleRelease   # -> app/build/outputs/bundle/release/app-release.aab
```

Die Signierung läuft über eine lokale, nicht eingecheckte `keystore.properties` (Vorlage: `keystore.properties.template`). Details in **[RELEASE.md](RELEASE.md)**.

- `applicationId`: `de.aicoding.fotofinder`
- Code-Namespace: `com.example.photofinder`
- `minSdk 29`, `targetSdk 34`, JVM-Target 17

---

## Berechtigungen

| Berechtigung | Wozu |
|---|---|
| `READ_MEDIA_IMAGES` (bzw. `READ_EXTERNAL_STORAGE` bis Android 12) | Zugriff auf die Fotos der Galerie |
| `ACCESS_MEDIA_LOCATION` | Auslesen der GPS-Daten aus den Foto-EXIF-Informationen |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Umkreissuche und Entfernungssortierung relativ zum aktuellen Standort |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Laden der Kartenkacheln, Ortsauflösung |
| `FOREGROUND_SERVICE` (+ `DATA_SYNC`) | Indexierung im Hintergrund |
| `POST_NOTIFICATIONS` | Fortschrittsanzeige der Indexierung |

> **Hinweis zu GPS:** Für das Lesen der Standortdaten ist der **volle** Fotozugriff plus `ACCESS_MEDIA_LOCATION` nötig. Die App fragt den Medienstandort bewusst als **separaten zweiten Schritt** nach dem Fotozugriff ab.

---

## Technologie

- **Sprache/UI:** Kotlin, Jetpack Compose, Material 3 (Material You)
- **Datenbank:** Room (lokaler Index, Schemaversion 3 mit Migrationen)
- **Hintergrundarbeit:** WorkManager / Vordergrunddienst
- **Bilder:** Coil, mit eigenem Fetcher für schnelle MediaStore-Thumbnails
- **OCR:** ML Kit Text Recognition (Latin, on-device)
- **Karten:** osmdroid; Kacheln von CARTO (OpenStreetMap-basiert, ohne API-Schlüssel)
- **Dependency Injection:** manuell über einen `ServiceLocator` (kein Hilt)

---

## Architektur

```
UI (Jetpack Compose)
   -> ViewModel (StateFlow / UiState)
      -> Repository
         -> MediaStore    (Fotos, EXIF/GPS, Thumbnails)
         -> Room          (Index: Tags, OCR-Text, Koordinaten, Ordner)
         -> ML Kit        (OCR)
         -> osmdroid/CARTO (Karten)
```

- Die **Suche** läuft als SQL-Vorfilter in Room und wird in Kotlin verfeinert (z. B. „ganzes Wort", Umkreis per Haversine).
- Die **Indexierung** verarbeitet OCR und GPS unabhängig und parallel; der Herkunftsordner wird im Hintergrund nachgetragen.
- **Bild-Performance:** eigener Coil-Fetcher nutzt vom System vorgenerierte MediaStore-Thumbnails statt Originalbilder zu dekodieren.

### Projektstruktur (Auszug)

```
app/src/main/java/com/example/photofinder/
├─ PhotoFinderApp.kt           # Application, ImageLoader, ServiceLocator-Init
├─ MainActivity.kt             # Compose-Host, Berechtigungen
├─ di/ServiceLocator.kt        # manuelle DI
├─ domain/model/               # Photo, SearchQuery, SortOption
├─ data/
│  ├─ db/                      # Room: Entity, DAO, Datenbank + Migrationen
│  ├─ media/                   # MediaStore-Quelle, Thumbnail-Fetcher
│  ├─ ocr/                     # ML-Kit-OCR-Service
│  └─ PhotoRepository.kt
├─ prefs/ConsentManager.kt
├─ work/                       # Indexierungs-Dienst + Status
├─ util/LocationResolver.kt    # Standort, Geocoding
├─ support/                    # Wiederverwendbares Feedback-/Spenden-Modul
└─ ui/
   ├─ theme/                   # Material-You-Theme
   └─ search/                  # SearchViewModel, SearchScreen (Galerie, Viewer, Karte)
```

---

## Feedback & Unterstützung

Über „Feedback und Unterstützen" (Überlaufmenü) öffnet sich ein Bildschirm mit einem In-App-Feedback-Formular (Versand per E-Mail) und einem Spenden-Link. Der gesamte Code dafür liegt im generischen `support`-Paket; app-spezifische Werte stehen in genau einer Datei (`support/FotofinderSupport.kt`) und sind so für weitere Apps wiederverwendbar.

---

## Datenschutz

Die App arbeitet lokal; Details stehen in der Datenschutzerklärung (siehe `Datenschutzerklaerung.md`). Daten verlassen das Gerät nur für Kartenkacheln (CARTO), die Ortsauflösung (System-Geocoder) und freiwilliges Feedback per E-Mail.

---

## Roadmap

Umgesetzt: Galerie, Tag-/OCR-/Datum-/Standortsuche, Sortierung, Gruppierung mit Headern, Raster-Dichte, Schnell-Scrollleiste, Vollbild-Viewer, Kartenansicht, Markieren-Modus mit Teilen/Löschen, Hintergrund-Indexierung, Thumbnail-Performance, Feedback/Spenden, Release-Vorbereitung.

Offen / Ideen: Tag-Verwaltung (umbenennen/zusammenführen/löschen), Wisch-/Bereichsauswahl, Favoriten/Ausblenden, gespeicherte Suchen, Marker-Clustering (osmdroid-bonuspack), Einstellungsseite, Papierkorb, Duplikat-Erkennung.

---

## Lizenz & Drittanbieter

Die Lizenz dieses Projekts ist noch festzulegen (z. B. MIT oder Apache-2.0).

Verwendete Open-Source-Bausteine u. a.: **osmdroid** (Apache-2.0), **Coil** (Apache-2.0), **Google ML Kit** (Google-Nutzungsbedingungen). Kartendaten © **OpenStreetMap**-Mitwirkende (ODbL), Kacheldarstellung © **CARTO**. Bei Veröffentlichung bitte die Attribution „© OpenStreetMap-Mitwirkende, © CARTO" anzeigen.
