# Changelog

All notable changes to Foldman. Versions follow [semantic versioning](https://semver.org); downloads are on the [releases page](https://github.com/deveshrai/Foldman/releases).

## [0.17.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Drag to reorder your tapes.** Hold a tape and drag it up or down the rack: the other tapes slide out of the way to open a gap where it will land, and you drop it in. (Dragging it onto the deck still loads it.) Adapters stay at the top.
- **Tap the tape under the deck** to open its J-card.
- **The app never rotates on the Fold**, folded or open, even with auto-rotate on.

### Changed
- **The rack closes up.** The tape in the deck leaves the rack and the others slide up to fill the gap; ejecting it slides it back into its place. Deleted tapes close up the same way.

## [0.16.1] — 2026-11-14 (Foldy Streaman)

### Changed
- **The J-card slides in from the left**: quick at first, then easing gently to a stop. Closing it slides it back out to the left.
- **The hinge runs the full height of the screen**, top edge to bottom edge, right on the fold, so it looks like what the phone folds on. It's one colour per theme (blue, gold, red, silver) with darker screws.

## [0.16.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Hinges down the fold** (Fold only): a long piano hinge in the theme's colours (blue and chrome, gold, red and chrome, silver and blue), centred in a gap between the rack and the deck, which are now equal halves. Settings > Theme > Hinges: One long hinge (default), Three hinges, or None.
- **Back up and restore tapes** (Settings > Backup): SAVE writes every tape (pictures, fonts, colours, positions and all) and your settings to one file wherever you choose; LOAD brings them all back, e.g. after reinstalling.

## [0.15.1] — 2026-11-14 (Foldy Streaman)

### Fixed
- **0.15.0 crashed on startup.** The new "Adapters on the rack" setting was read before it had been loaded. Settings now load before the app first draws.

## [0.15.0] — 2026-11-14 (Foldy Streaman)

### Added
- **The inside matches the theme.** Opened up, the body round the rack and the deck is the theme's own material (blue enamel, matte black, glossy cherry red, brushed silver), with lettering and highlights to match.
- **Picture on the cassette** (per tape, on its J-card under the buttons): the J-card picture fills the cassette's label where the colour goes, under the stripes and the title strip.
- **Settings > Adapters on the rack:** switch each adapter (My music files, YT Music, Spotify, Apple Music) off or on. Your own playlist tapes stay either way.

### Changed
- The upright cassette in the deck is drawn turned in place (no rotated layer), which also keeps the screen previews working.

## [0.14.5] — 2026-11-14 (Foldy Streaman)

### Fixed
- **A newly made tape bounced back from an empty deck.** Each tape on the rack kept the deck's state from when it first appeared, so a new tape still thought the deck was full. Tapes now always check the deck as it is.

### Changed
- In Cherry Red the "drop to load" glow is yellow, so red only ever means "eject the tape first".

## [0.14.4] — 2026-11-14 (Foldy Streaman)

### Fixed
- **Moving and zooming the J-card picture is smooth.** Each swipe or pinch used to move the picture only a tiny bit, because the gesture restarted every time the picture moved; now it follows your fingers all the way.

## [0.14.3] — 2026-11-14 (Foldy Streaman)

### Fixed
- **The plug follows where the music actually plays.** Switch the sound to "This phone" while headphones are still connected and the plug comes out; switch back and it goes in. While paused it keeps the last state; with nothing connected there's never a plug.
- Foldy now really hears about headphones connecting and disconnecting (the listener was being set up too early and never ran).

## [0.14.2] — 2026-11-14 (Foldy Streaman)

### Fixed
- **No plug in the headphone jack while playing through headphones.** Foldy asked Android where *its own* sound would go (the speaker, since Foldy itself isn't playing) instead of checking what's connected. It now shows the plug whenever Bluetooth, USB-C or wired headphones, a speaker or a car are connected, and re-checks every couple of seconds.

## [0.14.1] — 2026-11-14 (Foldy Streaman)

### Changed
- The volume thumbwheel now looks like the real thing: a dark, finely knurled wheel in a recessed slot, VOLUME printed upright beside it, and the numbers 1 to 10 printed next to the wheel, rolling with it as it turns.

## [0.14.0] — 2026-11-14 (Foldy Streaman)

### Added
- **New theme: Silver & Blue**, after an early-80s aluminium player: brushed silver body and side strip, a long pill-shaped clear window in a thick royal-blue frame, blue printing, and slanted silver keys with a dark outline in their own shape (thin green round PLAY, blue round STOP·EJECT). The clear window sits tight round the tape, with flat sides and rounded corners inside the blue pill.

- **Volume thumbwheel** on Cherry Red and Silver & Blue: a ribbed grey wheel with 1 to 10 printed round it. Roll it up or down; it ticks at each number. Original and Black & Gold keep their sliders.

### Changed
- Settings shows the themes two to a row.

## [0.13.0] — 2026-11-14 (Foldy Streaman)

### Added
- **New theme: Cherry Red** (Settings > Theme), after an early-80s auto-reverse player: glossy coral-red body with a black trim band, a dark smoked window, big outlined "AUTO REVERSE" lettering with a yellow "STEREO", and small chrome keys with black grip slits in a black side panel (PLAY on top, with its red dot).

## [0.12.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Settings > Tape shelf on the front screen** (off to start with). When on, the yellow button on the Original front (or a TAPES button on Black & Gold) opens the tape shelf there, so you can change tapes without opening the phone.

### Changed
- The J-card buttons are now **metal keys** like the player's: silver with an orange stripe (Original), black with a gold stripe (Black & Gold).

## [0.11.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Spotify playlists.** Paste a Spotify playlist link (Share → Copy link) on a new tape or in its J-card. Load the tape, close the phone and press PLAY: Spotify starts the playlist, and the keys, reels, volume buttons and song name work as with YouTube Music. Needs the Spotify app, signed in.
- A **Spotify adapter** tape on the rack, and your tapes say **YT** or **SP** on their spine. Spotify tapes are marked "SP ∞" on the cassette.

## [0.10.1] — 2026-11-14 (Foldy Streaman)

### Changed
- **Tapes show their picture on the rack**, with the title written across the middle of it.
- **Under the deck is now the tape's spine**, the same thin strip as on the rack (picture and title), so the cassette above it is bigger. The song playing is in the line underneath.

## [0.10.0] — 2026-11-14 (Foldy Streaman)

### Added
- **J-cards.** Tap one of your tapes in the rack: its card slides out with the title, a picture under it and what's on the tape. LABEL, PHOTO, FONT, COLOURS, PLAYLIST and DELETE buttons change it.
- **Pictures:** pick one from your phone, then drag to move it and pinch to zoom inside the frame. Foldy keeps its own copy.
- **Fonts:** classic pen, old typewriter, typewriter, neat handwriting, scribble and marker. The font goes on the J-card, the cassette label and the spine in the rack.
- **A small J-card under the deck:** the tape's title, its picture strip and the song playing.
- **Headphone jack:** bigger and more real. When the sound goes anywhere but the phone (Bluetooth, wired, USB, car) a plug sits in it with its cable hanging off the screen. Tap it to choose where the sound goes.
- **Opening and closing the Fold:** the inside screen darkens from the fold outwards as you close it, and clears again as you open it.
- **Settings > Stop the music when I open the phone** (Fold only, off to start with).
- **Volume buttons like headphones:** up = PLAY, up twice = next song, up three times = previous; down = PAUSE (again to carry on), down twice = STOP.

### Changed
- **Keys work like a Walkman:** PLAY does nothing while it's already down (playing, or paused). PAUSE pauses and un-pauses. STOP pops every key back up.

## [0.9.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Works on phones that don't fold.** The app opens on the player (portrait). Press STOP, then STOP·EJECT again: the cassette pops and the deck opens in landscape. Change tapes, then press **CLOSE** to go back to the player and press PLAY.
- On those phones the screen is locked: closed stays portrait, open stays landscape. Turning the phone never opens or closes it.
- Foldables (found by Android's hinge sensor, Samsung Fold / Flip model numbers, or a hinge seen before) work exactly as before, with none of this.

## [0.8.0] — 2026-11-14 (Foldy Streaman)

### Changed
- **No play keys inside any more.** Like a real cassette player: open the phone to change the tape, close it to play and control the music. The deck gets the extra room.

## [0.7.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Settings** (unfolded, next to "Cassette rack"): theme, click sounds, haptics, notification access, Apple Music sign-in and the music folder.
- **Themes.** Original (blue) and the new **Black & Gold**: matte black body, the tape seen through a round window, a brushed-gold band with round black knobs and a gold PLAY knob on top. The drop glow is gold in this theme.
- **Volume buttons on the front screen:** volume up = PLAY, volume down = PAUSE (with the key click). Unfolded they still change the volume.
- **The empty deck looks like the inside of a real one:** two spindles, a screw and two guide pins.

### Changed
- **A full deck won't take another tape:** it glows red with "Eject the tape first", and the tape goes back to the rack. The card's "Put in deck" button says the same.
- The yellow button is back on the Original front screen, a bit bigger (decoration only).

### Fixed
- The tape you pick up was blank when the deck was empty.

## [0.6.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Swipe the tape right to eject it.** It slides off the side of the screen with a click, the music stops and the deck is left empty ("No tape" on the front screen too).
- **Click when a tape goes in**: the tape slides the last bit into the slot and clicks as it seats, with a buzz.

### Changed
- The deck's slot is now only a millimetre or so bigger than the tape, so it fits snug.
- Front screen: the keys run top to bottom with a **red PLAY key at the top**, then REW, FF, PAUSE and STOP·EJECT.

## [0.5.0] — 2026-11-14 (Foldy Streaman)

### Changed
- **Tapes are only changed with the phone open.** The front screen no longer has the TAPES button or tape sheet; it says "Unfold to change tapes".
- **Foldy comes back after starting a playlist even without notification access**: about 4½ seconds after YT Music opens.
- PAUSE, and the skip buttons, now also reach YT Music when Foldy can't see its player.
- The front screen shows "Tap: turn on notification access" when that's missing (Foldy needs it to see and control YT Music).

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
