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
| Memory | The image memory cache is sized at 15% of the app's memory class instead of Coil's 20% (the full-screen backdrops this row also sized are gone, see below) | done |
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
| UI | The item details screen offers "Delete" only for live TV recordings (items the server marks as recordings or that sit in its recording folders, when the server allows the user to delete them), after a confirmation; films, episodes and other library media can never be deleted from the app; cancelling scheduled recordings stays | done |
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
| UI | Live TV library: the Guide, Recordings, Schedule and Series buttons are 192x108 cards with an icon and their label on the bottom shade, like the other cards; the "Live TV" library tile shows the TV icon on the art tone | done |
| UI | Settings open as a two-pane panel: the sections on the left, the chosen section on the right; group titles inside a screen are small grey labels, screen titles keep the large heading | done |
| Playback | Live TV player: a red "LIVE" badge and "number · channel" above the programme, the seek bar spans the programme on air with its start and end times and moves on to the next one by itself, the record button turns red when recording, focus moves between the transport buttons; a failed live stream is retried twice (after 1 and 2 s) before giving up; the in-player guide drops its corner status texts | done |
| Playback | Live TV player: Guide, Record and Channels are pill buttons with a text label, a "Next: title · time" line sits under the programme on air, the quick channel changer's cards look like the guide's cells, and a failed live stream is retried without closing the server's live stream and transcode first (the old one is closed once the retry has its own) | done |
| UI | Live TV guide: the programme on air gets a red dot before its name and a progress bar over its whole length that follows the clock every 30 s; no "<<" before programmes that began earlier (their start time shows it) and no channel and filter counts in the corners | done |
| UI | Programme details: the record buttons use red REC icons when set and the plain ones when not, the director column stays empty instead of "Unknown", and the info row shows "Live" as a red badge | done |
| UI | Settings: focus entering the right pane lands on its first item and Back returns to the open section; the single-screen settings panels (guide options and filters, library display, photo player, settings before login) take the right pane's width, surface and spacing | done |
| Fix | Live TV: the guide turns its background back on after the player (the player disables it); "in N hours" in the programme and record popups counts from the device zone instead of UTC; library cards without art and the Recordings, Schedule and Series buttons show icons; programme details skip empty info columns | done |
| UI | Live TV player: the programme title moves above the seek bar with its genres and episode as the overline, the "Next:" line and play method sit under the times, the top keeps only the LIVE badge, channel and clock, over a denser bottom shade | done |
| Fix | Live TV player: a channel still buffering 20 s after its stream is set counts as failed and goes through the live retries, then closes with an error, instead of waiting forever; a retry also stops a stream stuck in buffering | done |
| UI | Home: the server's Recordings library card shows the REC icon instead of a folder (the server gives it no collection type, so the app asks for the recordings folders once per user) | done |
| UI | No backdrops of films, shows or music behind the screens (browsing, search, details, music player, next up, still watching): every screen sits on the theme's plain background; the "Backdrop" setting is removed. The home spotlight, the library cards and the long pause screen keep their pictures | done |
| UI | Home: the Playlists library is left out of the "My media" row unless "Playlists in My media" (Settings → Home, off by default) is on | done |
| UI | Home: the Live TV library card shows a channel logo, whole and centred on the card's colour tone (one of the first 50 channels with a logo, a new one at each start of the app), the TV icon when none has one | done |
| UI | Home: photo library cards show one of the library's photos, picked at random by the server at each start of the app, instead of the colour tone | done |
| UI | Search: the text field is filled like the sign-in fields (solid when focused, so the focus and the text are visible in every theme), up and down move the focus out of it (the D-pad seemed dead after closing the settings over search), and the row follows the 32dp side margin | done |
| UI | Settings: the Subtitles, Home and Screensaver sections carry the "Settings" overline like the other sections | done |
| UI | Video player: the Skip button is a solid pill in the theme's colours like the live player's buttons (dimmed to 80% after 3 s instead of 40%), at the controls' safe margins; the bottom controls and their shade reach the screen's bottom edge (Leanback left a 1dp strip of video uncovered) | done |
| UI | Long pause screen: the end time sits under "Paused" in the header with a clock icon, and "Press any key to return" is centred on the screen on the times' line (position and duration) | done |
| Fix | Programme details opened for a channel load the programme on air when the one shown has ended, e.g. after watching the channel past its end | done |
| Licences | About → Licenses lists Jellyfin's Media3 FFmpeg decoder as Apache-2.0 and LGPL-2.1-or-later (the code it ships) instead of the GPL-3.0 of its POM | done |
| UI | Lighter home by default: libraries, Continue watching, Live TV, Next up and Latest; "Continue listening" and "Continue reading" are no longer default rows (sections set by the user, here or in the web client, stay). Books are dropped: the "Continue reading" section is removed from the choices, book libraries and audiobooks stay hidden | done |
| Playback | Skip button like a streaming service's "Skip intro": it stays 15 s instead of 8, and when it has hidden by itself centre, up or down call it back while the segment lasts (left and right keep seeking) | done |
| Dependencies | Jellyfin Kotlin SDK 1.9.4 (server API 12): calls follow the renamed API groups; the app now needs Jellyfin server 12.0 or newer. Changing the video stream no longer asks the server to stop the old encoding (server 12 has no such endpoint; it replaces the session's transcode itself). The transcode reason "rotated video not supported" is shown in playback info | done |
| Fix | Playback ignores a late "playing" event from the player after stop (null stream info crashed, upstream #5938) | done |
| Fix | "Scale on device" refresh-rate switching keeps the native resolution even when a lower resolution mode has a closer refresh rate (upstream #5893) | done |
| Fix | Switching between two audio tracks of the same language works when burned-in subtitles force a transcode, and a subtitle change keeps the audio track (upstream #5798) | done |
| Playback | The next episode picks the same audio and subtitle track as the last one (language, then forced flag, title and codec), not only the same language | done |
| Fix | An alternate version of an episode chosen in details is the one played, and the episode queue follows it (upstream #5931) | done |
| Playback | Rotated videos in containers whose rotation Media3 cannot read (MKV, TS, WebM...) are transcoded upright by the server; MP4/MOV still direct play | done |
| Audio | DTS core (`dca`) follows the DTS setting; in Auto, formats with a hardware decoder count as supported besides passthrough ones; audio offload is off | done |
| Audio | "Prefer FFmpeg for audio" still sends the bitstream to the receiver for every format the output can pass through (TrueHD, DTS-HD, Dolby); FFmpeg decodes only the rest. When the server must convert audio it picks AC3/E-AC3 before AAC if the bitstream settings allow them. MLP is no longer direct played (nothing in the app plays it), and TrueHD in MPEG-TS/M2TS goes through the server (Media3's TS reader cannot read it) | done |
| Playback | The new player keeps the shape of anamorphic video (pixel aspect ratio); stream and subtitle URLs are logged without the access token; the legacy player logs the exact play method (direct stream is no longer logged as direct play) | done |
| HDR | The Shield's Dolby Vision profile 7 workaround applies only when the display reports Dolby Vision, so a profile 7 file on an HDR10 TV is sent as HDR10 instead of SDR (upstream #5794); new "Out-of-spec Dolby Vision" override (off by default) | done |
| Fix | The Live TV guide gets its channel list on first use too (no last channel yet), so programmes are not asked for missing channels; the last channel's position is counted from 0. The "update your server to 10.11" notice is removed: servers below 12.0 cannot connect anymore | done |
| UI | Settings → Playback → Media segments: skip button duration (5–30 s or until the segment ends, default 15 s). Live TV guide options: "Restore last channel" (on by default) | done |
