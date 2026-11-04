# About Ephyra

Ephyra is a Jellyfin client for Android TV, forked from the official Jellyfin Android TV client. The name comes from
the ephyra, the small, light, young stage of a jellyfish. It is trimmed and tuned for the
**NVIDIA Shield TV** (2015, 2017 and 2019, Pro and non-Pro; Tegra X1 and X1+), and it runs without Google Mobile Services, so it also works on GMS-free
AOSP and LineageOS builds.

This file explains where the fork comes from and what it changes. It is also the change notice required by the
GPL-2.0, section 2(a).

## Origin

```text
jellyfin/jellyfin-androidtv  (upstream, GPL-2.0)
        │
        └── forked at 7ec47a0 (2026-10-29)
                │
                └── Ephyra  (this repository, branch ephyra/main)
```

The fork comes straight from upstream, with no fork in between.

## Upstream

| | |
|---|---|
| Project | Jellyfin for Android TV |
| Repository | <https://github.com/jellyfin/jellyfin-androidtv> |
| License | GPL-2.0 (see [`LICENSE`](LICENSE)) |
| Fork point | `7ec47a0014b8ac8bac56d66ed75cbe442706f0ce` (2026-10-29, "Translated using Weblate (Lithuanian)") |
| Fork-point tag | `upstream-fork-point` |

Upstream's full history and authorship are kept. Copyright stays with the Jellyfin contributors; see the
[contributors page](https://github.com/jellyfin/jellyfin-androidtv/graphs/contributors).

## License

Ephyra's own changes (the commits prefixed `ephyra:` and the files they add) are licensed under the GNU General Public
License version 2 or, at your option, any later version (GPL-2.0-or-later). The text of version 2 is in [`LICENSE`](LICENSE).

Upstream's code keeps its own terms. Its repository ships the GPL-2.0 text, and its source files name no version;
section 9 of the GPL-2.0 says that a program which names no version may be used under any version published by the
Free Software Foundation, and F-Droid lists upstream as GPL-2.0-or-later. Third-party libraries keep their own
licenses, listed in the app under Settings → About → Licenses.

## Keeping up with upstream

- Upstream is not merged wholesale: single upstream changes are cherry-picked into `ephyra/main` when they help the
  fork. The fork does not rebase.
- Fork-only commits are prefixed with `ephyra:`, so they stand apart from upstream history.
- The fork keeps the `org.jellyfin.androidtv` namespace and leaves upstream files where they are. Fork-specific
  code goes into new files, so each upstream merge touches as few lines as possible.
- English is the fork's only language in the code and the documentation. The app keeps all of upstream's
  languages. Upstream's string files stay untouched, so merges stay cheap. Ephyra's own strings live in
  `strings_ephyra.xml`, in English and Italian; other languages show them in English.

## Summary of changes

The status column shows how far each change has gone. A change becomes "done" when its commit is on
`ephyra/main`.

| Area | Change | Status |
|---|---|---|
| Dependencies | A build check fails if Google Play Services, Firebase, Play Core or the Cast SDK appear in the dependency graph | done |
| Dependencies | Accompanist permissions are replaced with AndroidX Activity Result APIs | done |
| Identity | Name Ephyra, application id `com.ephyra.app` (namespace unchanged); backup rules cover the new id | done |
| Build | minSdk 30 (Android 11), ARM only (arm64-v8a, plus armeabi-v7a for the 32-bit Shield TV 2019 tube), R8 strips debug logging | done |
| Identity | Own vector logo: launcher icon (with monochrome layer), TV banner and in-app logo; upstream's PNG icons removed | done |
| Identity | The app names itself Ephyra: welcome and help texts, search hint, settings, version line, About icon, the client name the server sees and the header of crash and playback reports. Texts about the Jellyfin server keep its name | done |
| Identity | Per-library view settings are saved on the server under the client id `ephyra`, apart from the official app's (settings shared with the web client stay shared) | done |
| Docs | README describes Ephyra (install, build, translations, support), with its own banner | done |
| Build | Baseline profile (`:baselineprofile` module, profileinstaller) so ART compiles startup code ahead of time, sideloaded APKs included; generated on a TV emulator, so it covers startup and the screens before sign-in | done |
| Playback | Hardware video decoders come first; for H.264, HEVC, VP9 and Dolby Vision the software decoders are skipped when a hardware one exists, so a failing decoder ends in an error instead of a stuttering software decode. The playback buffer is capped at a quarter of the app's heap (48-192 MB) | done |
| Playback | Software decoders are off by default, so AV1 (no hardware decoder on any Shield TV) is left to server transcoding. With them on, software AV1 is capped at 1080p. For H.264, HEVC, VP9 and Dolby Vision the limits sent to the server come from the hardware decoders whatever the setting, as the player uses | done |
| Playback | Refresh-rate switching on by default ("Scale on device": the display keeps its resolution, so the Shield can still upscale); while the app switches the display mode, Media3's own frame-rate requests are off | done |
| Memory | Backdrops (home, details and screensaver) are requested from the server at screen width and decoded at screen size instead of their original size; the image memory cache is sized at 15% of the app's memory class instead of Coil's 20% | done |
| UI | Home-screen channels and Watch Next off by default (the existing setting in Customization, upstream default on), so the background worker makes no server calls for them | done |
| UI | Home rows share their card views, so the first scroll inflates fewer cards; only if the Shield shows stutter there (upstream already turns off card shadows and row dimming, and its home defaults are shared with the web client) | planned |
| Playback | The player's top panel shows how the server delivers the stream (direct play, direct stream or transcoding) and, when it converts, the server's reasons | done |
| Updates | Built-in updater (Settings → About → Check for updates, release builds only): reads the latest GitHub release's `version.txt` and installs `ephyra-v<version>.apk` through PackageInstaller after the system's confirmation; no Google Play | done |
| Updates | Daily update check (on by default, can be turned off in Settings → About): when the app comes to the foreground and the last check is a day old, it checks GitHub and shows a notice on the home screen; installing stays a choice in Settings | done |
| UI | On a Shield TV with an HDR display, a one-time notice on the home screen says where to turn on the Shield's own content matching | done |
| Docs | The README links the upstream project wherever it names it; Ephyra's own changes are declared GPL-2.0-or-later (License section above) | done |
| Playback | The audio and subtitle menus of the player list the tracks in the user's preferred languages first (the server's language settings for the user, then the TV's language); the other tracks follow in their usual order | done |
| Playback | The details screen of a film or episode has "Audio" and "Subtitles" buttons: the picked tracks are sent with the first playback request, so the server builds the right stream at once instead of restarting a transcode mid-film (built-in player only) | done |
| UI | The MENU key on a card also offers "Reset progress" (half-watched items: clears the resume position only) and "Go to series" (episodes) | done |
| Playback | The skip button for intros and other segments dims after 3 seconds until it hides; during HDR and Dolby Vision playback (direct play or remux) the player's controls are dimmed, so they don't glare on an HDR display | done |
| Licenses | The licenses screen also lists the native libraries bundled inside other packages: FFmpeg (in the FFmpeg decoder), and libass with FreeType, FriBidi, HarfBuzz, Fontconfig, libunibreak and Expat (in the subtitle renderer) | done |
| Debug | Codec benchmark in debug builds (Developer options): plays Jellyfin's test clips from repo.jellyfin.org, reports the decoder and dropped frames per clip and what the server would do with the same clips in a library, and suggests settings from the numbers. Release APKs leave it out | done |
| CI | Release workflow (`ephyra-release.yaml`, on `v*` tags) that checks, signs and publishes the APK, `version.txt`, the R8 mapping and SHA-256 checksums to GitHub Releases; the signing secrets live in a protected `release` environment. Upstream's publish job (repo.jellyfin.org) removed; build, lint and test also run on `ephyra/main` | done |
| Code | Checks made obsolete by minSdk 30 removed: the Android 6 volume fallback in `playback/core` and the `-v26` folder of the launcher icon | done |
| Code | Android lint errors fixed: Media3 opt-ins declared per file, no restricted Leanback class in the player controls, configuration-aware string lookup in voice search, correct lint id on the media notification | done |
| Memory | The live TV guides (screen and in-player) and the item details build their content in a task tied to the screen's view (`LifecycleTask`) instead of an AsyncTask, so the work stops when the screen closes and no longer holds it in memory | done |
| UI | No "Delete" button on the item details screen (films, episodes, recordings and other media), so the app never deletes media from the server; cancelling scheduled recordings stays | done |
| CI | The lint workflow runs only detekt; Android lint runs in local checks and in the release workflow, and no longer uploads to code scanning | done |
| Playback | The request that opens a stream (PlaybackInfo) waits up to 2 minutes instead of 30 seconds, so a live TV channel the server is still tuning opens on the first try; every other request keeps the 30-second timeout | done |
| UI | "Black (OLED)" theme (Settings → Customization → App theme): true black backgrounds, popup menus and tiles, so an OLED TV keeps its pixels off around the content | done |

## What changes for users

- **Name and logo:** the app is called Ephyra and gets its own logo, so it is not mistaken for the official app. This
  follows Jellyfin's [branding guidelines](https://jellyfin.org/docs/general/contributing/branding). The
  description will say "a Jellyfin client".
- **Package name:** `com.ephyra.app` (debug builds: `com.ephyra.app.debug`). Ephyra installs next to the official
  app and does not replace it. Settings and logins do not carry over between the two apps. The code namespace stays
  `org.jellyfin.androidtv`, as upstream has it.
- **Signing:** releases are signed with the fork's own key. Android will not install them over the official app, or
  the other way round.
- **Distribution:** sideload only. APKs are published on GitHub Releases, which also works with Obtainium. Ephyra is
  not on Google Play, F-Droid or any other store.
- **Devices:** built for every NVIDIA Shield TV with Android 11 (Shield Experience 9): 2015, 2017, 2019 and 2019 Pro.
  Dolby Vision and AI upscaling are features of the 2019 models only. It may run on other ARM Android TV devices with
  Android 11 or later, but those devices are not tested.

## Viewing the changes

```sh
git remote add upstream https://github.com/jellyfin/jellyfin-androidtv.git
git fetch upstream

# Everything the fork changes, compared with the latest upstream it has merged
git diff upstream/master...ephyra/main

# File statistics only
git diff --stat upstream/master...ephyra/main

# Fork-only commits
git log --oneline --grep='^ephyra:' ephyra/main
```

Three dots compare against the last upstream commit merged into the fork, so upstream's own newer changes are left
out. `upstream-fork-point..ephyra/main` instead shows everything since the original fork point, upstream merges
included.

## Trademarks

Ephyra is not affiliated with or endorsed by the Jellyfin project or NVIDIA. "SHIELD" is a trademark of NVIDIA
Corporation; it is used here only to name the device the fork targets.
