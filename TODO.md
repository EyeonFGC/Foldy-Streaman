# Foldy Streaman: to do

## Fixed signing key (so every build installs as an update)

Your key file is `foldy-signing-key.txt` (Claude sent it to you on 2026-11-14). It is your app's
signing key as text. **Keep it private** (no Discord, no sharing): anyone with it could make an
update that installs over your app.

1. [ ] Save a copy of `foldy-signing-key.txt` somewhere private (e.g. your own Google Drive).
2. [ ] On a computer, open github.com/EyeonFGC/Foldy-Streaman → **Settings** → **Secrets and
       variables** → **Actions** → **New repository secret**.
       - Name: `FOLDY_KEYSTORE_B64` (exactly like that)
       - Secret: paste **all** of the text in `foldy-signing-key.txt`
       - **Add secret**
3. [ ] Tell Claude it's done: it will re-run the GitHub build and check the APK's key matches the
       one on your phone.

After that, any APK from the GitHub **Actions** page installs straight over your app.
