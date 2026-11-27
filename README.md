<h1 align="center">Velella</h1>
<h3 align="center">A de-Googled Jellyfin client for the NVIDIA Shield TV</h3>

---

<p align="center">
<img alt="Velella banner" width="480" src=".github/assets/velella-banner.svg"/>
<br/><br/>
<a href="LICENSE">
<img alt="GPL 2.0 License" src="https://img.shields.io/github/license/miracle091/velella.svg"/>
</a>
<a href="https://github.com/miracle091/velella/releases">
<img alt="Current Release" src="https://img.shields.io/github/release/miracle091/velella.svg"/>
</a>
</p>

Velella is a client for [Jellyfin](https://jellyfin.org) media servers, made for Android TV. It is a fork of the official
[Jellyfin for Android TV](https://github.com/jellyfin/jellyfin-androidtv) app, trimmed and tuned for the
**NVIDIA Shield TV**: every model from 2015 to the 2019 Pro. It needs no Google services, so it runs both on the stock Shield software and on
GMS-free AOSP or LineageOS builds.

The name comes from the velella, the by-the-wind sailor: a small blue relative of the jellyfish that drifts on the sea
with a stiff little sail.

Velella was called Ephyra until version 0.5.0. It is not [midasvo/ephyra](https://github.com/midasvo/ephyra), a
different Jellyfin client for phones, tablets and Android TV.

## What is different

- **No Google services:** no Play Services, Firebase, Play Core or Cast SDK. A build check fails if one ever slips in
  through a dependency.
- **Built for the Shield:** Android 11 or later, ARM only (64-bit, and 32-bit for the 2019 non-Pro), so the APK
  carries only the code a Shield runs.
- **Tuned for its hardware (in progress):** hardware decoders first, sensible memory limits, refresh-rate switching on
  by default.
- **Its own identity:** the app is called Velella and installs next to the official Jellyfin app instead of replacing
  it.

The changes of every release are in the [changelog](CHANGELOG.md); the [wiki](https://github.com/miracle091/velella/wiki)
describes the features, installation, building and contributing.

## Status

Velella is in early development. The plan goes phase by phase: dependencies and Google removal (done), build
retargeting (done), then playback, memory, interface, updates and releases. The first releases are out; expect rough
edges.

## Installing

Velella is installed by sideloading only: it is not on Google Play, F-Droid or any other store.

1. Download the APK from [Releases](https://github.com/miracle091/velella/releases), or add this repository to
   [Obtainium](https://github.com/ImranR98/Obtainium) to get updates.
2. Install it on the Shield with a file manager or over ADB: `adb install velella-v<version>.apk`. After that, the app checks for new releases by itself (Settings → About).

You need a Jellyfin server. If yours is too old, the app tells you which version it needs.

## Building

The app uses Gradle and needs JDK 21 and the Android SDK. Android Studio includes both. From the command line:

```shell
./gradlew assembleDebug
```

The APK ends up in `app/build/outputs/apk/debug/`. Debug builds use their own app id (`com.velella.app.debug`), so they
install next to a release build. The full check that CI runs is:

```shell
./gradlew assembleDebug detekt lint test
```

## Translations

The interface keeps every language of the official app; those translations come from Jellyfin's
[Weblate](https://translate.jellyfin.org/projects/jellyfin-android/jellyfin-androidtv), so improve them there. The few
texts that exist only in Velella are in English and Italian (`strings_velella.xml`); other languages show them in English.

## Upstream and support

Velella is a fork of [jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv), the official Jellyfin
for Android TV app, and keeps its history. Changes from that project are picked one by one when they help the Shield,
not merged wholesale. Report problems with Velella in [this repository's issues](https://github.com/miracle091/velella/issues),
not to the Jellyfin project, which does not support this fork.

## License and trademarks

Velella comes from [Jellyfin for Android TV](https://github.com/jellyfin/jellyfin-androidtv), which is distributed under
the [GPL-2.0](LICENSE). Velella's own changes are licensed under the GPL-2.0 or, at your option, any later version; see
[FORK.md](FORK.md#license) for details. It is not affiliated with or endorsed by
the Jellyfin project or NVIDIA. The Jellyfin name and logo belong to the Jellyfin project; "SHIELD" is a trademark of NVIDIA
Corporation, used here only to name the device Velella targets.
