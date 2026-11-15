# Changelog

All notable changes to Foldman. Versions follow [semantic versioning](https://semver.org); downloads are on the [releases page](https://github.com/deveshrai/Foldman/releases).

## [0.38.0] — 2026-11-15 (Foldy Streaman)

### Added
- **New theme: Crystal Clear** (Settings > Theme), after a see-through player from the 90s. A matte silver cover with a glass window showing the whole tape over a perforated silver mechanism, FOLDY and a big STREAMAN up the right-hand side in frosted lettering, a serial tag, and a yellow warning sticker ("highly contagious, may cause sudden dancing"). The side strip is brushed silver with metallic keys set in white: round previous/next, PLAY, PAUSE and STOP as tall pills, REW and FF round.
- The inside, hinge, tape drawer and J-card buttons follow the new theme.

## [0.37.1] — 2026-11-15 (Foldy Streaman)

### Changed
- **The open deck has a tape-head plate**, like the inside of a real player: grey metal with the pinch rollers, capstans, play and erase heads, screws and CASSETTE PLAYER stamped in. It sits on the deck floor under the tape's open edge, so you see it with the deck empty or as a tape slides out.

## [0.37.0] — 2026-11-15 (Foldy Streaman)

### Added
- **Phones that don't fold: Settings > Open the deck upright.** With it on, EJECT keeps the phone upright instead of turning it sideways: the tape lies flat across the top, with the rack underneath, and a tape's J-card slides in over the rack. There is more room for the J-card when the keyboard is up. Off (as before) = the deck opens sideways. Folding phones are not affected.

## [0.36.0] — 2026-11-15 (Foldy Streaman)

### Changed
- **J-card: choosing a tape's music.** The two "pick from my playlists" buttons are now stamped buttons like the six below them (YT MUSIC and SPOTIFY, each only when you're logged in), with a new SEARCH button beside them. SEARCH slides a box open under the buttons, pushing the rest of the card down. The PLAYLIST button now opens this row on the card instead of a pop-up.
- **Search Spotify from the J-card** (when logged in to Spotify): type an album, artist or playlist in the SEARCH box and press search on the keyboard. Albums and playlists are listed; either can be a tape. Pasting a YouTube Music or Spotify link in the same box still works.
- **Spotify albums can be tapes** and start from inside Foldy like playlists.
- Settings > Spotify no longer shows the Client ID box. The ID is built into the app, so it can't be deleted by mistake (and a damaged one typed earlier is ignored).

## [0.35.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Log in with Google for YouTube Music** (Settings > YouTube Music). Once logged in, a blank tape's J-card and the PLAYLIST button offer "Pick from my YT Music playlists": your own playlists, with "Liked music" first. The tape takes the playlist's name and cover picture if it has none yet.
- Foldy only asks to see your YouTube playlists (read-only). The login is Google's own screen; Foldy never sees the password.
- Needs: a Google Cloud project with the YouTube Data API v3 switched on, your Google account added as a test user, and an Android client registered for package `dev.foldman.streaman` with the app's SHA-1 fingerprint.

## [0.34.4] — 2026-11-14 (Foldy Streaman)

### Changed
- Lock screen padlock moved clear of other things: centred above the window on Sport Yellow (on the grey) and Silver & Blue (on the blue, now white), centred above the window on Cherry Red and Black & Gold, and in the middle of the black triangle on Red Chrome.
- Red Chrome: AUTO REVERSE sits a little lower and further left.
- Cherry Red: AUTO REVERSE is a clean outline now, without the lines where the letters' parts overlapped.

## [0.34.3] — 2026-11-14 (Foldy Streaman)

### Changed
- The TAPES button on the front screen (when the front tape shelf is on) is now a proper raised button on Black & Gold, Cherry Red, Silver & Blue and Red Chrome: built like the Original theme's yellow one, with a shadow and a cap that presses down, in each theme's own colour.

## [0.34.2] — 2026-11-14 (Foldy Streaman)

### Changed
- The lock screen padlock now waits open for about a second and a half after the phone wakes, so you see it before it moves. It opens like a real padlock: the shackle starts popped up and swung round to the far side, swings back over the body, then pushes down and clicks.

## [0.34.1] — 2026-11-14 (Foldy Streaman)

### Changed
- Notifications on the player moved and given snappier movement: they come in fast with a small bounce as they land, and dip slightly before shooting back out.
  - Original and Cherry Red: a dark panel drops into the top of the tape window.
  - Red Chrome: a dark panel rises into the bottom of the window.
  - Silver & Blue: slides in from the right, lettered up the window.
  - Black & Gold: zooms out from the middle of the round window.
  - Sport Yellow: as before (the dial strip swaps), now with the bounce.

## [0.34.0] — 2026-11-14 (Foldy Streaman)

### Added
- **A small glowing padlock on the player while it is showing over the lock screen**, so you can tell the phone is still locked. Each time the phone wakes, the padlock starts open, swings down and clicks shut. Same padlock on every theme, in the theme's colour and its own spot (on Sport Yellow: white, on the grey surround under the window). Only with "Show the player on the lock screen" switched on.

## [0.33.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Notifications on the player** (Settings > Notifications on the player: Off / App name only / Full message; off to begin with). A new notification shows on the front player for about five seconds, each theme in its own spot:
  - Sport Yellow: the FM numbers, bar and "unfold to change tapes" slide down out of the top pill and the notification slides in, then they swap back.
  - Original, Cherry Red, Red Chrome: the tape window darkens and the notification slides down into it in white.
  - Silver & Blue: the same, turned on its side to run up the tall window.
  - Black & Gold: in the middle of the round window.
- Silent notifications and music players' own "now playing" cards are left out. Nothing is stored.
- Needs: notification access turned on for Foldy. The phone's own pop-up still appears as well.

## [0.32.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Settings > Show the player on the lock screen.** When Foldy is the app you had open and the phone locks, waking it shows the front player (tape turning, keys working) instead of the lock screen. Opening the phone to the tapes, or leaving Foldy, asks you to unlock as normal. Android doesn't let an app replace the lock screen itself, so this only shows when Foldy was the last app on screen.

## [0.31.0] — 2026-11-14 (Foldy Streaman)

### Fixed
- **Logged in to Spotify, PLAY still jumped to the Spotify app.** Foldy can only tell Spotify to play when Spotify is awake on the phone. It now wakes Spotify quietly in the background first and keeps asking for a few seconds, and it is better at spotting which device in Spotify's list is this phone.
- If it still can't, a short message now says why (for example, a free account: Spotify only lets Premium accounts be started from another app) before it opens Spotify the old way.

## [0.30.1] — 2026-11-14 (Foldy Streaman)

### Changed
- The Spotify Client ID is now built in, so Settings > Spotify only needs a tap on LOG IN. (People logging in still have to be on the Spotify app's allowed list.)

## [0.30.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Log in with Spotify** (Settings > Spotify). Once logged in:
  - Spotify tapes start **without switching to the Spotify app**: Foldy tells Spotify over the internet to play the tape's playlist on this phone. (If Spotify can't be reached that way, it falls back to opening the playlist in Spotify as before.)
  - **Pick playlists from a list:** a blank tape's J-card, and the PLAYLIST button, offer "Pick from my Spotify playlists". The tape takes the playlist's name and cover picture if it has none yet.
- The login is Spotify's own page in the browser. Foldy never sees the password.
- Needs: a Spotify Premium account, and a free app registered at developer.spotify.com/dashboard with the redirect URI `foldystreaman://spotify-login`; its Client ID goes in Settings > Spotify.

## [0.29.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Settings > Come back here after a tape starts.** Newer Android versions stop an app bringing itself back to the front unless you allow "Appear on top" for it, which is why Foldy stayed in Spotify after starting a tape. Tap the row, allow it for Foldy Streaman, and Foldy returns by itself. Foldy does not draw anything over other apps.
- Needs: "Appear on top" allowed for Foldy Streaman (the row opens the right page).

## [0.28.1] — 2026-11-14 (Foldy Streaman)

### Fixed
- After starting a Spotify (or YT Music) tape, Foldy sometimes stayed in the music app instead of coming back by itself. It gave up if the music app was slow to appear; now it waits for it.

## [0.28.0] — 2026-11-14 (Foldy Streaman)

### Fixed
- **Spotify tapes still played whatever Spotify had on last.** Handing the playlist to Spotify quietly in the background doesn't work: Spotify says yes and then carries on with its old music. Spotify tapes now always open the playlist in Spotify itself and tell it to play, the same way YT Music tapes do: Spotify shows for a moment, then Foldy comes back.

## [0.27.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Settings > Volume buttons work the player.** On (as before): on the front screen, volume up = play, volume down = pause, double-tap to skip or stop. Off: they are normal volume buttons again.

## [0.26.0] — 2026-11-14 (Foldy Streaman)

### Fixed
- **Spotify tapes could play the wrong playlist.** When Spotify wasn't already running, Foldy opened the tape's playlist in Spotify but Spotify only showed it without starting it, so PLAY carried on with whatever Spotify played last. Foldy now tells Spotify to start the playlist, and hands it to Spotify's player again as soon as that player can be reached.
- Needs: notification access turned on for Foldy on that phone (Foldy asks for it on the front screen), so it can reach Spotify's player.

## [0.25.2] — 2026-11-14 (Foldy Streaman)

### Changed
- App icon: the tape deck and FOLDY are a little smaller, so the deck's corners no longer touch the edge of the icon.

## [0.25.1] — 2026-11-14 (Foldy Streaman)

### Changed
- **New app icon:** the Sport Yellow grey window with a tape in it, lying on its side on a yellow background, with FOLDY underneath.
- **Sport Yellow is now the theme a fresh install starts with.** If you've already picked a theme, yours stays as it is.

## [0.25.0] — 2026-11-14 (Foldy Streaman)

### Added
- **New theme: Sport Yellow** (Settings > Theme), after a late-80s sports player. Bright yellow body; a thick grey rubber window surround that runs off the edge of the case, with a very tight window on the tape; a raised radio-dial strip across the top with the FM band on it; FOLDY beside the window; STREAMAN over a big blue SPORT at the bottom.
- Its side strip is slimmer, with grey pill keys standing on end (PLAY and FF point up, REW points down) that stand proud of the case and sink in when pressed, round previous/next keys, and a yellow volume dial with black numbers.
- The inside, hinge, tape drawer and J-card buttons follow the new theme.

## [0.24.0] — 2026-11-14 (Foldy Streaman)

### Added
- **The battery light pulses green while the phone is charging** (cable or wireless pad), on every theme. Off the charger it goes back to being the running light.

### Changed
- The battery light and the word BATT are about twice as big. Red Chrome gets a battery light above its BATT label; Cherry Red's label now just says BATT so it fits at the new size.
- The headphone socket (and its plug and wire) is a little bigger.

## [0.23.1] — 2026-11-14 (Foldy Streaman)

### Fixed
- The buzz when a dragged tape passes another tape was too faint to feel; it is now a proper click, the same strength as a key press.

## [0.23.0] — 2026-11-14 (Foldy Streaman)

### Fixed
- **Album cover search said "Couldn't search just now".** The album database often answers "busy" on the first ask; the app now asks again by itself (up to four times) instead of giving up, and introduces itself in a way the database accepts.

### Added
- **A tick in your finger each time a tape you're dragging passes another tape** on the rack (when Haptics is on).

## [0.22.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Search album covers from the J-card.** Tap PHOTO (or the picture area) > "Search album covers", type an album or artist, and tap a cover to put it on the tape. No need to leave the app. Covers come from MusicBrainz and the Cover Art Archive (free, open music databases); only the words you type are sent.

### Changed
- **J-card buttons glow the moment you touch them** (edges and centre), then fade after you let go, so a quick tap shows the glow too.
- PHOTO now always opens a small menu: search covers, choose from the phone, and (when there is a picture) move & zoom or remove.

## [0.21.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Blank tape opens a blank J-card.** Tapping "Blank tape" puts a new tape on the rack and slides its J-card in, with a box on the card to paste the playlist link, and everything else (name, picture, font, colours) right there. Close it without a link and the blank is thrown away.
- **Settings > Dark J-cards:** dark textured paper, with the buttons in lighter versions of their colours.

### Changed
- **J-cards look like paper:** a speckled, fibrous texture and a small tear across the top-right corner.
- **J-card buttons are drawn onto the card** in the theme's colour (blue for Original and Silver & Blue, dark grey for Black & Gold, red for the red themes) and **glow** when pressed.
- **Skip buttons while not playing:** with the tape stopped, next/previous moves to that song but stays stopped until PLAY. With it paused, they act like STOP first (every key pops up), then move to the song.

## [0.20.0] — 2026-11-14 (Foldy Streaman)

### Added
- **Settings button at the bottom of the front-screen tape drawer**, opening the same Settings page.

### Changed
- **Key presses are snappier** (still smooth), and a key goes a little deeper while your finger is on it than where it rests when held down.
- **The front-screen tape drawer matches the theme**: the theme's body, lettering and highlight colours, with the tapes on a dark rack. Tapes with a picture show it there too.
- **Tapes in the drawer move smoothly**: they dip under your finger, and slide out and lift as a glide when loaded.

## [0.19.1] — 2026-11-14 (Foldy Streaman)

### Changed
- **Black & Gold:** the black knobs are a slightly lighter shade, so they stand out from the black ring round them.

## [0.19.0] — 2026-11-14 (Foldy Streaman)

### Changed
- **Keys on every theme are raised and press down into the casing.** Raised, the dark of the well shows below the key; pressed (or held down, like PLAY while a tape runs) the key slides down into it, its bottom tucks under the rim, the dark shows above it and a shadow falls across its top. The slanted Silver & Blue keys are flatter, with borders twice as thick. The Black & Gold knobs fill their ring when raised and shrink into it when pressed.
- **Pressing and releasing a key is a smooth movement.**
- **Cherry Red:** AUTO REVERSE is bigger and fills the width of the body.

### Fixed
- **STOP no longer leaves PAUSE down.** Pressing STOP while music plays pops every key up; only the PAUSE key pauses. After STOP, press PLAY to start again.

## [0.18.5] — 2026-11-14 (Foldy Streaman)

### Changed
- **Red Chrome:** AUTO REVERSE sits lower, clear of the tape window and centred in the black; STREAMAN and the song title start at the bottom, level with the black panel's lower corner.

## [0.18.4] — 2026-11-14 (Foldy Streaman)

### Changed
- **Red Chrome:** AUTO REVERSE is now a heavy Impact-style font, slanted, drawn as a white outline only.

## [0.18.3] — 2026-11-14 (Foldy Streaman)

### Changed
- **Red Chrome glass panel:** two big white wedges with a thin blue edge, above and below the tape window, like the printed panels on the original; AUTO REVERSE is bigger and runs along the lower wedge right by the tape; the small sticker and small triangles are gone; the window's right edge comes in, cutting off the outer part of the tape.

## [0.18.2] — 2026-11-14 (Foldy Streaman)

### Changed
- **Red Chrome has a round volume dial**: a black knurled dial lying flat, with white numbers 1 to 10 printed on its face. Turn it with a finger (clockwise is louder); the number at the marker gets bigger and glows.

## [0.18.1] — 2026-11-14 (Foldy Streaman)

### Changed
- **Red Chrome front:** STREAMAN runs up the left side with the song title beside it; FOLDY moved to the top right and the "unfold to change tapes" hint to the top left; the window in the black panel sits tight round the tape; a white and a black triangle are lined up with the sides of the tape's window.

## [0.18.0] — 2026-11-14 (Foldy Streaman)

### Added
- **New theme: Red Chrome**, after an early-80s auto-reverse player: metallic red body, a big black glass panel with a white "STEREO CASSETTE PLAYER" label and AUTO REVERSE lettering, a chrome band with a green light, FOLDY top left and a big white STREAMAN wordmark. Silver keys with a green PLAY, the volume thumbwheel, a chrome hinge inside.

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
