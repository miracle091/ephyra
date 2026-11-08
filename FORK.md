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
| UI | Spotlight at the top of the home screen (Customization → Home, on by default): the five films and series added most recently and not watched yet, in turn every 10 seconds, with a backdrop that fades out at the top, left and bottom, the title's logo in its bottom right corner when the server has one, genre, year, running time in the theme's colours and Play, Details and Favorite buttons (Play on a series starts its next up episode, resuming where it was left); it hides while a lower row is selected and stops rotating while it has the focus | done |
| UI | "Black (OLED)" theme (Settings → Customization → App theme): true black backgrounds, popup menus, tiles and settings panel, so an OLED TV keeps its pixels off around the content | done |
| UI | "Blur unwatched episodes" (Customization, off by default): cards of episodes not started yet show the blurred BlurHash of their picture instead of the scene, without downloading it; series and season posters stay as they are | done |
| Playback | "Stop at the end of the season" (Playback → Next up, off by default): the episodes queued after the one started stay in its season, so autoplay stops when the season ends | done |
| UI | Ephyra design system, first steps: in the Dark and Black themes the Compose parts (settings panel, toolbar, search, dialogs, player controls) and the Leanback screens (backgrounds, buttons, inputs, menus, progress bars) use Ephyra's colours (night blue, one cyan accent, a solid white focus), pill-shaped buttons and rounder cards | in progress |
| UI | Covers follow the Ephyra design system: the focused cover grows by 6% (upstream 15%) and gets a thin ring in the theme's accent colour, a cover without a picture is filled with one of six colour tones, picked from its title, instead of a grey icon, and a cover still loading shows the theme's darkest surface | done |
| UI | The Emerald and Muted purple themes are removed, as they don't follow the Ephyra design system. A device set to a removed theme opens in Dark | done |
| UI | Three coloured themes on the Ephyra design system (Settings → Customization → App theme): Abyss (deep teal, mint accent), Coral (warm dark red, coral accent) and Medusa (deep violet, lilac accent) | done |
| UI | Home layout: 32dp side margins (upstream 48 to 56dp) in the toolbar, the spotlight and the browse rows; the home rows size their cards so that four wide cards or eight posters fill a row edge to edge; the home rows and the library screens no longer show the selected item's backdrop behind them | done |
| UI | Library screens: the same 32dp side margins as the home; the library grid keeps an 8dp gap between cards and sits right below the title, and drops the item counter and the "Showing ... sorted by ..." line at the bottom; the startup and sign-in screens show the plain theme background instead of the server's splashscreen | done |
| UI | Home icons: Home and Search in the toolbar and Play, Details and Favorite in the spotlight are icons without labels; the favorite mark of a cover is a round badge like the watched one, next to it in the top right corner, instead of a red heart in the top left; the spotlight's favorite button uses the theme's accent instead of red | done |
| UI | User selection follows the design system's "Users" board: centred title with the server's name and address under it, 128dp avatars on surface-3 with the covers' accent ring and zoom, centred on both axes, ghost buttons for Add account and Change server, no server login disclaimer, focus on the first user; the startup toolbar and avatars use the user's theme colours (new `EphyraTheme` overload); the home toolbar's user button shows focus with the accent ring and zoom instead of a solid fill | done |
| UI | Sign in follows the same layout: centred title and server, the login method in a 640dp column (password fields 480dp), ghost buttons with new lock, QR code and close icons (`ic_ephyra_*`), focus on "Use a password" under Quick Connect; the Quick Connect code shows each character in its own tile, in groups of three (`QuickConnectCode`); ghost buttons tint their icons through the platform attribute too | done |
| UI | Server selection follows the same layout: one centred 640dp column with saved and discovered servers as bold sections, servers as 64dp rows with radius-md in the theme's colours (readable focus), Enter server address as a ghost button with an add icon; the startup toolbar's logo is shifted 15dp left to line up with the home's user button | done |
| UI | Music libraries open as a grid of albums like movies and shows, with their grid settings and the optional rows screen; the music player follows the design system's board: a 300dp cover with radius-lg, "Now playing · Album" in the accent, a bold 40sp title, "Artist · Year", the seekbar and 48dp icon buttons around a 64dp play button spread across its width, filling the screen with the queue below, over a moving backdrop made from the cover's blurhash; the home's now playing card is as tall as the library tiles, so it fits under the spotlight; the toolbar's mini player draws its progress ring around the cover instead of half cut over its edge; the library grid lets the focused card's zoom and ring draw past its top edge | done |
| UI | Album and playlist screens follow the design system's detail layout: the cover beside the genre overline, a bold title and the info row, 40dp icon-only buttons (ink, accent when active, solid ink focus fill, the label kept for TalkBack), track rows with number, title, artist and duration and a radius-md focus fill | done |
| UI | Film, series and person details share the home's margins and fonts: genre overline, bold title, the info row in ink-muted with neutral badges as the design system's badge, the overview as a focusable three-line text with a −12dp focus fill that opens in a surface-2 dialog with radius-lg, director, runtime and end time as labelled columns, icon-only buttons; a person's page opens on its favourite button and keeps a three-line biography | done |
| UI | The video player's controls follow the design system's "Player" board (legacy playback path, look only): overline (series and episode, or genres), bold title and play method over a top shade, the clock on the right; the seekbar, times and end time, then one row of 48dp round buttons (primary group left in transport order, secondary group right) near the bottom edge over a bottom shade; no dimming over the whole picture. Done by overriding Leanback's `lb_playback_transport_controls_row`, `lb_control_button_primary/secondary` layouts and dimens (`dimens_ephyra_osd.xml`) | done |
| UI | Cards that carry their name over the image (playlists, folders, artists, people) use the design system's card overlay: a bottom shade under the name in bold ink instead of a grey box | done |
| UI | Library cards (Movies, Shows, Music...) show the backdrop of the title added most recently to the library, with the library's name in the card overlay, instead of the server's image with the name printed on it; a library without backdrops shows its colour tone. The backdrop is looked up per user, as users of a server may see different titles | done |
| UI | Home rows: the bottom 48dp of the home fade into the theme's background, so a row cut by the screen edge fades out instead of ending in half a line of text; the selected row's cards start at a fixed line from the top instead of being centred, so the spotlight no longer pushes the first row's title out of sight | done |
| UI | The whole interface uses the Atkinson Hyperlegible Next font (and Atkinson Hyperlegible Mono for Quick Connect codes), designed for easy reading from a distance, bundled as variable fonts (SIL OFL 1.1, listed in the licenses screen) | done |
| UI | The screensaver is a minimal clock on the theme's background: a large clock and the date, moving slowly to a new place every minute against burn-in, instead of the collage of library backdrops; music playing still shows its own screen. The screensaver's age rating options and preferences are gone, as it shows no titles | done |
| UI | The startup window shows the Dark theme's background from the first frame instead of black, until the chosen theme takes over; the home spotlight's title, details and buttons sit centred on the backdrop's height instead of at its bottom | done |
| UI | Icons for an active state follow the theme's accent instead of red or the button grey: active shuffle, repeat and grid filters, the live TV guide's favourite heart, checked checkboxes and radio buttons | done |
| UI | A video left paused for 2 minutes shows a long pause screen instead of the screensaver: the title's backdrop dimmed, the player's header and progress (position, end time, duration) and a hint to return; after 10 minutes the picture dims further and the text shifts against burn-in. The screensaver opens on its content at once instead of fading from the clock, and its clock sits where the home toolbar's does | done |
| UI | The home spotlight shows the community rating with the gold star and the critics' rating with the Rotten Tomatoes logo before the year and runtime, as in the details' info row | done |
| UI | Settings follow the home spotlight's scale: each screen's header has an accent overline, a 28sp title and a 14sp caption; the panel sits on the theme's first surface, rows have medium corners and wider padding, and trailing values read as secondary text | done |
| UI | Home rows leave 12dp between cards instead of 8dp, so the focused card's 6% zoom and its ring stay off the next card; the card widths are recomputed so the rows still fill the screen | done |
| UI | The live TV guide matches the player and the home rows: rounded cells with the cards' margins and focus ring, times in the clock's figures, an accent bar on the programme on air, popups with the player's buttons; in the player a live channel's header reads "Live · number · channel" above the programme on air. Unused guide drawables and colours removed | done |
| UI | Home: the live TV section shows only the "On now" row, without the row of Guide, Recordings and Schedule buttons (they stay in the Live TV library); card titles and subtitles under the covers are larger and in the theme's ink colours; the bottom fade is 24dp and the spotlight 226dp high, so the first row's labels stay readable | done |

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
