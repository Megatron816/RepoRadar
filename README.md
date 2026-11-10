# GitHub Profile Explorer

An Android app (Kotlin, Jetpack Compose, Material 3) for exploring GitHub developers: profile, repositories,
language mix, contribution heatmap, side-by-side comparison, and ZIP downloads with a persistent history.

Data comes from GitHub's APIs, Google's Gemini API, and a local **Room** database. There is no custom backend.

## Candidate portfolios

Candidate Portfolio is an evidence view of a public GitHub profile. It reuses profile search, loads
public repositories page by page, and provides local filters for language, topic, minimum stars,
recency, archived status, and forks. Aggregates use only reported repository languages and topics,
label missing values, link facts to GitHub repositories, and show when data was fetched. Repository
popularity and activity are context only: this is not a skill score or hiring recommendation.

The feature uses `GET /users/{username}` and paginated `GET /users/{username}/repos` without requiring
login. Room stores only saved public profile metadata and repositories selected for a local portfolio;
it stores no OAuth tokens, private data, or local notes. Sharing uses the Android Sharesheet with a PDF generated in the app cache. The PDF includes the loaded
profile and only the repositories selected locally, stable GitHub IDs when available, public source URLs,
timestamps, and explicit missing/stale labels. It contains no local notes, private data, or OAuth tokens.
Select projects in Candidate portfolio, then tap **Share portfolio**. Temporary PDFs are stored under
`cache/portfolio-pdfs/` and files older than 24 hours are cleaned up before a new export; the shared
content URI remains readable by the receiving app. A `FileProvider` is declared for that cache directory.
Native Android `PdfDocument` renders URLs as visible source text; it does not add PDF hyperlink annotations.
There is no universally accessible portfolio URL because the app has no public portfolio backend.

## Features (all reachable from the sidebar)

| Sidebar entry | What it does |
|---|---|
| **Profile** | Search a username (or open a `github.com/<user>` link). Shows the profile card, an on-device **profile summary**, and the top repositories |
| **Gemini AI** | Send a text prompt to Gemini Flash and display the generated response |
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
app already makes. The Gemini screen uses the direct REST API without a custom backend. Configure `geminiApiKey` in
`local.properties` before building. This is a prototype integration: `BuildConfig` values are packaged
into the APK and do not protect the key from APK extraction. Use a backend or another server-side
secret-management approach for production.

### Downloading repositories

*Download ZIP* fetches the default branch through `GET /repos/{owner}/{repo}/zipball/{branch}` and saves it to
`Downloads/GitHubProfileExplorer/` using MediaStore (no storage permission needed). Each download is recorded in
Room with status, size, time and any error. Downloads continue while you move around the app, but not if
Android kills the app process; such entries are marked *interrupted* on the next launch and can be retried.
Deleting an entry also deletes its ZIP.

## Repository architecture explorer

From a public repository detail screen, **Explore architecture** scans the selected branch on-device.
The scanner uses the existing Retrofit/OkHttp stack and GitHub REST endpoints for repository metadata,
the branch tree, file contents, and recent commits; public scans do not require login. Each saved
snapshot records the repository ID, branch, commit SHA, scan time, graph nodes/edges, parser warnings,
and factual repository signals in Room. Saved scans can be reopened or deleted.

The architecture scanner uses isolated static parser adapters. The Gradle adapter supports `settings.gradle(.kts)`, root and module `build.gradle(.kts)`,
`gradle/libs.versions.toml`, `include ':module'`/`include(":module")`, `project(":module")`,
literal Maven coordinates, common configurations (`implementation`, `api`, `compileOnly`,
`runtimeOnly`, `testImplementation`, and `androidTestImplementation`), and resolvable
`libs.<alias>` catalog entries. It never executes Gradle code. Variables, dynamic versions,
convention plugins, generated modules, unsupported catalog indirection, malformed declarations,
and truncated trees remain unresolved or produce warnings rather than guessed relationships.

The initial additional adapters support `package.json` dependency sections (Node/TypeScript),
`requirements*.txt` (Python), basic `Cargo.toml` declarations (Rust), `go.mod` declarations
(Go), and `composer.json` require sections (PHP). Lock files and every recognized format without
a safe adapter are detected and shown as unsupported; they are never silently treated as parsed.
The adapter contract is extensible, deterministic, and never executes repository code. A finite
static parser cannot understand every language, build system, generated file, profile, plugin, or
dynamic expression.

Graphs can contain multiple ecosystems and project roots. Nodes retain declared and resolved
versions separately, dependency scope, source manifest, ecosystem, and project root. The current
lock-file adapters are intentionally not enabled until their resolution semantics are covered by
fixtures; a declaration is not evidence that a package is used at runtime or is safe/current.
Snapshots migrated from Room v4 to v5 retain all existing Gradle nodes and add completeness,
manifest coverage, ecosystem, project-root, resolved-version, and source-kind metadata.

| Ecosystem | Parsed now | Detected but not parsed yet |
|---|---|---|
| Gradle / Android / Kotlin | `settings.gradle[.kts]`, `build.gradle[.kts]`, `gradle/libs.versions.toml` static subset | wrapper properties and unsupported dynamic/catalog logic |
| Java / JVM | — | `pom.xml`, Maven profiles/parents, JVM build declarations not covered by the Gradle adapter |
| JavaScript / TypeScript | `package.json` dependency sections/workspaces; `package-lock.json` and `npm-shrinkwrap.json` package versions | `yarn.lock`, `pnpm-lock.yaml` |
| Python | `requirements.txt`, `requirements-*.txt` simple pinned/ranged lines | `pyproject.toml`, Pipenv/Poetry/uv lock semantics and includes |
| Rust / Go | basic `Cargo.toml` key declarations; `go.mod` require lines | `Cargo.lock`, `go.work`, `go.sum` resolution |
| PHP | `composer.json` `require`/`require-dev` | `composer.lock` resolution |
| .NET | `.csproj`/`.fsproj` `PackageReference` declarations | `packages.lock.json`, central package properties |
| Ruby, Swift, Dart, Elixir, Scala, C/C++ | simple static declaration subsets for listed primary manifests | lock-file resolution and dynamic DSL syntax |

This matrix is intentionally conservative: detection is not parsing, and unsupported or ambiguous
files make scan coverage partial instead of being omitted from the graph.

Scans are bounded to 40 likely manifest/configuration files and 100 KB per fetched file. GitHub tree
truncation, failed file reads, and scan limits are shown as partial analysis. The graph is deterministic
and includes a readable outline, module/external dependency filtering, pan/zoom, cycle/shared/isolated
analysis, and source manifest paths. Repository signals (last push, open issues, archived/fork status,
license, and recent commit paths) are observations linked to GitHub data, not health, security,
quality, abandonment, or developer-skill claims. Static declarations and activity signals cannot
establish that a dependency is safe or current.

## Setup

1. Open the project root (the folder containing `settings.gradle.kts`) in Android Studio. It will create `local.properties` with your SDK path.
2. **Optional, for sign-in:** create a GitHub OAuth App (Settings → Developer settings → OAuth Apps, tick *Enable Device Flow*) and add its client ID to `local.properties`:

   ```properties
   githubOAuthClientId=Iv1.xxxxxxxxxxxxxxxx
   ```

   Without it everything works except Contributions and the higher rate limit. The Account screen explains this.
3. **Optional, for Gemini AI:** create a Gemini API key at
   [Google AI Studio API keys](https://aistudio.google.com/apikey) and add it to the same
   untracked `local.properties` file:

   ```properties
   geminiApiKey=AIza...
   ```

   Rebuild after changing the key. If it is absent, the rest of the app works and the Gemini screen
   reports that configuration is missing.
4. Run the `app` configuration (minSdk 29).

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
* Up to 500 repositories per user are loaded. Architecture scans inspect at most 40 recognized
manifest/lock files, fetch at most 100 KB per file, and mark recursive-tree truncation, fetch
failures, unsupported manifests, and scan limits as partial coverage. Build output, vendored
dependencies, and binaries should not be interpreted as project declarations; the current tree
filter is bounded and does not download repository archives.
* `https://github.com/...` links open in the app only after you enable it under *Settings → Apps → Default apps → Open supported links*, because GitHub cannot verify the app as the domain owner (Android 12+).
* Private repositories are not downloadable; the sign-in scope is `read:user`.
* The README viewer shows text, headings, lists, code and links. Images, HTML layout and badges are skipped.

## Housekeeping

`scripts/clean.ps1` / `scripts/clean.sh` delete build output and caches (`-DryRun` / `--dry-run` previews). See `CLEANUP_REPORT.md` for what changed from the previous version.
