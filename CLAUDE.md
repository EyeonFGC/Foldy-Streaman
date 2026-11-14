# Foldy Streaman: notes for Claude

Personal fork of Foldman (Apache 2.0, keep NOTICE). Owner is not a developer: explain in plain words.

## Screen previews

`app/src/test/java/dev/foldman/ui/PreviewShots.kt` draws the screens on a computer
(Robolectric + Roborazzi): `./gradlew :app:testDebugUnitTest --tests dev.foldman.ui.PreviewShots`,
pictures land in `app/build/shots/`.

- Render as the **Galaxy Z Fold 8** by default (sizes from the owner's own screen recordings):
  - front (cover) screen: `w411dp-h650dp` (recording 1080 x 1708)
  - inside (unfolded) screen: `w832dp-h628dp` (recording 1432 x 1080, wider than tall)
- Only render the taller **Z Fold 8 Ultra** when asked.

## Building

- **Signing:** installed copies are signed with Foldy's own key (alias `androiddebugkey`, Android's
  standard debug password, valid to 2056, SHA-256 77:AA:F4:64...:FD:C2). GitHub builds use it from the
  Actions secret `FOLDY_KEYSTORE_B64`. A new cloud session makes its own different key, so **don't
  hand the owner an APK built locally in a new session** (it won't install over his copy): push and
  give him the GitHub Actions APK instead, or ask him for his saved `foldy-signing-key.txt` and decode it
  to `~/.android/debug.keystore` (`base64 -d`) before building. Never commit the key.

- Every push to `main` builds an APK on GitHub Actions (`.github/workflows/build-apk.yml`).
- Bump `appVersionName` / `appVersionCode` in `app/build.gradle.kts` and add a CHANGELOG entry
  with each change.
