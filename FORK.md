# About Ephyra

Ephyra is a Jellyfin client for Android TV, forked from the official Jellyfin Android TV client. The name comes from
the ephyra, the small, light, young stage of a jellyfish. It is trimmed and tuned for one device, the
**NVIDIA Shield TV Pro (2019, Tegra X1+)**, and it runs without Google Mobile Services, so it also works on GMS-free
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

## Keeping up with upstream

- Upstream is merged into `ephyra/main` with `git merge upstream/master`. The fork does not rebase.
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
| Build | minSdk 30 (Android 11), arm64-v8a only, R8 strips debug logging | planned |
| Build | Baseline profile for faster startup and smoother scrolling | planned |
| Playback | Hardware decoders come first for H.264, HEVC, VP9 and Dolby Vision; buffer size scales with the device's memory | planned |
| Playback | Software decoders are off by default; AV1 (no hardware decoder on Tegra X1+) is left to server transcoding | planned |
| Playback | Refresh-rate switching on by default | planned |
| Memory | Image caches have size limits; images are requested at display size | planned |
| UI | Home-screen channels and Watch Next off by default (the existing setting, upstream default on) | planned |
| UI | Lighter home defaults and a shared view pool for rows | planned |
| Updates | Built-in updater that installs releases from GitHub; no Google Play | planned |
| CI | Release workflow that publishes signed APKs with checksums to GitHub Releases; upstream's publish jobs removed | planned |

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
- **Devices:** built for the Shield TV Pro 2019. It may run on other arm64 Android TV devices with Android 11 or
  later, but those devices are not tested.

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
