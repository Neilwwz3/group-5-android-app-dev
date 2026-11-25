# MovieTracker

A local-first personal movie & TV tracking app for Android, built as the COMP7506 (Smartphone App Development) group project at The University of Hong Kong, 2026.

> The package name `hk.hkucs.group_6_android_app_dev` and the `Group6` prefix in theme and style names date from project creation. Renaming the package would make the installed app appear as a different app, so they were left unchanged. This is Group 5.

Track what you want to watch, are watching, and have watched; rate titles from 0 to 10; log each viewing session with duration and a short review; and browse your whole year in a diary-style heatmap. All data lives on your device — no account, no server.

![App screenshot](docs/design/app-running.png)

## Features

- **Library** — add, edit, search and filter movies and TV shows by watch status (Want to Watch / Watching / Watched / Dropped).
- **Online lookup (TMDB)** — search titles on TMDB when adding a title; title, year, genres, directors, cast, synopsis, runtime, episode count and poster are filled in automatically. Duplicates are detected and blocked. Works fully offline with manual entry as a fallback.
- **Manual poster picking** — choose any image from the device as a poster, or clear it.
- **Viewing diary** — a full-year activity heatmap plus a timeline of entries grouped by date; tap a day to jump to it.
- **Home & Profile** — continue-watching list, recent entries, monthly overview, collection stats and genre breakdown.
- **Sample data** — one tap in Profile loads a curated 30-title sample library with a year of viewing history, or clears everything.

## Tech stack

| | |
| --- | --- |
| Language | Java 11 (no Kotlin sources) |
| UI | XML layouts, Material Components (Material 3), AppCompat, ConstraintLayout |
| Architecture | MVVM with ViewModel + LiveData; a single Repository as the data entry point |
| Storage | Room (SQLite), local-only |
| Build | Gradle wrapper (Kotlin DSL), Android Gradle Plugin; versions catalogued in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) |
| SDK | minSdk 24 · targetSdk 37 · compileSdk 37 |

## Building and running

### Requirements

- Android Studio Quail 4 (2026.1.4) or newer — the project uses AGP 9.4, and older releases cannot sync it ([compatibility table](https://developer.android.com/build/releases/about-agp))
- JDK 17+ (Android Studio's bundled JDK works)
- Android SDK 37 with build tools (Android Studio downloads these on first sync)

### Android Studio

1. Clone the repository:
   ```bash
   git clone https://github.com/Neilwwz3/group-5-android-app-dev.git
   ```
2. Open the folder via **File → Open** and wait for Gradle sync to finish. Android Studio generates `local.properties` with your SDK path — never commit it.
3. Pick an emulator or a connected device and press **Run**.

### Command line

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk

./gradlew installDebug         # install onto a running device/emulator
./gradlew test                 # unit tests
./gradlew connectedDebugAndroidTest   # instrumentation tests
```

### TMDB online lookup

Online title lookup works without any setup. For the COMP7506 review, `app/build.gradle.kts` includes a reserved TMDB API key, so a fresh clone builds with online lookup enabled. The key will be revoked after the review. After that, online lookup needs your own key (see below); everything else in the app works without one.

To try it (needs an internet connection):

1. Open **Library** and tap **Add a title** below the list.
2. On the form, tap **Search titles online (TMDB)**.
3. Enter a title, e.g. `dune`, tap **Search**, then tap a result.
4. Title, year, type, genres, directors, cast, synopsis, runtime, episode count and poster are filled in. Check them and tap **Save**.

Picking a title that is already in your library shows "This title is already in your library." and adds nothing. Without a connection, the app says search needs a network connection, and you can still fill in the form by hand.

To use your own key instead, get a free v3 API key from [themoviedb.org](https://www.themoviedb.org/settings/api) and add one line to your **local** `local.properties` (already gitignored). It takes precedence over the reserved key:

```properties
tmdbApiKey=your_key_here
```

Then sync Gradle and run. The key is injected into `BuildConfig` at build time.

## Architecture

### Storage: Room

| Data | Where it lives |
| --- | --- |
| Titles and viewing records | **Room** (androidx.room 2.8.x, on SQLite) |
| Sample posters | Bundled in `assets/sample/posters/`; the database stores the file name |
| Posters downloaded from TMDB or picked from the device | Copied into the app's private `files/posters/` directory; the database stores the absolute path |

Room was chosen because it is an official Jetpack component (not a third-party library), works offline, validates SQL at compile time, returns `LiveData` from DAOs so the UI refreshes automatically, and supports Java through `annotationProcessor`. Hand-written `SQLiteOpenHelper`, Realm / ObjectBox and DataStore were rejected for boilerplate, unnecessary dependency, and Kotlin-coroutine orientation respectively. Room stays on **2.x**; Room 3 targets Kotlin/KSP.

Key dependencies: `androidx.room:room-runtime` + `room-compiler` (2.8.5), `lifecycle-viewmodel` / `lifecycle-livedata` (2.11.0), `androidx.fragment:fragment` (1.9.1). Charts are **not** drawn with a chart library — the heatmap, donut chart and bar chart are custom views.

#### Schema

**`media_item` — one movie or show**

| Column | Type | Notes |
| --- | --- | --- |
| `id` | TEXT primary key | `<slug>-<year>` for sample data, UUID for manual entries, `tmdb_<id>` for TMDB imports |
| `title` / `original_title` | TEXT | Display title / original title |
| `year` | INTEGER | Release year, nullable |
| `type` | TEXT | `movie` / `series` |
| `genres` / `directors` / `cast` / `tags` | TEXT | String lists stored as one column via a TypeConverter |
| `overview` | TEXT | Synopsis |
| `runtime_minutes` | INTEGER | Movie runtime / episode runtime |
| `total_episodes` | INTEGER | Total episodes for series; null for movies |
| `poster` | TEXT | Sample-poster file name, or absolute path of a poster file in app-private storage; nullable |
| `status` | TEXT | `want` / `watching` / `watched` / `dropped` |
| `rating` | REAL | Current rating 0–10, nullable |
| `source` | TEXT | `manual` / `sample` / `tmdb` |
| `tmdb_id` / `imdb_id` | TEXT | External ids, nullable; used for de-duplication |
| `created_at` / `updated_at` | INTEGER | Millisecond timestamps |

**`watch_record` — one viewing session**

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER autoincrement | |
| `media_id` | TEXT | Foreign key → `media_item.id`; cascades on delete; indexed |
| `date` | TEXT | `YYYY-MM-DD`, convenient for month grouping |
| `episodes` | TEXT | Episodes watched in this session (integer list); empty for movies |
| `minutes` | INTEGER | Minutes watched |
| `rating` | REAL | Rating for this session, nullable |
| `note` | TEXT | One-line review |
| `created_at` | INTEGER | Millisecond timestamp |

List fields are deliberately not normalised into separate tables: the dataset is a few hundred rows at most, so genre aggregation is done in Java and fewer tables mean fewer mistakes.

#### Code layout

```
data/db/
  AppDatabase.java          singleton, version = 1, exportSchema = false
  Converters.java           List<String> / List<Integer> ↔ TEXT
  entity/MediaItem.java
  entity/WatchRecord.java
  dao/MediaDao.java         CRUD, queries by status/type, title search, returns LiveData
  dao/WatchRecordDao.java   records per title, per period, monthly totals
data/repository/
  MediaRepository.java      the only data entry point for the UI; writes on a background thread
  SampleDataLoader.java     loads assets/sample/library.json
```

Rules:

- Only maintainers change `data/db/`. New fields or queries go through them.
- Fragments never touch DAOs directly — always ViewModel → Repository.
- All database writes use the Repository's background executor; `allowMainThreadQueries()` is not allowed.
- `fallbackToDestructiveMigration` is acceptable during development because sample data can be reloaded in one tap; revisit before a formal release.

### Screens

The app has four bottom-navigation tabs, each a Fragment switched manually by `MainActivity` (no Navigation component). Secondary screens are separate Activities opened through static `newIntent(...)` factories, so Back naturally returns to the originating tab and every screen owns its own code. All Activities are registered once in `AndroidManifest.xml`.

```
MainActivity (bottom navigation)
├── Home     HomeFragment     ──→ Title detail
├── Library  LibraryFragment  ──→ Title detail ──→ Add / edit title
│                                              └──→ Log a watch
├── Diary    DiaryFragment    ──→ Title detail
└── Profile  ProfileFragment (stats + settings)
```

#### Home `ui/home/`

- Continue watching: titles with status "watching", horizontal cards
- Recent entries: the last five viewing records
- Monthly overview: entries and hours this month
- Empty state pointing to Profile to load sample data

#### Library `ui/library/`

- Search field (title / original title)
- Filter: All / Want to Watch / Watching / Watched / Dropped
- Cards: poster, title, "year · type · genres", star rating, status badge
- "Add a title" button below the list
- Tapping a card opens the detail screen

#### Diary `ui/diary/`

- Top: a one-year viewing heatmap, one cell per day, darker = more records; tapping a cell jumps the timeline to that day
- Bottom: a timeline of records grouped by date, newest first

#### Profile `ui/profile/`

- Stats: totals (titles, watched, total time, average rating) and a genre donut chart
- Settings: load sample data, clear all data (both confirmed), About
- Monthly bar chart

### Secondary screens

| Screen | Package | Purpose |
| --- | --- | --- |
| Title detail | `ui/detail/` | Info, status switch, rating, viewing records, edit, delete, log a watch |
| Add / edit title | `ui/edit/` | One screen: no id means add, an id means edit |
| Log a watch | `ui/record/` | Date, duration, session rating, short review; saves and returns |
| TMDB search | `ui/search/` | Online lookup; returns the picked hit to the Add/Edit form |

### Shared components `ui/components/`

| Component | Used by |
| --- | --- |
| `HeatmapView`: one-year viewing heatmap | Diary |
| `DonutChartView`: genre donut | Profile |
| `MonthlyBarChartView`: monthly totals | Profile |

Ratings have no custom view: the detail, add/edit and log-a-watch screens take a number from 0 to 10 with one decimal in a text field, and library cards show it as five read-only stars (a standard `RatingBar`, half-star steps) next to the score.

### File naming

| Screen | Class | Layout | ViewModel |
| --- | --- | --- | --- |
| Home | `HomeFragment` | `fragment_home.xml` | `HomeViewModel` |
| Library | `LibraryFragment` | `fragment_library.xml`, `item_media.xml` | `LibraryViewModel` |
| Diary | `DiaryFragment` | `fragment_diary.xml`, `item_diary_record.xml` | `DiaryViewModel` |
| Profile | `ProfileFragment` | `fragment_profile.xml` | `ProfileViewModel` |
| Detail | `MediaDetailActivity` | `activity_media_detail.xml`, `item_record.xml` | `MediaDetailViewModel` |
| Add / edit | `EditMediaActivity` | `activity_edit_media.xml` | `EditMediaViewModel` |
| Log a watch | `AddRecordActivity` | `activity_add_record.xml` | `AddRecordViewModel` |
| TMDB search | `TmdbSearchActivity` | `activity_tmdb_search.xml`, `item_tmdb_result.xml` | `TmdbSearchViewModel` |

Out of scope by decision: a friends/social feature and Jellyfin import.

## Data API

Screens read and write data only through `MediaRepository`. The exact method names below are the contract between the UI and the data layer.

ViewModels extend `AndroidViewModel` and obtain the repository in their constructor:

```java
MediaRepository repo = MediaRepository.get(application);
```

- Methods starting with `observe` return `LiveData`; the UI refreshes automatically when data changes.
- Writes run on a background thread and return immediately. Pass a callback if you need completion; callbacks run on the main thread and may update the UI directly.

### Constants

| Class | Value | Displayed as |
| --- | --- | --- |
| `MediaStatus.WANT` | `"want"` | Want to Watch |
| `MediaStatus.WATCHING` | `"watching"` | Watching |
| `MediaStatus.WATCHED` | `"watched"` | Watched |
| `MediaStatus.DROPPED` | `"dropped"` | Dropped |
| `MediaType.MOVIE` | `"movie"` | Movie |
| `MediaType.SERIES` | `"series"` | TV Show |
| `Genres.ALL` | Drama, Comedy, Action, Sci-Fi, Animation, Mystery, Crime, Romance, Horror, Documentary, Fantasy, War | Genres may only be chosen from this list and are stored as these English words |

All UI text is English and lives in `strings.xml` (for example `status_want`).

### Data classes

**`MediaItem`**

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `String` | Leave empty on insert; generated on save |
| `title` / `originalTitle` | `String` | Title required (English display name) / original title nullable |
| `year` | `Integer` | Nullable |
| `type` | `String` | One of `MediaType` |
| `genres` / `directors` / `cast` / `tags` | `List<String>` | Never null; empty list when absent |
| `overview` | `String` | Synopsis |
| `runtimeMinutes` | `Integer` | Movie or episode runtime, nullable |
| `totalEpisodes` | `Integer` | Null for movies |
| `poster` | `String` | Sample-poster file name or absolute file path, nullable; the UI shows a placeholder when empty |
| `status` | `String` | One of `MediaStatus` |
| `rating` | `Float` | 0–10 with one decimal; null means not rated |

**`WatchRecord`**

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `long` | Generated |
| `mediaId` | `String` | Owning title |
| `date` | `String` | `YYYY-MM-DD` |
| `episodes` | `List<Integer>` | Episodes watched in this session; empty list for movies |
| `minutes` | `Integer` | Minutes watched, nullable |
| `rating` | `Float` | Session rating, nullable |
| `note` | `String` | One-line review, may be an empty string |

**`RecordWithMedia`** — a record plus its title's `mediaTitle`, `mediaPoster` and `mediaType`.
**`StatsSummary`** — `mediaCount`, `watchedCount`, `totalMinutes`, `averageRating` (nullable `Float`), `recordCount`.
**`GenreCount`** — `genre`, `count`.

### MediaRepository methods

**Titles**

| Method | Returns | Used by |
| --- | --- | --- |
| `observeAllMedia()` | `LiveData<List<MediaItem>>`, most recently updated first | Library (search, filters and sorting happen in the ViewModel) |
| `observeMedia(String id)` | `LiveData<MediaItem>`; becomes null after deletion | Detail, add/edit, log a watch |
| `observeMediaByStatus(String status)` | `LiveData<List<MediaItem>>` | Home — continue watching |
| `saveMedia(MediaItem item, Consumer<String> onSaved)` | Inserts or updates; the callback receives the id and may be null | Add / edit |
| `updateStatus(String id, String status)` | — | Detail |
| `updateRating(String id, Float rating)` | —; rating may be null | Detail |
| `deleteMedia(String id, Runnable onDone)` | Deletes the title together with its records | Detail |

**Viewing records**

| Method | Returns | Used by |
| --- | --- | --- |
| `observeRecords(String mediaId)` | `LiveData<List<WatchRecord>>`, newest first | Detail |
| `observeRecentRecords(int limit)` | `LiveData<List<RecordWithMedia>>` | Home — recent entries |
| `observeTimeline()` | `LiveData<List<RecordWithMedia>>`, all records, newest first | Diary timeline |
| `observeDailyCounts(String fromDate, String toDate)` | `LiveData<Map<String, Integer>>`: date → record count; days without records are absent | Diary heatmap |
| `addRecord(WatchRecord record, Runnable onDone)` | Saves a record. If the title was "want": movies become "watched" and series become "watching". If the title has no rating and this record has one, the title takes the record's rating | Log a watch |
| `deleteRecord(long recordId, Runnable onDone)` | Deletes a record; title status is unchanged | Detail |
| `restoreRecord(WatchRecord record, Runnable onDone)` | Undo delete: re-inserts the record under its original id without changing title status | Detail |

**Statistics**

| Method | Returns | Used by |
| --- | --- | --- |
| `observeSummary()` | `LiveData<StatsSummary>` over all data | Profile |
| `observeMonthSummary(String yearMonth)` | `LiveData<StatsSummary>` for e.g. `"2026-11"` | Home — monthly overview |
| `observeGenreCounts()` | `LiveData<List<GenreCount>>`, largest first | Profile — genre donut |
| `observeMonthlyTotals(String fromDate)` | `LiveData<List<MonthTotal>>`: per-month `count` and `minutes`, `month` like `"2026-11"`, oldest first | Profile — monthly bars |

**Sample data**

| Method | Effect |
| --- | --- |
| `SampleDataLoader.load(Context context, Runnable onDone)` | Clears existing data, then loads `assets/sample/library.json`, including `tmdbId` / `imdbId` |
| `SampleDataLoader.load(Context context, Runnable onDone, Consumer<Exception> onError)` | Same, but calls `onError` (leaving data untouched) when the file is missing or malformed; the settings screen should use this version |
| `MediaRepository.get(context).clearAll(Runnable onDone)` | Clears all titles and records |

## Sample data

File: `app/src/main/assets/sample/library.json` (`version: 1`). The dataset simulates one year (2025-12 to 2026-11) of viewing history for a single user and is used for demos and manual testing. Titles, credits, runtimes and episode counts were checked against TMDB on 2026-11-13, and every `poster` under `app/src/main/assets/sample/posters/` is the title's official TMDB poster. Title details, overviews and posters are generated in one run by a local pull script kept out of the repository (it uses a personal TMDB key); the viewing history is hand-written. TMDB is credited in the app's About dialog, as its terms require.

### Totals

| Category | Count |
| --- | --- |
| Movies | 20 |
| Series | 10 |
| Total | 30 |

### Status distribution

| Status | Count |
| --- | --- |
| watched | 14 |
| want | 8 |
| watching | 5 |
| dropped | 3 |

### Coverage

11 genres are used: Drama, Sci-Fi, Action, Mystery, Crime, Romance, Animation, Comedy, Fantasy, War, Horror.

- 3 titles rated 9.5 or above: The Shawshank Redemption 9.7, Farewell My Concubine 9.6, Interstellar 9.5.
- 3 titles below 5: Avengers: Endgame 4.0, Ready Player One 4.5, The Big Bang Theory 4.5 (all dropped).
- Most ratings fall in the 6–9 range; the 8 "want" titles are unscored.

### Viewing records

- Dates span **2025-12 to 2026-11**, with at least one record in each month.
- 63 records in total: 2025-12(1), 2026-01(11), 2026-02(6), 2026-03(4), 2026-04(5), 2026-05(7), 2026-06(4), 2026-07(3), 2026-08(4), 2026-09(5), 2026-10(2), 2026-11(11).
- **Multi-record days** make the heatmap show every shade: 2026-02-22 has 4 records (darkest), 2026-01-11 and 2026-11-07 have 3, and 2026-01-10, 2026-05-28, 2026-09-25 and 2026-11-08 have 2.
- **Streak**: 2026-01-05 to 2026-01-12 — 8 consecutive days (bingeing Game of Thrones).
- **Rewatches with changed ratings**: 18 titles have two or more records with different ratings (e.g. Interstellar 9.0 → 9.5, Inception 8.5 → 9.0, Parasite 8.5 → 8.8, Spirited Away 8.8 → 9.0, The Shawshank Redemption 9.5 → 9.7).

## Further documentation

- [`docs/design/design-system.md`](docs/design/design-system.md) — colors, typography, spacing, component previews.
- [`docs/research/competitors.md`](docs/research/competitors.md) — competitor analysis (Letterboxd, IMDb, Douban).

All user-facing text in the app is English; strings live in `res/values/strings.xml`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the branch/PR workflow and code conventions. All pull requests target the `dev` branch and require a green CI build plus one approving teammate.

## Acknowledgments

- Data and artwork provided by [The Movie Database (TMDB)](https://www.themoviedb.org). This product uses the TMDB API but is not endorsed or certified by TMDB.

## License

[MIT](LICENSE)
