# PersoSim for Android

PersoSim emulates electronic identity test cards over NFC on an Android device.
This version adds a profile list, a profile editor and PersoSim file import.
Current version: **0.18** (versionCode **28**).

The app requires Android 5.0 or later and NFC Host Card Emulation for card scans.
A separate reader or reader app initiates the NFC exchange.

## Features

- Starts the simulator and card emulation when the app opens.
- Restores the last selected profile and keeps the screen on in the main screen
  and editor.
- Includes all 17 unchanged original PersoSim 0.17.2 profiles. Profile 01 is the
  default; own and imported profiles appear above the originals.
- Shows document type in profile subtitles and full profile details, including
  document number and test PIN, CAN and PUK.
- Provides NFC checks for hardware support, activation, AID routing and simulator
  readiness, plus a diagnostic log.

## Create, copy and import profiles

**Neuer Ausweis** opens the editor with original profile 01 (Erika Mustermann).
**Details → Als Vorlage verwenden** opens a new copy of any original, imported or
own profile. Saving creates a separate own profile and activates it.

The editor supports document types ID (Personalausweis), UB (eID-Karte für
Unionsbürger) and AR/AS/AF (Aufenthaltstitel). Editable fields include document
number, issuing country, personal data, dates, address, residence community ID
and test PIN/CAN/PUK. The community ID must be entered explicitly; it is not
calculated from the address. Missing or unsupported template values must be
corrected before saving. Drafts survive activity recreation.

**PersoSim importieren** accepts UTF-8 PersoSim XML files (`.perso` or `.xml`, up to
2 MB) through Android's document picker. The import checks the supported XML
structure and initializes a candidate simulator before saving. Imported card data
and keys are retained. Using an imported profile as an editor template copies its
editable fields onto the standard profile 01 structure.

Own profiles can be edited. Own and imported profiles can be deleted after
confirmation. Deleting the active profile first loads profile 01. Profile changes
are blocked during NFC contact. Profiles created or imported on a device are
stored in app-private data.

## Build from source

This repository builds the Android app and the PersoSim simulator from Java
source. No original APK, extracted DEX files, apktool or compilation stubs are
used. Gradle compiles the app, resources and the vendored simulator sources;
external libraries are resolved from Google Maven and Maven Central.

The simulator sources are included because this port needs compatibility fixes
in profile loading and internal state copying. Keeping them here makes these
changes reviewable and includes them in every APK build.

Install JDK 17, [uv](https://docs.astral.sh/uv/), Android SDK Platform 36,
Build Tools 35.0.0 and Platform Tools. Set `ANDROID_HOME` to your Android SDK
or create an untracked `local.properties` with `sdk.dir=/path/to/android-sdk`.
The checked-in Gradle wrapper downloads the pinned Gradle distribution.

```sh
./gradlew :simulator:test :app:lintRelease :app:assembleRelease
uv run --no-project python de.persosim.android.app/tools/check_editor.py
uv run --no-project python de.persosim.android.app/tools/check_import.py
uv run --no-project python de.persosim.android.app/tools/check_apk.py \
  de.persosim.android.app/build/outputs/apk/release/app-release.apk
```

APK output: `de.persosim.android.app/build/outputs/apk/release/app-release.apk`.
It is a release build (debugging disabled), signed with the local Android debug key. Install it with:

```sh
adb -s DEVICE_SERIAL install -r de.persosim.android.app/build/outputs/apk/release/app-release.apk
```

Updates require the same signing key as the installed app. Keep the debug key
outside Git. Uninstalling removes locally stored profiles.

### GitHub Actions

[Build Android APK](.github/workflows/android.yml) runs on pushes, pull requests
and manual dispatch. It builds from source, runs simulator tests, Android lint,
editor/import checks and verifies the packaged originals and metadata. Download
`PersoSim-0.18` under the run's **Artifacts**. CI generates a temporary debug
signing key; APKs from different runs are not guaranteed to update each other.
No signing secret or original APK is required.

### Project layout

```text
de.persosim.android.app/
  src/                    Android UI, profiles, NFC, simulator and WLAN reader
  res/                    Android resources
  profiles/               17 unchanged original profiles
  original-profiles.json  Original profile provenance and expected hashes
  tools/profile_assets.py Validates profiles and generates build assets
  tools/check_*.py         Editor, import and APK checks
simulator/                Java library build and integration tests
third_party/              Vendored PersoSim/GlobalTester Java sources and licenses
.github/workflows/        APK build workflow
```

[third_party/README.md](third_party/README.md) records source versions and local
porting changes. Desktop Eclipse integrations are excluded from the Android
build. The launcher artwork is retained from the original Android 0.17.2 release.
The former Eclipse app is available in Git history.

### Verification and private profiles

The checks load all original profiles in the actual simulator and exercise eID
selection, EF.CardAccess and PACE setup. Editor/import checks cover validation,
MRZ/check digits, data groups, credentials, auxiliary verification values and
malformed XML. Android device tests additionally exercise the compiled runtime.
Run the device tests against a connected physical device:

```sh
ANDROID_SERIAL=DEVICE_SERIAL ./gradlew :app:connectedReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=de.persosim.android.app.RuntimeTest
```

Their profile and identity files use temporary storage, separate from saved
user profiles. JVM tests also check TLS pairing, rejection of unknown peers,
reader shutdown and WebSocket message handling.
An iPhone/AusweisApp scan must also be checked after NFC or simulator changes;
loading a profile alone does not verify PACE and secure messaging.

Keep individual profile files, device exports and backups outside the repository.
`.gitignore` excludes APKs, keystores, build output and profiles except the 17
explicitly named originals. Asset generation and APK verification reject missing,
modified or extra bundled profiles.

## WLAN reader

**Einstellungen** controls NFC emulation and the WLAN reader. Use **Neues Gerät
koppeln** and enter the displayed code in the reader's AusweisApp. Both devices
must be on the same WLAN. The reader uses TLS and remembers paired certificates
in app-private storage. **WLAN-Kopplungen löschen** removes those pairings.
Keep PersoSim visible during use, including PIN prompts. Existing WLAN identity, paired readers, device name, card profiles and the
last profile selection are retained when updating. The settings allow changing
the reader name and removing individual pairings.
The settings group these options under **Remote IFD** and **HostCardEmulation**.
**PIN-Pad-Modus** selects PIN input on the Android device. Turn off the Remote IFD
service before changing the PIN-pad mode or device name. **Kopplung abbrechen**
ends the current pairing attempt and returns the WLAN reader to normal operation;
Back or dismissing the pairing dialog does the same.

The app retains Android API 26 and disables debugging in the distributed APK,
matching the previously working APK. The iPhone scan was verified with these
settings after the source migration.
This is a sideload build, not a Play Store release. Updating the target SDK
and background-service behavior is a separate migration.

## Simulator limitations

Generated profiles update eID data groups, passport DG1, MRZ/check digits,
PIN/CAN/PUK and auxiliary verification values. Other profile 01 fields and security
material remain unchanged. Changing the document type does not change the
underlying applications, authorization rules or certificates. Signed document
hashes are not regenerated. The editor is intended for simulator data tests,
not full document-conformance or passive-authentication tests.

NFC behavior depends on the reader and Android device. The app registers the eID
AID `E80704007F00070302`; its NFC status check verifies the local configuration,
not the success of an exchange with a particular reader.

## Upstream and license

Based on [PersoSim Android](https://github.com/PersoSim/de.persosim.android.app)
and the [PersoSim simulator](https://github.com/PersoSim/de.persosim.simulator).
See the [PersoSim website](https://persosim.github.io/) and [LICENSE](LICENSE).
