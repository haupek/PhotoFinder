# Fotofinder – Technische Release-Vorbereitung

Diese Anleitung beschreibt den Weg von hier zu einem signierten, hochladbaren
Android App Bundle (.aab) für den Google Play Store.

> Keine Rechtsberatung. Prüfe Play-Richtlinien und rechtliche Angaben selbst.

## 1. applicationId prüfen (WICHTIG, dauerhaft!)

Die `applicationId` ist ab dem ersten Upload **unveränderbar**. Sie wurde von
`com.example.photofinder` (im Store nicht erlaubt) auf

```
applicationId = "de.aicoding.fotofinder"
```

geändert (in `app/build.gradle.kts`). Wenn du eine andere ID möchtest, ändere sie
JETZT vor dem ersten Upload. Der Code-Namespace (`com.example.photofinder`) bleibt
davon unberührt und muss nicht angefasst werden.

## 2. Versionierung

In `app/build.gradle.kts`:

```
versionCode = 1        // ganze Zahl, muss bei JEDEM Upload erhöht werden
versionName = "1.0.0"  // für Nutzer sichtbar
```

Für jedes weitere Release: `versionCode` +1 (z. B. 2, 3, …), `versionName` nach
Bedarf (z. B. "1.0.1").

## 3. Upload-Keystore erzeugen

Einmalig einen Keystore anlegen (JDK muss installiert sein). Im Projektstamm:

```
keytool -genkeypair -v \
  -keystore fotofinder-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias fotofinder
```

Du wirst nach einem Store- und Key-Passwort sowie Namensangaben gefragt.
**Bewahre die .jks-Datei und die Passwörter sicher auf** – ohne sie kannst du
keine Updates mehr signieren.

## 4. keystore.properties anlegen

`keystore.properties.template` nach `keystore.properties` kopieren (Projektstamm)
und ausfüllen:

```
storeFile=fotofinder-upload.jks
storePassword=DEIN_STORE_PASSWORT
keyAlias=fotofinder
keyPassword=DEIN_KEY_PASSWORT
```

Die `build.gradle.kts` liest diese Werte automatisch und signiert den
Release-Build damit. Ohne diese Datei bleibt der Release-Build unsigniert
(Debug-Builds funktionieren weiterhin).

## 5. Geheimnisse aus git heraushalten

Ergänze `.gitignore` (Projektstamm) um:

```
keystore.properties
*.jks
*.keystore
```

Diese Dateien dürfen NIE ins Repository gelangen.

## 6. Play App Signing (empfohlen)

Google Play verwaltet auf Wunsch den eigentlichen App-Signing-Schlüssel; du lädst
mit deinem Upload-Schlüssel hoch. Beim Anlegen der App in der Play Console
"Play App Signing" aktivieren – so ist der Schlüssel auch bei Verlust des
Upload-Keys wiederherstellbar. Der oben erzeugte Keystore ist dann dein
Upload-Schlüssel.

## 7. Release-Bundle bauen

```
./gradlew clean bundleRelease
```

Ergebnis:

```
app/build/outputs/bundle/release/app-release.aab
```

Diese .aab-Datei lädst du in der Play Console hoch. (Ein reines APK für Tests
ginge mit `./gradlew assembleRelease`.)

## 8. Optional: R8/Minify später aktivieren

Für kleinere Downloads kannst du später in `app/build.gradle.kts` im
`release`-Block `isMinifyEnabled = true` und `isShrinkResources = true` setzen.
Dann unbedingt einen Release-Build auf einem Gerät testen. Empfohlene Regeln in
`app/proguard-rules.pro` (Room, ML Kit und osmdroid liefern eigene Regeln mit;
diese Ergänzungen sind konservativ):

```
# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keepclassmembers class * { @androidx.room.* <methods>; }

# ML Kit Text Recognition
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Coil
-dontwarn coil.**
```

## 9. Checkliste vor dem Upload

- [ ] `applicationId` final bestätigt (dauerhaft!)
- [ ] `versionCode`/`versionName` gesetzt
- [ ] Keystore erzeugt, `keystore.properties` ausgefüllt, beides gesichert
- [ ] `.gitignore` schützt Keystore-Dateien
- [ ] `./gradlew bundleRelease` erzeugt eine signierte .aab
- [ ] Datenschutzerklärung online, URL in der Play Console eingetragen
- [ ] "Datensicherheit"-Formular ausgefüllt (siehe Play-Datensicherheit-Leitfaden)
- [ ] App-Icon, Screenshots, Feature-Grafik, Store-Beschreibung vorbereitet
- [ ] Zielgruppe/Inhaltsfreigabe ausgefüllt (nicht auf Kinder ausgerichtet)
