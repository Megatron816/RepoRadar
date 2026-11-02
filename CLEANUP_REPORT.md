# Cleanup report

What changed between the uploaded `GitHubProfileExplorer-completed.zip` and this project.

## Folder structure

| Before | After |
|---|---|
| Two nested projects: an outer root with its own `settings.gradle.kts` pointing into an inner `GitHubProfileExplorer/` that had *its own* `settings.gradle.kts`, `build.gradle.kts`, `gradle/` and `gradlew` | **One** project root. `app/` is a direct child of the root |
| 34 Kotlin files in one flat package | Layered packages: `data/`, `domain/`, `ui/<feature>/`, `di/` (see README) |
| Tests in a single `ProductLogicTest.kt` | One test class per unit, in the matching package |

## Generated files removed (~200 MB, 2,000+ files)

These are rebuilt automatically by Gradle / Android Studio and must not be shared or committed.

| Path | Size | Why it was there |
|---|---|---|
| `GitHubProfileExplorer/app/build/` | 176 MB | Compiled classes, dex, APK intermediates |
| `GitHubProfileExplorer/.gradle/` | 15 MB | Gradle cache of the inner project |
| `GitHubProfileExplorer/build/` | 7.8 MB | Root build output |
| `.gradle/` (outer) | 3.1 MB | Gradle cache of the outer project |
| `build/` (outer) | 0.5 MB | Root build output |
| `.idea/` (both) | 0.4 MB | Machine-specific IDE state |
| `.kotlin/` | 8 KB | Kotlin compiler session data |
| `local.properties` (both) | tiny | Contains *your* SDK path; Android Studio recreates it |

## Source files removed or replaced

| File | Reason |
|---|---|
| `HttpProfileSummaryService.kt`, `ProfileSummaryService.kt` | Backend-based "AI summary". Removed together with the backend (see below) |
| `GitHubRepoPagingSource.kt` and the Paging 3 dependencies | It re-downloaded the same repositories that `ProfileRepository` already fetched, doubling API usage against GitHub's 60 requests/hour limit. The repository list now uses the data that is already loaded |
| `Logger.kt` | Sat in the project root outside any source set, so it was never compiled, and nothing used it |
| `ExampleInstrumentedTest.kt` | Template placeholder; replaced by a real Room DAO test |
| `ProductLogicTest.kt` | Split into focused test classes |
| `app/src/main/keepRules/rules.keep` | Template file containing only comments |
| `app/.gitignore` | Redundant; the root `.gitignore` covers it |
| `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` | Template files containing only sample comments. Backup is now disabled (`allowBackup="false"`), which also stops the encrypted token and the download history from being restored on a device where they would be invalid |
| `SETUP.md` | Merged into `README.md` (and corrected, see below) |
| `ProfileSummaryCard.kt`, `ContributionsCard.kt`, big `ProfileScreen.kt` | Rewritten as separate screens, one per sidebar entry |

## Dependencies removed

`paging-runtime` and `paging-compose`, `ui-graphics` (already pulled in by `ui`), `ui-tooling` and `ui-tooling-preview` (no `@Preview` exists), `ui-test-junit4`, `ui-test-manifest`, `espresso-core` (no Compose/Espresso tests exist), the second `compose-bom` for `androidTest`, and the `unitTests.isIncludeAndroidResources` option (no Robolectric tests). None were used by any code.

## Bugs found and fixed while restructuring

* **OAuth sign-in could not work.** A shared interceptor forced `Accept: application/vnd.github+json` on *every* request, including GitHub's OAuth endpoints, which only return JSON for `Accept: application/json` (otherwise form-encoded, which Gson cannot parse). The interceptor now only sets a default, and the bearer token is only sent to `api.github.com`.
* **Client ID was never read from `local.properties`.** `SETUP.md` said to put it there, but the build only looked at Gradle properties. Both are now supported.
* **Contribution heatmap drew cells at random rows** (the row came from `date.hashCode()`). It now uses the real weekday and GitHub's own contribution levels.
* **Repositories were fetched twice** per search (see Paging above).
* **Deep links re-ran the search on every screen rotation.** They are now handled once.
* **Repository health reasons were hard-coded English** in the domain layer; they are now enums mapped to string resources so the Hindi translation applies.

## Reusable cleanup

`scripts/clean.ps1` (Windows) and `scripts/clean.sh` (macOS/Linux) delete the generated folders above.
Run with `-DryRun` / `--dry-run` first to see what would be removed.
