# GitHub Profile Explorer

An Android app (Kotlin, Jetpack Compose, Material 3) for exploring GitHub developers: profile, repositories,
language mix, contribution heatmap, side-by-side comparison, and ZIP downloads with a persistent history.

Data comes **only from GitHub's APIs** and a local **Room** database. There is no custom backend.

## Features (all reachable from the sidebar)

| Sidebar entry | What it does |
|---|---|
| **Profile** | Search a username (or open a `github.com/<user>` link). Shows the profile card, an on-device **profile summary**, and the top repositories |
| **Repositories** | Filter and sort (updated / stars / name) every repository, with a health badge. Tap one for details, README, share and download |
| **Languages** | Donut chart and breakdown of the user's languages |
| **Contributions** | GitHub-style heatmap with total, longest and current streak (needs sign-in) |
| **Compare** | Two developers side by side (followers, stars, health, languages, contributions) |
| **Downloads** | History of downloaded repository ZIPs (Room). Open, retry, delete, clear |
| **Reading queue** | Save repositories locally and track them as to read, exploring, or done |
| **Recently viewed** | Local profile browsing history with on-demand refresh |
| **Account** | Sign in with GitHub (device flow), sign out, API requests left |

On tablets and foldables (width ≥ 840 dp) the sidebar is permanent; on phones it is a drawer.

### About the profile summary

The summary is generated **on your device** from public repository data (languages, stars, topics, recent pushes,
licenses). It is rule-based, not a language model, and needs no network call beyond the GitHub requests the
app already makes. Calling a real AI model would require either a backend or an API key stored in the app,
which this project deliberately avoids.

### Downloading repositories

*Download ZIP* fetches the default branch through `GET /repos/{owner}/{repo}/zipball/{branch}` and saves it to
`Downloads/GitHubProfileExplorer/` using MediaStore (no storage permission needed). Each download is recorded in
Room with status, size, time and any error. Downloads continue while you move around the app, but not if
Android kills the app process; such entries are marked *interrupted* on the next launch and can be retried.
Deleting an entry also deletes its ZIP.

## Setup

1. Open the project root (the folder containing `settings.gradle.kts`) in Android Studio. It will create `local.properties` with your SDK path.
2. **Optional, for sign-in:** create a GitHub OAuth App (Settings → Developer settings → OAuth Apps, tick *Enable Device Flow*) and add its client ID to `local.properties`:

   ```properties
   githubOAuthClientId=Iv1.xxxxxxxxxxxxxxxx
   ```

   Without it everything works except Contributions and the higher rate limit. The Account screen explains this.
3. Run the `app` configuration (minSdk 29).

Command line: `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, and `./gradlew connectedDebugAndroidTest` (Room DAO test, needs a device/emulator).

## Project structure

```
app/src/main/java/com/example/githubprofileexplorer/
├── GitHubExplorerApp.kt, MainActivity.kt
├── di/            AppContainer (manual DI), AppViewModelFactory
├── data/
│   ├── model/       GitHub JSON models
│   ├── remote/      Retrofit APIs, NetworkModule (HTTP cache, headers), rate-limit monitor
│   ├── auth/        OAuth device flow, Keystore-backed token store
│   ├── local/        Room: AppDatabase, downloads, reading queue, profile history
│   └── repository/  Profile, Readme, Contribution, Download repositories
├── domain/        Pure logic: health score, language stats, streaks, profile insights, markdown parser
└── ui/
    ├── AppRoot.kt, Destination.kt   sidebar + navigation
    ├── components/                  shared widgets, icons, markdown renderer
    └── profile/ repos/ languages/ contributions/ compare/ downloads/ account/
```

## Notes and limitations

* GitHub allows 60 unauthenticated requests per hour; responses are cached so unchanged data (HTTP 304) does not count. Signing in raises the limit to 5,000.
* Up to 500 repositories per user are loaded.
* `https://github.com/...` links open in the app only after you enable it under *Settings → Apps → Default apps → Open supported links*, because GitHub cannot verify the app as the domain owner (Android 12+).
* Private repositories are not downloadable; the sign-in scope is `read:user`.
* The README viewer shows text, headings, lists, code and links. Images, HTML layout and badges are skipped.

## Housekeeping

`scripts/clean.ps1` / `scripts/clean.sh` delete build output and caches (`-DryRun` / `--dry-run` previews). See `CLEANUP_REPORT.md` for what changed from the previous version.
