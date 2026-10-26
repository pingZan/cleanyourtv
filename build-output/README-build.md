# Build & install (TCL TV)

## What was built

`build-output/tvhome-3.1-custom-release.apk`

- package `com.gathin.tvhome`, versionName 3.1, versionCode 10, minSdk 26, targetSdk 34
- release build, R8 minified + resource shrunk, **signed with the Android debug key**
  (SHA-256 `048d26e2d2ae3ea656e10d797dcdfddaa97fce48042b7d9ebf06346163013417`)
- SHA-256 of the APK file: `36B5C63242A312F5E74E36CD898241AF8F1CD82603BF79921C7A64CEBAAC3396`
- 3.0 (`tvhome-3.0-custom-release.apk`, versionCode 9, SHA-256
  `93A7EEFDB9A85DEA42BB0FCAA94CC4A5C6FA39BD1D537E6FC6087448ED6F7548`) is the
  previous build; both share the same signing key, so `install -r` upgrades in place.

## Local build (what was used here)

Requires JDK 17+ and an Android SDK with platform 34 + build-tools 34.0.0.

```powershell
$env:JAVA_HOME      = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:ANDROID_HOME   = "C:\Users\0_0\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
cd C:\Users\0_0\Documents\project1\cleanyourtv-main\launcher
.\gradlew.bat :app:assembleRelease --no-daemon --stacktrace
```

`launcher/local.properties` holds `sdk.dir=...` (not committed; CI does not need it).

The launcher is its own Gradle project: root `launcher/`, module `:app`.

## GitHub Actions

`.github/workflows/build-apk.yml` builds on push/manual dispatch, JDK 17 (temurin),
`gradle/actions/setup-gradle@v4`, uploads `tvhome-release-apk`. It runs with
`working-directory: launcher` and `./gradlew :app:assembleRelease`.

## Install on the TV

```powershell
$adb = "C:\platform-tools\adb.exe"
& $adb connect 192.168.1.111:5555
& $adb devices
```

Try an in-place update first:

```powershell
& $adb install -r "C:\Users\0_0\Documents\project1\cleanyourtv-main\build-output\tvhome-3.1-custom-release.apk"
```

If that fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (the installed copy was signed
with a different key), uninstall and install clean — this **erases the launcher's saved
settings, tile order and per-app customizations**:

```powershell
& $adb uninstall com.gathin.tvhome
& $adb install "C:\Users\0_0\Documents\project1\cleanyourtv-main\build-output\tvhome-3.1-custom-release.apk"
& $adb shell cmd package set-home-activity com.gathin.tvhome/.MainActivity
```

## Rollback

Keep the previous APK before replacing. If the old one is not on hand, the current launcher
can be re-flashed from its own release page after `adb uninstall com.gathin.tvhome`.
The stock/alternate launcher `com.spocky.projengmenu` is disabled on the TV and can be
restored with:

```powershell
& $adb shell pm enable com.spocky.projengmenu
& $adb shell cmd package set-home-activity com.spocky.projengmenu/.MainActivity
```

## Features in this build

1. Long-press a tile (dock or drawer) → **Sesuaikan**: rename the tile and pick an icon
   from a file manager (PNG/JPG/WebP; 320x180 banner or 512x512 square recommended).
   Reset returns the app's own label and icon.
2. **Launcher settings** → separate `Jumlah kolom Dock` and `Jumlah kolom Drawer` (3–6).
   Tiles and icons scale with the column count.
3. Long-press a tile → **Geser** to pick the tile up and move it with the arrows.
4. **App library is on** (`app_library`), which is what puts **Hapus dari Beranda** in the
   tile menu: remove an app from Home and it stays reachable in **Pustaka aplikasi** (the
   pill after the last Home row), where ordinary Android apps can also be added to Home.
5. The dock band is **light glass** (`dock_glass`): a white tint that fades downwards with a
   bright rim, instead of the solid grey band. `Opasitas pita dock` is hidden while it is on,
   because the glass ignores that setting.

The launcher UI is forced to Indonesian (`locale = in` in `res/values/config.xml`).
