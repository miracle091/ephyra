# Changelog

All notable changes to Velella are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and the versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html). While the version is 0.x, a minor
release may change behaviour or settings.

Velella is a fork of [Jellyfin for Android TV](https://github.com/jellyfin/jellyfin-androidtv). This file lists only
the fork's own changes, the commits prefixed `velella:` (`ephyra:` up to 0.5.0, when the app was called Ephyra);
upstream's history up to the fork point (tag
`upstream-fork-point`) is in the git log. Together with [`FORK.md`](FORK.md) it is the change notice required by
section 2(a) of the GPL-2.0.

## [Unreleased]

### Added

- Release notes are written from this changelog: the release workflow fills `.github/release-template.md` with the
  version's section and a link to it.
- A one-page showcase site (`site/`), published to GitHub Pages by the `velella-pages.yaml` workflow when it changes:
  twelve features, eight screenshots from the emulator and the install steps, with a link to Obtainium.
- The "Recently added" rows of the home screen end with a "See all" card that opens the library, as its tile in My
  media does.
- YouTube trailers play in the app through Invidious, never through YouTube: the trailer button asks the user's own
  instance first (Settings > Playback > Trailers), then the last one that worked and the public ones (their list is
  fetched at most once a day), 5 seconds each, as AVC video up to 1080p with AAC audio. Nothing is fetched until a
  trailer is picked; trailers on other sites are ignored.

### Changed

- Videos and live TV play in the new player for everyone, also after an update from a version that used the old
  one. The old player stays for this release only in debug builds, as "Previous video player" in Settings > Playback >
  Video player; it will be removed in the next release.
- The live TV guide is easier to read: a wider time scale (about two hours across instead of two and a half), so
  short programmes keep their titles and no programme is narrower than a quarter of an hour; only half-hour marks on
  the time line; the grid starts at the half hour now falls in; the focused row always stays whole on screen; the rows
  line up with their channels again; a programme on air has a lighter cell, its elapsed part tinted up to the current
  time and an "On air" tag with the time left, in place of the red dot (too easily taken for the recording mark) and
  the progress bar, updated as each minute starts, and at each new half hour the grid moves on by itself, keeping the
  selected channel; programmes already over are faded until focused; names too long for their programme or channel
  cell scroll while it has focus; channel logos get more room; the clock, home and the guide's own buttons sit where
  and as large as on the other screens.
- The Live TV screen is laid out like the home: the toolbar, then a spotlight of the programmes on air (name, an "On air"
  tag with the time left, channel, overview, Watch and Details), which moves on when a programme ends, then the rows.
  The On now, Coming up and Other channels rows are gone (the spotlight and the guide show them) and the first row is
  called "TV and recordings".
- The home and Live TV spotlights lay a veil of the theme's background over their image behind the text and the
  toolbar, so the overview, the clock and the icons stay readable over a bright picture.
- The home and Live TV spotlights rotate a little slower (every 12 seconds instead of 10), the current dash fills as
  the item's time passes, a name without a logo scrolls when it is too long instead of being cut, and the buttons and dashes stay in place
  whatever the length of the texts above them.
- Grid buttons (TV guide, Recordings, Genres...) show their name centred under the card, like the home's library
  cards, instead of in bold on it.
- The Live TV library shows its views first (TV guide, Recordings, Scheduled, Recorded series, renamed), then the
  recordings and channels, and no longer writes "No recordings" in the header.
- The home spotlight's Play and Details buttons show only their icons, like Favorite.
- The home spotlight spans the screen behind the toolbar, about 60% of its height, fading to the left and into the
  rows; it shows the title's logo in large (the name when there is none), the genres in the info row, up to three lines
  of overview (the tagline when there is none), labelled Play and Details buttons, and a dash for each title.

- Ephyra is now called Velella, with a new logo (a velella's float and sail, also next to About in the settings) and
  the application id `com.velella.app`, so it is no longer confused with
  [midasvo/ephyra](https://github.com/midasvo/ephyra), another Jellyfin client. It installs as a new app: sign in
  again and set it up anew, then uninstall Ephyra. The server lists it as "Velella" and keeps its per-library view
  settings under the client id `velella`; the release APK is `velella-v<version>.apk` and the repository moves to
  `miracle091/velella`.
- The issue and pull request templates are written for Velella: they point to this repository and the wiki instead of
  Jellyfin's forum and repository, and ask for the Shield model, the system version and, for playback bugs, the TV.
- The detekt and Android lint configurations live in `config/` (`config/detekt.yaml`, `config/android-lint.xml`).
- detekt fails the build on any issue, as `maxIssues: 0` intends: upstream's `ignoreFailures` only reported them to
  code scanning, so 11 warnings had slipped back in since the cleanup; they are fixed (the new player's backend
  factories split out of `createPlayer`, track selection in its own interface, shorter lines, named constants).
- The tests of `playback:core` use `kotlinx-coroutines-test` (tests only, not in the APK), so the new player's retry
  chain is tested through its media stream service, not just its retry rule.
- `FORK.md` keeps the origin, the license and the change notice; the per-change table moved to this changelog and
  the user documentation to the [wiki](https://github.com/miracle091/velella/wiki).
- detekt warnings cut from 574 to 0: Velella's own code fixed (named constants, split helpers and files, shorter
  parameter lists), upstream's mechanical warnings fixed (hashCode next to equals, explicit locale, unused
  parameters, long lines, file names matching their declaration).
- Upstream's long and complex functions split without behaviour changes (device profile, legacy subtitle switch,
  playback queries, Leanback channels, card presenter, info row, settings screens, home rows); Compose components take
  style and slot classes and Koin constructors take dependency groups. `detekt.yaml` uses the `comments` key of
  `ForbiddenComment`, so only `STOPSHIP:` is forbidden as intended.
- Library grids load faster: pages leave out the fields the grid never shows (chapters, trickplay, media streams,
  paths, overviews, taglines), their size follows the cards on screen instead of a fixed 100, and each new page is
  appended instead of diffing the whole list on the main thread.
- Cast and crew cards on the details screens are larger, with portrait pictures again: 120dp tall (instead of 88dp
  squares) and 7:9 like the other person cards.
- The new player frees its decoders and buffers when playback stops and builds a fresh player for the next one, so
  changes to the FFmpeg, libass and buffer settings apply without restarting the app.
- The new player switches the display to the video's frame rate (and back on exit) like the legacy one, with the
  same choice of mode, now shared by both players.
- The new player logs why playback failed and tells the rest of the app whether the stream, the decoder or the audio
  output was at fault, instead of a bare error state.
- The new player draws subtitles with the colours, outline, weight, size and position set in the subtitle settings,
  like the legacy one, which now shares the same code.
- The new player applies audio night mode to videos like the legacy one. Both players now free the effect when
  playback ends and never apply it to the device's whole audio output.
- The new player keeps the chosen playback speed for the next playback instead of falling back to normal speed
  while still showing the old one.
- The new player follows the zoom setting (fit, auto crop, stretch) like the legacy one; subtitles stay inside the
  whole picture so cropping never cuts them off.
- The new player's overlay looks like the legacy one: the series and episode over the title, how the server delivers
  the stream and why, the end time under the seek bar, the legacy margins and button order, and controls dimmed
  during HDR and Dolby Vision playback.
- The new player's controls have the legacy player's menus: subtitles and audio tracks (by the server's names) in
  sight, chapters when the video has them and the maximum bitrate under a "…" button that lists them with their
  current choice (Back returns from a menu to the list); picking a bitrate restarts the stream under it from the same
  position. Previous and next show only when there is an item to go to.
- The new player's seek bar is a thin line in the theme's accent colour that thickens when it has the focus; the time
  left ("-24:20") replaces the duration next to "Ends at", and the rewind and fast forward buttons show the skip
  length set in the settings inside a circular arrow.
- The new player shows the trickplay thumbnail and time of the picked position above the seek bar while scrubbing,
  as the legacy one did, and up from the buttons reaches the seek bar instead of hiding the controls.
- The new player downloads a video's trickplay thumbnails into the image cache as playback starts, so the first
  thumbnail while scrubbing shows at once instead of after a second.
- The new player answers the remote like the legacy one: centre plays or pauses while the controls are hidden, R1/R2
  and L1/L2 fast forward and rewind, Stop closes the player, and the Captions and Audio track keys open those menus.
  Its playback info panel shows the audio track playing instead of the first one.
- The first key pressed in the new video and photo players works: it used to only give the player the focus, when
  playback started and each time the controls hid.
- The new player shows the legacy player's Skip button inside media segments set to "ask to skip" (intros and outros
  by default): it dims, hides after the chosen delay and comes back on centre, up or down; centre skips to the end of
  the segment and back closes it. Until now the new player did nothing for these segments.
- The new player retries a failed stream like the legacy one: first without direct play, then as a transcode (at
  once for a decoder error), with a message each time; after three errors within 30 seconds, or when the server has
  no stream for the item, it closes (an unplayable item with another one after it is skipped instead).
- Seeking in a transcode the player can't seek in asks the server for a new stream at that position, as the legacy
  player does, instead of doing nothing.
- The new player shows the legacy player's Next up and Still watching screens over the last picture when an episode
  ends, with the same settings, countdowns and watched-episode count; Cancel closes the player. It also closes by
  itself when the queue ends, instead of staying on a black screen.
- The new player asks the server for live TV channels as the legacy one does: the request opens the channel's
  stream, waits up to 2 minutes while the server tunes it and follows the "direct play live TV" setting. Live
  channels, `.strm` files and other remote sources are no longer refused (a remote HTTP source plays from its own
  address); sources Media3 can't read directly go through the server.
- A live channel that has not started playing 45 seconds after the new player got its stream counts as a stream
  error, so the retry chain tries it again or gives up (about two minutes on a dead channel). Cold channels took up to
  34 seconds to start on the Shield, so the legacy player's 20 seconds would restart them before they could play.
- The new player's live TV controls follow the legacy player's: the "Live" badge and the channel at the top, the
  programme on air with a bar of its elapsed time between its start and end, the next programme and how the server
  delivers the stream above the buttons, no seeking, and TV guide, Record (red while set, with the series question),
  Channels and previous channel buttons, as icons only. Down with the controls hidden, or Channels, opens a row of the channels at the
  bottom of the picture; OK tunes to one in the same player. The controls area grows when its content needs it.
- The new player tells the server which live stream it played, so the server closes it (and frees the tuner) when
  playback stops or moves to another stream; a live stream that never started (a retry, another channel or Back
  replaced it, or it came after the user had moved on) is closed too and does not start late, so failed attempts no
  longer keep tuners busy. The retry chain counts its attempts again only after 30 seconds of real playback, not
  merely 30 seconds after the last error, and ignores a late error of the stream a retry is already replacing, so a
  channel that never starts reaches the transcode and gives up instead of retrying forever. Stream
  addresses that are not the server's are logged by their host only, since they may hold credentials.
- Trailers skip an Invidious instance that hands out YouTube's own addresses instead of proxying the video, and the
  trailer player refuses any request or redirect to YouTube's or Google's video hosts. A malformed instance address
  or answer moves on to the next instance instead of closing the app; leaving the trailer stops the search; the
  trailer takes the audio focus, so music playing in the app pauses.
- The new player tells the server which version (media source) and audio track it plays, and knows the subtitle
  tracks of the stream.
- The new player loads subtitles the server hands out as separate files and shows the subtitle the server picks
  from the user's settings (or none), instead of ExoPlayer's own choice; the reports carry the subtitle track.
- The Audio and Subtitles buttons on the details screen work with the new player too: the picked tracks (and
  version) are requested from the server, which then streams, side-loads or burns in the subtitle as needed.
- The new player switches audio and subtitle tracks when another client asks (the web dashboard's remote control):
  in the player when the stream carries the track, otherwise with a new stream from the server at the same position
  (still paused if it was; if the server can't build it, or playback moved on meanwhile, the current stream plays on).
  The reports carry the tracks actually shown.
- The new player follows the "burn in subtitles when transcoding" setting and tells the server where playback starts,
  like the legacy one; a subtitle burnt in is not loaded a second time as a file.
- The new player's seek bar keeps a knob at the playing position when it doesn't have the focus, like the legacy
  one; at half the focused size it reads better from the couch than the legacy dot.
- Scrubbing the seek bar seeks once, 0.6 s after the last arrow press (or at once with centre), instead of on every
  press, so a transcoded stream restarts once; every press of a quick run counts. Music shares the seek bar.
- Left and right seek quickly in the new player while the controls are hidden: presses add up by the fast forward and
  rewind amounts, a toast shows the distance and the target, and one seek follows the last press (centre seeks at
  once).

### Removed

- The More like this row on the details of films, trailers and shows.
- The home's On now row and its Live TV home section (the Live TV screen's spotlight shows what is on air; a home slot
  set to it falls back to its default), and the Recordings card among the home's libraries (the Live TV screen opens
  the recordings); the libraries row is called "Libraries" ("Library" with only one)
  instead of "My media" and the Next up row "Next episode", also in the TV shows library.
- Trailers no longer open the YouTube app or a browser, and the app no longer declares that it looks for apps that open
  YouTube links.
- Dead code: the recommended-programmes rows (`QueryType.LiveTvProgram`, left without a caller once the On now rows
  went) and the `SimilarSeries` and `Premieres` row types no screen creates.
- Dead code: functions and members with no caller (`PopoverMenu.kt`, `ItemRowAdapter.setSiblingRow`,
  `GuideFilters.isMovies`, `HorizontalGridPresenter.enableChildRoundedCorners`, `DisplayPreferencesStore.clearCache`,
  `ImageHelper.getLogoImageUrl`, `MenuBuilder.subMenu`, `toHtmlSpanned`, `createSeriesGetNextUpRequest`).
- Playback classes never instantiated: the album, instant mix, track and episode queue suppliers of
  `playback:jellyfin` with their `PagedQueueSupplier` and `SequenceQueueSupplier` bases, `BasicMediaStream` and
  `NoOpPlayerVolumeState`; three unused audio effect fields of `VideoManager` and seven unused imports.
- Resources reported unused by Android lint: 46 drawables, 2 animations, a layout, the old logo colours and unused
  colours, styles and values.
- Upstream repository leftovers: `CODEOWNERS`, `renovate.json`, the stale, triage and merge-conflict workflows (they
  only run in repositories owned by Jellyfin) and `fastlane/` (store texts of the official app).

### Fixed

- In the video player, the first press of right just after the controls appear moves to the next button again: the
  play/pause button now takes the focus as soon as the controls show, not at the end of their slide in.
- A screen opened from navigation no longer stays blank now and then, and a freshly created screen gets its arguments
  (upstream fixes `5e9140df4` and `8653f7b06`). An empty library grid no longer crashes when the focus moves (upstream
  `10d01122a`).
- Display settings work for folders that are not libraries, fast scrolling in a library grid keeps the focus, and lazy
  lists remember their focus (upstream `388e641a5`, `971257d77` and `1dc9390fb`).
- The home button, clock and now playing bug of the details screen fade out while a row below the details is selected,
  so they no longer sit on top of the cards that scroll up under them.
- Long cast and crew names no longer overlap the next card: a name or role longer than its card scrolls by itself
  inside the card, on every card, with faded edges so neighbouring names stay apart; names that fit stay still.
- A trailer shows a spinner while the Invidious instances are asked and the stream starts, instead of a black screen.
- Down from the seek bar of the new player goes to play/pause, not to whichever button was nearest.
- YouTube trailers play again: Invidious instances now answer with YouTube's own stream addresses even when asked for
  proxied ones, so every trailer was refused; the streams now come through the instance's own proxy (`/latest_version`).
  invidious.nikkosphere.com joins the fallback instances.
- Leaving the new player refreshes the details screen below, as the legacy player does: the resume point, the
  watched state and the episode played last show at once, and the focus moves to Resume or Play.
- The new player skips media segments as set in Settings (intro, outro, recap, preview, commercial) instead of always
  skipping intros; "ask to skip" comes with its skip button. A failed segment request no longer stops segment skipping
  for the rest of the queue.
- A video in the new player is treated as playback in progress like one in the legacy player: the screensaver shows
  it when paused, another client's "display content" no longer interrupts it, and a playback started remotely replaces
  its screen instead of opening a second player on top. The in-app screensaver comes after the short pause delay for a
  video paused in the new player too.
- The new player's audio night mode really turns on: it was decided before the first video was known, so it never
  applied.
- When the server burns a subtitle into a converted stream, the new player no longer shows a side-loaded subtitle
  flagged as default on top of it.
- After a track switch that needs a new stream while a video is paused in the new player, the server sees the new
  session at once instead of losing it until playback resumes.
- The skip button's icon shows again: it was tinted into a solid light disc; it keeps its own theme colours now.
- Scrubbing the seek bar of the new player no longer reads as a pause: no pause toast or play icon while picking a
  position.
- The new player (development option, also used for music) reports playback to the server reliably: the stop report
  was lost because the queue was cleared before it was sent, moving to the next episode reported neither the stop of
  the previous one nor the start of the next, errors were reported as normal stops, and progress was sent only on
  pause; it now goes every 3 s while playing and every 15 s while paused, like the legacy player.
- The new player resumes videos where they were left and starts a queue at the chosen item: the resume seek was
  issued before the stream was known and the queue always started at its first item.
- Card titles under posters (seasons, films in the rows) are centred like the line under them, instead of starting at
  the left.

## [0.5.0] - 2026-11-09

### Added

- Settings → Playback → Media segments: duration of the skip button (5–30 s or until the segment ends, default 15 s).
- Live TV guide option "Restore last channel" (on by default).
- Accent colour setting apart from the theme (Settings → Customization): theme default or one of seven presets.
- "Playlists in My media" (Settings → Home, off by default).
- Details screen: the tagline under the title; info row groups divided by thin lines, the official rating as an
  outlined badge.
- Series details: a "Next: S1:E3 · Title" card beside the buttons that plays the next up episode.
- Cast and guest star rows with compact cards (88dp picture, name and role scrolling when too long).
- Season screen: runtime, community rating and end time under each episode's title.
- "Compact episode list" (Settings → Customization, off by default): episodes as rows with picture, title, meta
  line and plot, opening on the first episode.
- Home: the Live TV library card shows a channel logo, photo library cards show one of the library's photos.
- The home spotlight and the long pause screen pan their backdrop slowly (off when system animations are off); the
  spotlight shows the tagline and an info row.
- "Out-of-spec Dolby Vision" override (off by default).
- A person without a photo gets a poster-shaped cover in the art tone of their name.

### Changed

- Jellyfin Kotlin SDK 1.9.4 (server API 12): **Jellyfin server 12.0 or newer is required**. The play method's
  transcode reasons include "rotated video not supported".
- No backdrops of films, shows or music behind the screens; the "Backdrop" setting is removed. The spotlight, the
  library cards and the long pause screen keep their pictures.
- Lighter default home: libraries, Continue watching, Live TV, Next up and Latest.
- The skip button stays 15 s instead of 8; once hidden, centre, up or down call it back while the segment lasts.
- The skip button is a solid pill in the theme's colours; the player's bottom controls reach the screen's edge.
- One progress bar style on every card (bottom edge, full width, 4dp, theme accent).
- One header line on every screen: clock and first element on the same line, 32dp from the edges.
- Details and album screens use 168dp covers; the first row under the cover uses 120dp posters.
- The next episode picks the same audio and subtitle track as the last one (language, forced flag, title, codec).
- Rotated videos in containers whose rotation Media3 cannot read (MKV, TS, WebM) are transcoded upright.
- DTS core follows the DTS setting; in Auto, formats with a hardware decoder count as supported; audio offload is off.
- "Prefer FFmpeg for audio" keeps bitstreaming every format the output can pass through; the server prefers AC3/E-AC3
  over AAC when it converts audio. MLP is no longer direct played, TrueHD in MPEG-TS/M2TS goes through the server.
- The new player keeps the pixel aspect ratio of anamorphic video and asks the server for the chosen audio track.
- About → Licenses lists the Media3 FFmpeg decoder as Apache-2.0 and LGPL-2.1-or-later.
- Search: the text field is filled like the sign-in fields and follows the 32dp margin.

### Removed

- Books: the "Continue reading" section; book libraries and audiobooks stay hidden.
- The "update your server to 10.11" notice (servers below 12.0 cannot connect anymore).

### Fixed

- A late "playing" event after stop no longer crashes playback (upstream #5938).
- "Scale on device" keeps the native resolution even when a lower mode has a closer refresh rate (upstream #5893).
- Switching between two audio tracks of the same language works when burned-in subtitles force a transcode, and a
  subtitle change keeps the audio track (upstream #5798); a burned-in subtitle stays on after an audio switch.
- An alternate version of an episode chosen in details is the one played (upstream #5931).
- The Dolby Vision profile 7 workaround applies only on displays that report Dolby Vision, so HDR10 TVs get HDR10
  instead of SDR (upstream #5794).
- Blu-ray transport streams (`.m2ts`, `.mts`) go to the server at once instead of failing a direct play first.
- A live channel still buffering 20 s after its stream is set goes through the live retries instead of waiting forever.
- Programme details opened for a channel load the programme on air when the one shown has ended.
- The Live TV guide gets its channel list on first use too.
- A server discovered with an unreachable IPv4 address (for example a Docker address) is listed with the address its
  reply came from.
- The chosen theme survives when Android recreates the activity.
- Search: the D-pad leaves the text field, the cursor is visible, and Back keeps the focus on the results.
- The Settings overline is shown on the Subtitles, Home and Screensaver sections.

### Security

- Stream and subtitle URLs are logged without the access token.

## [0.4.0] - 2026-11-08

### Added

- Ephyra design system in the Dark and Black themes, for Compose and Leanback screens: night blue, one cyan accent,
  solid white focus, pill-shaped buttons, rounder cards.
- Three coloured themes: Abyss, Coral and Medusa.
- The Atkinson Hyperlegible Next font (and Atkinson Hyperlegible Mono for Quick Connect codes) across the interface.
- Long pause screen: a video paused for 2 minutes shows the dimmed backdrop, the header and progress instead of the
  screensaver, shifting against burn-in after 10 minutes.
- The home spotlight shows the community and critics' ratings and the title's logo.
- Library cards show the backdrop of the title added most recently (per user).
- Live TV player: "LIVE" badge, "number · channel", a seek bar spanning the programme on air, a "Next:" line, Guide,
  Record and Channels as pill buttons; a failed live stream is retried twice.
- Live TV guide: a red dot and a progress bar on the programme on air.
- Deleting live TV recordings, after a confirmation, when the server allows it; library media can never be deleted.
- The REC icon on the server's Recordings library card.

### Changed

- Covers grow by 6% on focus (upstream 15%) with a thin accent ring; a cover without a picture shows a colour tone.
- Home: 32dp side margins, cards sized to fill a row, 12dp gap between cards, no row backdrops, a fading bottom edge,
  rows aligned on a fixed line; the live TV buttons row is gone and card labels are larger.
- Library screens: 32dp margins, an 8dp grid gap, no item counter; the startup and sign-in screens drop the server's
  splashscreen.
- Home toolbar and spotlight use icons without labels; the favorite mark is a round badge.
- User selection, sign in (with a tiled Quick Connect code) and server selection follow the design system's boards.
- Music libraries open as album grids; the music player, album, playlist, film, series and person screens follow the
  design system's layouts.
- The video player's controls follow the design system's "Player" board (legacy playback path, look only).
- Cards with the name over the image use a bottom shade instead of a grey box.
- The screensaver is a minimal clock that moves every minute against burn-in.
- The startup window shows the Dark theme's background from the first frame.
- Active state icons (shuffle, repeat, filters, favourites, checkboxes) use the theme's accent.
- Settings open as a two-pane panel with the spotlight's type scale; focus lands on the first item of the right pane.
- The live TV guide uses rounded cells with the cards' focus ring; the Live TV library buttons are cards.
- Programme details: REC icons for set recordings and a "Live" badge.

### Removed

- The Emerald and Muted purple themes; a device set to one opens in Dark.
- The screensaver's age rating options.

### Fixed

- The live TV guide turns its background back on after the player.
- "In N hours" in programme and record popups counts from the device's time zone instead of UTC.
- Library cards without art and the Recordings, Schedule and Series buttons show icons.

## [0.3.0] - 2026-11-05

### Fixed

- The menu names in the Shield HDR hint.
- The codec benchmark runs its server calls off the main thread.
- The settings panel is black in the black theme.

## [0.2.0] - 2026-11-04

### Added

- Support for every Shield TV model: armeabi-v7a next to arm64-v8a for the 32-bit Shield TV 2019 tube.
- The player's top panel shows how the server delivers the stream and, when it converts, why.
- "Audio" and "Subtitles" buttons on the details screen; the picked tracks go with the first playback request.
- "Reset progress" and "Go to series" in the card menu.
- On a Shield TV with an HDR display, a one-time hint about the Shield's content matching.
- "Black (OLED)" theme.
- "Blur unwatched episodes" (off by default) and "Stop at the end of the season" (off by default).
- Spotlight at the top of the home screen with the five newest unwatched films and series.
- Codec benchmark in debug builds (Developer options).
- The licenses screen lists the native libraries bundled inside other packages (FFmpeg, libass and its dependencies).

### Changed

- The audio and subtitle menus list tracks in the user's preferred languages first.
- The skip button dims after 3 seconds; the player's controls are dimmed during HDR and Dolby Vision playback.
- Opening a stream (PlaybackInfo) waits up to 2 minutes, so a live channel the server is still tuning opens.
- The live TV guides and the item details tie their work to the screen's view lifecycle instead of an AsyncTask.
- The lint workflow runs only detekt; Android lint runs locally and in the release workflow.
- The README links the upstream project; Ephyra's own changes are licensed GPL-2.0-or-later.

### Removed

- The Delete button on item details.
- Checks made obsolete by minSdk 30 (Android 6 volume fallback, `-v26` launcher icon folder).
- Unused VLC codes.

### Fixed

- Android lint errors (Media3 opt-ins, restricted Leanback class, configuration-aware strings, notification lint id).
- The codec benchmark on Android 11; its strings fall back to English.

## [0.1.0] - 2026-11-03

### Added

- Name Ephyra, application id `com.ephyra.app`, own logo, launcher icon and TV banner; installs next to the official
  app.
- Built-in updater from GitHub Releases (Settings → About) and a daily update check, both without Google Play.
- Baseline profile, so ART compiles startup code ahead of time, sideloaded APKs included.
- Release workflow that checks, signs and publishes the APK, `version.txt`, the R8 mapping and checksums.
- A build check that fails if Google Play Services, Firebase, Play Core or the Cast SDK enter the dependencies.

### Changed

- minSdk 30 (Android 11), ARM only; R8 strips debug logging.
- Hardware video decoders come first; for H.264, HEVC, VP9 and Dolby Vision software decoders are skipped when a
  hardware one exists. The playback buffer is capped at a quarter of the app's heap.
- Software decoders are off by default, so AV1 is transcoded by the server; with them on, software AV1 is capped at
  1080p.
- Refresh-rate switching is on by default ("Scale on device").
- Coil's caches are bounded and images are requested at display size.
- Home-screen channels and Watch Next are off by default.
- Per-library view settings are saved on the server under the client id `ephyra`.
- Accompanist permissions are replaced with AndroidX Activity Result APIs.
- README rewritten for Ephyra.

### Removed

- Upstream's publish jobs (repo.jellyfin.org).

[Unreleased]: https://github.com/miracle091/velella/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/miracle091/velella/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/miracle091/velella/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/miracle091/velella/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/miracle091/velella/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/miracle091/velella/compare/upstream-fork-point...v0.1.0
