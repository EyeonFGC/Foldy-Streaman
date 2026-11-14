# Changelog

All notable changes to Foldman. Versions follow [semantic versioning](https://semver.org); downloads are on the [releases page](https://github.com/deveshrai/Foldman/releases).

## [0.4.0] — 2026-11-14 (Foldy Streaman)

### Changed
- **PLAY and PAUSE no longer switch you over to YT Music.** The keys control YT Music in the background. (Before, PLAY opened YT Music whenever Foldy couldn't see its player.)
- **Starting a tape's playlist stays in Foldy where possible**: if YT Music accepts the playlist directly, its screen never opens. Otherwise YT Music opens just long enough to start the playlist, and Foldy jumps back in front as soon as the music starts.
- If YT Music isn't running at all, PLAY opens it once and Foldy comes straight back when it plays.

## [0.3.1] — 2026-11-14 (Foldy Streaman)

### Changed
- **Picking up a tape flips it over into a whole cassette** in your hand. Over the deck it turns upright and grows to fit the slot.
- **The deck glows light blue with a blue outline** while you hold a tape over it (a faint glow as soon as you pick one up).

## [0.3.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Open to pick, close to play.** Unfolded, the phone is now a cassette rack beside an open deck. Tap a tape to read its card; hold a tape and drag it into the deck to load it. Close the phone and press PLAY on the front-screen player.
- Tape cards: what's on the tape, with Put in deck and Edit. The built-in sources sit on the rack too (My music files, YT Music adapter, Apple Music adapter).
- "Blank tape" at the bottom of the rack makes a new tape from a playlist.

### Changed
- The front-screen player shows the tape's name before a song starts, and reads STREAMAN.

## [0.2.0] — 2026-11-13 (Foldy Streaman)

### Added
- **Your own tapes.** "+ New tape" on the tape rack: paste a YouTube Music playlist link, write a title on the label, and pick the shell and label colours. Tap EDIT to change a tape or throw it away.
- **Tape in, PLAY, playlist plays.** Load one of your tapes and press PLAY: YT Music starts that playlist, and the keys, reels and counter control it like before.
- The cassette in the deck shows your tape's colours and handwritten title.

### Changed
- Renamed to **Foldy Streaman** with its own app ID, so it installs beside the original Foldman.

## [0.1.2] — 2026-10-21

### Changed
- **Foldman's settings no longer go into Android backups or device transfers.** They include your Apple Music user token, which now stays on the phone. When you move to a new phone, sign in to Apple Music again there.

## [0.1.1] — 2026-10-21

### Fixed
- **Apple Music showed the next song instead of the one playing.** MusicKit lists only the songs *after* the current one in its queue, and Foldman read the playing song's position from that list. The title, artist, cassette label and cover now come from the song that's actually playing, and the queue and Up next start from it.
- **"Track 0 of N"** for Apple Music now shows the right position and count, including songs already played.

### Changed
- Apple Music playlists always play in order: Foldman explicitly turns shuffle off when it loads one, and turns it off again if Apple's player ever reports it on.

## [0.1.0] — 2026-10-21

The first release.

### Added
- **Three postures.** On the cover screen, an edge-to-edge upright portable player with the tape on end behind a window and the keys down a silver side strip. Unfolded, the same player laid down as a deck between a library panel and a now-playing panel. Half-open on a table (Flex mode), the deck on the top half and the library on the bottom.
- **The mechanism.** Latching PLAY, PAUSE, STOP·EJECT, FF and REW keys and AMS skip buttons. Reels turn at tape speed ÷ radius with spin-up and coast-down. A mechanical tape counter, VU needles that follow the real audio on local files, an OPERATE LED, synthesised key clicks and haptics, and CLICK / HAPTIC switches.
- **Local folders.** Media3 playback in a background service with media controls. Album art comes from embedded art or a cover image in the folder, and each sub-folder is listed as an album.
- **Apple Music via MusicKit** (optional AARs). Sign-in, library playlist browsing through the Apple Music API, and in-app streaming in a separate, crash-isolated process that resumes the song after a crash.
- **YT Music and Apple Music via media sessions.** Transport, artwork and queue, using notification access.
- Adaptive launcher icon with a themed (monochrome) layer.

[0.1.2]: https://github.com/deveshrai/Foldman/releases/tag/v0.1.2
[0.1.1]: https://github.com/deveshrai/Foldman/releases/tag/v0.1.1
[0.1.0]: https://github.com/deveshrai/Foldman/releases/tag/v0.1.0
