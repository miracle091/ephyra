# About Ephyra

Ephyra is a Jellyfin client for Android TV, forked from the official Jellyfin Android TV client. It is trimmed and
tuned for the **NVIDIA Shield TV** (2015, 2017 and 2019, Pro and non-Pro; Tegra X1 and X1+), and it runs without
Google Mobile Services, so it also works on GMS-free AOSP and LineageOS builds.

This file states where the fork comes from and how it is licensed. It is the change notice required by the GPL-2.0,
section 2(a), together with [`CHANGELOG.md`](CHANGELOG.md), which lists every change and the date of the release that
brought it. The user and developer documentation is in the [wiki](https://github.com/miracle091/ephyra/wiki).

## Origin

```text
jellyfin/jellyfin-androidtv  (upstream, GPL-2.0)
        │
        └── forked at 7ec47a0 (2026-10-29)
                │
                └── Ephyra  (this repository, branch ephyra/main)
```

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

## What the fork changes

- Every commit after the fork point is an Ephyra change, prefixed `ephyra:`; `git log upstream-fork-point..ephyra/main`
  lists them with their dates, and `git diff upstream-fork-point...ephyra/main` shows the changed files.
- [`CHANGELOG.md`](CHANGELOG.md) summarises the changes per release.
- Upstream is not merged: an upstream fix that helps the fork is adapted by hand, and the changelog names its upstream
  issue or pull request.

For users, the main differences are:

- **Name and logo:** the app is called Ephyra and has its own logo, so it is not mistaken for the official app,
  following Jellyfin's [branding guidelines](https://jellyfin.org/docs/general/contributing/branding).
- **Package name:** `com.ephyra.app` (debug builds: `com.ephyra.app.debug`). Ephyra installs next to the official
  app and does not replace it; settings and logins do not carry over. The code namespace stays `org.jellyfin.androidtv`.
- **Signing:** releases are signed with the fork's own key, so Android will not install them over the official app, or
  the other way round.
- **Distribution:** sideload only, from GitHub Releases (Obtainium works too).

## Trademarks

Ephyra is not affiliated with or endorsed by the Jellyfin project or NVIDIA. "SHIELD" is a trademark of NVIDIA
Corporation; it is used here only to name the device the fork targets.
