package hk.hkucs.group_6_android_app_dev.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import hk.hkucs.group_6_android_app_dev.BuildConfig;
import hk.hkucs.group_6_android_app_dev.data.db.entity.MediaItem;
import hk.hkucs.group_6_android_app_dev.data.model.MediaStatus;
import hk.hkucs.group_6_android_app_dev.data.model.MediaType;

/**
 * Minimal TMDB v3 client built on HttpURLConnection + org.json (no third-party
 * libraries). Only used while the user is looking a title up; everything it returns
 * is stored on the device, so the app stays local-first. The API key comes from
 * BuildConfig (local.properties) and is never written to the repository.
 */
public final class TmdbApi {

    private static final String API_BASE = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/";
    /** Thumbnail size for search results; w500 is stored for the library poster. */
    public static final String THUMB_SIZE = "w342";
    private static final String POSTER_SIZE = "w500";

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** One search result. */
    public static final class Hit {
        public final String tmdbId;
        public final String mediaType; // MediaType.MOVIE or MediaType.SERIES
        public final String title;
        @Nullable public final Integer year;
        @Nullable public final String posterUrl;

        public Hit(String tmdbId, String mediaType, String title,
                @Nullable Integer year, @Nullable String posterUrl) {
            this.tmdbId = tmdbId;
            this.mediaType = mediaType;
            this.title = title;
            this.year = year;
            this.posterUrl = posterUrl;
        }
    }

    /**
     * TMDB genre id -> the app's fixed vocabulary (see the README's Sample data section
     * mapping table). Unmapped ids (Family, History, Music, Western, Kids, ...) are
     * dropped by returning null.
     */
    private static final Map<Integer, String> GENRE_MAP = new HashMap<>();
    static {
        GENRE_MAP.put(28, "Action");   // Action
        GENRE_MAP.put(12, "Action");   // Adventure
        GENRE_MAP.put(10759, "Action"); // Action & Adventure (TV)
        GENRE_MAP.put(878, "Sci-Fi");  // Science Fiction
        GENRE_MAP.put(10765, "Sci-Fi"); // Sci-Fi & Fantasy (TV)
        GENRE_MAP.put(53, "Mystery");  // Thriller
        GENRE_MAP.put(96, "Mystery");  // Mystery (movie)
        GENRE_MAP.put(9648, "Mystery"); // Mystery (TV)
        GENRE_MAP.put(10752, "War");   // War
        GENRE_MAP.put(10768, "War");   // War & Politics (TV)
        GENRE_MAP.put(16, "Animation");
        GENRE_MAP.put(35, "Comedy");
        GENRE_MAP.put(80, "Crime");
        GENRE_MAP.put(99, "Documentary");
        GENRE_MAP.put(14, "Fantasy");
        GENRE_MAP.put(18, "Drama");
        GENRE_MAP.put(10749, "Romance");
        GENRE_MAP.put(27, "Horror");
    }

    private TmdbApi() {
    }

    public static boolean isEnabled() {
        return !BuildConfig.TMDB_API_KEY.isEmpty();
    }

    /** Searches movies and TV shows. {@code onError} fires on network/parse failure. */
    public static void search(@NonNull String query, @NonNull Consumer<List<Hit>> onResults,
            @NonNull Runnable onError) {
        IO.execute(() -> {
            try {
                String url = API_BASE + "/search/multi?api_key=" + BuildConfig.TMDB_API_KEY
                        + "&language=en-US&include_adult=false&page=1&query="
                        + URLEncoder.encode(query, "UTF-8");
                JSONObject json = new JSONObject(get(url));
                List<Hit> hits = new ArrayList<>();
                JSONArray results = json.optJSONArray("results");
                for (int i = 0; results != null && i < results.length(); i++) {
                    JSONObject o = results.getJSONObject(i);
                    String type = o.optString("media_type");
                    if (!"movie".equals(type) && !"tv".equals(type)) {
                        continue; // skip person results and anything unsupported
                    }
                    String title = o.has("title") ? o.optString("title")
                            : o.optString("name");
                    String date = "movie".equals(type) ? o.optString("release_date")
                            : o.optString("first_air_date");
                    Integer year = parseYear(date);
                    String poster = o.optString("poster_path", "");
                    hits.add(new Hit(String.valueOf(o.getInt("id")),
                            "movie".equals(type) ? MediaType.MOVIE : MediaType.SERIES,
                            title, year,
                            poster.isEmpty() ? null : IMAGE_BASE + THUMB_SIZE + poster));
                }
                MAIN.post(() -> onResults.accept(hits));
            } catch (Exception e) {
                MAIN.post(onError);
            }
        });
    }

    /**
     * Fetches full details for one hit (fields mapped exactly like the sample
     * pull) and downloads the w500 poster into the app's private files dir. The
     * returned item is ready to save; {@code id} is {@code "tmdb_<tmdbId>"} so a
     * title can never be stored twice.
     */
    public static void buildItem(@NonNull Context context, @NonNull Hit hit,
            @NonNull Consumer<MediaItem> onDone, @NonNull Runnable onError) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            try {
                boolean movie = MediaType.MOVIE.equals(hit.mediaType);
                String url = API_BASE + (movie ? "/movie/" : "/tv/") + hit.tmdbId
                        + "?api_key=" + BuildConfig.TMDB_API_KEY
                        + "&language=en-US&append_to_response="
                        + (movie ? "credits" : "credits,external_ids");
                JSONObject json = new JSONObject(get(url));

                MediaItem item = new MediaItem();
                item.id = "tmdb_" + hit.tmdbId;
                item.tmdbId = hit.tmdbId;
                item.imdbId = movie ? json.optString("imdb_id", "")
                        : json.optJSONObject("external_ids") == null ? ""
                                : json.optJSONObject("external_ids").optString("imdb_id", "");
                if (item.imdbId.isEmpty()) {
                    item.imdbId = null;
                }
                item.title = movie ? json.optString("title") : json.optString("name");
                item.originalTitle = emptyToNull(
                        movie ? json.optString("original_title") : json.optString("original_name"));
                item.year = parseYear(movie ? json.optString("release_date")
                        : json.optString("first_air_date"));
                item.type = movie ? MediaType.MOVIE : MediaType.SERIES;
                item.genres = mapGenres(json.optJSONArray("genres"));
                item.overview = emptyToNull(json.optString("overview"));
                JSONObject credits = json.optJSONObject("credits");
                item.directors = movie ? directorsFromCrew(credits)
                        : creatorsFrom(json.optJSONArray("created_by"));
                item.cast = castFrom(credits);
                item.runtimeMinutes = movie ? json.optInt("runtime", 0)
                        : firstEpisodeRuntime(json.optJSONArray("episode_run_time"));
                item.totalEpisodes = movie ? null
                        : json.optInt("number_of_episodes", 0);
                if (item.runtimeMinutes != null && item.runtimeMinutes <= 0) {
                    item.runtimeMinutes = null;
                }
                if (item.totalEpisodes != null && item.totalEpisodes <= 0) {
                    item.totalEpisodes = null;
                }
                item.status = MediaStatus.WANT;
                item.source = "tmdb";
                item.poster = downloadPoster(app, hit.tmdbId, json.optString("poster_path"));
                MAIN.post(() -> onDone.accept(item));
            } catch (Exception e) {
                MAIN.post(onError);
            }
        });
    }

    // ---- internals ----

    private static String get(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        try (InputStream in = conn.getInputStream()) {
            return new String(PosterLoader.readAll(in), "UTF-8");
        } finally {
            conn.disconnect();
        }
    }

    /** Stores the poster as a JPEG under files/posters/; null when there is none. */
    @Nullable
    private static String downloadPoster(Context app, String tmdbId, String posterPath) {
        if (posterPath == null || posterPath.isEmpty()) {
            return null;
        }
        try {
            byte[] data = fetchBytes(IMAGE_BASE + POSTER_SIZE + posterPath);
            File dir = new File(app.getFilesDir(), "posters");
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return null;
            }
            File out = new File(dir, "tmdb_" + tmdbId + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(data);
            }
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] fetchBytes(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(20_000);
        try (InputStream in = conn.getInputStream()) {
            return PosterLoader.readAll(in);
        } finally {
            conn.disconnect();
        }
    }

    private static List<String> mapGenres(@Nullable JSONArray genres) {
        Set<String> mapped = new LinkedHashSet<>();
        for (int i = 0; genres != null && i < genres.length(); i++) {
            String name = GENRE_MAP.get(genres.optJSONObject(i).optInt("id"));
            if (name != null) {
                mapped.add(name);
            }
        }
        List<String> result = new ArrayList<>();
        for (String name : mapped) {
            if (result.size() == 3) {
                break; // keep at most three genres, in TMDB order
            }
            result.add(name);
        }
        return result;
    }

    private static List<String> directorsFromCrew(@Nullable JSONObject credits) {
        List<String> names = new ArrayList<>();
        JSONArray crew = credits == null ? null : credits.optJSONArray("crew");
        for (int i = 0; crew != null && i < crew.length() && names.size() < 2; i++) {
            JSONObject person = crew.optJSONObject(i);
            if ("Director".equals(person.optString("job"))) {
                names.add(person.optString("name"));
            }
        }
        return names;
    }

    private static List<String> creatorsFrom(@Nullable JSONArray createdBy) {
        List<String> names = new ArrayList<>();
        for (int i = 0; createdBy != null && i < createdBy.length() && names.size() < 2; i++) {
            names.add(createdBy.optJSONObject(i).optString("name"));
        }
        return names;
    }

    private static List<String> castFrom(@Nullable JSONObject credits) {
        List<String> names = new ArrayList<>();
        JSONArray cast = credits == null ? null : credits.optJSONArray("cast");
        for (int i = 0; cast != null && i < cast.length() && names.size() < 4; i++) {
            names.add(cast.optJSONObject(i).optString("name"));
        }
        return names;
    }

    @Nullable
    private static Integer firstEpisodeRuntime(@Nullable JSONArray runtimes) {
        return runtimes != null && runtimes.length() > 0 ? runtimes.optInt(0) : null;
    }

    @Nullable
    private static Integer parseYear(String date) {
        if (date == null || date.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
