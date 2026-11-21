# Competitor Analysis

Three apps were reviewed hands-on on 2026-11-06 to decide what MovieTracker should and should not do. This analysis shaped the feature set in the README.

## 1. Douban

### Product information

| Item | Content |
| --- | --- |
| App name | Douban |
| Developer | Beijing Douwang Technology Company Limited |
| App version | 7.137.0 |
| iOS version | 26.6.1 |
| Review date | 2026-11-06 |
| Positioning | A cultural-life community combining book/film/music data, ratings and reviews, personal records and social features |

### Screens and rating flow

Five bottom tabs: Home, Books/Films/Music, Groups, Market, Me.

Screens relevant to viewing records:

| Screen | Purpose |
| --- | --- |
| Home | Browse recommendations, enter Books/Films/Music or search |
| Film list / charts | Find films, browse others' ratings and recommendations |
| Title detail | See metadata, aggregate rating and reviews; enter the mark-and-rate flow |
| Rating editor | Pick stars, write a review, submit |
| Personal record | See marked titles and records, including a viewing calendar |

Other features: Douban also covers books and music (mark, rate, review); community features include groups, feeds and diaries; the Market offers shopping.

**Path from home to a completed rating:** Home → Books/Films/Music → choose a film → Watched → pick stars → submit — about **5 taps**.

### Recording and rating

| Check | Finding |
| --- | --- |
| Rating scale | Personal 1–5 stars; aggregate shown out of 10. Whole stars only, no half stars |
| Sub-ratings | No separate story / acting / cinematography scores |
| Short reviews | Supported; film pages carry short and long reviews |
| Watch date | Cannot set the actual viewing date; only the marking date is recorded |
| Per-episode rating | Not supported |
| Editing a rating | Supported from the title or the personal record |
| Rating history | Previous values are not viewable |
| Rewatches | Multiple viewing dates and separate ratings per watch are supported |

### Weaknesses

**1. Coarse rating steps.** A user who finds a film between two whole stars has no way to express it; only adjacent whole-star choices exist.

**2. Limited rating dimensions and record keeping.** A user wanting separate story/acting/cinematography scores, per-episode tracking, or a backdated watch date is not served: there are no sub-ratings, no per-episode ratings, no custom viewing date, and no rating history.

**3. Splash ads and a crowded UI slow down quick tasks.** Opening the app shows a splash ad, and the home screen mixes many content types; users who mainly log viewing must browse past unrelated entries. App-store reviews also report excessive advertising.

### Target user

Best for users who want Chinese-language word-of-mouth when choosing films, and who also record books and music and take part in reviews and discussion.

## 2. Letterboxd

### Product information

| Item | Content |
| --- | --- |
| App name | Letterboxd |
| Developer | Letterboxd Ltd |
| App version | 4.13.6 |
| iOS version | 26.6.1 |
| Review date | 2026-11-06 |
| Positioning | A film-first platform combining viewing diary, ratings and reviews, lists and a film community |

### Screens and rating flow

Five bottom tabs: Popular, Search, the central "+" (add a viewing record), Activity, Profile.

Screens relevant to viewing records:

| Screen | Purpose |
| --- | --- |
| Popular / Home | Trending films, reviews, lists and friends' activity |
| Search and film lists | Find films, users and lists; browse and filter |
| Film detail | Metadata, aggregate rating, reviews, own watched/watchlist state |
| Rating and log editor | Stars, review, watch date, rewatch flag and tags |
| Profile | Watched films, diary, reviews, watchlist and personal lists |
| Statistics | Yearly and lifetime stats; full personalised stats are paid |

Other features: themed and personal lists, a watchlist, following other users, activity feeds, likes, tags, and editorial content.

**Path from home to a completed rating:** Home → "+" → search/choose a film → stars/review → save — about **4 taps**.

### Recording and rating

| Check | Finding |
| --- | --- |
| Rating scale | 5 stars with half steps (0.5–5); aggregate also on a 5-star scale |
| Sub-ratings | No separate story / acting / cinematography scores |
| Short reviews | Reviews can be written, edited later, and marked as spoilers |
| Watch date | A viewing record can carry a specific date; merely marking "watched" does not create a dated diary entry |
| Per-episode rating | No complete per-episode tracking or rating system for TV |
| Editing a rating | Supported, including editing ratings inside existing diary entries |
| Rating history | Different records keep different ratings, but edits to one record do not preserve its previous value |
| Rewatches | Multiple records per film with separate dates, ratings and reviews |

Two cases must be distinguished: adding a rewatch keeps earlier records intact, whereas editing an existing record overwrites it.

References:

- [Letterboxd help centre](https://letterboxd.com/faq/)
- [Marking watched vs. logging a film](https://letterboxd.zendesk.com/hc/en-us/articles/15178778774543-What-s-the-difference-between-marking-a-film-watched-and-logging-it)
- [Effect of rating a film on other entries](https://letterboxd.zendesk.com/hc/en-us/articles/15178802067855-When-I-rate-a-film-do-all-my-ratings-for-it-change)

### Weaknesses

**1. Limited TV support and no per-episode ratings.** Letterboxd is film-first; TV coverage is partial, with some miniseries, TV movies and special episodes having entries but no general per-episode tracking. Users must fall back to other apps, scattering their records. (See [Letterboxd's TV support note](https://letterboxd.zendesk.com/hc/en-us/articles/15269096507407-Do-you-support-TV-shows).)

**2. Following directors and bulk list-building are awkward.** There is no direct "follow director" entry, and no one-tap way to add a director's whole filmography to a list; users add films one by one. App-store reviewers also ask for sorting by genre, director or actor, and for collaborative lists.

**3. Reviews have no draft saving.** Published reviews can be edited, but a work-in-progress review cannot be saved as a draft while watching.

**4. Asking a question is tied to a viewing record.** There is no question entry separate from reviews; posting a review would mark an unwatched film as watched, distorting the record. App-store reviewers ask to ask questions without changing watch status.

### Target user

Best for film-centric users who want dated viewing logs, rewatch history, curated lists and interaction with other film fans.

## 3. IMDb

### Product information

| Item | Content |
| --- | --- |
| App name | IMDb Movies & TV |
| Developer | IMDb.com, Inc. |
| App version | 17.1 |
| iOS version | 26.6.1 |
| Review date | 2026-11-06 |
| Positioning | A broad platform centred on film/TV and cast data, with ratings and reviews, watchlist, watch history and industry news |

### Screens and rating flow

Three bottom tabs: Home, Search, You.

Screens relevant to viewing records:

| Screen | Purpose |
| --- | --- |
| Home | Recommendations, trailers, charts and news |
| Search and lists | Find films, shows and people |
| Title detail | Metadata, aggregate rating, cast and user reviews |
| Season and episode pages | Browse seasons/episodes; open episode details and rate them |
| Rating screen | Pick a personal score and submit, optionally marking spoilers |
| You | Personal ratings, watchlist, lists and watch history |
| Watch history | Titles the system treats as watched |

Other features: extensive cast and crew data, filmographies, trailers, interviews, news and awards.

Reference: [IMDb iOS app FAQ](https://help.imdb.com/article/imdb/mobile-web-apps/ios-app-faq/GJ7P484D8FA9C48Q)

**Path from home to a completed rating:** Home → choose a title → Rate → choose a score → submit — about **4 taps**. Rating one episode of a series requires going through the episode list first.

Reference: [How to submit a rating on IMDb](https://origin-help.imdb.com/article/imdb/track-movies-tv/how-do-i-submit-my-rating-on-imdb/G9R8NF943K39DQDT)

### Recording and rating

| Check | Finding |
| --- | --- |
| Rating scale | Personal 1–10 whole numbers; aggregate out of 10, weighted |
| Sub-ratings | No separate story / acting / cinematography scores |
| Short reviews | Supported, but with a minimum length (at least 300 characters) |
| Watch date | No custom actual viewing date |
| Per-episode rating | Supported; episode ratings and series ratings are calculated separately |
| Editing a rating | Supported; a new score replaces the old one |
| Rating history | Previous versions are not viewable |
| Rewatches | No multiple-viewing diary and no separate rating per watch |
| Marking watched | Can mark watched on its own; rating, reviewing or checking in also counts as watched |

References:

- [IMDb ratings FAQ](https://help.imdb.com/article/imdb/featured-content/ratings-faq/G67Y87TFYYP6TWAV)
- [Change or delete a rating](https://origin-help.imdb.com/article/imdb/track-movies-tv/how-do-i-change-or-delete-my-rating-on-a-title/GD9UAYG9EUUERMUH)
- [User review guidelines](https://help.imdb.com/article/contribution/contribution-information/user-review-guidelines/GABTWSNLDNFLPRRH)
- [Mark as watched and watch history](https://help.imdb.com/article/imdb/new-features-updates/mark-as-watched-faq/GR2SD7Y4LZVNHUVH)
- [Lists FAQ and export](https://help.imdb.com/article/imdb/track-movies-tv/lists-faq/GNQMN47VZSE7KW38)

### Weaknesses

**1. The minimum review length hurts quick notes.** A user who wants one sentence — a favourite performance, a reaction to the ending — must pad it out or record it elsewhere; the official rule requires at least 300 characters.

**2. Navigation is not intuitive.** Reviewers report that the mobile layout and navigation make content hard to find, that updates degraded a previously clear structure, and that ads further interfere.

**3. Coarse rating precision.** Personal ratings are whole numbers from 1 to 10, with no decimal option and no precision setting; users used to half or decimal scores cannot express them, which discourages migration from notes apps.

**4. The review editor lacks content protection.** The keyboard covers the input, the page cannot scroll, and leaving the screen or rotating can reload it, losing everything typed; there is no draft saving or exit confirmation.

### Target user

Best for users who want rich film and cast reference data, public word-of-mouth, and the ability to rate both films and individual TV episodes.
